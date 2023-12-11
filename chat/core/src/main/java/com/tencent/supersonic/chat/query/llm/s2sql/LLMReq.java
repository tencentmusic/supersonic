package com.tencent.supersonic.chat.query.llm.s2sql;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import lombok.Data;

@Data
public class LLMReq {

    private String queryText;

    private FilterCondition filterCondition;

    private LLMSchema schema;

    private List<ElementValue> linking;

    private String currentDate;

    private String priorExts;

    private SqlGenerationMode sqlGenerationMode = SqlGenerationMode.TWO_STEP_AUTO_COT_SELF_CONSISTENCY;

    @Data
    public static class ElementValue {

        private String fieldName;

        private String fieldValue;

    }

    @Data
    public static class LLMSchema {

        private String domainName;

        private String modelName;

        private List<String> fieldNameList;

    }

    @Data
    public static class FilterCondition {

        private String tableName;
    }

    public enum SqlGenerationMode {

        ONE_STEP_AUTO_COT("1_pass_auto_cot"),

        ONE_STEP_AUTO_COT_SELF_CONSISTENCY("1_pass_auto_cot_self_consistency"),

        TWO_STEP_AUTO_COT("2_pass_auto_cot"),

        TWO_STEP_AUTO_COT_SELF_CONSISTENCY("2_pass_auto_cot_self_consistency");


        private String name;

        SqlGenerationMode(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

    }
}
