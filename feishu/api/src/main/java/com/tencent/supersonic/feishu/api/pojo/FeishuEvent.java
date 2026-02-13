package com.tencent.supersonic.feishu.api.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuEvent {

    private String schema;

    private Header header;

    private Map<String, Object> event;

    // url_verification fields
    private String challenge;
    private String token;
    private String type;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        @JsonProperty("event_id")
        private String eventId;

        @JsonProperty("event_type")
        private String eventType;

        private String token;

        @JsonProperty("create_time")
        private String createTime;

        @JsonProperty("tenant_key")
        private String tenantKey;

        @JsonProperty("app_id")
        private String appId;
    }
}
