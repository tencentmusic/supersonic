package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class DeepSeekRequest {
    private String serviceName;
    private String serviceType = "text_to_text";
    private String requestId;
    private String sessionId;
    private DeepSeekParams params;
}
