package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Deprecated
public class TwoPassSCSqlGenStrategy extends SqlGenStrategy {

    @Override
    public LLMResp generate(LLMReq llmReq) {
        //1.recall exemplars
        keyPipelineLog.info("TwoPassSCSqlGenStrategy llmReq:{}", llmReq);

        List<List<Map<String, String>>> exampleListPool = promptHelper.getFewShotExemplars(llmReq);

        //2.generate schema linking prompt for each self-consistency inference
        List<String> linkingPromptPool = new ArrayList<>();
        for (List<Map<String, String>> exampleList : exampleListPool) {
            String prompt = generateLinkingPrompt(llmReq, exampleList);
            linkingPromptPool.add(prompt);
        }

        List<String> linkingResults = new CopyOnWriteArrayList<>();
        ChatLanguageModel chatLanguageModel = getChatLanguageModel(llmReq.getLlmConfig());
        linkingPromptPool.parallelStream().forEach(
                linkingPrompt -> {
                    Prompt prompt = PromptTemplate.from(JsonUtil.toString(linkingPrompt)).apply(new HashMap<>());
                    keyPipelineLog.info("TwoPassSCSqlGenStrategy step one reqPrompt:{}", prompt.toSystemMessage());
                    Response<AiMessage> linkingResult = chatLanguageModel.generate(prompt.toSystemMessage());
                    String result = linkingResult.content().text();
                    keyPipelineLog.info("TwoPassSCSqlGenStrategy step one modelResp:{}", result);
                    linkingResults.add(OutputFormat.getSchemaLink(result));
                }
        );
        List<String> sortedList = OutputFormat.formatList(linkingResults);

        //3.generate sql generation prompt for each self-consistency inference
        List<String> sqlPromptPool = new ArrayList<>();
        for (int i = 0; i < sortedList.size(); i++) {
            String schemaLinkStr = sortedList.get(i);
            List<Map<String, String>> fewshotExampleList = exampleListPool.get(i);
            String sqlPrompt = generateSqlPrompt(llmReq, schemaLinkStr, fewshotExampleList);
            sqlPromptPool.add(sqlPrompt);
        }

        //4.perform multiple self-consistency inferences parallelly
        List<String> sqlTaskPool = new CopyOnWriteArrayList<>();
        sqlPromptPool.parallelStream().forEach(sqlPrompt -> {
            Prompt linkingPrompt = PromptTemplate.from(JsonUtil.toString(sqlPrompt)).apply(new HashMap<>());
            keyPipelineLog.info("TwoPassSCSqlGenStrategy step two reqPrompt:{}", linkingPrompt.toSystemMessage());
            Response<AiMessage> sqlResult = chatLanguageModel.generate(linkingPrompt.toSystemMessage());
            String result = sqlResult.content().text();
            keyPipelineLog.info("TwoPassSCSqlGenStrategy step two modelResp:{}", result);
            sqlTaskPool.add(result);
        });

        //5.format response.
        Pair<String, Map<String, Double>> sqlMapPair = OutputFormat.selfConsistencyVote(sqlTaskPool);
        LLMResp llmResp = new LLMResp();
        llmResp.setQuery(llmReq.getQueryText());
        //TODO: should use the same few-shot exemplars as the one chose by self-consistency vote
        llmResp.setSqlRespMap(OutputFormat.buildSqlRespMap(exampleListPool.get(0), sqlMapPair.getRight()));
        return llmResp;
    }

    private String generateLinkingPrompt(LLMReq llmReq, List<Map<String, String>> exampleList) {
        String instruction = "# Find the schema_links for generating SQL queries for each question "
                + "based on the database schema and Foreign keys.";

        List<String> exampleKeys = Arrays.asList("questionAugmented", "dbSchema", "generatedSchemaLinkingCoT");
        String exampleTemplate = "dbSchema\nQ: questionAugmented\nA: generatedSchemaLinkingCoT";
        String exampleFormat = InputFormat.format(exampleTemplate, exampleKeys, exampleList);

        Pair<String, String> questionPrompt = promptHelper.transformQuestionPrompt(llmReq);
        String dbSchema = questionPrompt.getLeft();
        String questionAugmented = questionPrompt.getRight();
        String newCaseTemplate = "%s\nQ: %s\nA: Let’s think step by step. In the question \"%s\", we are asked:";
        String newCasePrompt = String.format(newCaseTemplate, dbSchema, questionAugmented, questionAugmented);

        return instruction + InputFormat.SEPERATOR + exampleFormat + InputFormat.SEPERATOR + newCasePrompt;
    }

    private String generateSqlPrompt(LLMReq llmReq, String schemaLinkStr,
                                     List<Map<String, String>> fewshotExampleList) {
        String instruction = "# Use the the schema links to generate the SQL queries for each of the questions.";
        List<String> exampleKeys = Arrays.asList("questionAugmented", "dbSchema", "generatedSchemaLinkings", "sql");
        String exampleTemplate = "dbSchema\nQ: questionAugmented\n" + "Schema_links: generatedSchemaLinkings\n"
                + "SQL: sql";

        String schemaLinkingPrompt = InputFormat.format(exampleTemplate, exampleKeys, fewshotExampleList);
        Pair<String, String> questionPrompt = promptHelper.transformQuestionPrompt(llmReq);
        String dbSchema = questionPrompt.getLeft();
        String questionAugmented = questionPrompt.getRight();
        String newCaseTemplate = "%s\nQ: %s\nSchema_links: %s\nSQL: ";
        String newCasePrompt = String.format(newCaseTemplate, dbSchema, questionAugmented, schemaLinkStr);
        return instruction + InputFormat.SEPERATOR + schemaLinkingPrompt + InputFormat.SEPERATOR + newCasePrompt;
    }

    @Override
    public void afterPropertiesSet() {
        SqlGenStrategyFactory.addSqlGenerationForFactory(LLMReq.SqlGenType.TWO_PASS_AUTO_COT_SELF_CONSISTENCY, this);
    }
}
