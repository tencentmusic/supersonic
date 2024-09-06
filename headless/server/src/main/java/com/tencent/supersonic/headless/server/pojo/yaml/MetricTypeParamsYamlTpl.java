package com.tencent.supersonic.headless.server.pojo.yaml;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class MetricTypeParamsYamlTpl {

    private List<MeasureYamlTpl> measures = Lists.newArrayList();

    private List<MetricParamYamlTpl> metrics = Lists.newArrayList();

    private List<FieldParamYamlTpl> fields = Lists.newArrayList();

    private String expr;
}
