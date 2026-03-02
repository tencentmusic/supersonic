package com.tencent.supersonic.headless.server.metrics;

import com.tencent.supersonic.common.metrics.ReportMetricConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Helper methods for template-report counters and timers.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MeterRegistry.class)
public class TemplateReportMetrics {

    private final MeterRegistry registry;

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordScheduleDispatch(String result) {
        counter(ReportMetricConstants.SCHEDULE_DISPATCH_TOTAL, ReportMetricConstants.TagKeys.RESULT,
                result).increment();
    }

    public void recordScheduleRetryExhausted() {
        counter(ReportMetricConstants.SCHEDULE_RETRY_EXHAUSTED_TOTAL).increment();
    }

    public void recordExecution(String result, String source, Timer.Sample sample) {
        counter(ReportMetricConstants.EXECUTION_TOTAL, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.SOURCE, source).increment();
        sample.stop(timer(ReportMetricConstants.EXECUTION_DURATION,
                ReportMetricConstants.TagKeys.RESULT, result, ReportMetricConstants.TagKeys.SOURCE,
                source));
    }

    public void recordDelivery(String result, String type, long durationMs) {
        counter(ReportMetricConstants.DELIVERY_TOTAL, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.TYPE, type).increment();
        timer(ReportMetricConstants.DELIVERY_DURATION, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.TYPE, type).record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDeliveryRetry(String result, String type, long durationMs) {
        counter(ReportMetricConstants.DELIVERY_RETRY_TOTAL, ReportMetricConstants.TagKeys.RESULT,
                result, ReportMetricConstants.TagKeys.TYPE, type).increment();
        timer(ReportMetricConstants.DELIVERY_DURATION, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.TYPE, type, "retry", "true").record(durationMs,
                        TimeUnit.MILLISECONDS);
    }

    public void recordExport(String result, String format, long durationMs) {
        counter(ReportMetricConstants.EXPORT_TOTAL, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.FORMAT, format).increment();
        timer(ReportMetricConstants.EXPORT_DURATION, ReportMetricConstants.TagKeys.RESULT, result,
                ReportMetricConstants.TagKeys.FORMAT, format).record(durationMs,
                        TimeUnit.MILLISECONDS);
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(withModule(tags)).register(registry);
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name).tags(withModule(tags)).register(registry);
    }

    private String[] withModule(String... tags) {
        String[] merged = new String[tags.length + 2];
        merged[0] = ReportMetricConstants.TagKeys.MODULE;
        merged[1] = ReportMetricConstants.MODULE;
        System.arraycopy(tags, 0, merged, 2, tags.length);
        return merged;
    }
}
