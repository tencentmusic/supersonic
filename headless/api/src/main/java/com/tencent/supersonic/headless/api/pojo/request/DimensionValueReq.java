package com.tencent.supersonic.headless.api.pojo.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DimensionValueReq {

    private Integer agentId;

    @NotNull
    private Long elementID;

    private Long modelId;

    private String bizName;

    @NotNull
    private String value;
}
