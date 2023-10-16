package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class DimensionValueReq {
    private Integer agentId;

    private Long elementID;

    private Long modelId;

    private String bizName;

    private Object value;
}
