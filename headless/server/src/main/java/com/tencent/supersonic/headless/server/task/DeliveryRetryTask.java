package com.tencent.supersonic.headless.server.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryException;
import com.tencent.supersonic.headless.server.service.delivery.ReportDeliveryChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Scheduled task for automatic retry of failed deliveries with exponential backoff.
 *
 * Retry intervals (exponential backoff): - Retry 1: 1 minute - Retry 2: 2 minutes - Retry 3: 4
 * minutes - Retry 4: 8 minutes - Retry 5: 16 minutes (max)
 */
@Component
@Slf4j
public class DeliveryRetryTask {

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long BASE_RETRY_INTERVAL_MS = 60_000; // 1 minute

    private final ReportDeliveryRecordMapper recordMapper;
    private final ReportDeliveryConfigMapper configMapper;
    private final ReportExecutionMapper executionMapper;
    private final ReportScheduleMapper scheduleMapper;
    private final Map<DeliveryType, ReportDeliveryChannel> channelMap;
    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    public DeliveryRetryTask(ReportDeliveryRecordMapper recordMapper,
            ReportDeliveryConfigMapper configMapper, ReportExecutionMapper executionMapper,
            ReportScheduleMapper scheduleMapper, List<ReportDeliveryChannel> channels) {
        this.recordMapper = recordMapper;
        this.configMapper = configMapper;
        this.executionMapper = executionMapper;
        this.scheduleMapper = scheduleMapper;
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(ReportDeliveryChannel::getType, Function.identity()));
    }

    /**
     * Run every minute to check for records that need retry.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void retryFailedDeliveries() {
        try {
            List<ReportDeliveryRecordDO> recordsToRetry = findRecordsToRetry();

            if (recordsToRetry.isEmpty()) {
                return;
            }

            log.info("Found {} delivery records to retry", recordsToRetry.size());

            for (ReportDeliveryRecordDO record : recordsToRetry) {
                try {
                    retryDelivery(record);
                } catch (Exception e) {
                    log.error("Error retrying delivery record {}: {}", record.getId(),
                            e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in delivery retry task", e);
        }
    }

    private List<ReportDeliveryRecordDO> findRecordsToRetry() {
        Date now = new Date();

        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ReportDeliveryRecordDO::getStatus, DeliveryStatus.FAILED.name())
                .isNotNull(ReportDeliveryRecordDO::getNextRetryAt)
                .le(ReportDeliveryRecordDO::getNextRetryAt, now)
                .lt(ReportDeliveryRecordDO::getRetryCount, DEFAULT_MAX_RETRIES)
                .orderByAsc(ReportDeliveryRecordDO::getNextRetryAt).last("LIMIT 10"); // Process in
                                                                                      // batches

        return recordMapper.selectList(wrapper);
    }

    private void retryDelivery(ReportDeliveryRecordDO record) {
        log.info("Retrying delivery: recordId={}, attempt={}", record.getId(),
                record.getRetryCount() + 1);

        // Get config
        ReportDeliveryConfigDO config = configMapper.selectById(record.getConfigId());
        if (config == null) {
            log.warn("Config not found for record {}, skipping retry", record.getId());
            record.setNextRetryAt(null); // Stop retrying
            recordMapper.updateById(record);
            return;
        }

        // Skip if config is disabled
        if (config.getEnabled() == null || !config.getEnabled()) {
            log.info("Config {} is disabled, skipping retry for record {}", config.getId(),
                    record.getId());
            record.setNextRetryAt(null);
            recordMapper.updateById(record);
            return;
        }

        // Build context from original execution and schedule data
        Long rowCount = null;
        String scheduleName = "Schedule " + record.getScheduleId();
        String reportName = "Report";
        if (record.getExecutionId() != null) {
            ReportExecutionDO execution = executionMapper.selectById(record.getExecutionId());
            if (execution != null) {
                rowCount = execution.getRowCount();
            }
        }
        if (record.getScheduleId() != null) {
            ReportScheduleDO schedule = scheduleMapper.selectById(record.getScheduleId());
            if (schedule != null) {
                scheduleName = schedule.getName();
                reportName = schedule.getName();
            }
        }
        DeliveryContext context = DeliveryContext.builder().scheduleId(record.getScheduleId())
                .executionId(record.getExecutionId()).fileLocation(record.getFileLocation())
                .tenantId(record.getTenantId()).scheduleName(scheduleName).reportName(reportName)
                .rowCount(rowCount)
                .executionTime(DateUtils.format(record.getCreatedAt(), "yyyy-MM-dd HH:mm:ss"))
                .build();

        long startTime = System.currentTimeMillis();
        int retryCount = record.getRetryCount() != null ? record.getRetryCount() + 1 : 1;

        try {
            record.setStatus(DeliveryStatus.SENDING.name());
            record.setRetryCount(retryCount);
            record.setStartedAt(new Date());
            record.setErrorMessage(null);
            recordMapper.updateById(record);

            // Execute delivery
            executeDelivery(config, context);

            // Success
            long deliveryTimeMs = System.currentTimeMillis() - startTime;
            record.setStatus(DeliveryStatus.SUCCESS.name());
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(deliveryTimeMs);
            record.setNextRetryAt(null);
            recordMapper.updateById(record);

            // Reset config consecutive failures
            resetConfigFailures(config);

            log.info("Delivery retry successful: recordId={}, attempt={}", record.getId(),
                    retryCount);
            if (reportMetrics != null) {
                reportMetrics.recordDeliveryRetry("success",
                        normalizeType(config.getDeliveryType()), deliveryTimeMs);
            }

        } catch (DeliveryException e) {
            long deliveryTimeMs = System.currentTimeMillis() - startTime;
            record.setStatus(DeliveryStatus.FAILED.name());
            record.setErrorMessage(truncate(e.getMessage(), 2000));
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(deliveryTimeMs);
            record.setRetryCount(retryCount);

            // Calculate next retry time with exponential backoff
            if (retryCount < DEFAULT_MAX_RETRIES) {
                long backoffMs = calculateBackoff(retryCount);
                record.setNextRetryAt(new Date(System.currentTimeMillis() + backoffMs));
                log.info("Delivery retry {} failed, next retry in {}ms: recordId={}", retryCount,
                        backoffMs, record.getId());
            } else {
                record.setNextRetryAt(null);
                log.warn("Delivery retry {} failed, max retries reached: recordId={}", retryCount,
                        record.getId());
            }

            recordMapper.updateById(record);
            if (reportMetrics != null) {
                reportMetrics.recordDeliveryRetry("error", normalizeType(config.getDeliveryType()),
                        deliveryTimeMs);
            }
        }
    }

    private void executeDelivery(ReportDeliveryConfigDO config, DeliveryContext context) {
        DeliveryType type;
        try {
            type = DeliveryType.valueOf(config.getDeliveryType());
        } catch (IllegalArgumentException e) {
            throw new DeliveryException("Unknown delivery type: " + config.getDeliveryType(),
                    false);
        }

        ReportDeliveryChannel channel = channelMap.get(type);
        if (channel == null) {
            throw new DeliveryException("No channel implementation for type: " + type, false);
        }

        channel.deliver(config.getDeliveryConfig(), context);
    }

    private void resetConfigFailures(ReportDeliveryConfigDO config) {
        if (config.getConsecutiveFailures() != null && config.getConsecutiveFailures() > 0) {
            config.setConsecutiveFailures(0);
            config.setUpdatedAt(new Date());
            configMapper.updateById(config);
        }
    }

    /**
     * Calculate exponential backoff interval.
     *
     * @param retryCount current retry count (1-based)
     * @return delay in milliseconds
     */
    private long calculateBackoff(int retryCount) {
        // Exponential backoff: 1min, 2min, 4min, 8min, 16min
        return BASE_RETRY_INTERVAL_MS * (1L << (retryCount - 1));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private String normalizeType(String type) {
        return type == null ? "unknown" : type.toLowerCase();
    }
}
