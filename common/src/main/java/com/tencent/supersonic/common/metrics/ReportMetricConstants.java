package com.tencent.supersonic.common.metrics;

/**
 * Metric names and tag keys for template-report observability.
 *
 * <p>
 * Only constants are defined here to avoid cross-module runtime coupling.
 * </p>
 */
public final class ReportMetricConstants {

    private ReportMetricConstants() {}

    public static final String MODULE = "template_report";

    // counter metrics
    public static final String SCHEDULE_DISPATCH_TOTAL =
            "supersonic.report.schedule.dispatch.total";
    public static final String SCHEDULE_RETRY_EXHAUSTED_TOTAL =
            "supersonic.report.schedule.retry.exhausted.total";
    public static final String EXECUTION_TOTAL = "supersonic.report.execution.total";
    public static final String DELIVERY_TOTAL = "supersonic.report.delivery.total";
    public static final String DELIVERY_RETRY_TOTAL = "supersonic.report.delivery.retry.total";
    public static final String EXPORT_TOTAL = "supersonic.report.export.total";

    // timer metrics
    public static final String EXECUTION_DURATION = "supersonic.report.execution.duration";
    public static final String DELIVERY_DURATION = "supersonic.report.delivery.duration";
    public static final String EXPORT_DURATION = "supersonic.report.export.duration";

    // gauge metrics
    public static final String EXPORT_PENDING = "supersonic.report.export.pending";
    public static final String DELIVERY_RETRY_PENDING = "supersonic.report.delivery.retry.pending";
    public static final String DELIVERY_CONFIG_DISABLED =
            "supersonic.report.delivery.config.disabled";

    public static final class TagKeys {
        private TagKeys() {}

        public static final String MODULE = "module";
        public static final String RESULT = "result";
        public static final String SOURCE = "source";
        public static final String TYPE = "type";
        public static final String FORMAT = "format";
        public static final String ERROR_TYPE = "error_type";
    }
}
