package com.tencent.supersonic.headless.common.materialization.request;


import lombok.Data;

@Data
public class MaterializationSourceReq {

    private Long materializationId = 0L;
    private Long dataSourceId = 0L;
    private String dataTime;
}
