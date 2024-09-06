package com.tencent.supersonic.headless.chat.parser.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.PromptConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
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

    private static final String INSTRUCTION =
            ""
                    + "\n#Role: You are a data analyst experienced in SQL languages."
                    + "#Task: You will be provided with a natural language question asked by users,"
                    + "please convert it to a SQL query so that relevant data could be returned "
                    + "by executing the SQL query against underlying database."
                    + "\n#Rules:"
                    + "1.ALWAYS generate column specified in the `Schema`, DO NOT hallucinate."
                    + "2.ALWAYS specify date filter using `>`,`<`,`>=`,`<=` operator."
                    + "3.ALWAYS calculate the absolute date range by yourself."
                    + "4.DO NOT include date filter in the where clause if not explicitly expressed in the `Question`."
                    + "5.DO NOT miss the AGGREGATE operator of metrics, always add it if needed."
                    + "6.ONLY respond with the converted SQL statement."
                    + "\n#Exemplars:\n{{exemplar}}"
                    + "Question:{{question}},Schema:{{schema}},SideInfo:{{information}},SQL:";

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
                            ChatLanguageModel chatLanguageModel =
                                    getChatLanguageModel(llmReq.getModelConfig());
                            Response<AiMessage> response =
                                    chatLanguageModel.generate(prompt.toUserMessage());
                            String sqlOutput =
                                    StringUtils.normalizeSpace(response.content().text());
                            // replace ```
                            String sqlOutputFormat =
                                    sqlOutput.replaceAll("(?s)```sql\\s*(.*?)\\s*```", "$1").trim();
                            output2Prompt.put(sqlOutputFormat, prompt);
                            keyPipelineLog.info(
                                    "OnePassSCSqlGenStrategy modelResp:\n{}", sqlOutputFormat);
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
