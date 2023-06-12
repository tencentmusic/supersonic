package com.tencent.supersonic.semantic.api.core.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasourceRelaReq {

    private Long id;

    @NotNull(message = "class id cat not be null")
    private Long domainId;

    @NotNull(message = "datasource id cat not be null")
    private Long datasourceFrom;

    @NotNull(message = "datasource id cat not be null")
    private Long datasourceTo;

    @NotNull(message = "join key cat not be null")
    private String joinKey;

}