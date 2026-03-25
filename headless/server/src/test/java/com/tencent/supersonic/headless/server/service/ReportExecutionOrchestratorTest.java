package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.ExecutionSource;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import com.tencent.supersonic.headless.server.service.impl.QueryConfigParser;
import com.tencent.supersonic.headless.server.service.impl.ReportExecutionOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
