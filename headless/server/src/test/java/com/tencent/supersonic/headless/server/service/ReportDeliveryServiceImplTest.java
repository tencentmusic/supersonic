package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryRateLimiter;
import com.tencent.supersonic.headless.server.service.impl.ReportDeliveryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportDeliveryServiceImplTest {

    private ReportDeliveryConfigMapper configMapper;
    private ReportDeliveryRecordMapper recordMapper;
    private ReportDeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        configMapper = mock(ReportDeliveryConfigMapper.class);
        recordMapper = mock(ReportDeliveryRecordMapper.class);
        DeliveryRateLimiter rateLimiter = mock(DeliveryRateLimiter.class);
        when(rateLimiter.acquire(any())).thenReturn(0D);

        service = new ReportDeliveryServiceImpl(recordMapper, Collections.emptyList(), rateLimiter);
        ReflectionTestUtils.setField(service, "baseMapper", configMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getConfigByIdShouldRejectCrossTenantRecord() {
        TenantContext.setTenantId(2L);
        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(1L);
        config.setTenantId(1L);
        when(configMapper.selectById(1L)).thenReturn(config);

        assertThrows(InvalidPermissionException.class, () -> service.getConfigById(1L));
    }

    @Test
    void deleteConfigShouldRejectCrossTenantRecord() {
        TenantContext.setTenantId(2L);
        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(1L);
        config.setTenantId(1L);
        when(configMapper.selectById(1L)).thenReturn(config);

        assertThrows(InvalidPermissionException.class, () -> service.deleteConfig(1L));
        verify(configMapper, never()).deleteById(1L);
    }

    @Test
    void retryDeliveryShouldRejectCrossTenantRecord() {
        TenantContext.setTenantId(2L);
        ReportDeliveryRecordDO record = new ReportDeliveryRecordDO();
        record.setId(7L);
        record.setTenantId(1L);
        record.setStatus(DeliveryStatus.FAILED.name());
        when(recordMapper.selectById(7L)).thenReturn(record);

        assertThrows(InvalidPermissionException.class, () -> service.retryDelivery(7L));
        verify(configMapper, never()).selectById(any());
    }

    @Test
    void getConfigListShouldAddTenantFilter() {
        TenantContext.setTenantId(3L);

        service.getConfigList(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20));

        verify(configMapper).selectPage(any(), any(QueryWrapper.class));
    }

    @Test
    void getConfigByIdShouldRejectRecordWithoutTenantId() {
        TenantContext.setTenantId(2L);
        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(1L);
        when(configMapper.selectById(1L)).thenReturn(config);

        assertThrows(InvalidPermissionException.class, () -> service.getConfigById(1L));
    }
}
