package com.tencent.supersonic.chat.query.plugin.dsl;

import java.util.List;
import lombok.Data;

@Data
public class LLMResp {

    private String query;

    private String domainName;

    private String sqlOutput;

    private List<String> fields;

    private String schemaLinkingOutput;

    private String schemaLinkStr;
}
