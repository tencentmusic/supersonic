package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String sessionId;
}
