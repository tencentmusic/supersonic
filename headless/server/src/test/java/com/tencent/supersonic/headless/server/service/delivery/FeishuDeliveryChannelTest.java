package com.tencent.supersonic.headless.server.service.delivery;

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

        FeishuDeliveryChannel channel = new FeishuDeliveryChannel(restTemplate);
        ReflectionTestUtils.setField(channel, "apiBaseUrl", "https://s2.example.com/");
        ReflectionTestUtils.setField(channel, "downloadSigningSecret", "test-secret");
        ReflectionTestUtils.setField(channel, "downloadTokenTtlSeconds", 604800L);

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
        String expectedUrlPrefix =
                "https://s2.example.com/api/public/reportSchedules/8/executions/9:download";
        assertTrue(content.contains("📊 Report 8"));
        assertTrue(content.contains("共 1 条数据"));
        assertTrue(content.contains("下载: " + expectedUrlPrefix));
        assertTrue(content.contains("expires="));
        assertTrue(content.contains("token="));

        Map<String, Object> actionDiv = (Map<String, Object>) elements.get(1);
        List<Object> actions = (List<Object>) actionDiv.get("actions");
        Map<String, Object> button = (Map<String, Object>) actions.get(0);
        assertTrue(((String) button.get("url")).startsWith(expectedUrlPrefix));
        assertTrue(((String) button.get("url")).contains("expires="));
        assertTrue(((String) button.get("url")).contains("token="));
    }

    @Test
    @SuppressWarnings("unchecked")
    void interactiveMessageShouldDropDownloadUrlLineWhenNoFileExists() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":0,\"msg\":\"ok\"}", HttpStatus.OK));

        FeishuDeliveryChannel channel = new FeishuDeliveryChannel(restTemplate);
        ReflectionTestUtils.setField(channel, "apiBaseUrl", "https://s2.example.com/");
        ReflectionTestUtils.setField(channel, "downloadSigningSecret", "test-secret");
        ReflectionTestUtils.setField(channel, "downloadTokenTtlSeconds", 604800L);

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
}
