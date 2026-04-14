package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.common.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ReportExecutionContext 字段全 final，JsonUtil 的 ObjectMapper 没注册 ParameterNamesModule —— 若没有
 * 
 * @JsonDeserialize(builder = ...)，Jackson 会抛 "no Creators, like default constructor"， toVO() 把异常
 *                          catch 成 DEBUG 日志，结果执行记录列表里 triggerType / templateName / hasPreview 全部为空。
 *                          这里锁定 往返 + 预览 两个场景的 反序列化路径，避免回归。
 */
class ExecutionSnapshotDataSerializationTest {

    @Test
    void contextRoundTripRetainsSource() {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).scheduleId(99L)
                .scheduleName("demo").datasetId(10L).source(ExecutionSource.SCHEDULE).build();

        String json = JsonUtil.toString(new ExecutionSnapshotData(ctx, null));
        ExecutionSnapshotData round = JsonUtil.toObject(json, ExecutionSnapshotData.class);

        assertNotNull(round);
        assertNotNull(round.getContext());
        assertEquals(ExecutionSource.SCHEDULE, round.getContext().getSource());
        assertEquals("demo", round.getContext().getScheduleName());
        assertEquals(10L, round.getContext().getDatasetId());
    }

    @Test
    void snapshotWithPreviewRowsRoundTrips() {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).scheduleId(99L)
                .source(ExecutionSource.WEB).build();
        ExecutionSnapshotData snapshot =
                new ExecutionSnapshotData(ctx, List.of(Map.of("city", "sz", "gmv", 100)));
        snapshot.setRenderedSql("select city, gmv from t");

        ExecutionSnapshotData round =
                JsonUtil.toObject(JsonUtil.toString(snapshot), ExecutionSnapshotData.class);

        assertNotNull(round.getContext());
        assertEquals(ExecutionSource.WEB, round.getContext().getSource());
        assertEquals("select city, gmv from t", round.getRenderedSql());
        assertNotNull(round.getResultPreview());
        assertEquals(1, round.getResultPreview().size());
        assertEquals("sz", round.getResultPreview().get(0).get("city"));
    }
}
