package com.tencent.supersonic.headless.server.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReportExecutionContext {
    private final Long tenantId;
    private final Long templateId;
    private final Long templateVersion;
    private final Long datasetId;
    private final Long scheduleId;
    private final String scheduleName;
    private final Long operatorUserId;
    private final ExecutionSource source;
    private final String queryConfig;
    private final Map<String, Object> resolvedParams;
    private final OutputConfig outputConfig;
    private final List<Long> deliveryConfigIds;
}
