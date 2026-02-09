package com.tencent.supersonic.headless.server.service.delivery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WeChatWork (企业微信) delivery channel. Sends report notification to WeChatWork bot webhook.
 */
@Component
@Slf4j
public class WeChatWorkDeliveryChannel implements ReportDeliveryChannel {

    private final RestTemplate restTemplate;

    public WeChatWorkDeliveryChannel() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public DeliveryType getType() {
        return DeliveryType.WECHAT_WORK;
    }

    @Override
    public void deliver(String configJson, DeliveryContext context) throws DeliveryException {
        WeChatWorkConfig config = parseConfig(configJson);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = buildPayload(config, context);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(config.getWebhookUrl(), request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DeliveryException(
                        "WeChatWork API returned non-2xx status: " + response.getStatusCode());
            }

            // Check WeChatWork response
            JSONObject respBody = JSON.parseObject(response.getBody());
            if (respBody != null && respBody.getInteger("errcode") != null
                    && respBody.getInteger("errcode") != 0) {
                throw new DeliveryException(
                        "WeChatWork API error: " + respBody.getString("errmsg"));
            }

            log.info("WeChatWork delivery successful: scheduleId={}", context.getScheduleId());

        } catch (RestClientException e) {
            log.error("WeChatWork delivery failed", e);
            throw new DeliveryException("WeChatWork request failed: " + e.getMessage(), e);
        } catch (DeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeChatWork delivery error", e);
            throw new DeliveryException("WeChatWork delivery error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPayload(WeChatWorkConfig config, DeliveryContext context) {
        Map<String, Object> payload = new HashMap<>();

        if ("news".equals(config.getMsgType())) {
            // News (图文) message
            payload.put("msgtype", "news");
            Map<String, Object> news = new HashMap<>();
            List<Map<String, Object>> articles = new ArrayList<>();

            Map<String, Object> article = new HashMap<>();
            article.put("title", StringUtils.isNotBlank(config.getTitle()) ? config.getTitle()
                    : "Report: " + context.getReportName());
            article.put("description", buildDescription(config, context));

            if (StringUtils.isNotBlank(config.getDownloadUrl())) {
                article.put("url",
                        config.getDownloadUrl() + "?executionId=" + context.getExecutionId());
            }

            if (StringUtils.isNotBlank(config.getPicUrl())) {
                article.put("picurl", config.getPicUrl());
            }

            articles.add(article);
            news.put("articles", articles);
            payload.put("news", news);

        } else if ("text".equals(config.getMsgType())) {
            // Text message
            payload.put("msgtype", "text");
            Map<String, Object> text = new HashMap<>();
            text.put("content", buildTextContent(config, context));

            if (config.getMentionedList() != null && !config.getMentionedList().isEmpty()) {
                text.put("mentioned_list", config.getMentionedList());
            }
            if (config.getMentionedMobileList() != null
                    && !config.getMentionedMobileList().isEmpty()) {
                text.put("mentioned_mobile_list", config.getMentionedMobileList());
            }

            payload.put("text", text);

        } else {
            // Default: Markdown message
            payload.put("msgtype", "markdown");
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("content", buildMarkdownContent(config, context));
            payload.put("markdown", markdown);
        }

        return payload;
    }

    private String buildDownloadUrl(WeChatWorkConfig config, DeliveryContext context) {
        return StringUtils.isNotBlank(config.getDownloadUrl())
                ? config.getDownloadUrl() + "?executionId=" + context.getExecutionId()
                : "";
    }

    private String buildDescription(WeChatWorkConfig config, DeliveryContext context) {
        String downloadUrl = buildDownloadUrl(config, context);

        // Use custom content if provided
        String template = config.getEffectiveContent();
        if (StringUtils.isNotBlank(template)) {
            return TemplateResolver.resolve(template, context, downloadUrl);
        }

        return String.format("%s - %s", context.getReportName(), context.getExecutionTime());
    }

    private String buildTextContent(WeChatWorkConfig config, DeliveryContext context) {
        String downloadUrl = buildDownloadUrl(config, context);

        // Use custom content if provided
        String template = config.getEffectiveContent();
        if (StringUtils.isNotBlank(template)) {
            return TemplateResolver.resolve(template, context, downloadUrl);
        }

        // Default template
        StringBuilder sb = new StringBuilder();
        sb.append("【报表推送】").append(context.getReportName()).append("\n");
        sb.append("执行时间: ").append(context.getExecutionTime()).append("\n");
        sb.append("数据量: ").append(context.getRowCount()).append(" 条");
        if (StringUtils.isNotBlank(downloadUrl)) {
            sb.append("\n下载链接: ").append(downloadUrl);
        }
        return sb.toString();
    }

    private String buildMarkdownContent(WeChatWorkConfig config, DeliveryContext context) {
        String downloadUrl = buildDownloadUrl(config, context);

        // Use custom content if provided
        String template = config.getEffectiveContent();
        if (StringUtils.isNotBlank(template)) {
            return TemplateResolver.resolve(template, context, downloadUrl);
        }

        // Default template
        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 <font color=\"info\">").append(context.getReportName())
                .append("</font>\n\n");
        sb.append("> **执行时间**: ").append(context.getExecutionTime()).append("\n");
        sb.append("> **数据量**: ").append(context.getRowCount()).append(" 条\n\n");

        if (StringUtils.isNotBlank(downloadUrl)) {
            sb.append("[点击下载报表](").append(downloadUrl).append(")");
        }

        return sb.toString();
    }

    @Override
    public boolean validateConfig(String configJson) {
        WeChatWorkConfig config = parseConfig(configJson);
        if (StringUtils.isBlank(config.getWebhookUrl())) {
            throw new IllegalArgumentException("WeChatWork webhook URL is required");
        }
        if (!config.getWebhookUrl().contains("qyapi.weixin.qq.com")) {
            throw new IllegalArgumentException(
                    "Invalid WeChatWork webhook URL (must contain qyapi.weixin.qq.com)");
        }
        return true;
    }

    private WeChatWorkConfig parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            throw new IllegalArgumentException("WeChatWork config is required");
        }
        try {
            return JSON.parseObject(configJson, WeChatWorkConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WeChatWork config JSON: " + e.getMessage());
        }
    }

    @Data
    public static class WeChatWorkConfig {
        private String webhookUrl;
        private String msgType; // text, markdown, news
        private String title;
        private String content; // User-defined message template with ${variable} placeholders
        private String contentTemplate; // Deprecated: use content instead (kept for backward
                                        // compat)
        private String downloadUrl;
        private String picUrl; // For news type
        private List<String> mentionedList; // User IDs to @mention
        private List<String> mentionedMobileList; // Phone numbers to @mention

        /**
         * Get the effective content template, preferring 'content' over 'contentTemplate'.
         */
        public String getEffectiveContent() {
            return StringUtils.isNotBlank(content) ? content : contentTemplate;
        }
    }
}
