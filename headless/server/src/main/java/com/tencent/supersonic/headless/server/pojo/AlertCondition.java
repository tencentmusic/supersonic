package com.tencent.supersonic.headless.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single condition in an alert rule.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertCondition {

    /**
     * The field name in the query result that holds the metric value to evaluate.
     */
    private String metricField;

    /**
     * The field name in the query result that holds the baseline value (for DEVIATION operators).
     * May be blank for non-deviation operators.
     */
    private String baselineField;

    /**
     * The field name used to identify the dimension of a row (e.g. "app", "region"). Used to
     * generate alertKey and dimensionValue.
     */
    private String dimensionField;

    /**
     * The threshold value for comparison.
     */
    private Double threshold;

    /**
     * The comparison operator.
     */
    private AlertConditionOperator operator;

    /**
     * Severity level: WARNING or CRITICAL.
     */
    private String severity;

    /**
     * Message template string. Supports ${field} placeholders and computed fields ${deviation_pct}
     * and ${metric_value}.
     */
    private String messageTemplate;
}
