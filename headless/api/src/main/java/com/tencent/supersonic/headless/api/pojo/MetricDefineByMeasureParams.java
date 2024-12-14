package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class MetricDefineByMeasureParams extends MetricDefineParams {

    private List<Measure> measures = Lists.newArrayList();
}
