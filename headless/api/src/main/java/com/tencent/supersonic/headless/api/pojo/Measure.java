package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Measure {

    private String name;

    private String agg;

    private String expr;

    private String bizName;

    private Integer isCreateMetric = 0;

    private String constraint;

    private String alias;

    private String unit;

    public Measure(String name, String bizName, String expr, String agg, String unit,
            Integer isCreateMetric) {
        this.name = name;
        this.agg = agg;
        this.isCreateMetric = isCreateMetric;
        this.bizName = bizName;
        this.expr = expr;
        this.unit = unit;
    }

    public Measure(String name, String bizName, String agg, Integer isCreateMetric) {
        this.name = name;
        this.agg = agg;
        this.isCreateMetric = isCreateMetric;
        this.bizName = bizName;
        this.expr = bizName;
    }

    public Measure(String bizName, String constraint) {
        this.bizName = bizName;
        this.constraint = constraint;
    }

    public String getFieldName() {
        return expr;
    }

}
