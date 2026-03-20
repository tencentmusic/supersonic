package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.headless.server.pojo.AlertCondition;
import com.tencent.supersonic.headless.server.pojo.AlertConditionOperator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure evaluation engine for alert rules.
 *
 * <p>
 * This service is intentionally free of any DB access, external HTTP calls, or Spring-managed state
 * beyond its own bean scope. It takes query result rows and alert conditions as input and returns a
 * list of triggered {@link AlertEventCandidate} objects that can be persisted or dispatched by the
 * caller.
 */
@Service
@Slf4j
public class AlertEvaluator {

    /**
     * Describes a single triggered alert condition for one result row.
     */
    @Data
    @AllArgsConstructor
    public static class AlertEventCandidate {

        /** Zero-based index of the {@link AlertCondition} that triggered. */
        private int conditionIndex;

        /**
         * Stable deduplication key: {@code ruleId + "_" + conditionIndex + "_" + dimensionValue}.
         */
        private String alertKey;

        /** Severity label from the condition, e.g. {@code "WARNING"} or {@code "CRITICAL"}. */
        private String severity;

        /** Extracted metric value from the row (may be {@code null} for ABSENCE checks). */
        private Double metricValue;

        /** Extracted baseline value from the row (null when baselineField is blank). */
        private Double baselineValue;

        /**
         * Relative deviation in percent; non-null only for DEVIATION_GT and DEVIATION_LT_NEGATIVE
         * operators.
         */
        private Double deviationPct;

        /** Value of the dimensionField cell in this row; {@code "unknown"} when absent. */
        private String dimensionValue;

        /**
         * Rendered message string with all {@code ${field}} placeholders substituted and Markdown
         * special characters escaped (AG-09).
         */
        private String message;
    }

