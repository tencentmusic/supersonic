package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class LLMSqlCorrector extends BaseSemanticCorrector {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "S2SQL_CORRECTOR";
    private static final String INSTRUCTION = ""
            + "#Role: You are a senior data engineer experienced in writing SQL."
            + "\n#Task: Your will be provided with a user question and the SQL written by a junior engineer,"
            + "please take a review and help correct it if necessary." + "\n#Rules: "
            + "1.ALWAYS specify time range using `>`,`<`,`>=`,`<=` operator."
            + "2.DO NOT calculate date range using functions."
            + "3.SQL columns and values must be mentioned in the `#Schema`."
            + "\n#Question:{{question}} #Schema:{{schema}} #InputSQL:{{sql}} #Response:";

    public LLMSqlCorrector() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt(INSTRUCTION).name("语义SQL修正")
                .appModule(AppModule.CHAT).description("通过大模型对解析S2SQL做二次修正").enable(false).build());
    }

    @Data
    @ToString
    static class SemanticSql {
        @Description("either positive or negative")
        private String opinion;

        @Description("corrected sql if negative")
        private String sql;
    }

    interface SemanticSqlExtractor {
        SemanticSql generateSemanticSql(String text);
    }

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        ChatApp chatApp = chatQueryContext.getRequest().getChatAppConfig().get(APP_KEY);
        if (!chatQueryContext.getRequest().getText2SQLType().enableLLM() || Objects.isNull(chatApp)
                || !chatApp.isEnable()) {
            return;
        }

        Text2SQLExemplar exemplar = (Text2SQLExemplar) semanticParseInfo.getProperties()
                .get(Text2SQLExemplar.PROPERTY_KEY);

        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor =
                AiServices.create(SemanticSqlExtractor.class, chatLanguageModel);
        Prompt prompt = generatePrompt(chatQueryContext.getRequest().getQueryText(),
                semanticParseInfo, chatApp.getPrompt(), exemplar);
        SemanticSql s2Sql = extractor.generateSemanticSql(prompt.toUserMessage().singleText());
        keyPipelineLog.info("LLMSqlCorrector modelReq:\n{} \nmodelResp:\n{}", prompt.text(), s2Sql);
        if ("NEGATIVE".equalsIgnoreCase(s2Sql.getOpinion())
                && StringUtils.isNotBlank(s2Sql.getSql())) {
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(s2Sql.getSql());
        }
    }

    private Prompt generatePrompt(String queryText, SemanticParseInfo semanticParseInfo,
            String promptTemplate, Text2SQLExemplar exemplar) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("question", queryText);
        variable.put("sql", semanticParseInfo.getSqlInfo().getCorrectedS2SQL());
        variable.put("schema", exemplar.getDbSchema());

        return PromptTemplate.from(promptTemplate).apply(variable);
    }
}
