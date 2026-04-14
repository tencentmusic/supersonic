package com.tencent.supersonic.headless.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryContext;

import java.util.List;

/**
 * Service for managing report delivery configurations and executing deliveries.
 */
public interface ReportDeliveryService {

    // ========== Config CRUD ==========

    ReportDeliveryConfigDO createConfig(ReportDeliveryConfigDO config);

    ReportDeliveryConfigDO updateConfig(ReportDeliveryConfigDO config);

    void deleteConfig(Long id);

    ReportDeliveryConfigDO getConfigById(Long id);

    Page<ReportDeliveryConfigDO> getConfigList(Page<ReportDeliveryConfigDO> page);

    List<ReportDeliveryConfigDO> getConfigsByIds(List<Long> ids);

    // ========== Delivery Execution ==========

    /**
     * Deliver a report to multiple channels.
     *
     * @param configIds list of delivery config IDs
     * @param context delivery context
     * @return list of created delivery records
     */
    List<ReportDeliveryRecordDO> deliver(List<Long> configIds, DeliveryContext context);

    /**
     * Test delivery configuration by sending a test message.
     *
     * @param configId config ID to test
     * @return created test delivery record
     */
    ReportDeliveryRecordDO testDelivery(Long configId);

    // ========== Delivery Records ==========

    Page<ReportDeliveryRecordDO> getDeliveryRecords(Page<ReportDeliveryRecordDO> page,
            Long configId, Long scheduleId, Long executionId);

    /**
     * Retry a failed delivery.
     *
     * @param recordId the delivery record ID to retry
     * @return the updated delivery record (status SUCCESS on success; on failure the persisted
     *         FAILED record is still written and a {@code DeliveryException} is rethrown)
     */
    ReportDeliveryRecordDO retryDelivery(Long recordId);

    // ========== Statistics ==========

    /**
     * Get delivery statistics summary.
     *
     * @param days number of days to include (default 7)
     * @return statistics summary
     */
    DeliveryStatistics getStatistics(Integer days);

    /**
     * Get daily delivery counts for trend charts.
     *
     * @param days number of days to include
     * @return list of daily statistics
     */
    java.util.List<DailyDeliveryStats> getDailyStats(Integer days);

    /**
     * Statistics summary POJO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class DeliveryStatistics {
        private long totalDeliveries;
        private long successCount;
        private long failedCount;
        private long pendingCount;
        private double successRate;
        private java.util.Map<String, Long> countByType;
        private java.util.Map<String, Double> successRateByType;
        private Double avgDeliveryTimeMs;
    }

    /**
     * Daily statistics for trend charts.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class DailyDeliveryStats {
        private String date;
        private long total;
        private long success;
        private long failed;
        private double successRate;
    }
}
