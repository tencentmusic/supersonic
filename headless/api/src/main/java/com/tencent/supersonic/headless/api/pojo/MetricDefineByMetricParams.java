package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class MetricDefineByMetricParams extends MetricDefineParams {

    private List<MetricParam> metrics = Lists.newArrayList();

}
