package com.tencent.supersonic.feishu.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeishuMessage {

    private String openId;
    private String chatId;
    private String messageId;
    private String chatType;
    private String content;
    private List<Mention> mentions;
    private Integer agentId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mention {
        private String id;
        private String openId;
        private String name;
    }
}
