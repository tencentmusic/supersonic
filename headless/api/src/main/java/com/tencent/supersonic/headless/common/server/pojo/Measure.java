package com.tencent.supersonic.headless.common.server.pojo;

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

    private String createMetric;

    private String bizName;

    private Integer isCreateMetric = 0;

    public Measure(String name, String bizName, String agg, Integer isCreateMetric) {
        this.name = name;
        this.agg = agg;
        this.isCreateMetric = isCreateMetric;
        this.bizName = bizName;
    }

    public String getFieldName() {
        return expr;
    }

}
