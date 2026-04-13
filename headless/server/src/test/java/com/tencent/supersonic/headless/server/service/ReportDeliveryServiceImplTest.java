package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.pojo.DeliveryStatus;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryException;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryRateLimiter;
import com.tencent.supersonic.headless.server.service.delivery.FeishuDeliveryChannel;
import com.tencent.supersonic.headless.server.service.delivery.ReportDeliveryChannel;
import com.tencent.supersonic.headless.server.service.impl.ReportDeliveryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    void testDeliveryShouldInvokeFeishuChannel() {
        TenantContext.setTenantId(1L);

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(2L);
        config.setTenantId(1L);
        config.setName("Feishu Test");
        config.setDeliveryType(DeliveryType.FEISHU.name());
        config.setDeliveryConfig(
                "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\",\"msgType\":\"post\"}");
        when(configMapper.selectById(2L)).thenReturn(config);

        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel feishuChannel = new FeishuDeliveryChannel(restTemplate);
        DeliveryRateLimiter rateLimiter = mock(DeliveryRateLimiter.class);
        when(rateLimiter.acquire(any())).thenReturn(0D);

        ReportDeliveryServiceImpl feishuService = new ReportDeliveryServiceImpl(recordMapper,
                List.<ReportDeliveryChannel>of(feishuChannel), rateLimiter);
        ReflectionTestUtils.setField(feishuService, "baseMapper", configMapper);

        ReportDeliveryRecordDO returned = feishuService.testDelivery(2L);

        verify(restTemplate, times(1)).postForEntity(
                eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"), any(), eq(String.class));

        // Verify the new persistence path. The captor holds the live record reference,
        // so status reads the final state — assert only fields that testDelivery sets
        // once and never mutates (tenant/config/scheduleId/deliveryKey). The sentinel
        // scheduleId=0 and TEST_ deliveryKey are what keep test rows out of statistics.
        ArgumentCaptor<ReportDeliveryRecordDO> insertCaptor =
                ArgumentCaptor.forClass(ReportDeliveryRecordDO.class);
        verify(recordMapper, times(1)).insert(insertCaptor.capture());
        ReportDeliveryRecordDO inserted = insertCaptor.getValue();
        assertEquals(Long.valueOf(1L), inserted.getTenantId());
        assertEquals(Long.valueOf(2L), inserted.getConfigId());
        assertEquals(DeliveryType.FEISHU.name(), inserted.getDeliveryType());
        assertEquals(Long.valueOf(0L), inserted.getScheduleId());
        assertTrue(inserted.getDeliveryKey().startsWith("TEST_"));

        verify(recordMapper, atLeast(2)).updateById(any(ReportDeliveryRecordDO.class));
        assertNotNull(returned);
        assertEquals(DeliveryStatus.SUCCESS.name(), returned.getStatus());
    }

    @Test
    void testDeliveryShouldAcceptFeishuStatusCodeSuccessResponse() {
        TenantContext.setTenantId(1L);

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(2L);
        config.setTenantId(1L);
        config.setDeliveryType(DeliveryType.FEISHU.name());
        config.setDeliveryConfig(
                "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\",\"msgType\":\"post\"}");
        when(configMapper.selectById(2L)).thenReturn(config);

        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"StatusCode\":0,\"StatusMessage\":\"success\"}",
                        HttpStatus.OK));

        FeishuDeliveryChannel feishuChannel = new FeishuDeliveryChannel(restTemplate);
        DeliveryRateLimiter rateLimiter = mock(DeliveryRateLimiter.class);
        when(rateLimiter.acquire(any())).thenReturn(0D);

        ReportDeliveryServiceImpl feishuService = new ReportDeliveryServiceImpl(recordMapper,
                List.<ReportDeliveryChannel>of(feishuChannel), rateLimiter);
        ReflectionTestUtils.setField(feishuService, "baseMapper", configMapper);

        ReportDeliveryRecordDO returned = feishuService.testDelivery(2L);

        assertNotNull(returned);
        assertEquals(DeliveryStatus.SUCCESS.name(), returned.getStatus());
    }

    @Test
    void testDeliveryShouldRejectFeishuStatusCodeErrorResponse() {
        TenantContext.setTenantId(1L);

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(2L);
        config.setTenantId(1L);
        config.setDeliveryType(DeliveryType.FEISHU.name());
        config.setDeliveryConfig(
                "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\",\"msgType\":\"post\"}");
        when(configMapper.selectById(2L)).thenReturn(config);

        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        "{\"StatusCode\":19024,\"StatusMessage\":\"Key Words Not Found\"}",
                        HttpStatus.OK));

        FeishuDeliveryChannel feishuChannel = new FeishuDeliveryChannel(restTemplate);
        DeliveryRateLimiter rateLimiter = mock(DeliveryRateLimiter.class);
        when(rateLimiter.acquire(any())).thenReturn(0D);

        ReportDeliveryServiceImpl feishuService = new ReportDeliveryServiceImpl(recordMapper,
                List.<ReportDeliveryChannel>of(feishuChannel), rateLimiter);
        ReflectionTestUtils.setField(feishuService, "baseMapper", configMapper);

        DeliveryException ex =
                assertThrows(DeliveryException.class, () -> feishuService.testDelivery(2L));
        assertTrue(ex.getMessage().contains("Key Words Not Found"));
    }

    @Test
    void testDeliveryShouldPersistFailedStateWhenChannelThrows() {
        TenantContext.setTenantId(1L);

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(2L);
        config.setTenantId(1L);
        config.setDeliveryType(DeliveryType.FEISHU.name());
        config.setDeliveryConfig(
                "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\"}");
        when(configMapper.selectById(2L)).thenReturn(config);

        ReportDeliveryChannel failingChannel = mock(ReportDeliveryChannel.class);
        when(failingChannel.getType()).thenReturn(DeliveryType.FEISHU);
        org.mockito.Mockito.doThrow(new DeliveryException("webhook boom")).when(failingChannel)
                .deliver(any(String.class), any());

        DeliveryRateLimiter rateLimiter = mock(DeliveryRateLimiter.class);
        when(rateLimiter.acquire(any())).thenReturn(0D);

        ReportDeliveryServiceImpl failingService = new ReportDeliveryServiceImpl(recordMapper,
                List.<ReportDeliveryChannel>of(failingChannel), rateLimiter);
        ReflectionTestUtils.setField(failingService, "baseMapper", configMapper);

        assertThrows(DeliveryException.class, () -> failingService.testDelivery(2L));

        // On failure the PENDING row is inserted and a FAILED update must still be
        // persisted (verified by the last updateById capture). @Transactional
        // noRollbackFor=RuntimeException keeps this write alive under real JDBC.
        verify(recordMapper, times(1)).insert(any(ReportDeliveryRecordDO.class));
        ArgumentCaptor<ReportDeliveryRecordDO> updateCaptor =
                ArgumentCaptor.forClass(ReportDeliveryRecordDO.class);
        verify(recordMapper, atLeast(1)).updateById(updateCaptor.capture());
        ReportDeliveryRecordDO last =
                updateCaptor.getAllValues().get(updateCaptor.getAllValues().size() - 1);
        assertEquals(DeliveryStatus.FAILED.name(), last.getStatus());
        assertTrue(
                last.getErrorMessage() != null && last.getErrorMessage().contains("webhook boom"));
    }

    @Test
    void testDeliveryShouldThrowWhenFeishuChannelNotRegistered() {
        TenantContext.setTenantId(1L);

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(2L);
        config.setTenantId(1L);
        config.setName("Feishu Test");
        config.setDeliveryType(DeliveryType.FEISHU.name());
        config.setDeliveryConfig(
                "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\"}");
        when(configMapper.selectById(2L)).thenReturn(config);

        DeliveryException ex =
                assertThrows(DeliveryException.class, () -> service.testDelivery(2L));
        assertEquals("No channel implementation for type: FEISHU", ex.getMessage());
    }
}
