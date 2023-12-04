package com.tencent.supersonic.chat.parser.sql.llm.prompt;

import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SqlPromptGenerator {

    public String generateSchemaLinkingPrompt(String question, String modelName, List<String> fieldsList,
            List<ElementValue> priorSchemaLinks, List<Map<String, String>> exampleList) {

        String exampleTemplate = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n"
                + "问题:{question}\n分析:{analysis} 所以Schema_links是:\nSchema_links:{schemaLinks}";

        List<String> exampleKeys = Arrays.asList("tableName", "fieldsList", "priorSchemaLinks", "question", "analysis",
                "schemaLinks");

        String schemaLinkingPrompt = InputFormat.format(exampleTemplate, exampleKeys, exampleList);

        String newCaseTemplate = "Table {tableName}, columns = {fieldsList}, prior_schema_links = {priorSchemaLinks}\n"
                + "问题:{question}\n分析: 让我们一步一步地思考。";

        String newCasePrompt = newCaseTemplate.replace("{tableName}", modelName)
                .replace("{fieldsList}", fieldsList.toString())
                .replace("{priorSchemaLinks}", getPriorSchemaLinks(priorSchemaLinks))
                .replace("{question}", question);

        String instruction = "# 根据数据库的表结构,参考先验信息,找出为每个问题生成SQL查询语句的schema_links";
        return instruction + InputFormat.SEPERATOR + schemaLinkingPrompt + InputFormat.SEPERATOR + newCasePrompt;
    }

    private String getPriorSchemaLinks(List<ElementValue> priorSchemaLinks) {
        return priorSchemaLinks.stream()
                .map(elementValue -> "'" + elementValue.getFieldName() + "'->" + elementValue.getFieldValue())
                .collect(Collectors.joining(",", "[", "]"));
    }

    public String generateSqlPrompt(String question, String modelName, String schemaLinkStr, String dataDate,
            List<Map<String, String>> exampleList) {

        List<String> exampleKeys = Arrays.asList("question", "currentDate", "tableName", "schemaLinks", "sql");
        String exampleTemplate = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\n"
                + "Schema_links:{schemaLinks}\nSQL:{sql}";

        String sqlExamplePrompt = InputFormat.format(exampleTemplate, exampleKeys, exampleList);

        String newCaseTemplate = "问题:{question}\nCurrent_date:{currentDate}\nTable {tableName}\n"
                + "Schema_links:{schemaLinks}\nSQL:";

        String newCasePrompt = newCaseTemplate.replace("{question}", question)
                .replace("{currentDate}", dataDate)
                .replace("{tableName}", modelName)
                .replace("{schemaLinks}", schemaLinkStr);

        String instruction = "# 根据schema_links为每个问题生成SQL查询语句";
        return instruction + InputFormat.SEPERATOR + sqlExamplePrompt + InputFormat.SEPERATOR + newCasePrompt;
    }

}