package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq.SqlGenType;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMResp;
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
public class TwoPassSqlGenStrategy extends SqlGenStrategy {

    @Override
    public LLMResp generate(LLMReq llmReq) {
        keyPipelineLog.info("llmReq:{}", llmReq);
        List<Map<String, String>> sqlExamples = exemplarManager.recallExemplars(llmReq.getQueryText(),
                optimizationConfig.getText2sqlExampleNum());

        String linkingPromptStr = promptGenerator.generateLinkingPrompt(llmReq, sqlExamples);

        Prompt prompt = PromptTemplate.from(JsonUtil.toString(linkingPromptStr)).apply(new HashMap<>());
        keyPipelineLog.info("step one request prompt:{}", prompt.toSystemMessage());
        ChatLanguageModel chatLanguageModel = getChatLanguageModel(llmReq.getLlmConfig());
        Response<AiMessage> response = chatLanguageModel.generate(prompt.toSystemMessage());
        keyPipelineLog.info("step one model response:{}", response.content().text());
        String schemaLinkStr = OutputFormat.getSchemaLink(response.content().text());
        String generateSqlPrompt = promptGenerator.generateSqlPrompt(llmReq, schemaLinkStr, sqlExamples);

        Prompt sqlPrompt = PromptTemplate.from(JsonUtil.toString(generateSqlPrompt)).apply(new HashMap<>());
        keyPipelineLog.info("step two request prompt:{}", sqlPrompt.toSystemMessage());
        Response<AiMessage> sqlResult = chatLanguageModel.generate(sqlPrompt.toSystemMessage());
        String result = sqlResult.content().text();
        keyPipelineLog.info("step two model response:{}", result);
        Map<String, Double> sqlMap = new HashMap<>();
        sqlMap.put(result, 1D);
        keyPipelineLog.info("schemaLinkStr:{},sqlMap:{}", schemaLinkStr, sqlMap);

        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        llmResp.setSqlRespMap(OutputFormat.buildSqlRespMap(sqlExamples, sqlMap));
        return llmResp;
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenStrategyFactory.addSqlGenerationForFactory(SqlGenType.TWO_PASS_AUTO_COT, this);
    }
}
