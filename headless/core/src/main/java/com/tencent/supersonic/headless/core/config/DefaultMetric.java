package com.tencent.supersonic.headless.core.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DefaultMetric {

    /**
     * default metrics
     */
    private Long metricId;

    /**
     * default time span unit
     */
    private Integer unit;

    /**
     * default time type: DAY
     * DAY, WEEK, MONTH, YEAR
     */
    private String period;

    private String bizName;
    private String name;

    public DefaultMetric(Long metricId, Integer unit, String period) {
        this.metricId = metricId;
        this.unit = unit;
        this.period = period;
    }

    public DefaultMetric(String bizName, Integer unit, String period) {
        this.bizName = bizName;
        this.unit = unit;
        this.period = period;
    }
}