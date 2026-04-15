package com.tencent.supersonic.headless.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.api.pojo.request.ReportDeliveryConfigReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryRecordResp;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;

import java.util.List;

/**
 * Service for managing report delivery configurations and executing deliveries.
 */
public interface ReportDeliveryService {

    // ========== Config CRUD ==========

    ReportDeliveryConfigResp createConfig(ReportDeliveryConfigReq req);

    ReportDeliveryConfigResp updateConfig(ReportDeliveryConfigReq req);

    void deleteConfig(Long id);

    ReportDeliveryConfigResp getConfigById(Long id);

    Page<ReportDeliveryConfigResp> getConfigList(Page<ReportDeliveryConfigResp> page);

    List<ReportDeliveryConfigResp> getConfigsByIds(List<Long> ids);

    // ========== Delivery Execution ==========

    /**
     * Deliver a report to multiple channels.
     */
    List<ReportDeliveryRecordResp> deliver(List<Long> configIds, DeliveryContext context);

    /**
     * Test delivery configuration by sending a test message.
     */
    ReportDeliveryRecordResp testDelivery(Long configId);

    // ========== Delivery Records ==========

    Page<ReportDeliveryRecordResp> getDeliveryRecords(Page<ReportDeliveryRecordResp> page,
            Long configId, Long scheduleId, Long executionId);

    /**
     * Retry a failed delivery.
     */
    ReportDeliveryRecordResp retryDelivery(Long recordId);

    // ========== Statistics ==========

    DeliveryStatistics getStatistics(Integer days);

    List<DailyDeliveryStats> getDailyStats(Integer days);

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
