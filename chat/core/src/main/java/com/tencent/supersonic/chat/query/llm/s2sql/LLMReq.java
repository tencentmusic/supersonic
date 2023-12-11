package com.tencent.supersonic.chat.query.llm.s2sql;

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

    private SqlGenerationMode sqlGenerationMode = SqlGenerationMode.TWO_STEP;

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

        ONE_STEP("ONE_STEP"),

        TWO_STEP("TWO_STEP"),

        TWO_STEP_CS("TWO_STEP_CS");


        private String name;

        SqlGenerationMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }
}
