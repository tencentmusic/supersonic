package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class DimensionValueReq {
    private Long modelId;

    private String bizName;

    private Object value;
}
