package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UnAvailableItemResp {

    private List<MetricResp> metricResps;

    private List<DimensionResp> dimensionResps;
}
