package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class DeepSeekParams {
    private List<Message> messages;
    private String model;
    private boolean stream = true;
    private Integer maxTokens = 4096;
}
