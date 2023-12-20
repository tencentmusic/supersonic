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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwoPassSCSqlGeneration implements SqlGeneration, InitializingBean {

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

        //2.generator linking prompt,and parallel generate response.
        List<String> linkingPromptPool = sqlPromptGenerator.generatePromptPool(llmReq, exampleListPool, false);
        List<String> linkingResults = new CopyOnWriteArrayList<>();
        linkingPromptPool.parallelStream().forEach(
                linkingPrompt -> {
                    Prompt prompt = PromptTemplate.from(JsonUtil.toString(linkingPrompt)).apply(new HashMap<>());
                    Response<AiMessage> linkingResult = chatLanguageModel.generate(prompt.toSystemMessage());
                    String result = linkingResult.content().text();
                    linkingResults.add(OutputFormat.getSchemaLink(result));
                }
        );
        List<String> sortedList = OutputFormat.formatList(linkingResults);
        Pair<String, Map<String, Double>> linkingMap = OutputFormat.selfConsistencyVote(sortedList);
        //3.generator sql prompt,and parallel generate response.
        List<String> sqlPromptPool = sqlPromptGenerator.generateSqlPromptPool(llmReq, sortedList, exampleListPool);
        List<String> sqlTaskPool = new CopyOnWriteArrayList<>();
        sqlPromptPool.parallelStream().forEach(sqlPrompt -> {
            Prompt linkingPrompt = PromptTemplate.from(JsonUtil.toString(sqlPrompt)).apply(new HashMap<>());
            Response<AiMessage> sqlResult = chatLanguageModel.generate(linkingPrompt.toSystemMessage());
            String result = sqlResult.content().text();
            sqlTaskPool.add(result);
        });
        //4.format response.
        Pair<String, Map<String, Double>> sqlMap = OutputFormat.selfConsistencyVote(sqlTaskPool);
        log.info("linkingMap result:{},sqlMap:{}", linkingMap, sqlMap);
        return sqlMap.getRight();
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenerationFactory.addSqlGenerationForFactory(SqlGenerationMode.TWO_PASS_AUTO_COT_SELF_CONSISTENCY, this);
    }
}