    /**
     * Evaluate every combination of result row × alert condition.
     *
     * @param ruleId alert rule ID used to build {@link AlertEventCandidate#alertKey}
     * @param rows query result rows (from {@code SemanticQueryResp.getResultList()})
     * @param conditions ordered list of conditions defined on the rule
     * @return list of triggered candidates; empty when nothing fires
     */
    public List<AlertEventCandidate> evaluate(Long ruleId, List<Map<String, Object>> rows,
            List<AlertCondition> conditions) {
        List<AlertEventCandidate> results = new ArrayList<>();

        if (conditions == null || conditions.isEmpty()) {
            log.debug("AlertEvaluator: no conditions defined for ruleId={}", ruleId);
            return results;
        }

        if (rows == null || rows.isEmpty()) {
            log.debug("AlertEvaluator: no rows for ruleId={}, checking ABSENCE conditions", ruleId);
            for (int conditionIndex = 0; conditionIndex < conditions.size(); conditionIndex++) {
                AlertCondition condition = conditions.get(conditionIndex);
                if (condition.getOperator() == AlertConditionOperator.ABSENCE) {
                    String alertKey = ruleId + "_" + conditionIndex + "_no_data";
                    String message =
                            renderMessage(condition.getMessageTemplate(), Map.of(), null, null);
                    results.add(new AlertEventCandidate(conditionIndex, alertKey,
                            condition.getSeverity(), null, null, null, "no_data", message));
                    log.debug("ABSENCE triggered (no rows): ruleId={}, conditionIndex={}", ruleId,
                            conditionIndex);
                }
            }
            return results;
        }

        for (Map<String, Object> row : rows) {
            for (int conditionIndex = 0; conditionIndex < conditions.size(); conditionIndex++) {
                AlertCondition condition = conditions.get(conditionIndex);
                evaluateRowCondition(ruleId, row, condition, conditionIndex, results);
            }
        }

        return results;
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private void evaluateRowCondition(Long ruleId, Map<String, Object> row,
            AlertCondition condition, int conditionIndex, List<AlertEventCandidate> results) {

        Double metricValue = toDouble(row.get(condition.getMetricField()));
        Double baselineValue = StringUtils.isBlank(condition.getBaselineField()) ? null
                : toDouble(row.get(condition.getBaselineField()));
        String dimensionValue = toDimensionString(row.get(condition.getDimensionField()));
        Double threshold = condition.getThreshold();
        AlertConditionOperator operator = condition.getOperator();

        boolean triggered = false;
        Double deviationPct = null;

        switch (operator) {
            case GT:
                triggered = metricValue != null && metricValue > threshold;
                break;

            case LT:
                triggered = metricValue != null && metricValue < threshold;
                break;

            case GTE:
                triggered = metricValue != null && metricValue >= threshold;
                break;

            case LTE:
                triggered = metricValue != null && metricValue <= threshold;
                break;

            case DEVIATION_GT:
                if (metricValue != null && baselineValue != null && baselineValue != 0.0) {
                    deviationPct = Math.abs((metricValue - baselineValue) / baselineValue) * 100.0;
                    triggered = deviationPct > threshold;
                }
                // else: baseline absent or zero → skip (not triggered)
                break;

            case DEVIATION_LT_NEGATIVE:
                if (metricValue != null && baselineValue != null && baselineValue != 0.0) {
                    deviationPct = (metricValue - baselineValue) / baselineValue * 100.0;
                    triggered = deviationPct < -threshold;
                }
                // else: baseline absent or zero → skip
                break;

            case ABSENCE:
                triggered = metricValue == null || metricValue == 0.0;
                break;

            default:
                log.warn("AlertEvaluator: unknown operator {} for ruleId={}, conditionIndex={}",
                        operator, ruleId, conditionIndex);
        }

        if (!triggered) {
            return;
        }

        String alertKey = ruleId + "_" + conditionIndex + "_" + dimensionValue;
        String message =
                renderMessage(condition.getMessageTemplate(), row, metricValue, deviationPct);

        results.add(new AlertEventCandidate(conditionIndex, alertKey, condition.getSeverity(),
                metricValue, baselineValue, deviationPct, dimensionValue, message));

        log.debug(
                "Alert triggered: ruleId={}, conditionIndex={}, alertKey={}, "
                        + "severity={}, metricValue={}, deviationPct={}",
                ruleId, conditionIndex, alertKey, condition.getSeverity(), metricValue,
                deviationPct);
    }

    /**
     * Render a message template.
     *
     * <p>
     * Substitutes {@code ${field}} with the escaped cell value for every key present in
     * {@code row}, then substitutes the computed fields {@code ${metric_value}} and
     * {@code ${deviation_pct}}. All substituted values have Markdown special characters escaped
     * (AG-09).
     */
    private String renderMessage(String template, Map<String, Object> row, Double metricValue,
            Double deviationPct) {
        if (StringUtils.isBlank(template)) {
            return "";
        }

        String rendered = template;

        // Substitute row fields
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String escaped =
                    escapeMarkdown(entry.getValue() != null ? entry.getValue().toString() : "");
            rendered = rendered.replace(placeholder, escaped);
        }

        // Substitute computed fields
        String metricValueStr = metricValue != null ? escapeMarkdown(metricValue.toString()) : "";
        rendered = rendered.replace("${metric_value}", metricValueStr);

        String deviationPctStr =
                deviationPct != null ? escapeMarkdown(String.format("%.1f", deviationPct)) : "";
        rendered = rendered.replace("${deviation_pct}", deviationPctStr);

        return rendered;
    }

    /**
     * Escape Markdown special characters in a substituted value (AG-09). Characters escaped:
     * {@code [ ] ( ) * ` _}
     */
    private String escapeMarkdown(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)")
                .replace("*", "\\*").replace("`", "\\`").replace("_", "\\_");
    }

    /**
     * Convert a row cell value to {@code Double}; returns {@code null} for missing or non-numeric
     * values.
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert a row cell value to a dimension string; returns {@code "unknown"} when absent or
     * blank. Truncated to 200 characters to prevent alertKey from exceeding VARCHAR(300) when
     * combined with the {@code ruleId_conditionIndex_} prefix.
     */
    private String toDimensionString(Object value) {
        if (value == null) {
            return "unknown";
        }
        String str = value.toString();
        if (StringUtils.isBlank(str)) {
            return "unknown";
        }
        return str.length() > 200 ? str.substring(0, 200) : str;
    }
}
