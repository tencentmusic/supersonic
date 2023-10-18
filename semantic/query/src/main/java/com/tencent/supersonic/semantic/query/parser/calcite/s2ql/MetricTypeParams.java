package com.tencent.supersonic.semantic.query.parser.calcite.s2ql;

import java.util.List;
import lombok.Data;

@Data
public class MetricTypeParams {

    private List<Measure> measures;

    private String expr;

}
