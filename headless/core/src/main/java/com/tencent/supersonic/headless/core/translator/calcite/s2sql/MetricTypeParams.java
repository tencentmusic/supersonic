package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import lombok.Data;

import java.util.List;

@Data
public class MetricTypeParams {

    private List<Measure> measures;
    private List<Measure> metrics;
    private List<Measure> fields;
    private boolean isFieldMetric = false;
    private String expr;
}
