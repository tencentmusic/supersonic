package com.tencent.supersonic.semantic.api.core.response;

import java.util.List;
import lombok.Data;

@Data
public class DomainSchemaRelaResp {

    private Long domainId;

    private DatasourceResp datasource;

    private List<MetricResp> metrics;

    private List<DimensionResp> dimensions;


}
