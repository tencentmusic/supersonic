package com.tencent.supersonic.semantic.api.model.pojo;

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

    private String constraint;

    private String alias;

    private String createMetric;

    private String bizName;

    private Integer isCreateMetric = 0;

    private Long datasourceId;

    public Measure(String name, String bizName, String agg, Integer isCreateMetric) {
        this.name = name;
        this.agg = agg;
        this.isCreateMetric = isCreateMetric;
        this.bizName = bizName;
    }

    public Measure(String bizName, Long datasourceId) {
        this.bizName = bizName;
        this.datasourceId = datasourceId;
    }
}
