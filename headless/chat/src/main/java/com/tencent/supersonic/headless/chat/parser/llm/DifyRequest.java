package com.tencent.supersonic.headless.chat.parser.llm;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.apache.htrace.fasterxml.jackson.annotation.JsonProperty;

@Data
public class DifyRequest {
    private String query;
    private Map<String, String> inputs = new HashMap<>();
    @JsonProperty("conversation_id")
    private String responseMode = "blocking";
    private String user;
    @JsonProperty("conversation_id")
    private String conversationId;
    @JsonProperty("auto_generate_name")
    private Boolean autoGenerateName = false;
}
