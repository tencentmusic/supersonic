package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;


import java.util.List;
import java.util.Map;

public class PromptEnhancer {
    public static String enhanceDDLInfo(String modelName, Map<String, DataTypeEnums> dataTypeEnumsMap,
                                         List<String> fieldNameList, String linkingSqlPrompt) {
        StringBuilder ddlInfo = new StringBuilder();
        ddlInfo.append("The user provides a question and you provide Logical SQL. You will only respond with Logical "
                + "SQL code and not with any explanations.\n"
                + "Respond with only Logical SQL code.Do not answer with any explanations--just the Logical SQL code.\n"
                + "You may use the following Logical DDL statements as a reference for what tables might be available."
                + " Use responses to past questions also to guide you:\n \n");

        // 将dataTypeEnumsMap 转换 为DDL语句
        ddlInfo.append("CREATE TABLE " + modelName + " (\n");
        for (String key : fieldNameList) {
            if (null != dataTypeEnumsMap.get(key)) {
                ddlInfo.append(key + " " + dataTypeEnumsMap.get(key).name() + ",\n");
            } else {
                ddlInfo.append(key + " " + "STRING" + ",\n");
            }
        }
        ddlInfo.append(");\n");
        ddlInfo.append("\n");
        ddlInfo.append("You may use the following documentation as a reference for what tables might be available. "
                + "Use responses to past questions also to guide you.\n");
        ddlInfo.append(linkingSqlPrompt);
        return ddlInfo.toString();
    }

    public static String enhanceDDLInfo(LLMReq llmReq, String linkingSqlPrompt) {
        String modelName = llmReq.getSchema().getDataSetName();
        Map<String, DataTypeEnums> dataTypeEnumsMap = llmReq.getSchema().getFieldNameDataTypeMap();
        return enhanceDDLInfo(modelName, dataTypeEnumsMap, llmReq.getSchema().getFieldNameList(), linkingSqlPrompt);
    }

    public static String getDDLInfo(LLMReq llmReq) {
        String modelName = llmReq.getSchema().getDataSetName();
        Map<String, DataTypeEnums> dataTypeEnumsMap = llmReq.getSchema().getFieldNameDataTypeMap();
        StringBuilder ddlInfo = new StringBuilder("You may use the following Logical DDL statements as a reference "
                + "for what tables might be available.\n \n");
        ddlInfo.append("#DDLInfo: \n");
        ddlInfo.append("CREATE TABLE " + modelName + " (\n");
        for (String key : dataTypeEnumsMap.keySet()) {
            if (null != dataTypeEnumsMap.get(key)) {
                ddlInfo.append(key + " " + dataTypeEnumsMap.get(key).name() + ",\n");
            } else {
                ddlInfo.append(key + " " + "VARCHAR" + ",\n");
            }
        }
        ddlInfo.append(");\n");
        return ddlInfo.toString();
    }
}
