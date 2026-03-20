package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryException;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryRateLimiter;
import com.tencent.supersonic.headless.server.service.delivery.ReportDeliveryChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of ReportDeliveryService using strategy pattern for channel dispatch.
 */
@Service
@Slf4j
public class ReportDeliveryServiceImpl
        extends ServiceImpl<ReportDeliveryConfigMapper, ReportDeliveryConfigDO>
        implements ReportDeliveryService {

    private static final int DEFAULT_MAX_CONSECUTIVE_FAILURES = 5;

    private final ReportDeliveryRecordMapper recordMapper;
    private final Map<DeliveryType, ReportDeliveryChannel> channelMap;
    private final DeliveryRateLimiter rateLimiter;
    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    public ReportDeliveryServiceImpl(ReportDeliveryRecordMapper recordMapper,
            List<ReportDeliveryChannel> channels, DeliveryRateLimiter rateLimiter) {
        this.recordMapper = recordMapper;
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(ReportDeliveryChannel::getType, Function.identity()));
        this.rateLimiter = rateLimiter;
        log.info("Registered {} delivery channels: {}", channelMap.size(), channelMap.keySet());
    }

    // ========== Config CRUD ==========

    @Override
    public ReportDeliveryConfigDO createConfig(ReportDeliveryConfigDO config) {
        validateConfig(config);

        config.setCreatedAt(new Date());
        config.setUpdatedAt(new Date());
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        if (config.getConsecutiveFailures() == null) {
            config.setConsecutiveFailures(0);
        }
        if (config.getMaxConsecutiveFailures() == null) {
            config.setMaxConsecutiveFailures(DEFAULT_MAX_CONSECUTIVE_FAILURES);
        }
        baseMapper.insert(config);
        return config;
    }

    @Override
    public ReportDeliveryConfigDO updateConfig(ReportDeliveryConfigDO config) {
        validateConfig(config);
        config.setUpdatedAt(new Date());
        // Reset consecutive failures when manually updating
        if (config.getEnabled() != null && config.getEnabled()) {
            config.setConsecutiveFailures(0);
        }
        baseMapper.updateById(config);
        return config;
    }

    @Override
    public void deleteConfig(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public ReportDeliveryConfigDO getConfigById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Page<ReportDeliveryConfigDO> getConfigList(Page<ReportDeliveryConfigDO> page) {
        QueryWrapper<ReportDeliveryConfigDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().orderByDesc(ReportDeliveryConfigDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<ReportDeliveryConfigDO> getConfigsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return baseMapper.selectBatchIds(ids);
    }

    // ========== Delivery Execution ==========

    @Override
    public List<ReportDeliveryRecordDO> deliver(List<Long> configIds, DeliveryContext context) {
        List<ReportDeliveryRecordDO> records = new ArrayList<>();

        if (configIds == null || configIds.isEmpty()) {
            log.debug("No delivery configs specified, skipping delivery");
            return records;
        }

        List<ReportDeliveryConfigDO> configs = getConfigsByIds(configIds);
        if (configs.isEmpty()) {
            log.warn("No delivery configs found for ids: {}", configIds);
            return records;
        }

        for (ReportDeliveryConfigDO config : configs) {
            if (config.getEnabled() == null || !config.getEnabled()) {
                log.debug("Skipping disabled delivery config: {}", config.getId());
                continue;
            }

            // Build idempotency key
            String deliveryKey = buildDeliveryKey(context.getScheduleId(),
                    context.getExecutionTime(), config.getId());

            // Check if already delivered
            if (isAlreadyDelivered(deliveryKey)) {
                log.info("Delivery already exists, skipping: key={}", deliveryKey);
                continue;
            }

            // Create delivery record
            ReportDeliveryRecordDO record = new ReportDeliveryRecordDO();
            record.setDeliveryKey(deliveryKey);
            record.setScheduleId(context.getScheduleId());
            record.setExecutionId(context.getExecutionId());
            record.setConfigId(config.getId());
            record.setDeliveryType(config.getDeliveryType());
            record.setStatus(DeliveryStatus.PENDING.name());
            record.setFileLocation(context.getFileLocation());
            record.setTenantId(context.getTenantId());
            record.setCreatedAt(new Date());
            record.setRetryCount(0);
            recordMapper.insert(record);

            // Execute delivery with timing
            long startTime = System.currentTimeMillis();
            try {
                record.setStatus(DeliveryStatus.SENDING.name());
                record.setStartedAt(new Date());
                recordMapper.updateById(record);

                executeDelivery(config, context);

                long deliveryTimeMs = System.currentTimeMillis() - startTime;
                record.setStatus(DeliveryStatus.SUCCESS.name());
                record.setCompletedAt(new Date());
                record.setDeliveryTimeMs(deliveryTimeMs);
                recordMapper.updateById(record);

                // Reset consecutive failures on success
                resetConsecutiveFailures(config);
                if (reportMetrics != null) {
                    reportMetrics.recordDelivery("success",
                            normalizeDeliveryType(config.getDeliveryType()), deliveryTimeMs);
                }

                log.info("Delivery successful: configId={}, type={}, scheduleId={}, timeMs={}",
                        config.getId(), config.getDeliveryType(), context.getScheduleId(),
                        deliveryTimeMs);

            } catch (DeliveryException e) {
                long deliveryTimeMs = System.currentTimeMillis() - startTime;
                log.error("Delivery failed: configId={}, type={}", config.getId(),
                        config.getDeliveryType(), e);
                record.setStatus(DeliveryStatus.FAILED.name());
                record.setErrorMessage(truncate(e.getMessage(), 2000));
                record.setCompletedAt(new Date());
                record.setDeliveryTimeMs(deliveryTimeMs);

                // Set up automatic retry with exponential backoff (first retry in 1 minute)
                if (e.isRetryable()) {
                    record.setMaxRetries(DEFAULT_MAX_CONSECUTIVE_FAILURES);
                    record.setNextRetryAt(new Date(System.currentTimeMillis() + 60_000));
                }

                recordMapper.updateById(record);

                // Track consecutive failures
                handleDeliveryFailure(config, e.getMessage());
                if (reportMetrics != null) {
                    reportMetrics.recordDelivery("error",
                            normalizeDeliveryType(config.getDeliveryType()), deliveryTimeMs);
                }
            }

            records.add(record);
        }

        return records;
    }

    /**
     * Reset consecutive failures counter on successful delivery.
     */
    private void resetConsecutiveFailures(ReportDeliveryConfigDO config) {
        if (config.getConsecutiveFailures() != null && config.getConsecutiveFailures() > 0) {
            config.setConsecutiveFailures(0);
            config.setUpdatedAt(new Date());
            baseMapper.updateById(config);
        }
    }

    /**
     * Track consecutive failures and auto-disable if threshold exceeded.
     */
    private void handleDeliveryFailure(ReportDeliveryConfigDO config, String errorMessage) {
        int failures =
                (config.getConsecutiveFailures() != null ? config.getConsecutiveFailures() : 0) + 1;
        int maxFailures =
                config.getMaxConsecutiveFailures() != null ? config.getMaxConsecutiveFailures()
                        : DEFAULT_MAX_CONSECUTIVE_FAILURES;

        config.setConsecutiveFailures(failures);
        config.setUpdatedAt(new Date());

        if (failures >= maxFailures) {
            config.setEnabled(false);
            String reason =
                    String.format("Auto-disabled after %d consecutive failures. Last error: %s",
                            failures, truncate(errorMessage, 400));
            config.setDisabledReason(reason);
            log.warn(
                    "Auto-disabling delivery config {} after {} consecutive failures (threshold: {})",
                    config.getId(), failures, maxFailures);
        }

        baseMapper.updateById(config);
    }

    @Override
    public void testDelivery(Long configId) {
        ReportDeliveryConfigDO config = getConfigById(configId);
        if (config == null) {
            throw new IllegalArgumentException("Delivery config not found: " + configId);
        }

        DeliveryContext testContext = DeliveryContext.builder().scheduleId(0L).executionId(0L)
                .scheduleName("Test Schedule").reportName("Test Report").outputFormat("XLSX")
                .rowCount(100L).executionTime(new Date().toString()).build();

        executeDelivery(config, testContext);
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

        // Apply rate limiting before delivery
        double waitTime = rateLimiter.acquire(type);
        if (waitTime > 0) {
            log.debug("Rate limited for {}ms before delivering via {}", waitTime * 1000, type);
        }

        channel.deliver(config.getDeliveryConfig(), context);
    }

    // ========== Delivery Records ==========

    @Override
    public Page<ReportDeliveryRecordDO> getDeliveryRecords(Page<ReportDeliveryRecordDO> page,
            Long scheduleId, Long executionId) {
        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        if (scheduleId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getScheduleId, scheduleId);
        }
        if (executionId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getExecutionId, executionId);
        }
        wrapper.lambda().orderByDesc(ReportDeliveryRecordDO::getCreatedAt);
        return recordMapper.selectPage(page, wrapper);
    }

    @Override
    public void retryDelivery(Long recordId) {
        ReportDeliveryRecordDO record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("Delivery record not found: " + recordId);
        }

        if (!DeliveryStatus.FAILED.name().equals(record.getStatus())) {
            throw new IllegalStateException("Can only retry failed deliveries");
        }

        ReportDeliveryConfigDO config = getConfigById(record.getConfigId());
        if (config == null) {
            throw new IllegalStateException(
                    "Delivery config no longer exists: " + record.getConfigId());
        }

        DeliveryContext context = DeliveryContext.builder().scheduleId(record.getScheduleId())
                .executionId(record.getExecutionId()).fileLocation(record.getFileLocation())
                .tenantId(record.getTenantId()).scheduleName("Retry").reportName("Retry")
                .executionTime(record.getCreatedAt().toString()).build();

        long startTime = System.currentTimeMillis();
        try {
            record.setStatus(DeliveryStatus.SENDING.name());
            record.setRetryCount(record.getRetryCount() != null ? record.getRetryCount() + 1 : 1);
            record.setStartedAt(new Date());
            record.setErrorMessage(null);
            recordMapper.updateById(record);

            executeDelivery(config, context);

            long deliveryTimeMs = System.currentTimeMillis() - startTime;
            record.setStatus(DeliveryStatus.SUCCESS.name());
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(deliveryTimeMs);
            recordMapper.updateById(record);

            // Reset failures on manual retry success
            resetConsecutiveFailures(config);
            if (reportMetrics != null) {
                reportMetrics.recordDeliveryRetry("success",
                        normalizeDeliveryType(config.getDeliveryType()), deliveryTimeMs);
            }

        } catch (DeliveryException e) {
            long deliveryTimeMs = System.currentTimeMillis() - startTime;
            record.setStatus(DeliveryStatus.FAILED.name());
            record.setErrorMessage(truncate(e.getMessage(), 2000));
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(deliveryTimeMs);
            recordMapper.updateById(record);
            if (reportMetrics != null) {
                reportMetrics.recordDeliveryRetry("error",
                        normalizeDeliveryType(config.getDeliveryType()), deliveryTimeMs);
            }
            throw e;
        }
    }

    // ========== Statistics ==========

    @Override
    public DeliveryStatistics getStatistics(Integer days) {
        int queryDays = days != null && days > 0 ? days : 7;
        Date startDate = getStartDate(queryDays);

        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().ge(ReportDeliveryRecordDO::getCreatedAt, startDate);
        List<ReportDeliveryRecordDO> records = recordMapper.selectList(wrapper);

        long total = records.size();
        long success = records.stream()
                .filter(r -> DeliveryStatus.SUCCESS.name().equals(r.getStatus())).count();
        long failed = records.stream()
                .filter(r -> DeliveryStatus.FAILED.name().equals(r.getStatus())).count();
        long pending =
                records.stream().filter(r -> DeliveryStatus.PENDING.name().equals(r.getStatus())
                        || DeliveryStatus.SENDING.name().equals(r.getStatus())).count();

        double successRate = total > 0 ? (double) success / total * 100 : 0;

        // Count by type
        Map<String, Long> countByType = records.stream().collect(Collectors
                .groupingBy(ReportDeliveryRecordDO::getDeliveryType, Collectors.counting()));

        // Success rate by type
        Map<String, Double> successRateByType = new HashMap<>();
        Map<String, List<ReportDeliveryRecordDO>> byType =
                records.stream().collect(Collectors.groupingBy(r -> r.getDeliveryType()));
        for (Map.Entry<String, List<ReportDeliveryRecordDO>> entry : byType.entrySet()) {
            List<ReportDeliveryRecordDO> typeRecords = entry.getValue();
            long typeTotal = typeRecords.size();
            long typeSuccess = typeRecords.stream()
                    .filter(r -> DeliveryStatus.SUCCESS.name().equals(r.getStatus())).count();
            successRateByType.put(entry.getKey(),
                    typeTotal > 0 ? (double) typeSuccess / typeTotal * 100 : 0);
        }

        // Average delivery time
        Double avgDeliveryTime = records.stream().filter(r -> r.getDeliveryTimeMs() != null)
                .mapToLong(ReportDeliveryRecordDO::getDeliveryTimeMs).average().orElse(0);

        return DeliveryStatistics.builder().totalDeliveries(total).successCount(success)
                .failedCount(failed).pendingCount(pending).successRate(successRate)
                .countByType(countByType).successRateByType(successRateByType)
                .avgDeliveryTimeMs(avgDeliveryTime).build();
    }

    @Override
    public List<DailyDeliveryStats> getDailyStats(Integer days) {
        int queryDays = days != null && days > 0 ? days : 7;
        Date startDate = getStartDate(queryDays);

        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().ge(ReportDeliveryRecordDO::getCreatedAt, startDate);
        List<ReportDeliveryRecordDO> records = recordMapper.selectList(wrapper);

        // Group by date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<ReportDeliveryRecordDO>> byDate =
                records.stream().collect(Collectors.groupingBy(r -> r.getCreatedAt().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)));

        // Build daily stats for each day in range (descending order - newest first)
        List<DailyDeliveryStats> dailyStats = new ArrayList<>();
        LocalDate date = LocalDate.now();
        for (int i = 0; i < queryDays; i++) {
            String dateStr = date.format(formatter);
            List<ReportDeliveryRecordDO> dayRecords =
                    byDate.getOrDefault(dateStr, new ArrayList<>());

            long total = dayRecords.size();
            long success = dayRecords.stream()
                    .filter(r -> DeliveryStatus.SUCCESS.name().equals(r.getStatus())).count();
            long failed = dayRecords.stream()
                    .filter(r -> DeliveryStatus.FAILED.name().equals(r.getStatus())).count();
            double rate = total > 0 ? (double) success / total * 100 : 0;

            dailyStats.add(DailyDeliveryStats.builder().date(dateStr).total(total).success(success)
                    .failed(failed).successRate(rate).build());

            date = date.minusDays(1);
        }

        return dailyStats;
    }

    private Date getStartDate(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -(days - 1));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // ========== Helpers ==========

    private void validateConfig(ReportDeliveryConfigDO config) {
        if (config.getDeliveryType() == null) {
            throw new IllegalArgumentException("Delivery type is required");
        }

        DeliveryType type;
        try {
            type = DeliveryType.valueOf(config.getDeliveryType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid delivery type: " + config.getDeliveryType());
        }

        ReportDeliveryChannel channel = channelMap.get(type);
        if (channel != null) {
            channel.validateConfig(config.getDeliveryConfig());
        }
    }

    private String buildDeliveryKey(Long scheduleId, String executionTime, Long configId) {
        return scheduleId + "_" + executionTime + "_" + configId;
    }

    private boolean isAlreadyDelivered(String deliveryKey) {
        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ReportDeliveryRecordDO::getDeliveryKey, deliveryKey)
                .eq(ReportDeliveryRecordDO::getStatus, DeliveryStatus.SUCCESS.name());
        return recordMapper.selectCount(wrapper) > 0;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private String normalizeDeliveryType(String type) {
        return type == null ? "unknown" : type.toLowerCase();
    }
}
