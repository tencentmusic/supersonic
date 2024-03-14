package com.tencent.supersonic.headless.core.config;


import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import lombok.Data;

import java.util.List;

@Data
public class EntityInternalDetail {

    List<DimSchemaResp> dimensionList;
    List<MetricSchemaResp> metricList;
}