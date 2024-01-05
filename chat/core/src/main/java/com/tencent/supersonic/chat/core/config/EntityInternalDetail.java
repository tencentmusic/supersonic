package com.tencent.supersonic.chat.core.config;


import com.tencent.supersonic.headless.api.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.response.MetricSchemaResp;
import java.util.List;
import lombok.Data;

@Data
public class EntityInternalDetail {

    List<DimSchemaResp> dimensionList;
    List<MetricSchemaResp> metricList;
}