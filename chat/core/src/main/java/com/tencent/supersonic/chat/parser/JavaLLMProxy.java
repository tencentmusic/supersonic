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
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLMProxy based on langchain4j Java version.
 */
@Slf4j
@Component
public class JavaLLMProxy implements LLMProxy {

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
                SqlGenerationMode.valueOf(llmReq.getSqlGenerationMode()));
        String modelName = llmReq.getSchema().getModelName();
        Map<String, Double> sqlWeight = sqlGeneration.generation(llmReq, modelClusterKey);

        LLMResp result = new LLMResp();
        result.setQuery(llmReq.getQueryText());
        result.setModelName(modelName);
        result.setSqlWeight(sqlWeight);
        return result;
    }

    @Override
    public FunctionResp requestFunction(FunctionReq functionReq) {

        FunctionPromptGenerator promptGenerator = ContextUtils.getBean(FunctionPromptGenerator.class);

        String functionCallPrompt = promptGenerator.generateFunctionCallPrompt(functionReq.getQueryText(),
                functionReq.getPluginConfigs());

        ChatLanguageModel chatLanguageModel = ContextUtils.getBean(ChatLanguageModel.class);

        String functionSelect = chatLanguageModel.generate(functionCallPrompt);

        return OutputFormat.functionCallParse(functionSelect);
    }

}
