package com.tencent.supersonic.headless.common.server.response;

import lombok.Data;

import java.util.List;

@Data
public class ModelSchemaRelaResp {

    private Long domainId;

    private ModelResp model;

    private List<MetricResp> metrics;

    private List<DimensionResp> dimensions;


}
