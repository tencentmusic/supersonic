package com.tencent.supersonic.headless.server.pojo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

// 反序列化必须走 Lombok @Builder：字段全 final，无无参构造；JsonUtil 未注册
// ParameterNamesModule，Jackson 读回 ExecutionSnapshotData.context 时 否则会抛
// "no Creators" → 执行记录列表里 triggerType/templateName/hasPreview 全部留空。
@Data
@Builder
@JsonDeserialize(builder = ReportExecutionContext.ReportExecutionContextBuilder.class)
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

    @JsonPOJOBuilder(withPrefix = "")
    public static class ReportExecutionContextBuilder {
    }
}
