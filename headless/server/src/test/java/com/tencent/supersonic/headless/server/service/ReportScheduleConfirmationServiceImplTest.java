package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleConfirmationMapper;
import com.tencent.supersonic.headless.server.service.impl.ReportScheduleConfirmationServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportScheduleConfirmationServiceImplTest {

    private ReportScheduleConfirmationMapper mapper;
    private ReportScheduleConfirmationServiceImpl service;

    @BeforeAll
    static void initMybatisTableInfo() {
        // The impl's cancelWrapper uses `.lambda()` chains on ReportScheduleConfirmationDO.
        // MyBatis-Plus needs the TableInfo registered before it can resolve lambda references;
        // a real SqlSessionFactory would do this at startup. In a mock-based unit test we have
        // to populate it ourselves.
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ReportScheduleConfirmationDO.class);
    }

    @BeforeEach
    void setUp() {
        mapper = mock(ReportScheduleConfirmationMapper.class);
        service = new ReportScheduleConfirmationServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPendingShouldOverrideTenantIdFromContext() {
        // Ambient tenant is 42; a malicious caller tries to forge tenantId via the Req... but the
        // DTO no longer carries that field, so only the context value reaches the DB.
        TenantContext.setTenantId(42L);

        ReportScheduleConfirmationReq req = new ReportScheduleConfirmationReq();
        req.setUserId(7L);
        req.setChatId(100);
        req.setActionType("CREATE");
        req.setPayloadJson("{}");
        req.setExpireAt(new Date(System.currentTimeMillis() + 60_000));

        ReportScheduleConfirmationResp resp = service.createPending(req);

        ArgumentCaptor<ReportScheduleConfirmationDO> captor =
                ArgumentCaptor.forClass(ReportScheduleConfirmationDO.class);
        verify(mapper).insert(captor.capture());
        ReportScheduleConfirmationDO persisted = captor.getValue();

        assertEquals(Long.valueOf(42L), persisted.getTenantId());
        assertNotNull(persisted.getCreatedAt());
        assertEquals("PENDING", persisted.getStatus());
        assertNotNull(persisted.getConfirmToken());
        assertTrue(!persisted.getConfirmToken().isBlank());
        assertNotNull(resp);
    }

    @Test
    void createPendingShouldStampCreatedAtWithServerClock() {
        TenantContext.setTenantId(1L);
        long before = System.currentTimeMillis();

        ReportScheduleConfirmationReq req = new ReportScheduleConfirmationReq();
        req.setUserId(1L);
        req.setChatId(1);
        req.setActionType("CREATE");
        service.createPending(req);

        long after = System.currentTimeMillis();
        ArgumentCaptor<ReportScheduleConfirmationDO> captor =
                ArgumentCaptor.forClass(ReportScheduleConfirmationDO.class);
        verify(mapper).insert(captor.capture());
        long stamped = captor.getValue().getCreatedAt().getTime();
        assertTrue(stamped >= before && stamped <= after, "createdAt should be stamped between "
                + before + " and " + after + " but was " + stamped);
    }

    @Test
    void createPendingShouldThrowWhenTenantContextMissing() {
        // No TenantContext set — must refuse rather than silently persist with null.
        ReportScheduleConfirmationReq req = new ReportScheduleConfirmationReq();
        req.setUserId(1L);
        req.setChatId(1);

        assertThrows(InvalidPermissionException.class, () -> service.createPending(req));
        verify(mapper, org.mockito.Mockito.never()).insert(any(ReportScheduleConfirmationDO.class));
        verify(mapper, org.mockito.Mockito.never()).update(any(), any(UpdateWrapper.class));
    }
}
