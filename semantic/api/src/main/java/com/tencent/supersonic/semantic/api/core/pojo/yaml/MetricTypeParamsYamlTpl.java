package com.tencent.supersonic.semantic.api.core.pojo.yaml;

import java.util.List;
import lombok.Data;

@Data
public class MetricTypeParamsYamlTpl {

    private List<MeasureYamlTpl> measures;

    private String expr;


}
