package com.tencent.supersonic.headless.chat.parser.llm;

import lombok.Data;

@Data
public class DifyResult {
    private String event = "";
    private String taskId = "";
    private String id = "";
    private String messageId = "";
    private String answer = "";
}
