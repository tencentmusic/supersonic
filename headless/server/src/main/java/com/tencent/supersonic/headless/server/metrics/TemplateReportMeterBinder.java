package com.tencent.supersonic.headless.server.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import com.tencent.supersonic.common.metrics.ReportMetricConstants;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Registers P0 gauges for template report stability monitoring.
 */
@Component
public class TemplateReportMeterBinder extends AbstractMeterBinder {

    private final ExportTaskMapper exportTaskMapper;
    private final ReportDeliveryRecordMapper deliveryRecordMapper;
    private final ReportDeliveryConfigMapper deliveryConfigMapper;

    public TemplateReportMeterBinder(ExportTaskMapper exportTaskMapper,
            ReportDeliveryRecordMapper deliveryRecordMapper,
            ReportDeliveryConfigMapper deliveryConfigMapper) {
        super(Tags.of(ReportMetricConstants.TagKeys.MODULE, ReportMetricConstants.MODULE));
        this.exportTaskMapper = exportTaskMapper;
        this.deliveryRecordMapper = deliveryRecordMapper;
        this.deliveryConfigMapper = deliveryConfigMapper;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        Gauge.builder(ReportMetricConstants.EXPORT_PENDING, exportTaskMapper,
                this::countExportPending).description("Count of pending export tasks")
                .tags(commonTags()).strongReference(true).register(registry);

        Gauge.builder(ReportMetricConstants.DELIVERY_RETRY_PENDING, deliveryRecordMapper,
                this::countRetryPending)
                .description("Count of failed delivery records waiting for retry")
                .tags(commonTags()).strongReference(true).register(registry);

        Gauge.builder(ReportMetricConstants.DELIVERY_CONFIG_DISABLED, deliveryConfigMapper,
                this::countDisabledConfigs)
                .description("Count of disabled delivery channel configs").tags(commonTags())
                .strongReference(true).register(registry);
    }

    private double countExportPending(ExportTaskMapper mapper) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ExportTaskDO>()
                .eq(ExportTaskDO::getStatus, ExportTaskStatus.PENDING.name()));
        return Objects.requireNonNullElse(count, 0L);
    }

    private double countRetryPending(ReportDeliveryRecordMapper mapper) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ReportDeliveryRecordDO>()
                .eq(ReportDeliveryRecordDO::getStatus, DeliveryStatus.FAILED.name())
                .isNotNull(ReportDeliveryRecordDO::getNextRetryAt));
        return Objects.requireNonNullElse(count, 0L);
    }

    private double countDisabledConfigs(ReportDeliveryConfigMapper mapper) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<ReportDeliveryConfigDO>()
                .eq(ReportDeliveryConfigDO::getEnabled, false));
        return Objects.requireNonNullElse(count, 0L);
    }
}
