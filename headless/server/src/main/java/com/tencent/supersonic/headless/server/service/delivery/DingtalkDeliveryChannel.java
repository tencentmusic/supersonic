package com.tencent.supersonic.headless.server.service.delivery;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DingTalk delivery channel. Sends report notification to DingTalk bot webhook.
 */
@Component
@Slf4j
public class DingtalkDeliveryChannel implements ReportDeliveryChannel {

    private final RestTemplate restTemplate;

    public DingtalkDeliveryChannel() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public DeliveryType getType() {
        return DeliveryType.DINGTALK;
    }

    @Override
    public void deliver(String configJson, DeliveryContext context) throws DeliveryException {
        DingtalkConfig config = parseConfig(configJson);

        try {
            String url = config.getWebhookUrl();

            // Add signature if secret is configured
            if (StringUtils.isNotBlank(config.getSecret())) {
                long timestamp = System.currentTimeMillis();
                String sign = generateSign(timestamp, config.getSecret());
                url = url + "&timestamp=" + timestamp + "&sign="
                        + URLEncoder.encode(sign, StandardCharsets.UTF_8);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = buildPayload(config, context);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DeliveryException(
                        "DingTalk API returned non-2xx status: " + response.getStatusCode());
            }

            // Check DingTalk response
            JSONObject respBody = JSON.parseObject(response.getBody());
            if (respBody != null && respBody.getInteger("errcode") != null
                    && respBody.getInteger("errcode") != 0) {
                throw new DeliveryException("DingTalk API error: " + respBody.getString("errmsg"));
            }

            log.info("DingTalk delivery successful: scheduleId={}", context.getScheduleId());

        } catch (RestClientException e) {
            log.error("DingTalk delivery failed", e);
            throw new DeliveryException("DingTalk request failed: " + e.getMessage(), e);
        } catch (DeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("DingTalk delivery error", e);
            throw new DeliveryException("DingTalk delivery error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPayload(DingtalkConfig config, DeliveryContext context) {
        Map<String, Object> payload = new HashMap<>();

        if ("actionCard".equals(config.getMsgType())) {
            // ActionCard message
            payload.put("msgtype", "actionCard");
            Map<String, Object> actionCard = new HashMap<>();
            actionCard.put("title", StringUtils.isNotBlank(config.getTitle()) ? config.getTitle()
                    : "Report: " + context.getReportName());
            actionCard.put("text", buildMarkdownText(config, context));
            actionCard.put("btnOrientation", "0");

            if (StringUtils.isNotBlank(config.getDownloadUrl())) {
                List<Map<String, String>> btns = new ArrayList<>();
                Map<String, String> btn = new HashMap<>();
                btn.put("title", "Download Report");
                btn.put("actionURL",
                        config.getDownloadUrl() + "?executionId=" + context.getExecutionId());
                btns.add(btn);
                actionCard.put("btns", btns);
            }

            payload.put("actionCard", actionCard);
        } else {
            // Default: Markdown message
            payload.put("msgtype", "markdown");
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", StringUtils.isNotBlank(config.getTitle()) ? config.getTitle()
                    : "Report: " + context.getReportName());
            markdown.put("text", buildMarkdownText(config, context));
            payload.put("markdown", markdown);

            // @mentions
            if (config.getAtMobiles() != null && !config.getAtMobiles().isEmpty()) {
                Map<String, Object> at = new HashMap<>();
                at.put("atMobiles", config.getAtMobiles());
                at.put("isAtAll", false);
                payload.put("at", at);
            }
        }

        return payload;
    }

    private String buildMarkdownText(DingtalkConfig config, DeliveryContext context) {
        String downloadUrl = StringUtils.isNotBlank(config.getDownloadUrl())
                ? config.getDownloadUrl() + "?executionId=" + context.getExecutionId()
                : "";

        // Use custom content if provided, otherwise use default template
        if (StringUtils.isNotBlank(config.getContent())) {
            return TemplateResolver.resolve(config.getContent(), context, downloadUrl);
        }

        // Default template
        return String.format(
                "### 📊 %s\n\n" + "**执行时间**: %s\n" + "**数据量**: %d 条\n\n"
                        + (StringUtils.isNotBlank(downloadUrl) ? "[点击下载报表](" + downloadUrl + ")"
                                : ""),
                context.getReportName(), context.getExecutionTime(), context.getRowCount());
    }

    private String generateSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(signData);
    }

    @Override
    public boolean validateConfig(String configJson) {
        DingtalkConfig config = parseConfig(configJson);
        if (StringUtils.isBlank(config.getWebhookUrl())) {
            throw new IllegalArgumentException("DingTalk webhook URL is required");
        }
        if (!config.getWebhookUrl().contains("oapi.dingtalk.com")) {
            throw new IllegalArgumentException(
                    "Invalid DingTalk webhook URL (must contain oapi.dingtalk.com)");
        }
        return true;
    }

    private DingtalkConfig parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            throw new IllegalArgumentException("DingTalk config is required");
        }
        try {
            return JSON.parseObject(configJson, DingtalkConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid DingTalk config JSON: " + e.getMessage());
        }
    }

    @Data
    public static class DingtalkConfig {
        private String webhookUrl;
        private String secret;
        private String msgType; // markdown, actionCard
        private String title;
        private String content; // User-defined message template with ${variable} placeholders
        private String downloadUrl;
        private List<String> atMobiles; // Phone numbers to @mention
    }
}
