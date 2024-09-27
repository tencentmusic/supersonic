package com.tencent.supersonic.headless.chat.parser.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.PromptConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OnePassSCSqlGenStrategy extends SqlGenStrategy {

    public static final String INSTRUCTION =
            ""
                    + "\n#Role: You are a data analyst experienced in SQL languages."
                    + "\n#Task: You will be provided with a natural language question asked by users,"
                    + "please convert it to a SQL query so that relevant data could be returned "
                    + "by executing the SQL query against underlying database."
                    + "\n#Rules:"
                    + "\n1.ALWAYS generate columns and values specified in the `Schema`, DO NOT hallucinate."
                    + "\n2.ALWAYS specify date filter using `>`,`<`,`>=`,`<=` operator."
                    + "\n3.DO NOT include date filter in the where clause if not explicitly expressed in the `Question`."
                    + "\n4.DO NOT calculate date range using functions."
                    + "\n5.DO NOT calculate date range using DATE_SUB."
                    + "\n6.DO NOT miss the AGGREGATE operator of metrics, always add it as needed."
                    + "\n7.ALWAYS USE `with` statement to handle secondary calculation scenario."
                    + "\n#Exemplars:\n{{exemplar}}"
                    + "#Question:\nQuestion:{{question}},Schema:{{schema}},SideInfo:{{information}}";

    @Data
    static class SemanticSql {
        @Description("thought or remarks to tell users about the sql, make it short.")
        private String thought;

        @Description("sql to generate")
        private String sql;
    }

    interface SemanticSqlExtractor {
        SemanticSql generateSemanticSql(String text);
    }

    @Override
    public LLMResp generate(LLMReq llmReq) {
        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        // 1.recall exemplars
        keyPipelineLog.info("OnePassSCSqlGenStrategy llmReq:\n{}", llmReq);
        List<List<Text2SQLExemplar>> exemplarsList = promptHelper.getFewShotExemplars(llmReq);

        // 2.generate sql generation prompt for each self-consistency inference
        Map<Prompt, List<Text2SQLExemplar>> prompt2Exemplar = new HashMap<>();
        for (List<Text2SQLExemplar> exemplars : exemplarsList) {
            llmReq.setDynamicExemplars(exemplars);
            Prompt prompt = generatePrompt(llmReq, llmResp);
            prompt2Exemplar.put(prompt, exemplars);
        }
        ChatLanguageModel chatLanguageModel = getChatLanguageModel(llmReq.getModelConfig());
        SemanticSqlExtractor extractor =
                AiServices.create(SemanticSqlExtractor.class, chatLanguageModel);

        // 3.perform multiple self-consistency inferences parallelly
        Map<String, Prompt> output2Prompt = new ConcurrentHashMap<>();
        prompt2Exemplar
                .keySet()
                .parallelStream()
                .forEach(
                        prompt -> {
                            keyPipelineLog.info(
                                    "OnePassSCSqlGenStrategy reqPrompt:\n{}",
                                    prompt.toUserMessage());
                            SemanticSql s2Sql =
                                    extractor.generateSemanticSql(
                                            prompt.toUserMessage().singleText());
                            output2Prompt.put(s2Sql.getSql(), prompt);
                            keyPipelineLog.info(
                                    "OnePassSCSqlGenStrategy modelResp:\n{}", s2Sql.getSql());
                        });

        // 4.format response.
        Pair<String, Map<String, Double>> sqlMapPair =
                ResponseHelper.selfConsistencyVote(Lists.newArrayList(output2Prompt.keySet()));
        llmResp.setSqlOutput(sqlMapPair.getLeft());
        List<Text2SQLExemplar> usedExemplars =
                prompt2Exemplar.get(output2Prompt.get(sqlMapPair.getLeft()));
        llmResp.setSqlRespMap(ResponseHelper.buildSqlRespMap(usedExemplars, sqlMapPair.getRight()));

        return llmResp;
    }

    private Prompt generatePrompt(LLMReq llmReq, LLMResp llmResp) {
        StringBuilder exemplars = new StringBuilder();
        for (Text2SQLExemplar exemplar : llmReq.getDynamicExemplars()) {
            String exemplarStr =
                    String.format(
                            "Question:%s,Schema:%s,SideInfo:%s,SQL:%s\n",
                            exemplar.getQuestion(),
                            exemplar.getDbSchema(),
                            exemplar.getSideInfo(),
                            exemplar.getSql());
            exemplars.append(exemplarStr);
        }
        String dataSemantics = promptHelper.buildSchemaStr(llmReq);
        String sideInformation = promptHelper.buildSideInformation(llmReq);
        llmResp.setSchema(dataSemantics);
        llmResp.setSideInfo(sideInformation);

        Map<String, Object> variable = new HashMap<>();
        variable.put("exemplar", exemplars);
        variable.put("question", llmReq.getQueryText());
        variable.put("schema", dataSemantics);
        variable.put("information", sideInformation);

        // use custom prompt template if provided.
        PromptConfig promptConfig = llmReq.getPromptConfig();
        String promptTemplate = INSTRUCTION;
        if (promptConfig != null && StringUtils.isNotBlank(promptConfig.getPromptTemplate())) {
            promptTemplate = promptConfig.getPromptTemplate();
        }
        return PromptTemplate.from(promptTemplate).apply(variable);
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenStrategyFactory.addSqlGenerationForFactory(
                LLMReq.SqlGenType.ONE_PASS_SELF_CONSISTENCY, this);
    }
}
