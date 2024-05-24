package com.tencent.supersonic.headless.core.chat.query.llm.s2sql;

import com.fasterxml.jackson.annotation.JsonValue;
import com.tencent.supersonic.headless.api.pojo.LLMConfig;
import lombok.Data;

import java.util.List;

@Data
public class LLMReq {

    private String queryText;

    private FilterCondition filterCondition;

    private LLMSchema schema;

    private List<ElementValue> linking;

    private String currentDate;

    private String priorExts;

    private String sqlGenerationMode;

    private LLMConfig llmConfig;

    @Data
    public static class ElementValue {

        private String fieldName;

        private String fieldValue;

    }

    @Data
    public static class LLMSchema {

        private String domainName;

        private String dataSetName;

        private Long dataSetId;

        private List<String> fieldNameList;

    }

    @Data
    public static class FilterCondition {

        private String tableName;
    }

    public enum SqlGenType {

        ONE_PASS_AUTO_COT("1_pass_auto_cot"),

        ONE_PASS_AUTO_COT_SELF_CONSISTENCY("1_pass_auto_cot_self_consistency"),

        TWO_PASS_AUTO_COT("2_pass_auto_cot"),

        TWO_PASS_AUTO_COT_SELF_CONSISTENCY("2_pass_auto_cot_self_consistency");


        private String name;

        SqlGenType(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

        public static SqlGenType getMode(String name) {
            for (SqlGenType sqlGenType : SqlGenType.values()) {
                if (sqlGenType.name.equals(name)) {
                    return sqlGenType;
                }
            }
            return null;
        }

    }
}
