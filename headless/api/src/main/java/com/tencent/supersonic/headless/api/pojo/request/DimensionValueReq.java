package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class DimensionValueReq {

    private Integer agentId;

    @NotNull
    private Long elementID;

    private Long modelId;

    private String bizName;

    @NotNull
    private String value;

    private Set<Long> dataSetIds;

}
