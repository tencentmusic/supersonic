package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class DeepSeekResponse {
    private String serviceName;
    private String requestId;
    private String sessionId;
    private String reasonContent;
    private String content;
    private String messageId;
    private String createTimestamp;
    private String endFlag;
}
