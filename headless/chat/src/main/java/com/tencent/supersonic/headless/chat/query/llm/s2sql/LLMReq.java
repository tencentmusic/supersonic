package com.tencent.supersonic.headless.chat.query.llm.s2sql;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class LLMReq {
    private Long queryId;
    private String queryText;
    private LLMSchema schema;
    private List<Term> terms;
    private String currentDate;
    private String priorExts;
    private SqlGenType sqlGenType;
    private Map<String, ChatApp> chatAppConfig;
    private String customPrompt;
    private List<Text2SQLExemplar> dynamicExemplars;

    @Data
    public static class ElementValue {
        private String fieldName;
        private String fieldValue;
    }

    @Data
    public static class LLMSchema {
        private String databaseType;
        private String databaseVersion;
        private Long dataSetId;
        private String dataSetName;
        private List<SchemaElement> metrics;
        private List<SchemaElement> dimensions;
        private List<ElementValue> values;
        private SchemaElement partitionTime;
        private SchemaElement primaryKey;

        public List<String> getFieldNameList() {
            Set<String> fieldNameList = new HashSet<>();
            if (CollectionUtils.isNotEmpty(metrics)) {
                fieldNameList.addAll(
                        metrics.stream().map(SchemaElement::getName).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(dimensions)) {
                fieldNameList.addAll(dimensions.stream().map(SchemaElement::getName)
                        .collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(values)) {
                fieldNameList.addAll(values.stream().map(ElementValue::getFieldName)
                        .collect(Collectors.toList()));
            }
            if (Objects.nonNull(partitionTime)) {
                fieldNameList.add(partitionTime.getName());
            }
            return new ArrayList<>(fieldNameList);
        }
    }

    @Data
    public static class Term {
        private String name;
        private String description;
        private List<String> alias = Lists.newArrayList();
    }

    public enum SqlGenType {
        ONE_PASS_SELF_CONSISTENCY("1_pass_self_consistency");

        private final String name;

        SqlGenType(String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}
