package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.AlertCondition;
import com.tencent.supersonic.headless.server.pojo.AlertConditionOperator;
import com.tencent.supersonic.headless.server.service.impl.AlertEvaluator;
import com.tencent.supersonic.headless.server.service.impl.AlertEvaluator.AlertEventCandidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertEvaluatorTest {

    private final AlertEvaluator evaluator = new AlertEvaluator();

    // ---- ABSENCE with empty rows ----

    @Test
    void absenceTriggersWhenRowsAreNull() {
        AlertCondition condition = buildCondition(AlertConditionOperator.ABSENCE, "amount", 0.0);
        List<AlertEventCandidate> results = evaluator.evaluate(1L, null, List.of(condition));

        assertEquals(1, results.size());
        assertEquals("no_data", results.get(0).getDimensionValue());
        assertNull(results.get(0).getMetricValue());
    }

    @Test
    void absenceTriggersWhenRowsAreEmpty() {
        AlertCondition condition = buildCondition(AlertConditionOperator.ABSENCE, "amount", 0.0);
        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, Collections.emptyList(), List.of(condition));

        assertEquals(1, results.size());
        assertEquals("1_0_no_data", results.get(0).getAlertKey());
    }

    @Test
    void nonAbsenceConditionsDoNotTriggerOnEmptyRows() {
        AlertCondition gtCondition = buildCondition(AlertConditionOperator.GT, "amount", 100.0);
        AlertCondition absenceCondition =
                buildCondition(AlertConditionOperator.ABSENCE, "amount", 0.0);

        List<AlertEventCandidate> results = evaluator.evaluate(1L, Collections.emptyList(),
                List.of(gtCondition, absenceCondition));

        // Only the ABSENCE condition should trigger
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getConditionIndex());
    }

    // ---- ABSENCE with rows present ----

    @Test
    void absenceTriggersWhenMetricIsNullInRow() {
        AlertCondition condition = buildCondition(AlertConditionOperator.ABSENCE, "amount", 0.0);
        Map<String, Object> row = Map.of("name", "test");
        // "amount" key missing → metricValue is null → ABSENCE triggers

        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, List.of(row), List.of(condition));

        assertEquals(1, results.size());
        assertNull(results.get(0).getMetricValue());
    }

    @Test
    void absenceTriggersWhenMetricIsZeroInRow() {
        AlertCondition condition = buildCondition(AlertConditionOperator.ABSENCE, "amount", 0.0);
        Map<String, Object> row = Map.of("amount", 0.0);

        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, List.of(row), List.of(condition));

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getMetricValue());
    }

    @Test
    void absenceDoesNotTriggerWhenMetricIsPresent() {
        AlertCondition condition = buildCondition(AlertConditionOperator.ABSENCE, "amount", 0.0);
        Map<String, Object> row = Map.of("amount", 42.0);

        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, List.of(row), List.of(condition));

        assertTrue(results.isEmpty());
    }

    // ---- GT / LT basic sanity ----

    @Test
    void gtTriggersWhenAboveThreshold() {
        AlertCondition condition = buildCondition(AlertConditionOperator.GT, "amount", 100.0);
        Map<String, Object> row = Map.of("amount", 150.0);

        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, List.of(row), List.of(condition));

        assertEquals(1, results.size());
        assertEquals(150.0, results.get(0).getMetricValue());
    }

    @Test
    void gtDoesNotTriggerWhenBelowThreshold() {
        AlertCondition condition = buildCondition(AlertConditionOperator.GT, "amount", 100.0);
        Map<String, Object> row = Map.of("amount", 50.0);

        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, List.of(row), List.of(condition));

        assertTrue(results.isEmpty());
    }

    // ---- Edge cases ----

    @Test
    void emptyConditionsReturnsEmpty() {
        Map<String, Object> row = Map.of("amount", 42.0);

        List<AlertEventCandidate> results =
                evaluator.evaluate(1L, List.of(row), Collections.emptyList());

        assertTrue(results.isEmpty());
    }

    @Test
    void nullConditionsReturnsEmpty() {
        List<AlertEventCandidate> results = evaluator.evaluate(1L, List.of(Map.of()), null);
        assertTrue(results.isEmpty());
    }

    @Test
    void multipleRowsMultipleConditions() {
        AlertCondition gt = buildCondition(AlertConditionOperator.GT, "amount", 100.0);
        AlertCondition lt = buildCondition(AlertConditionOperator.LT, "amount", 10.0);
        List<Map<String, Object>> rows =
                List.of(Map.of("amount", 150.0), Map.of("amount", 5.0), Map.of("amount", 50.0));

        List<AlertEventCandidate> results = evaluator.evaluate(1L, rows, List.of(gt, lt));

        // row 0 triggers GT, row 1 triggers LT
        assertEquals(2, results.size());
    }

    // ---- helpers ----

    private AlertCondition buildCondition(AlertConditionOperator operator, String metricField,
            Double threshold) {
        AlertCondition condition = new AlertCondition();
        condition.setOperator(operator);
        condition.setMetricField(metricField);
        condition.setDimensionField("name");
        condition.setThreshold(threshold);
        condition.setSeverity("WARNING");
        condition.setMessageTemplate("Alert: ${metric_value}");
        return condition;
    }
}
