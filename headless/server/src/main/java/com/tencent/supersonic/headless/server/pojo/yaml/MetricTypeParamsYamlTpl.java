package com.tencent.supersonic.headless.server.pojo.yaml;

import lombok.Data;

import java.util.List;

@Data
public class MetricTypeParamsYamlTpl {

    private List<MeasureYamlTpl> measures;

    private String expr;


}
