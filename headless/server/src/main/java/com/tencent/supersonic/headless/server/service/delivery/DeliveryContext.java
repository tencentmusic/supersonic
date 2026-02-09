package com.tencent.supersonic.headless.server.service.delivery;

import lombok.Builder;
import lombok.Data;

/**
 * Context information for report delivery.
 */
@Data
@Builder
public class DeliveryContext {
    private Long scheduleId;
    private Long executionId;
    private String scheduleName;
    private String reportName;
    private String fileLocation;
    private String outputFormat;
    private Long rowCount;
    private Long tenantId;
    private String executionTime;
}
