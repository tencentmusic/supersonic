package com.tencent.supersonic.common.util;

import lombok.Data;

@Data
public class DifyResult {
    private String event = "";
    private String taskId = "";
    private String conversationId = "";
    private String id = "";
    private String messageId = "";
    private String answer = "";
}
