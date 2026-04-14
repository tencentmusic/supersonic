package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.exception.ParamValidationException;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.ExecutionSource;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import com.tencent.supersonic.headless.server.service.impl.QueryConfigParser;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExecutionOrchestratorTest {

    @Mock
    private ReportExecutionMapper executionMapper;
    @Mock
    private SemanticLayerService semanticLayerService;
    @Mock
    private SemanticTemplateService templateService;
    @Mock
    private QueryConfigParser queryConfigParser;
    @Mock
    private UserService userService;
    @Mock
    private SchemaService schemaService;
    @Mock
    private DataSetAuthService dataSetAuthService;
    @Mock
    private SensitiveLevelConfig sensitiveLevelConfig;

    @InjectMocks
    private ReportExecutionOrchestrator orchestrator;

    @Test
    void executeShouldUseRealOwnerIdentityWhenQuerying() throws Exception {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .scheduleId(100L).operatorUserId(7L).source(ExecutionSource.SCHEDULE)
                .queryConfig("{\"type\":\"sql\"}").deliveryConfigIds(Collections.emptyList())
                .build();

        User owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        owner.setTenantId(1L);
        when(userService.getUserById(7L)).thenReturn(owner);

        var queryReq = new com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq();
        queryReq.setDataSetId(10L);
        when(queryConfigParser.parse(ctx.getQueryConfig(), 10L, null)).thenReturn(queryReq);

        SemanticQueryResp queryResp = new SemanticQueryResp();
        queryResp.setColumns(Collections.emptyList());
        queryResp.setResultList(Collections.emptyList());
        queryResp.setSql("select 1");
        when(semanticLayerService.queryByReq(eq(queryReq), any(User.class))).thenReturn(queryResp);

        orchestrator.execute(ctx);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(semanticLayerService).queryByReq(eq(queryReq), userCaptor.capture());
        assertEquals("alice", userCaptor.getValue().getName());
        assertEquals(1L, userCaptor.getValue().getTenantId());
    }

    @Test
    void executeShouldFilterUnauthorizedMidFieldsWhenExpandingDetailGroups() throws Exception {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .scheduleId(100L).operatorUserId(7L).source(ExecutionSource.SCHEDULE)
                .queryConfig("{\"queryType\":\"DETAIL\",\"groups\":[]}")
                .deliveryConfigIds(Collections.emptyList()).build();

        User owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        owner.setTenantId(1L);
        when(userService.getUserById(7L)).thenReturn(owner);

        QueryStructReq queryReq = new QueryStructReq();
        queryReq.setDataSetId(10L);
        queryReq.setQueryType(QueryType.DETAIL);
        queryReq.setGroups(Collections.emptyList());
        when(queryConfigParser.parse(ctx.getQueryConfig(), 10L, null)).thenReturn(queryReq);

        SemanticSchemaResp schema = new SemanticSchemaResp();
        schema.setDimensions(List.of(dim("city", SensitiveLevelEnum.LOW.getCode()),
                dim("phone", SensitiveLevelEnum.MID.getCode())));
        schema.setMetrics(List.of(metric("gmv", SensitiveLevelEnum.HIGH.getCode())));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);
        when(dataSetAuthService.checkDataSetAdminPermission(10L, owner)).thenReturn(false);
        when(sensitiveLevelConfig.isMidLevelRequireAuth()).thenReturn(true);

        AuthorizedResourceResp authResp = new AuthorizedResourceResp();
        authResp.setAuthResList(List.of(new AuthRes(1L, "gmv")));
        when(dataSetAuthService.queryAuthorizedResources(10L, owner)).thenReturn(authResp);

        SemanticQueryResp queryResp = new SemanticQueryResp();
        queryResp.setColumns(Collections.emptyList());
        queryResp.setResultList(Collections.emptyList());
        queryResp.setSql("select city,gmv from t");
        when(semanticLayerService.queryByReq(any(QueryStructReq.class), any(User.class)))
                .thenReturn(queryResp);

        orchestrator.execute(ctx);

        ArgumentCaptor<QueryStructReq> reqCaptor = ArgumentCaptor.forClass(QueryStructReq.class);
        verify(semanticLayerService).queryByReq(reqCaptor.capture(), any(User.class));
        assertEquals(List.of("city", "gmv"), reqCaptor.getValue().getGroups());
    }

    @Test
    void executeShouldFailFastOnCrossTenantOwner() throws Exception {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .scheduleId(100L).operatorUserId(7L).source(ExecutionSource.SCHEDULE)
                .queryConfig("{\"type\":\"sql\"}").deliveryConfigIds(Collections.emptyList())
                .build();

        User owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        owner.setTenantId(2L);
        when(userService.getUserById(7L)).thenReturn(owner);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orchestrator.execute(ctx));
        assertEquals(
                "Report execution failed: Owner tenantId=2 does not match schedule tenantId=1 for ownerId=7",
                ex.getMessage());
        verify(semanticLayerService, never()).queryByReq(any(), any());
        verify(executionMapper).insert(any(ReportExecutionDO.class));
        verify(executionMapper).updateById(any(ReportExecutionDO.class));
    }

    /**
     * P0 联调清单：非法上下文应在编排查询前拦截并给出明确原因（编排器对外统一包装为 RuntimeException）。
     */
    @Test
    void executeShouldRejectNullDatasetId() throws Exception {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(null)
                .scheduleId(100L).operatorUserId(7L).source(ExecutionSource.MANUAL)
                .queryConfig("{\"type\":\"sql\"}").deliveryConfigIds(Collections.emptyList())
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orchestrator.execute(ctx));
        assertEquals("Report execution failed: datasetId is required", ex.getMessage());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        verify(semanticLayerService, never()).queryByReq(any(), any());
        verify(executionMapper).insert(any(ReportExecutionDO.class));
        verify(executionMapper).updateById(any(ReportExecutionDO.class));
    }

    @Test
    void executeShouldRejectMissingRequiredTemplateParameter() throws Exception {
        SemanticTemplateConfig.ConfigParam p = new SemanticTemplateConfig.ConfigParam();
        p.setKey("region");
        p.setName("区域");
        p.setRequired(true);
        p.setType("TEXT");

        SemanticTemplateConfig cfg = new SemanticTemplateConfig();
        cfg.setConfigParams(List.of(p));

        SemanticTemplate template = new SemanticTemplate();
        template.setId(5L);
        template.setStatus(1);
        template.setTemplateConfig(cfg);

        when(templateService.getTemplateById(eq(5L), any(User.class))).thenReturn(template);

        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .templateId(5L).scheduleId(100L).operatorUserId(7L).source(ExecutionSource.MANUAL)
                .resolvedParams(Collections.emptyMap()).queryConfig("{\"type\":\"sql\"}")
                .deliveryConfigIds(Collections.emptyList()).build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orchestrator.execute(ctx));
        assertTrue(ex.getMessage().contains("Parameter validation failed"));
        assertTrue(ex.getMessage().contains("region"));
        assertTrue(ex.getCause() instanceof ParamValidationException);
        ParamValidationException pve = (ParamValidationException) ex.getCause();
        assertTrue(pve.getValidationErrors().stream()
                .anyMatch(err -> err.contains("region") && err.contains("missing")));
        verify(semanticLayerService, never()).queryByReq(any(), any());
        verify(executionMapper).insert(any(ReportExecutionDO.class));
        verify(executionMapper).updateById(any(ReportExecutionDO.class));
    }

    @Test
    void executeShouldRejectInvalidDatabaseTemplateParameter() throws Exception {
        SemanticTemplateConfig.ConfigParam p = new SemanticTemplateConfig.ConfigParam();
        p.setKey("dbId");
        p.setName("库");
        p.setRequired(false);
        p.setType("DATABASE");

        SemanticTemplateConfig cfg = new SemanticTemplateConfig();
        cfg.setConfigParams(List.of(p));

        SemanticTemplate template = new SemanticTemplate();
        template.setId(5L);
        template.setStatus(1);
        template.setTemplateConfig(cfg);

        when(templateService.getTemplateById(eq(5L), any(User.class))).thenReturn(template);

        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .templateId(5L).scheduleId(100L).operatorUserId(7L).source(ExecutionSource.MANUAL)
                .resolvedParams(Map.of("dbId", "not-a-number")).queryConfig("{\"type\":\"sql\"}")
                .deliveryConfigIds(Collections.emptyList()).build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orchestrator.execute(ctx));
        assertTrue(ex.getMessage().contains("dbId"));
        assertTrue(ex.getMessage().contains("database ID"));
        assertTrue(ex.getCause() instanceof ParamValidationException);
        verify(semanticLayerService, never()).queryByReq(any(), any());
        verify(executionMapper).insert(any(ReportExecutionDO.class));
        verify(executionMapper).updateById(any(ReportExecutionDO.class));
    }

    /** P0 §1.2 异常场景 E-1：模板下线后执行应 FAILED 且错误信息可读 */
    @Test
    void executeShouldRecordFailedWhenTemplateOffline() throws Exception {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .templateId(99L).scheduleId(100L).operatorUserId(7L)
                .source(ExecutionSource.SCHEDULE).queryConfig("{\"type\":\"sql\"}")
                .deliveryConfigIds(Collections.emptyList()).build();

        SemanticTemplate offlineTpl = new SemanticTemplate();
        offlineTpl.setId(99L);
        offlineTpl.setStatus(2);
        when(templateService.getTemplateById(eq(99L), any(User.class))).thenReturn(offlineTpl);

        assertThrows(RuntimeException.class, () -> orchestrator.execute(ctx));

        ArgumentCaptor<ReportExecutionDO> failed = ArgumentCaptor.forClass(ReportExecutionDO.class);
        verify(executionMapper).updateById(failed.capture());
        assertEquals("FAILED", failed.getValue().getStatus());
        String err = failed.getValue().getErrorMessage();
        assertTrue(err.contains("offline") || err.contains("skipped"));
        verify(semanticLayerService, never()).queryByReq(any(), any());
    }

    /** P0 §1.2 异常场景 E-2：查询层超时类异常应 FAILED 且 errorMessage 含超时语义 */
    @Test
    void executeShouldRecordFailedWhenQuerySocketTimeout() throws Exception {
        ReportExecutionContext ctx = ReportExecutionContext.builder().tenantId(1L).datasetId(10L)
                .scheduleId(100L).operatorUserId(7L).source(ExecutionSource.SCHEDULE)
                .queryConfig("{\"type\":\"sql\"}").deliveryConfigIds(Collections.emptyList())
                .build();

        User owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        owner.setTenantId(1L);
        when(userService.getUserById(7L)).thenReturn(owner);

        var queryReq = new com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq();
        queryReq.setDataSetId(10L);
        when(queryConfigParser.parse(ctx.getQueryConfig(), 10L, null)).thenReturn(queryReq);
        when(semanticLayerService.queryByReq(eq(queryReq), any(User.class)))
                .thenThrow(new SocketTimeoutException("Read timed out"));

        assertThrows(RuntimeException.class, () -> orchestrator.execute(ctx));

        ArgumentCaptor<ReportExecutionDO> failed = ArgumentCaptor.forClass(ReportExecutionDO.class);
        verify(executionMapper).updateById(failed.capture());
        assertEquals("FAILED", failed.getValue().getStatus());
        assertTrue(failed.getValue().getErrorMessage().toLowerCase().contains("timed"));
    }

    private DimSchemaResp dim(String bizName, Integer sensitiveLevel) {
        DimSchemaResp dim = new DimSchemaResp();
        dim.setName(bizName);
        dim.setBizName(bizName);
        dim.setSensitiveLevel(sensitiveLevel);
        return dim;
    }

    private MetricSchemaResp metric(String bizName, Integer sensitiveLevel) {
        MetricSchemaResp metric = new MetricSchemaResp();
        metric.setName(bizName);
        metric.setBizName(bizName);
        metric.setSensitiveLevel(sensitiveLevel);
        return metric;
    }
}
