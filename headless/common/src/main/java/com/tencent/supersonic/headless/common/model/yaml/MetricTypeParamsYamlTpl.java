package com.tencent.supersonic.headless.common.model.yaml;

import java.util.List;
import lombok.Data;

@Data
public class MetricTypeParamsYamlTpl {

    private List<MeasureYamlTpl> measures;

    private String expr;


}
