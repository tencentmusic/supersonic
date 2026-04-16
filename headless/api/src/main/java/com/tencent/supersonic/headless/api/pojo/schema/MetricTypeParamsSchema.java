package com.tencent.supersonic.headless.api.pojo.schema;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class MetricTypeParamsSchema {

    private List<MeasureSchema> measures = Lists.newArrayList();

    private List<MetricParamSchema> metrics = Lists.newArrayList();

    private List<FieldParamSchema> fields = Lists.newArrayList();

    private String expr;
}
