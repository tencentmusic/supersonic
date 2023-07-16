package com.tencent.supersonic.semantic.query.application.parser.calcite.dsl;

import java.util.List;
import lombok.Data;

@Data
public class MetricTypeParams {

    private List<Measure> measures;

    private String expr;

}
