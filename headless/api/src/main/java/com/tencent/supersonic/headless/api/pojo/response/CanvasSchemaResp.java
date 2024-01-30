package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class CanvasSchemaResp {

    private Long domainId;

    private ModelResp model;

    private List<MetricResp> metrics;

    private List<DimensionResp> dimensions;


}
