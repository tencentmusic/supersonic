package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq.ElementValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SqlPromptGenerator {

    public String generatorLinkingAndSqlPrompt(LLMReq llmReq, List<Map<String, String>> exampleList) {
        String instruction =
                "# Find the schema_links for generating SQL queries for each question based on the database schema "
                        + "and Foreign keys. Then use the the schema links to generate the "
                        + "SQL queries for each of the questions.";

        List<String> exampleKeys = Arrays.asList("questionAugmented", "dbSchema", "generatedSchemaLinkingCoT", "sql");
        String exampleTemplate = "dbSchema\nQ: questionAugmented\nA: generatedSchemaLinkingCoT\nSQL: sql";

        String exampleFormat = InputFormat.format(exampleTemplate, exampleKeys, exampleList);

        Pair<String, String> questionPrompt = transformQuestionPrompt(llmReq);
        String dbSchema = questionPrompt.getLeft();
        String questionAugmented = questionPrompt.getRight();

        String newCaseTemplate = "%s\nQ: %s\nA: Let’s think step by step. In the question \"%s\", we are asked:";
        String newCasePrompt = String.format(newCaseTemplate, dbSchema, questionAugmented, questionAugmented);

        return instruction + InputFormat.SEPERATOR + exampleFormat + InputFormat.SEPERATOR + newCasePrompt;
    }

    public String generateLinkingPrompt(LLMReq llmReq, List<Map<String, String>> exampleList) {
        String instruction = "# Find the schema_links for generating SQL queries for each question "
                + "based on the database schema and Foreign keys.";

        List<String> exampleKeys = Arrays.asList("questionAugmented", "dbSchema", "generatedSchemaLinkingCoT");
        String exampleTemplate = "dbSchema\nQ: questionAugmented\nA: generatedSchemaLinkingCoT";
        String exampleFormat = InputFormat.format(exampleTemplate, exampleKeys, exampleList);

        Pair<String, String> questionPrompt = transformQuestionPrompt(llmReq);
        String dbSchema = questionPrompt.getLeft();
        String questionAugmented = questionPrompt.getRight();
        String newCaseTemplate = "%s\nQ: %s\nA: Let’s think step by step. In the question \"%s\", we are asked:";
        String newCasePrompt = String.format(newCaseTemplate, dbSchema, questionAugmented, questionAugmented);

        return instruction + InputFormat.SEPERATOR + exampleFormat + InputFormat.SEPERATOR + newCasePrompt;
    }

    public String generateSqlPrompt(LLMReq llmReq, String schemaLinkStr, List<Map<String, String>> fewshotExampleList) {
        String instruction = "# Use the the schema links to generate the SQL queries for each of the questions.";
        List<String> exampleKeys = Arrays.asList("questionAugmented", "dbSchema", "generatedSchemaLinkings", "sql");
        String exampleTemplate = "dbSchema\nQ: questionAugmented\n" + "Schema_links: generatedSchemaLinkings\n"
                + "SQL: sql";

        String schemaLinkingPrompt = InputFormat.format(exampleTemplate, exampleKeys, fewshotExampleList);
        Pair<String, String> questionPrompt = transformQuestionPrompt(llmReq);
        String dbSchema = questionPrompt.getLeft();
        String questionAugmented = questionPrompt.getRight();
        String newCaseTemplate = "%s\nQ: %s\nSchema_links: %s\nSQL: ";
        String newCasePrompt = String.format(newCaseTemplate, dbSchema, questionAugmented, schemaLinkStr);
        return instruction + InputFormat.SEPERATOR + schemaLinkingPrompt + InputFormat.SEPERATOR + newCasePrompt;
    }

    public List<String> generatePromptPool(LLMReq llmReq, List<List<Map<String, String>>> exampleListPool,
                                           boolean isSqlPrompt) {
        List<String> promptPool = new ArrayList<>();
        for (List<Map<String, String>> exampleList : exampleListPool) {
            String prompt;
            if (isSqlPrompt) {
                prompt = generatorLinkingAndSqlPrompt(llmReq, exampleList);
            } else {
                prompt = generateLinkingPrompt(llmReq, exampleList);
            }
            promptPool.add(prompt);
        }
        return promptPool;
    }

    public List<List<Map<String, String>>> getExampleCombos(List<Map<String, String>> exampleList, int numFewShots,
                                                            int numSelfConsistency) {
        List<List<Map<String, String>>> results = new ArrayList<>();
        for (int i = 0; i < numSelfConsistency; i++) {
            List<Map<String, String>> shuffledList = new ArrayList<>(exampleList);
            Collections.shuffle(shuffledList);
            results.add(shuffledList.subList(0, numFewShots));
        }
        return results;
    }

    public Pair<String, String> transformQuestionPrompt(LLMReq llmReq) {
        String modelName = llmReq.getSchema().getDataSetName();
        List<String> fieldNameList = llmReq.getSchema().getFieldNameList();
        List<LLMReq.ElementValue> linking = llmReq.getLinking();
        String currentDate = llmReq.getCurrentDate();
        String priorExts = llmReq.getPriorExts();

        String dbSchema = "Table: " + modelName + ", Columns = " + fieldNameList + "\nForeign_keys: []";

        List<String> priorLinkingList = new ArrayList<>();
        for (ElementValue priorLinking : linking) {
            String fieldName = priorLinking.getFieldName();
            String fieldValue = priorLinking.getFieldValue();
            priorLinkingList.add("‘" + fieldValue + "‘是一个‘" + fieldName + "‘");
        }
        String currentDataStr = "当前的日期是" + currentDate;
        String linkingListStr = String.join("，", priorLinkingList);
        String questionAugmented = String.format("%s (补充信息:%s 。 %s) (备注: %s)", llmReq.getQueryText(), linkingListStr,
                currentDataStr, priorExts);
        return Pair.of(dbSchema, questionAugmented);
    }

    public Map<String, String> transformQuestionMultiTurnPrompt(LLMReq llmReq, Long dataSetId) {
        List<String> contextualQuestionAugmented = new ArrayList<>();
        List<String> fullFieldNameList = llmReq.getSchema().getFieldNameList();
        List<ElementValue> fullLinking = llmReq.getLinking();
        if (!CollectionUtils.isEmpty(llmReq.getContextualParseInfoList())) {
            llmReq.getContextualParseInfoList().forEach(o -> {
                Map<String, Object> context = JsonUtil.toMap(
                        JsonUtil.toString(o.getProperties().get(Constants.CONTEXT)), String.class, Object.class);
                LLMReq contextualLlmReq = JsonUtil.toObject(JsonUtil.toString(context.get("llmReq")), LLMReq.class);
                fullFieldNameList.addAll(contextualLlmReq.getSchema().getFieldNameList());
                Pair<String, String> contextualQuestion = transformQuestionPrompt(contextualLlmReq);
                contextualQuestionAugmented.add("\"" + contextualQuestion.getRight() + "\"");
                fullLinking.addAll(contextualLlmReq.getLinking());
            });
        }
        List<String> fieldNameList = fullFieldNameList.stream().collect(Collectors.toSet())
                .stream().collect(Collectors.toList());
        llmReq.getSchema().setFieldNameList(fieldNameList);


        String modelName = llmReq.getSchema().getDataSetName();
        //List<String> fieldNameList = llmReq.getSchema().getFieldNameList();
        List<LLMReq.ElementValue> linking = llmReq.getLinking();
        String currentDate = llmReq.getCurrentDate();
        String priorExts = llmReq.getPriorExts();

        String dbSchema = "Table: " + modelName + ", Columns = " + fieldNameList + "\nForeign_keys: []";

        List<String> priorLinkingList = new ArrayList<>();
        for (ElementValue priorLinking : linking) {
            String fieldName = priorLinking.getFieldName();
            String fieldValue = priorLinking.getFieldValue();
            priorLinkingList.add("‘" + fieldValue + "‘是一个‘" + fieldName + "‘");
        }
        String currentDataStr = "当前的日期是" + currentDate;
        String linkingListStr = String.join("，", priorLinkingList);
        String contextualQuestionAugmentedStr = String.join(",", contextualQuestionAugmented);
        String questionAugmented = String.format("%s (补充信息:%s 。 %s) (备注: %s)", llmReq.getQueryText(), linkingListStr,
                currentDataStr, priorExts);
        String fullQuestionAugmented = String.format("contextual questions：[%s]。current question：%s",
                contextualQuestionAugmentedStr, questionAugmented);

        llmReq.setLinking(fullLinking.stream().collect(Collectors.toSet()).stream().collect(Collectors.toList()));
        Map<String, String> result = new HashMap<>();
        result.put("dbSchema", dbSchema);
        result.put("fullQuestionAugmented", fullQuestionAugmented);
        result.put("questionAugmented", questionAugmented);
        return result;
    }

    public List<String> generateSqlPromptPool(LLMReq llmReq, List<String> schemaLinkStrPool,
                                              List<List<Map<String, String>>> fewshotExampleListPool) {
        List<String> sqlPromptPool = new ArrayList<>();
        for (int i = 0; i < schemaLinkStrPool.size(); i++) {
            String schemaLinkStr = schemaLinkStrPool.get(i);
            List<Map<String, String>> fewshotExampleList = fewshotExampleListPool.get(i);
            String sqlPrompt = generateSqlPrompt(llmReq, schemaLinkStr, fewshotExampleList);
            sqlPromptPool.add(sqlPrompt);
        }
        return sqlPromptPool;
    }

    public String generateMultiTurnLinkingAndSqlPrompt(LLMReq llmReq,
                                                       List<Map<String, String>> exampleList,
                                                       Long dataSetId) {

        String instruction =
                "#this is a multi-turn text-to-sql scenes,you need consider the contextual questions,find the "
                        + "schema_links for generating SQL queries for current question based on the "
                        + "contextual questions, database schema and Foreign keys. Then use the schema links "
                        + "to generate the SQL queries for current questions.";

        List<String> exampleKeys = Arrays.asList("questionAugmented", "dbSchema", "generatedSchemaLinkingCoT", "sql");
        String exampleTemplate = "dbSchema\nQ: questionAugmented\nA: generatedSchemaLinkingCoT\nSQL: sql";

        String exampleFormat = InputFormat.format(exampleTemplate, exampleKeys, exampleList);

        Map<String, String> questionPrompt = transformQuestionMultiTurnPrompt(llmReq, dataSetId);
        String dbSchema = questionPrompt.get("dbSchema");
        String questionAugmented = questionPrompt.get("questionAugmented");
        String fullQuestionAugmented = questionPrompt.get("fullQuestionAugmented");

        String newCaseTemplate = "%s\nQ: %s\nA: Let’s think step by step.In the question \"%s\", "
                + "please consider contextual question，based on the contextual "
                + "questions, database schema and Foreign keys. generate the SQL queries for current questions.";
        String newCasePrompt = String.format(newCaseTemplate, dbSchema, fullQuestionAugmented, questionAugmented);

        return instruction + InputFormat.SEPERATOR + exampleFormat + InputFormat.SEPERATOR + newCasePrompt;
    }

}
