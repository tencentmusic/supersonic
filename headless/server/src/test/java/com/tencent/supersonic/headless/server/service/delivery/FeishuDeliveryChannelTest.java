package com.tencent.supersonic.headless.server.service.delivery;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeishuDeliveryChannelTest {

    @Test
    @SuppressWarnings("unchecked")
    void interactiveMessageShouldResolveTitleTemplateAndAutoBuildDownloadUrl() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel = new FeishuDeliveryChannel(restTemplate, tenantConfig(),
                downloadProps("test-secret", 3600L));
        ReflectionTestUtils.setField(channel, "apiBaseUrl", "https://s2.example.com/");

        DeliveryContext context = DeliveryContext.builder().scheduleId(8L).executionId(9L)
                .reportName("Report 8").scheduleName("Daily Report").rowCount(1L)
                .executionTime("2025-03-11 10:00:00").fileLocation("/tmp/report.xlsx")
                .outputFormat("XLSX").build();
        String configJson = "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\","
                + "\"msgType\":\"interactive\"," + "\"title\":\"报表推送：${reportName}\","
                + "\"content\":\"📊 ${reportName}\\n报表已生成，共 ${rowCount} 条数据。\\n下载: ${downloadUrl}\"}";

        channel.deliver(configJson, context);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                requestCaptor.capture(), eq(String.class));

        Map<String, Object> payload = requestCaptor.getValue().getBody();
        Map<String, Object> card = (Map<String, Object>) payload.get("card");
        Map<String, Object> header = (Map<String, Object>) card.get("header");
        Map<String, Object> title = (Map<String, Object>) header.get("title");
        assertEquals("报表推送：Report 8", title.get("content"));

        List<Object> elements = (List<Object>) card.get("elements");
        Map<String, Object> infoDiv = (Map<String, Object>) elements.get(0);
        Map<String, Object> text = (Map<String, Object>) infoDiv.get("text");
        String content = (String) text.get("content");
        assertTrue(content.contains("📊 Report 8"));
        assertTrue(content.contains("共 1 条数据"));
        assertFalse(content.contains("${downloadUrl}"));
        assertFalse(content.contains("下载:"));

        Map<String, Object> actionDiv = (Map<String, Object>) elements.get(1);
        List<Object> actions = (List<Object>) actionDiv.get("actions");
        Map<String, Object> button = (Map<String, Object>) actions.get(0);
        Map<String, Object> value = (Map<String, Object>) button.get("value");
        assertEquals("report_download", value.get("action"));
        assertEquals(8L, ((Number) value.get("scheduleId")).longValue());
        assertEquals(9L, ((Number) value.get("executionId")).longValue());
        assertNull(value.get("downloadUrl"));
        assertNull(button.get("url"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void interactiveMessageShouldDropDownloadUrlLineWhenNoFileExists() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel = new FeishuDeliveryChannel(restTemplate, tenantConfig(),
                downloadProps("test-secret", 604800L));
        ReflectionTestUtils.setField(channel, "apiBaseUrl", "https://s2.example.com/");

        DeliveryContext context = DeliveryContext.builder().scheduleId(0L).executionId(0L)
                .reportName("Test Report").rowCount(100L).executionTime("2025-03-11 10:00:00")
                .outputFormat("XLSX").build();
        String configJson = "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\","
                + "\"msgType\":\"interactive\","
                + "\"content\":\"报表已生成，共 ${rowCount} 条数据。\\n下载: ${downloadUrl}\"}";

        channel.deliver(configJson, context);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                requestCaptor.capture(), eq(String.class));

        Map<String, Object> payload = requestCaptor.getValue().getBody();
        Map<String, Object> card = (Map<String, Object>) payload.get("card");
        List<Object> elements = (List<Object>) card.get("elements");
        Map<String, Object> infoDiv = (Map<String, Object>) elements.get(0);
        Map<String, Object> text = (Map<String, Object>) infoDiv.get("text");
        String content = (String) text.get("content");
        assertTrue(content.contains("共 100 条数据"));
        assertFalse(content.contains("${downloadUrl}"));
        assertFalse(content.contains("下载:"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void textMessageShouldNotExposeAutoSignedDownloadUrl() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel = new FeishuDeliveryChannel(restTemplate, tenantConfig(),
                downloadProps("test-secret", 604800L));
        ReflectionTestUtils.setField(channel, "apiBaseUrl", "https://s2.example.com/");

        DeliveryContext context = DeliveryContext.builder().scheduleId(8L).executionId(9L)
                .reportName("Report 8").rowCount(1L).executionTime("2025-03-11 10:00:00")
                .fileLocation("/tmp/report.xlsx").outputFormat("XLSX").build();
        String configJson = "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\","
                + "\"msgType\":\"text\","
                + "\"content\":\"报表已生成，共 ${rowCount} 条数据。\\n下载: ${downloadUrl}\"}";

        channel.deliver(configJson, context);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                requestCaptor.capture(), eq(String.class));

        Map<String, Object> payload = requestCaptor.getValue().getBody();
        Map<String, Object> content = (Map<String, Object>) payload.get("content");
        String text = (String) content.get("text");
        assertTrue(text.contains("共 1 条数据"));
        assertFalse(text.contains("${downloadUrl}"));
        assertFalse(text.contains("/api/public/reportSchedules/8/executions/9:download"));
        assertFalse(text.contains("下载:"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void postMessageShouldKeepOnlyExplicitDownloadUrl() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel =
                new FeishuDeliveryChannel(restTemplate, tenantConfig(), downloadProps("", 604800L));

        DeliveryContext context = DeliveryContext.builder().scheduleId(8L).executionId(9L)
                .reportName("Report 8").rowCount(1L).executionTime("2025-03-11 10:00:00")
                .fileLocation("/tmp/report.xlsx").outputFormat("XLSX").build();
        String configJson = "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\","
                + "\"downloadUrl\":\"https://s2.example.com/custom-download\","
                + "\"content\":\"报表已生成，共 ${rowCount} 条数据。\\n下载: ${downloadUrl}\"}";

        channel.deliver(configJson, context);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                requestCaptor.capture(), eq(String.class));

        Map<String, Object> payload = requestCaptor.getValue().getBody();
        Map<String, Object> content = (Map<String, Object>) payload.get("content");
        Map<String, Object> post = (Map<String, Object>) content.get("post");
        Map<String, Object> zhCn = (Map<String, Object>) post.get("zh_cn");
        List<List<Object>> contentList = (List<List<Object>>) zhCn.get("content");
        Map<String, Object> textNode = (Map<String, Object>) contentList.get(0).get(0);
        String text = (String) textNode.get("text");
        assertTrue(text.contains("下载: https://s2.example.com/custom-download?executionId=9"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void interactiveMessageShouldOmitDownloadButtonWhenSigningSecretMissing() {
        // signing secret 未配置 → 无法生成可用的下载 URL（既没有 /api/public/ 签名参数，
        // 也不能退到 /api/v1/ 这种需要 S2 登录态的端点——飞书客户端打开会 401）。
        // 正确行为是不渲染下载按钮，让部署方显式修配置。
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel =
                new FeishuDeliveryChannel(restTemplate, tenantConfig(), downloadProps("", 3600L));
        ReflectionTestUtils.setField(channel, "apiBaseUrl", "https://s2.example.com/");

        DeliveryContext context = DeliveryContext.builder().scheduleId(8L).executionId(9L)
                .reportName("Report 8").scheduleName("Daily Report").rowCount(1L)
                .executionTime("2025-03-11 10:00:00").fileLocation("/tmp/report.xlsx")
                .outputFormat("XLSX").build();
        String configJson = "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\","
                + "\"msgType\":\"interactive\"}";

        channel.deliver(configJson, context);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                requestCaptor.capture(), eq(String.class));

        Map<String, Object> payload = requestCaptor.getValue().getBody();
        Map<String, Object> card = (Map<String, Object>) payload.get("card");
        List<Object> elements = (List<Object>) card.get("elements");
        // 整张卡片里不应该出现任何 "tag":"action" 节点。
        for (Object el : elements) {
            Map<String, Object> elementMap = (Map<String, Object>) el;
            assertFalse("action".equals(elementMap.get("tag")),
                    "expected no action div when signing secret is missing");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void alertCardShouldIncludeConfiguredDownloadUrlAsUrlButton() {
        // Alert 没有 report owner / schedule 概念，下载按钮必须是 url button 直接跳转，
        // 不能走 report_download callback（callback 在 CardActionHandler 里会因为 scheduleId 缺失拒发）。
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel =
                new FeishuDeliveryChannel(restTemplate, tenantConfig(), downloadProps("", 604800L));
        DeliveryContext context = DeliveryContext.builder().executionId(12L).alertRuleId(3L)
                .alertRuleName("订单异常告警").alertContent("订单量低于阈值").alertSeverity("WARNING")
                .alertedCount(1).totalChecked(20).executionTime("2025-03-11T10:00:00")
                .alertEventIds(List.of(101L)).build();
        String configJson = "{\"webhookUrl\":\"https://open.feishu.cn/open-apis/bot/v2/hook/abc\","
                + "\"downloadUrl\":\"https://s2.example.com/alerts\"}";

        channel.deliver(configJson, context);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://open.feishu.cn/open-apis/bot/v2/hook/abc"),
                requestCaptor.capture(), eq(String.class));

        Map<String, Object> payload = requestCaptor.getValue().getBody();
        Map<String, Object> card = (Map<String, Object>) payload.get("card");
        List<Object> elements = (List<Object>) card.get("elements");
        Map<String, Object> actionDiv = (Map<String, Object>) elements.get(elements.size() - 1);
        List<Object> actions = (List<Object>) actionDiv.get("actions");
        Map<String, Object> downloadButton = (Map<String, Object>) actions.get(2);

        assertEquals("下载报表", ((Map<String, Object>) downloadButton.get("text")).get("content"));
        // URL button → button.url is set, value is absent (不走 callback)
        assertEquals("https://s2.example.com/alerts?executionId=12", downloadButton.get("url"));
        assertNull(downloadButton.get("value"));
    }

    private TenantConfig tenantConfig() {
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setDefaultTenantId(1L);
        return tenantConfig;
    }

    private ReportDownloadProperties downloadProps(String secret, long ttlSeconds) {
        ReportDownloadProperties p = new ReportDownloadProperties();
        org.springframework.test.util.ReflectionTestUtils.setField(p, "signingSecret", secret);
        org.springframework.test.util.ReflectionTestUtils.setField(p, "tokenTtlSeconds",
                ttlSeconds);
        return p;
    }
}
