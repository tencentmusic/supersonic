package com.tencent.supersonic.chat.query.llm.s2ql;

import java.util.List;
import lombok.Data;

@Data
public class LLMReq {

    private String queryText;

    private LLMSchema schema;

    private List<ElementValue> linking;

    private String currentDate;

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
}
