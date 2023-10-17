package com.tencent.supersonic.semantic.api.materialization.request;


import lombok.Data;

@Data
public class MaterializationSourceReq {

    private Long materializationId = 0L;
    private Long dataSourceId = 0L;
    private String dataTime;
}
