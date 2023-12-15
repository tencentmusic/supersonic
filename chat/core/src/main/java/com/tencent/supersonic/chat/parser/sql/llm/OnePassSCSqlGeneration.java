package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnePassSCSqlGeneration implements SqlGeneration, InitializingBean {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private SqlExampleLoader sqlExampleLoader;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Autowired
    private SqlPromptGenerator sqlPromptGenerator;

    @Override
    public Map<String, Double> generation(LLMReq llmReq, String modelClusterKey) {
        //1.retriever sqlExamples and generate exampleListPool
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
                    Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());
                    llmResults.add(response.content().text());
                }
        );
        //3.format response.
        List<String> schemaLinkingResults = llmResults.stream()
                .map(llmResult -> OutputFormat.getSchemaLinks(llmResult)).collect(Collectors.toList());
        List<String> candidateSortedList = OutputFormat.formatList(schemaLinkingResults);
        Pair<String, Map<String, Double>> linkingMap = OutputFormat.selfConsistencyVote(candidateSortedList);
        List<String> sqlList = llmResults.stream()
                .map(llmResult -> OutputFormat.getSql(llmResult)).collect(Collectors.toList());
        List<String> sqlListSortedList = OutputFormat.formatList(sqlList);
        Pair<String, Map<String, Double>> sqlMap = OutputFormat.selfConsistencyVote(sqlListSortedList);
        log.info("linkingMap result:{},sqlMap:{}", linkingMap, sqlMap);
        return sqlMap.getRight();
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenerationFactory.addSqlGenerationForFactory(SqlGenerationMode.ONE_PASS_AUTO_COT_SELF_CONSISTENCY, this);
    }
}
