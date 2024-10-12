package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.pojo.ChatApp;
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

@Slf4j
public class LLMSqlCorrector extends BaseSemanticCorrector {

    private static final Logger keyPipelineLog = LoggerFactory.getLogger("keyPipeline");

    public static final String APP_KEY = "S2SQL_CORRECTOR";
    private static final String INSTRUCTION = ""
            + "\n#Role: You are a senior data engineer experienced in writing SQL."
            + "\n#Task: Your will be provided with a user question and the SQL written by a junior engineer,"
            + "please take a review and help correct it if necessary." + "\n#Rules: "
            + "\n1.ALWAYS follow the output format: `opinion=(POSITIVE|NEGATIVE),sql=(corrected sql if NEGATIVE; empty string if POSITIVE)`."
            + "\n2.NO NEED to check date filters as the junior engineer seldom makes mistakes in this regard."
            + "\n3.DO NOT miss the AGGREGATE operator of metrics, always add it as needed."
            + "\n4.ALWAYS use `with` statement if nested aggregation is needed."
            + "\n5.ALWAYS enclose alias created by `AS` command in underscores."
            + "\n6.ALWAYS translate alias created by `AS` command to the same language as the `#Question`."
            + "\n#Question:{{question}} #InputSQL:{{sql}} #Response:";

    public LLMSqlCorrector() {
        ChatAppManager.register(ChatApp.builder().key(APP_KEY).prompt(INSTRUCTION).name("语义SQL修正")
                .description("").enable(false).build());
    }

    @Data
    @ToString
    static class SemanticSql {
        @Description("positive or negative opinion about the sql")
        private String opinion;

        @Description("corrected sql")
        private String sql;
    }

    interface SemanticSqlExtractor {
        SemanticSql generateSemanticSql(String text);
    }

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        ChatApp chatApp = chatQueryContext.getChatAppConfig().get(APP_KEY);
        if (!chatQueryContext.getText2SQLType().enableLLM() || !chatApp.isEnable()) {
            return;
        }

        ChatLanguageModel chatLanguageModel =
                ModelProvider.getChatModel(chatApp.getChatModelConfig());
        SemanticSqlExtractor extractor =
                AiServices.create(SemanticSqlExtractor.class, chatLanguageModel);
        Prompt prompt = generatePrompt(chatQueryContext.getQueryText(), semanticParseInfo,
                chatApp.getPrompt());
        keyPipelineLog.info("LLMSqlCorrector reqPrompt:\n{}", prompt.text());
        SemanticSql s2Sql = extractor.generateSemanticSql(prompt.toUserMessage().singleText());
        keyPipelineLog.info("LLMSqlCorrector modelResp:\n{}", s2Sql);
        if ("NEGATIVE".equals(s2Sql.getOpinion()) && StringUtils.isNotBlank(s2Sql.getSql())) {
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(s2Sql.getSql());
        }
    }

    private Prompt generatePrompt(String queryText, SemanticParseInfo semanticParseInfo,
            String promptTemplate) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("question", queryText);
        variable.put("sql", semanticParseInfo.getSqlInfo().getCorrectedS2SQL());

        return PromptTemplate.from(promptTemplate).apply(variable);
    }
}
