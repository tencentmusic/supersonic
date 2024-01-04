package com.tencent.supersonic.chat.core.parser.sql.llm;


import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMReq.SqlGenerationMode;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnePassSCSqlGeneration implements SqlGeneration, InitializingBean {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");
    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private SqlExampleLoader sqlExampleLoader;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Autowired
    private SqlPromptGenerator sqlPromptGenerator;

    @Override
    public LLMResp generation(LLMReq llmReq, String modelClusterKey) {
        //1.retriever sqlExamples and generate exampleListPool
        keyPipelineLog.info("modelClusterKey:{},llmReq:{}", modelClusterKey, llmReq);

        List<Map<String, String>> sqlExamples = sqlExampleLoader.retrieverSqlExamples(llmReq.getQueryText(),
                optimizationConfig.getText2sqlCollectionName(), optimizationConfig.getText2sqlExampleNum());

        List<List<Map<String, String>>> exampleListPool = sqlPromptGenerator.getExampleCombos(sqlExamples,
                optimizationConfig.getText2sqlFewShotsNum(), optimizationConfig.getText2sqlSelfConsistencyNum());

        //2.generator linking and sql prompt by sqlExamples,and parallel generate response.
        List<String> linkingSqlPromptPool = sqlPromptGenerator.generatePromptPool(llmReq, exampleListPool, true);
        List<String> llmResults = new CopyOnWriteArrayList<>();
        linkingSqlPromptPool.parallelStream().forEach(linkingSqlPrompt -> {
                    Prompt prompt = PromptTemplate.from(JsonUtil.toString(linkingSqlPrompt))
                            .apply(new HashMap<>());
                    keyPipelineLog.info("request prompt:{}", prompt.toSystemMessage());
                    Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());
                    String result = response.content().text();
                    llmResults.add(result);
                    keyPipelineLog.info("model response:{}", result);
                }
        );
        //3.format response.
        List<String> schemaLinkingResults = llmResults.stream()
                .map(llmResult -> OutputFormat.getSchemaLinks(llmResult)).collect(Collectors.toList());
        List<String> candidateSortedList = OutputFormat.formatList(schemaLinkingResults);
        Pair<String, Map<String, Double>> linkingMap = OutputFormat.selfConsistencyVote(candidateSortedList);
        List<String> sqlList = llmResults.stream()
                .map(llmResult -> OutputFormat.getSql(llmResult)).collect(Collectors.toList());

        Pair<String, Map<String, Double>> sqlMapPair = OutputFormat.selfConsistencyVote(sqlList);
        keyPipelineLog.info("linkingMap:{} sqlMap:{}", linkingMap, sqlMapPair.getRight());

        LLMResp result = new LLMResp();
        result.setQuery(llmReq.getQueryText());
        result.setSqlRespMap(OutputFormat.buildSqlRespMap(sqlExamples, sqlMapPair.getRight()));
        return result;
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenerationFactory.addSqlGenerationForFactory(SqlGenerationMode.ONE_PASS_AUTO_COT_SELF_CONSISTENCY, this);
    }
}
