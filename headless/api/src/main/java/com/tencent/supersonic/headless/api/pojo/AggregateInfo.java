package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AggregateInfo {
    private List<MetricInfo> metricInfos = new ArrayList<>();
}
