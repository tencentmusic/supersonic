package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.api.service.ReportScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardActionHandlerTest {

    @Mock
    private SuperSonicApiClient apiClient;
    @Mock
    private FeishuMessageSender messageSender;
    @Mock
    private ReportScheduleService reportScheduleService;

    private FeishuProperties feishuProperties;
    private CardActionHandler handler;
    private User owner;

    @BeforeEach
    void setUp() {
        feishuProperties = new FeishuProperties();
        feishuProperties.setApiBaseUrl("https://s2.example.com");
        handler = new CardActionHandler(apiClient, messageSender, reportScheduleService,
                feishuProperties);

        owner = new User();
        owner.setId(7L);
        owner.setName("alice");
        owner.setTenantId(1L);
    }

    @Test
    void reportDownloadShouldSendDownloadCardWhenUserHasPermission() {
        Map<String, Object> actionValue = new HashMap<>();
        actionValue.put("action", "report_download");
        actionValue.put("downloadUrl",
                "https://s2.example.com/api/public/reportSchedules/8/executions/9:download?expires=1&token=abc");
        actionValue.put("scheduleId", 8L);
        actionValue.put("executionId", 9L);
        when(reportScheduleService.getScheduleById(8L, owner)).thenReturn(new ReportScheduleResp());

        handler.handle(actionValue, owner, "ou_alice");

        ArgumentCaptor<Map<String, Object>> cardCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messageSender).sendCard(anyString(), cardCaptor.capture());
        verify(reportScheduleService, times(1)).getScheduleById(8L, owner);

        Map<String, Object> card = cardCaptor.getValue();
        Map<String, Object> header = (Map<String, Object>) card.get("header");
        Map<String, Object> title = (Map<String, Object>) header.get("title");
        assertEquals("报表下载", title.get("content"));
    }

    @Test
    void reportDownloadShouldRefuseWhenUserLacksPermission() {
        Map<String, Object> actionValue = new HashMap<>();
        actionValue.put("action", "report_download");
        actionValue.put("downloadUrl",
                "https://s2.example.com/api/public/reportSchedules/8/executions/9:download?expires=1&token=abc");
        actionValue.put("scheduleId", 8L);
        actionValue.put("executionId", 9L);
        when(reportScheduleService.getScheduleById(8L, owner))
                .thenThrow(new InvalidPermissionException("只有调度创建人或管理员才能查看此调度"));

        handler.handle(actionValue, owner, "ou_alice");

        ArgumentCaptor<Map<String, Object>> cardCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messageSender).sendCard(anyString(), cardCaptor.capture());
        Map<String, Object> card = cardCaptor.getValue();
        Map<String, Object> header = (Map<String, Object>) card.get("header");
        Map<String, Object> title = (Map<String, Object>) header.get("title");
        assertEquals("无权限下载", title.get("content"));
        // 拒绝卡片里不应该包含原 downloadUrl
        List<Object> elements = (List<Object>) card.get("elements");
        boolean containsUrl = elements.stream().anyMatch(
                el -> el.toString().contains("api/public/reportSchedules/8/executions/9:download"));
        assertFalse(containsUrl, "deny card must not leak the underlying signed download URL");
    }

    @Test
    void reportDownloadShouldRejectNonWhitelistedUrl() {
        Map<String, Object> actionValue = new HashMap<>();
        actionValue.put("action", "report_download");
        actionValue.put("downloadUrl", "https://evil.example.org/steal");
        actionValue.put("scheduleId", 8L);
        actionValue.put("executionId", 9L);

        handler.handle(actionValue, owner, "ou_alice");

        ArgumentCaptor<Map<String, Object>> cardCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messageSender).sendCard(anyString(), cardCaptor.capture());
        verify(reportScheduleService, never()).getScheduleById(any(), any());

        Map<String, Object> header = (Map<String, Object>) cardCaptor.getValue().get("header");
        Map<String, Object> title = (Map<String, Object>) header.get("title");
        assertEquals("下载地址无效", title.get("content"));
    }

    @Test
    void reportDownloadShouldRefuseWhenScheduleIdMissing() {
        Map<String, Object> actionValue = new HashMap<>();
        actionValue.put("action", "report_download");
        actionValue.put("downloadUrl",
                "https://s2.example.com/api/public/reportSchedules/8/executions/9:download");

        handler.handle(actionValue, owner, "ou_alice");

        ArgumentCaptor<Map<String, Object>> cardCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messageSender).sendCard(anyString(), cardCaptor.capture());
        verify(reportScheduleService, never()).getScheduleById(any(), any());

        Map<String, Object> header = (Map<String, Object>) cardCaptor.getValue().get("header");
        Map<String, Object> title = (Map<String, Object>) header.get("title");
        assertEquals("暂不能下载", title.get("content"));
    }

    @Test
    void alertConfirmShouldStillWorkAndNotTouchScheduleService() {
        Map<String, Object> actionValue = new HashMap<>();
        actionValue.put("action", "alert_confirm");
        actionValue.put("ruleId", 3L);
        actionValue.put("eventIds", "101,102");

        handler.handle(actionValue, owner, "ou_alice");

        verify(apiClient, times(2)).transitionAlertEvent(any(Long.class), any(), any(), any(),
                any());
        verify(reportScheduleService, never()).getScheduleById(any(), any());
        verify(messageSender).sendCard(anyString(), any());
    }
}
