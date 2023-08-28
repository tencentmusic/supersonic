package com.tencent.supersonic.semantic.api.model.response;

import java.util.List;
import lombok.Data;

@Data
public class ModelSchemaRelaResp {

    private Long domainId;

    private DatasourceResp datasource;

    private List<MetricResp> metrics;

    private List<DimensionResp> dimensions;


}
