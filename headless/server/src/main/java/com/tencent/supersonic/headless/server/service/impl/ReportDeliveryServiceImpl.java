package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.service.ReportDeliveryService;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryException;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryRateLimiter;
import com.tencent.supersonic.headless.server.service.delivery.ReportDeliveryChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /** Sentinel scheduleId used by {@link #testDelivery(Long)} to mark test rows. */
    private static final long TEST_DELIVERY_SCHEDULE_ID = 0L;

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

        if (config.getTenantId() == null) {
            config.setTenantId(TenantContext.getTenantId());
        }
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
        assertTenantAccess(getConfigById(config.getId()));
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
        getConfigById(id);
        baseMapper.deleteById(id);
    }

    @Override
    public ReportDeliveryConfigDO getConfigById(Long id) {
        ReportDeliveryConfigDO config = baseMapper.selectById(id);
        assertTenantAccess(config);
        return config;
    }

    @Override
    public Page<ReportDeliveryConfigDO> getConfigList(Page<ReportDeliveryConfigDO> page) {
        QueryWrapper<ReportDeliveryConfigDO> wrapper = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.lambda().eq(ReportDeliveryConfigDO::getTenantId, tenantId);
        }
        wrapper.lambda().orderByDesc(ReportDeliveryConfigDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<ReportDeliveryConfigDO> getConfigsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ReportDeliveryConfigDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(ReportDeliveryConfigDO::getId, ids);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.lambda().eq(ReportDeliveryConfigDO::getTenantId, tenantId);
        }
        return baseMapper.selectList(wrapper);
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

            // Build idempotency key. Uses executionId (unique PK from s2_report_execution)
            // rather than executionTime — concurrent manual executions can otherwise format
            // to the same second and collide on idx_delivery_key.
            String deliveryKey = buildDeliveryKey(context.getScheduleId(), context.getExecutionId(),
                    config.getId());

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

    // noRollbackFor: the catch blocks persist FAILED state before rethrowing; without
    // this, the default RuntimeException rollback would wipe those writes and leave
    // the row stuck in SENDING.
    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public ReportDeliveryRecordDO testDelivery(Long configId) {
        ReportDeliveryConfigDO config = getConfigById(configId);

        DeliveryContext testContext = DeliveryContext.builder().scheduleId(0L).executionId(0L)
                .scheduleName("Test Schedule").reportName("Test Report").outputFormat("XLSX")
                .rowCount(100L).executionTime(DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"))
                .build();

        ReportDeliveryRecordDO record = new ReportDeliveryRecordDO();
        record.setDeliveryKey("TEST_" + System.currentTimeMillis() + "_" + config.getId());
        record.setScheduleId(testContext.getScheduleId());
        record.setExecutionId(testContext.getExecutionId());
        record.setConfigId(config.getId());
        record.setDeliveryType(config.getDeliveryType());
        record.setStatus(DeliveryStatus.PENDING.name());
        record.setTenantId(config.getTenantId());
        record.setCreatedAt(new Date());
        record.setRetryCount(0);
        recordMapper.insert(record);

        long startTime = System.currentTimeMillis();
        try {
            record.setStatus(DeliveryStatus.SENDING.name());
            record.setStartedAt(new Date());
            recordMapper.updateById(record);

            executeDelivery(config, testContext);

            record.setStatus(DeliveryStatus.SUCCESS.name());
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(System.currentTimeMillis() - startTime);
            recordMapper.updateById(record);
            return record;
        } catch (DeliveryException e) {
            record.setStatus(DeliveryStatus.FAILED.name());
            record.setErrorMessage(truncate(e.getMessage(), 2000));
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(System.currentTimeMillis() - startTime);
            recordMapper.updateById(record);
            throw e;
        } catch (RuntimeException e) {
            record.setStatus(DeliveryStatus.FAILED.name());
            record.setErrorMessage(truncate(e.getMessage(), 2000));
            record.setCompletedAt(new Date());
            record.setDeliveryTimeMs(System.currentTimeMillis() - startTime);
            recordMapper.updateById(record);
            throw e;
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
            Long configId, Long scheduleId, Long executionId) {
        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getTenantId, tenantId);
        }
        if (configId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getConfigId, configId);
        }
        if (scheduleId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getScheduleId, scheduleId);
        } else {
            // Exclude test-delivery records (testDelivery uses scheduleId=0 as a sentinel)
            wrapper.lambda().ne(ReportDeliveryRecordDO::getScheduleId, TEST_DELIVERY_SCHEDULE_ID);
        }
        if (executionId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getExecutionId, executionId);
        }
        wrapper.lambda().orderByDesc(ReportDeliveryRecordDO::getCreatedAt);
        return recordMapper.selectPage(page, wrapper);
    }

    // See testDelivery: the catch blocks persist FAILED state before rethrowing.
    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public ReportDeliveryRecordDO retryDelivery(Long recordId) {
        ReportDeliveryRecordDO record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("Delivery record not found: " + recordId);
        }
        assertTenantAccess(record);

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
                .executionTime(DateUtils.format(record.getCreatedAt(), "yyyy-MM-dd HH:mm:ss"))
                .build();

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
            return record;

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
    public ReportDeliveryService.DeliveryStatistics getStatistics(Integer days) {
        int queryDays = days != null && days > 0 ? days : 7;
        Date startDate = getStartDate(queryDays);

        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getTenantId, tenantId);
        }
        wrapper.lambda().ge(ReportDeliveryRecordDO::getCreatedAt, startDate)
                .ne(ReportDeliveryRecordDO::getScheduleId, TEST_DELIVERY_SCHEDULE_ID);
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
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getTenantId, tenantId);
        }
        wrapper.lambda().ge(ReportDeliveryRecordDO::getCreatedAt, startDate)
                .ne(ReportDeliveryRecordDO::getScheduleId, TEST_DELIVERY_SCHEDULE_ID);
        List<ReportDeliveryRecordDO> records = recordMapper.selectList(wrapper);

        // Group by date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<ReportDeliveryRecordDO>> byDate =
                records.stream().collect(Collectors.groupingBy(r -> r.getCreatedAt().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)));

        // Build daily stats only for dates that have records (descending order)
        List<DailyDeliveryStats> dailyStats = byDate.entrySet().stream()
                .sorted(Map.Entry.<String, List<ReportDeliveryRecordDO>>comparingByKey().reversed())
                .map(entry -> {
                    List<ReportDeliveryRecordDO> dayRecords = entry.getValue();
                    long total = dayRecords.size();
                    long success = dayRecords.stream()
                            .filter(r -> DeliveryStatus.SUCCESS.name().equals(r.getStatus()))
                            .count();
                    long failed = dayRecords.stream()
                            .filter(r -> DeliveryStatus.FAILED.name().equals(r.getStatus()))
                            .count();
                    double rate = (double) success / total * 100;
                    return DailyDeliveryStats.builder().date(entry.getKey()).total(total)
                            .success(success).failed(failed).successRate(rate).build();
                }).collect(Collectors.toList());

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

    private String buildDeliveryKey(Long scheduleId, Long executionId, Long configId) {
        return scheduleId + "_" + executionId + "_" + configId;
    }

    private boolean isAlreadyDelivered(String deliveryKey) {
        QueryWrapper<ReportDeliveryRecordDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ReportDeliveryRecordDO::getDeliveryKey, deliveryKey)
                .eq(ReportDeliveryRecordDO::getStatus, DeliveryStatus.SUCCESS.name());
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.lambda().eq(ReportDeliveryRecordDO::getTenantId, tenantId);
        }
        return recordMapper.selectCount(wrapper) > 0;
    }

    private void assertTenantAccess(ReportDeliveryConfigDO config) {
        if (config == null) {
            throw new InvalidPermissionException("推送配置不存在或无权访问");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            throw new InvalidPermissionException("租户上下文未建立");
        }
        if (config.getTenantId() == null) {
            throw new InvalidPermissionException("推送配置未绑定租户，禁止访问");
        }
        if (!Objects.equals(currentTenantId, config.getTenantId())) {
            throw new InvalidPermissionException("无权访问其他租户的推送配置");
        }
    }

    private void assertTenantAccess(ReportDeliveryRecordDO record) {
        if (record == null) {
            throw new InvalidPermissionException("推送记录不存在或无权访问");
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            throw new InvalidPermissionException("租户上下文未建立");
        }
        if (record.getTenantId() == null) {
            throw new InvalidPermissionException("推送记录未绑定租户，禁止访问");
        }
        if (!Objects.equals(currentTenantId, record.getTenantId())) {
            throw new InvalidPermissionException("无权访问其他租户的推送记录");
        }
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
