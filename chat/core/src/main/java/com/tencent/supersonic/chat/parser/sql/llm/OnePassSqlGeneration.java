package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMSqlResp;
import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnePassSqlGeneration implements SqlGeneration, InitializingBean {

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
        //1.retriever sqlExamples
        keyPipelineLog.info("modelClusterKey:{},llmReq:{}", modelClusterKey, llmReq);
        List<Map<String, String>> sqlExamples = sqlExampleLoader.retrieverSqlExamples(llmReq.getQueryText(),
                optimizationConfig.getText2sqlCollectionName(), optimizationConfig.getText2sqlExampleNum());

        //2.generator linking and sql prompt by sqlExamples,and generate response.
        String promptStr = sqlPromptGenerator.generatorLinkingAndSqlPrompt(llmReq, sqlExamples);

        Prompt prompt = PromptTemplate.from(JsonUtil.toString(promptStr)).apply(new HashMap<>());
        keyPipelineLog.info("request prompt:{}", prompt.toSystemMessage());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());
        String result = response.content().text();
        keyPipelineLog.info("model response:{}", result);
        //3.format response.
        String schemaLinkStr = OutputFormat.getSchemaLinks(response.content().text());
        String sql = OutputFormat.getSql(response.content().text());
        Map<String, LLMSqlResp> sqlRespMap = new HashMap<>();
        sqlRespMap.put(sql, LLMSqlResp.builder().sqlWeight(1D).fewShots(sqlExamples).build());
        keyPipelineLog.info("schemaLinkStr:{},sqlRespMap:{}", schemaLinkStr, sqlRespMap);

        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        llmResp.setSqlRespMap(sqlRespMap);
        return llmResp;
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenerationFactory.addSqlGenerationForFactory(SqlGenerationMode.ONE_PASS_AUTO_COT, this);
    }
}
