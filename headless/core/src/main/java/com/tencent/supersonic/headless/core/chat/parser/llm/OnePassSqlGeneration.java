package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlResp;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OnePassSqlGeneration extends BaseSqlGeneration {

    @Override
    public LLMResp generation(LLMReq llmReq, Long dataSetId) {
        //1.retriever sqlExamples
        keyPipelineLog.info("dataSetId:{},llmReq:{}", dataSetId, llmReq);
        List<Map<String, String>> sqlExamples = sqlExamplarLoader.retrieverSqlExamples(llmReq.getQueryText(),
                optimizationConfig.getText2sqlExampleNum());

        //2.generator linking and sql prompt by sqlExamples,and generate response.
        String promptStr = sqlPromptGenerator.generatorLinkingAndSqlPrompt(llmReq, sqlExamples);

        Prompt prompt = PromptTemplate.from(JsonUtil.toString(promptStr)).apply(new HashMap<>());
        keyPipelineLog.info("request prompt:{}", prompt.toSystemMessage());
        ChatLanguageModel chatLanguageModel = getChatLanguageModel(llmReq.getLlmConfig());
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
