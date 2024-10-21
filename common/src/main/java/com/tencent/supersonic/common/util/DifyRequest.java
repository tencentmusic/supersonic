package com.tencent.supersonic.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DifyRequest {
    private String query;
    private Map<String, String> inputs = new HashMap<>();
    private String responseMode = "blocking";
    private String user;
    @JsonProperty("conversation_id")
    private String conversationId;
    @JsonProperty("auto_generate_name")
    private Boolean autoGenerateName = false;
}
