package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwoStepSqlGeneration implements SqlGeneration, InitializingBean {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private SqlExampleLoader sqlExampleLoader;

    @Autowired
    private OptimizationConfig optimizationConfig;

    @Autowired
    private SqlPromptGenerator sqlPromptGenerator;

    @Override
    public String generation(LLMReq llmReq, String modelClusterKey) {
        String text2sqlCollectionName = optimizationConfig.getText2sqlCollectionName();
        int text2sqlFewShotsNum = optimizationConfig.getText2sqlFewShotsNum();
        String queryText = llmReq.getQueryText();

        List<Map<String, String>> sqlExamples = sqlExampleLoader.retrieverSqlExamples(queryText, text2sqlCollectionName,
                text2sqlFewShotsNum);

        String modelName = llmReq.getSchema().getModelName();
        List<String> fieldNameList = llmReq.getSchema().getFieldNameList();
        List<ElementValue> linking = llmReq.getLinking();

        String linkingPromptStr = sqlPromptGenerator.generateSchemaLinkingPrompt(queryText, modelName, fieldNameList,
                linking, sqlExamples);

        Prompt linkingPrompt = PromptTemplate.from(JsonUtil.toString(linkingPromptStr)).apply(new HashMap<>());
        Response<AiMessage> linkingResult = chatLanguageModel.generate(linkingPrompt.toSystemMessage());

        String schemaLinkStr = OutputFormat.schemaLinkParse(linkingResult.content().text());

        String generateSqlPrompt = sqlPromptGenerator.generateSqlPrompt(queryText, modelName, schemaLinkStr,
                llmReq.getCurrentDate(), sqlExamples);

        Prompt sqlPrompt = PromptTemplate.from(JsonUtil.toString(generateSqlPrompt)).apply(new HashMap<>());
        Response<AiMessage> sqlResult = chatLanguageModel.generate(sqlPrompt.toSystemMessage());
        return sqlResult.content().text();
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenerationFactory.addSqlGenerationForFactory(SqlGenerationMode.TWO_STEP_AUTO_COT, this);
    }
}
