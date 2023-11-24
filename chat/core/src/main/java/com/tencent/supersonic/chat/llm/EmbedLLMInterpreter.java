package com.tencent.supersonic.chat.llm;

import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.llm.prompt.FunctionCallPromptGenerator;
import com.tencent.supersonic.chat.llm.prompt.OutputFormat;
import com.tencent.supersonic.chat.llm.prompt.SqlExampleLoader;
import com.tencent.supersonic.chat.llm.prompt.SqlPromptGenerator;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionReq;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.common.util.ContextUtils;
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

@Slf4j
public class EmbedLLMInterpreter implements LLMInterpreter {

    public LLMResp query2sql(LLMReq llmReq, Long modelId) {

        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);

        SqlExampleLoader sqlExampleLoader = ContextUtils.getBean(SqlExampleLoader.class);

        OptimizationConfig config = ContextUtils.getBean(OptimizationConfig.class);

        List<Map<String, String>> sqlExamples = sqlExampleLoader.retrieverSqlExamples(llmReq.getQueryText(),
                config.getText2sqlCollectionName(), config.getText2sqlFewShotsNum());

        String queryText = llmReq.getQueryText();
        String modelName = llmReq.getSchema().getModelName();
        List<String> fieldNameList = llmReq.getSchema().getFieldNameList();
        List<ElementValue> linking = llmReq.getLinking();

        SqlPromptGenerator sqlPromptGenerator = ContextUtils.getBean(SqlPromptGenerator.class);
        String linkingPromptStr = sqlPromptGenerator.generateSchemaLinkingPrompt(queryText, modelName, fieldNameList,
                linking, sqlExamples);

        Prompt linkingPrompt = PromptTemplate.from(JsonUtil.toString(linkingPromptStr)).apply(new HashMap<>());
        Response<AiMessage> linkingResult = chatLanguageModel.generate(linkingPrompt.toSystemMessage());

        String schemaLinkStr = OutputFormat.schemaLinkParse(linkingResult.content().text());

        String generateSqlPrompt = sqlPromptGenerator.generateSqlPrompt(queryText, modelName, schemaLinkStr,
                llmReq.getCurrentDate(), sqlExamples);

        Prompt sqlPrompt = PromptTemplate.from(JsonUtil.toString(generateSqlPrompt)).apply(new HashMap<>());
        Response<AiMessage> sqlResult = chatLanguageModel.generate(sqlPrompt.toSystemMessage());

        LLMResp result = new LLMResp();
        result.setQuery(queryText);
        result.setSchemaLinkingOutput(linkingPromptStr);
        result.setSchemaLinkStr(schemaLinkStr);
        result.setModelName(modelName);
        result.setSqlOutput(sqlResult.content().text());
        return result;
    }

    @Override
    public FunctionResp requestFunction(FunctionReq functionReq) {

        FunctionCallPromptGenerator promptGenerator = ContextUtils.getBean(FunctionCallPromptGenerator.class);

        String functionCallPrompt = promptGenerator.generateFunctionCallPrompt(functionReq.getQueryText(),
                functionReq.getPluginConfigs());

        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);

        String functionSelect = chatLanguageModel.generate(functionCallPrompt);

        return OutputFormat.functionCallParse(functionSelect);
    }

}
