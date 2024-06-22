package com.tencent.supersonic.headless.chat.query.llm.s2sql;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
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

    private SqlGenType sqlGenType;

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

        private List<SchemaElement> metrics;

        private List<SchemaElement> dimensions;

        private List<Term> terms;

    }

    @Data
    public static class FilterCondition {

        private String tableName;
    }

    @Data
    public static class Term {

        private String name;

        private String description;

        private List<String> alias = Lists.newArrayList();

    }

    public enum SqlGenType {
        ONE_PASS_SELF_CONSISTENCY("1_pass_self_consistency"),
        TWO_PASS_AUTO_COT_SELF_CONSISTENCY("2_pass_auto_cot_self_consistency");

        private String name;

        SqlGenType(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }

    }
}
