package com.tencent.supersonic.headless.api.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

// STUB — fully rewritten in Phase B (Task B4)
public interface ReportDeliveryService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class DeliveryStatistics {

        private Long totalDeliveries;

        private Long successCount;

        private Long failedCount;

        private Long pendingCount;

        private Double successRate;

        private Map<String, Long> countByType;

        private Map<String, Double> successRateByType;

        private Double avgDeliveryTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class DailyDeliveryStats {

        private String date;

        private Long total;

        private Long success;

        private Long failed;

        private Double successRate;
    }
}
