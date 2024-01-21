package com.tencent.supersonic.headless.server.pojo.yaml;

import lombok.Data;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

@Data
public class MetricTypeParamsYamlTpl {

    private List<MeasureYamlTpl> measures = Lists.newArrayList();

    private List<MetricParamYamlTpl> metrics = Lists.newArrayList();

    private List<FieldParamYamlTpl> fields = Lists.newArrayList();

    private String expr;

}
