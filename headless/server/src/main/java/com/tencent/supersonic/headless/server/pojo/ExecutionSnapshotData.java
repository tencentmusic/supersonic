package com.tencent.supersonic.headless.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Wrapper serialised into {@code s2_report_execution.execution_snapshot}.
 *
 * <p>
 * Layout:
 *
 * <pre>
 * {
 *   "context":       { ... ReportExecutionContext fields ... },
 *   "renderedSql":   "SELECT ...",          // fully rendered SQL that actually ran; null for struct queries
 *   "resultPreview": [ { col: val, ... }, ... ]   // up to 20 rows; null when not yet available
 * }
 * </pre>
 *
 * <p>
 * This envelope lets the audit-replay API return result rows and the concrete SQL without
 * re-executing the query, while keeping {@link ReportExecutionContext} immutable.
 */
@Data
@NoArgsConstructor
public class ExecutionSnapshotData {

    /** Immutable execution context captured at trigger time. */
    private ReportExecutionContext context;

    /**
     * The fully rendered SQL string that was actually submitted to the data source. For
     * SqlTemplateConfig schedules this differs from {@code context.queryConfig} (which holds the
     * raw template with {@code ${variable}} placeholders). {@code null} for structured-query
     * executions where no raw SQL is captured.
     */
    private String renderedSql;

    /**
     * First N (≤ 20) result rows captured after a successful query execution. {@code null} when the
     * execution is still RUNNING or when it FAILed before producing results.
     */
    private List<Map<String, Object>> resultPreview;

    /** Convenience constructor for the initial (pre-result) snapshot. */
    public ExecutionSnapshotData(ReportExecutionContext context,
            List<Map<String, Object>> resultPreview) {
        this.context = context;
        this.resultPreview = resultPreview;
    }
}
