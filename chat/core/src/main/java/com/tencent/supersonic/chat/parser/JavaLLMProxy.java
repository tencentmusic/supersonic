package com.tencent.supersonic.chat.parser;

import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionPromptGenerator;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionReq;
import com.tencent.supersonic.chat.parser.plugin.function.FunctionResp;
import com.tencent.supersonic.chat.parser.sql.llm.OutputFormat;
import com.tencent.supersonic.chat.parser.sql.llm.SqlGeneration;
import com.tencent.supersonic.chat.parser.sql.llm.SqlGenerationFactory;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * LLMProxy based on langchain4j Java version.
 */
@Slf4j
@Component
public class JavaLLMProxy implements LLMProxy {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    @Override
    public boolean isSkip(QueryContext queryContext) {
        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);
        if (Objects.isNull(chatLanguageModel)) {
            log.warn("chatLanguageModel is null, skip :{}", JavaLLMProxy.class.getName());
            return true;
        }
        return false;
    }

    public LLMResp query2sql(LLMReq llmReq, String modelClusterKey) {

        SqlGeneration sqlGeneration = SqlGenerationFactory.get(
                SqlGenerationMode.getMode(llmReq.getSqlGenerationMode()));
        String modelName = llmReq.getSchema().getModelName();
        LLMResp result = sqlGeneration.generation(llmReq, modelClusterKey);
        result.setQuery(llmReq.getQueryText());
        result.setModelName(modelName);
        return result;
    }

    @Override
    public FunctionResp requestFunction(FunctionReq functionReq) {

        FunctionPromptGenerator promptGenerator = ContextUtils.getBean(FunctionPromptGenerator.class);

        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);
        String functionCallPrompt = promptGenerator.generateFunctionCallPrompt(functionReq.getQueryText(),
                functionReq.getPluginConfigs());
        keyPipelineLog.info("functionCallPrompt:{}", functionCallPrompt);
        String response = chatLanguageModel.generate(functionCallPrompt);
        keyPipelineLog.info("functionCall response:{}", response);
        return OutputFormat.functionCallParse(response);
    }

}
