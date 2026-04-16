package com.tencent.supersonic.headless.server.service.delivery;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.api.util.ReportDownloadTokenUtils;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Feishu (Lark) delivery channel. Sends report notification to Feishu bot webhook.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FeishuDeliveryChannel implements ReportDeliveryChannel {

    /** Feishu custom bot webhook hard limit: request body must be ≤ 20 KB. */
    private static final int MAX_PAYLOAD_BYTES = 20 * 1024;

    private final RestTemplate restTemplate;
    private final TenantConfig tenantConfig;
    private final ReportDownloadProperties downloadProperties;

    @Value("${s2.feishu.api-base-url:}")
    private String apiBaseUrl;

    @Override
    public DeliveryType getType() {
        return DeliveryType.FEISHU;
    }

    @Override
    public void deliver(String configJson, DeliveryContext context) throws DeliveryException {
        FeishuConfig config = parseConfig(configJson);
        validateWebhookUrl(config.getWebhookUrl());
        log.info("Feishu deliver entry: scheduleId={}, webhookHost={}", context.getScheduleId(),
                URI.create(config.getWebhookUrl()).getHost());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = buildPayload(config, context);

            // Add signature if secret is configured
            if (StringUtils.isNotBlank(config.getSecret())) {
                long timestamp = System.currentTimeMillis() / 1000;
                String sign = generateSign(timestamp, config.getSecret());
                payload.put("timestamp", String.valueOf(timestamp));
                payload.put("sign", sign);
            }

            int payloadBytes = JSON.toJSONString(payload).getBytes(StandardCharsets.UTF_8).length;
            if (payloadBytes > MAX_PAYLOAD_BYTES) {
                throw new DeliveryException("Feishu payload exceeds 20KB limit (" + payloadBytes
                        + " bytes). Reduce report content or alert detail.", false);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            log.info("Feishu sending request: scheduleId={}, payloadBytes={}",
                    context.getScheduleId(), payloadBytes);
            long t0 = System.currentTimeMillis();
            ResponseEntity<String> response =
                    restTemplate.postForEntity(config.getWebhookUrl(), request, String.class);
            log.info("Feishu received response: scheduleId={}, status={}, elapsedMs={}",
                    context.getScheduleId(), response.getStatusCode(),
                    System.currentTimeMillis() - t0);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DeliveryException(
                        "Feishu API returned non-2xx status: " + response.getStatusCode());
            }

            // Check Feishu response
            JSONObject respBody = JSON.parseObject(response.getBody());
            validateFeishuResponse(respBody);

            log.info("Feishu delivery successful: scheduleId={}", context.getScheduleId());

        } catch (RestClientException e) {
            log.error("Feishu delivery failed", e);
            throw new DeliveryException("Feishu request failed: " + e.getMessage(), e);
        } catch (DeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Feishu delivery error", e);
            throw new DeliveryException("Feishu delivery error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPayload(FeishuConfig config, DeliveryContext context) {
        Map<String, Object> payload = new HashMap<>();

        if (context.getAlertContent() != null) {
            // Alert delivery: always use interactive card
            payload.put("msg_type", "interactive");
            payload.put("card", buildAlertCard(config, context));
        } else if ("interactive".equals(config.getMsgType())) {
            // Interactive card message
            payload.put("msg_type", "interactive");
            payload.put("card", buildCard(config, context));
        } else if ("text".equals(config.getMsgType())) {
            // Plain text — cheapest format, useful for connectivity tests
            payload.put("msg_type", "text");
            payload.put("content", buildTextContent(config, context));
        } else {
            // Default: post message (rich text)
            payload.put("msg_type", "post");
            payload.put("content", buildPostContent(config, context));
        }

        return payload;
    }

    private Map<String, Object> buildAlertCard(FeishuConfig config, DeliveryContext context) {
        Map<String, Object> card = new HashMap<>();
        String downloadUrl = buildDownloadUrl(config, context);

        // Header
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> title = new HashMap<>();
        title.put("tag", "plain_text");
        title.put("content", "🚨 数据告警 — " + LocalDate.now(ZoneId.of("Asia/Shanghai")).toString());
        header.put("title", title);
        header.put("template", "CRITICAL".equals(context.getAlertSeverity()) ? "red" : "orange");
        card.put("header", header);

        // Elements
        List<Object> elements = new ArrayList<>();

        // 1. Alert summary info
        Map<String, Object> infoDiv = new HashMap<>();
        infoDiv.put("tag", "div");
        Map<String, Object> infoText = new HashMap<>();
        infoText.put("tag", "lark_md");
        String ruleName = context.getAlertRuleName() != null ? context.getAlertRuleName() : "告警规则";
        int totalChecked = context.getTotalChecked() != null ? context.getTotalChecked() : 0;
        int alertedCount = context.getAlertedCount() != null ? context.getAlertedCount() : 0;
        String executionTime = context.getExecutionTime() != null ? context.getExecutionTime() : "";
        infoText.put("content",
                String.format("**规则**: %s\n**检查时间**: %s\n**检查行数**: %d | **告警行数**: %d", ruleName,
                        executionTime, totalChecked, alertedCount));
        infoDiv.put("text", infoText);
        elements.add(infoDiv);

        // 2. Separator
        Map<String, Object> hr1 = new HashMap<>();
        hr1.put("tag", "hr");
        elements.add(hr1);

        // 3. Alert content (pre-rendered, already escaped)
        Map<String, Object> contentDiv = new HashMap<>();
        contentDiv.put("tag", "div");
        Map<String, Object> contentText = new HashMap<>();
        contentText.put("tag", "lark_md");
        contentText.put("content", context.getAlertContent());
        contentDiv.put("text", contentText);
        elements.add(contentDiv);

        // 4. Separator
        Map<String, Object> hr2 = new HashMap<>();
        hr2.put("tag", "hr");
        elements.add(hr2);

        // 5. Note
        Map<String, Object> note = new HashMap<>();
        note.put("tag", "note");
        List<Object> noteElements = new ArrayList<>();
        Map<String, Object> noteText = new HashMap<>();
        noteText.put("tag", "plain_text");
        noteText.put("content", "静默期内同一告警不重复发送");
        noteElements.add(noteText);
        note.put("elements", noteElements);
        elements.add(note);

        // Add action buttons if alert event IDs or a download URL are available.
        List<Object> actions = new ArrayList<>();
        if (context.getAlertEventIds() != null && !context.getAlertEventIds().isEmpty()) {
            // "我已知悉" button — transitions events to CONFIRMED
            Map<String, Object> confirmBtn = new HashMap<>();
            confirmBtn.put("tag", "button");
            confirmBtn.put("type", "primary");
            confirmBtn.put("text", Map.of("tag", "plain_text", "content", "我已知悉"));
            confirmBtn.put("value",
                    Map.of("action", "alert_confirm", "ruleId", context.getAlertRuleId(),
                            "eventIds", context.getAlertEventIds().stream().map(String::valueOf)
                                    .collect(Collectors.joining(","))));
            actions.add(confirmBtn);

            // "我来处理" button — transitions events to ASSIGNED (with current user as assignee)
            Map<String, Object> assignBtn = new HashMap<>();
            assignBtn.put("tag", "button");
            assignBtn.put("type", "default");
            assignBtn.put("text", Map.of("tag", "plain_text", "content", "我来处理"));
            assignBtn.put("value",
                    Map.of("action", "alert_assign", "ruleId", context.getAlertRuleId(), "eventIds",
                            context.getAlertEventIds().stream().map(String::valueOf)
                                    .collect(Collectors.joining(","))));
            actions.add(assignBtn);
        }
        if (StringUtils.isNotBlank(downloadUrl)) {
            // Alert 按钮直接跳 config.downloadUrl（告警详情页之类），不走 callback —— 因为 alert 没有
            // 报表 owner 概念，对 callback 做权限校验没有意义；也避免 callback 里 sendReportDownload
            // 发现 scheduleId==null 时给用户发"暂不能下载"卡。
            actions.add(
                    buildUrlDownloadButton(downloadUrl, actions.isEmpty() ? "primary" : "default"));
        }
        if (!actions.isEmpty()) {
            Map<String, Object> actionDiv = new HashMap<>();
            actionDiv.put("tag", "action");
            actionDiv.put("actions", actions);
            elements.add(actionDiv);
        }

        card.put("elements", elements);
        return card;
    }

    private Map<String, Object> buildCard(FeishuConfig config, DeliveryContext context) {
        Map<String, Object> card = new HashMap<>();

        // Show the callback button if: (a) a custom downloadUrl is configured and IDs are present,
        // OR (b) the auto-sign path would produce a valid URL (pre-flight check via
        // buildDownloadUrl).
        boolean renderDownloadButton = (StringUtils.isNotBlank(config.getDownloadUrl())
                && context.getScheduleId() != null && context.getExecutionId() != null)
                || StringUtils.isNotBlank(buildDownloadUrl(config, context));

        // Header
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> title = new HashMap<>();
        title.put("tag", "plain_text");
        title.put("content", buildTitle(config, context, ""));
        header.put("title", title);
        header.put("template", "blue");
        card.put("header", header);

        // Elements
        List<Object> elements = new ArrayList<>();

        // Report info
        Map<String, Object> infoDiv = new HashMap<>();
        infoDiv.put("tag", "div");
        Map<String, Object> text = new HashMap<>();
        text.put("tag", "lark_md");

        // Use custom content if provided, otherwise use default template
        String contentText;
        if (StringUtils.isNotBlank(config.getContent())) {
            contentText = TemplateResolver.resolve(config.getContent(), context, "");
        } else {
            contentText = String.format("📊 %s\n\n报表已生成，共 %d 条数据。\n\n执行时间: %s",
                    context.getReportName(), context.getRowCount(), context.getExecutionTime());
        }
        text.put("content", contentText);
        infoDiv.put("text", text);
        elements.add(infoDiv);

        // Download link if available
        if (renderDownloadButton) {
            Map<String, Object> actionDiv = new HashMap<>();
            actionDiv.put("tag", "action");
            List<Object> actions = new ArrayList<>();
            actions.add(buildCallbackDownloadButton(context, config.getDownloadUrl(), "primary"));
            actionDiv.put("actions", actions);
            elements.add(actionDiv);
        }

        card.put("elements", elements);
        return card;
    }

    /**
     * Callback button for report cards. Feishu echoes the value payload back to the bot's event
     * webhook on click, allowing CardActionHandler to verify the clicker's Feishu-S2 binding and
     * read permission before revealing the download link.
     */
    private Map<String, Object> buildCallbackDownloadButton(DeliveryContext context,
            String customDownloadUrl, String type) {
        Map<String, Object> button = new HashMap<>();
        button.put("tag", "button");
        button.put("type", type);
        button.put("text", Map.of("tag", "plain_text", "content", "下载报表"));
        Map<String, Object> value = new HashMap<>();
        value.put("action", "report_download");
        if (StringUtils.isNotBlank(customDownloadUrl)) {
            value.put("customDownloadUrl", customDownloadUrl);
        }
        if (context.getScheduleId() != null) {
            value.put("scheduleId", context.getScheduleId());
        }
        if (context.getExecutionId() != null) {
            value.put("executionId", context.getExecutionId());
        }
        if (context.getTenantId() != null) {
            value.put("tenantId", context.getTenantId());
        }
        button.put("value", value);
        return button;
    }

    /**
     * URL button variant used by alert cards and fallback cases. The signed URL is already
     * bearer-protected. Alert cards use this because alerts have no specific schedule owner to
     * verify permissions against.
     */
    private Map<String, Object> buildUrlDownloadButton(String downloadUrl, String type) {
        Map<String, Object> button = new HashMap<>();
        button.put("tag", "button");
        button.put("type", type);
        button.put("text", Map.of("tag", "plain_text", "content", "下载报表"));
        button.put("url", downloadUrl);
        return button;
    }

    private Map<String, Object> buildPostContent(FeishuConfig config, DeliveryContext context) {
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> post = new HashMap<>();
        Map<String, Object> zhCn = new HashMap<>();

        String downloadUrl = buildTemplateDownloadUrl(config, context);

        String title = StringUtils.isNotBlank(config.getTitle()) ? config.getTitle()
                : "📊 " + context.getReportName();
        zhCn.put("title", TemplateResolver.resolve(title, context, downloadUrl));

        List<List<Object>> contentList = new ArrayList<>();
        List<Object> line1 = new ArrayList<>();
        Map<String, Object> text1 = new HashMap<>();
        text1.put("tag", "text");

        // Use custom content if provided, otherwise use default template
        String contentText;
        if (StringUtils.isNotBlank(config.getContent())) {
            contentText = TemplateResolver.resolve(config.getContent(), context, downloadUrl);
        } else {
            contentText = String.format("📊 %s\n\n执行时间: %s\n数据量: %d 条"
                    + (StringUtils.isNotBlank(downloadUrl) ? "\n\n点击下载: " + downloadUrl : ""),
                    context.getReportName(), context.getExecutionTime(), context.getRowCount());
        }
        text1.put("text", contentText);
        line1.add(text1);
        contentList.add(line1);

        zhCn.put("content", contentList);
        post.put("zh_cn", zhCn);
        content.put("post", post);

        return content;
    }

    private String buildTitle(FeishuConfig config, DeliveryContext context, String downloadUrl) {
        String title = StringUtils.isNotBlank(config.getTitle()) ? config.getTitle()
                : "📊 " + context.getReportName();
        return TemplateResolver.resolve(title, context, downloadUrl);
    }

    private Map<String, Object> buildTextContent(FeishuConfig config, DeliveryContext context) {
        Map<String, Object> content = new HashMap<>();
        String downloadUrl = buildTemplateDownloadUrl(config, context);
        String text;
        if (StringUtils.isNotBlank(config.getContent())) {
            text = TemplateResolver.resolve(config.getContent(), context, downloadUrl);
        } else {
            text = String.format("📊 %s\n执行时间: %s\n数据量: %d 条%s", context.getReportName(),
                    context.getExecutionTime(), context.getRowCount(),
                    StringUtils.isNotBlank(downloadUrl) ? "\n下载: " + downloadUrl : "");
        }
        content.put("text", text);
        return content;
    }

    private String buildDownloadUrl(FeishuConfig config, DeliveryContext context) {
        if (StringUtils.isNotBlank(config.getDownloadUrl())) {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.fromUriString(config.getDownloadUrl());
            if (context.getExecutionId() != null) {
                builder.queryParam("executionId", context.getExecutionId());
            }
            return builder.build().toUriString();
        }
        if (StringUtils.isBlank(apiBaseUrl)) {
            log.warn(
                    "s2.feishu.api-base-url is not configured — Feishu download button suppressed. "
                            + "Set s2.feishu.api-base-url to the public base URL of this SuperSonic instance "
                            + "to enable signed download links (scheduleId={}).",
                    context.getScheduleId());
            return "";
        }
        if (StringUtils.isBlank(context.getFileLocation()) || context.getScheduleId() == null
                || context.getExecutionId() == null) {
            log.warn("Output file not generated for scheduleId={} executionId={} — "
                    + "Feishu download button suppressed. Ensure outputFormat is set in the schedule.",
                    context.getScheduleId(), context.getExecutionId());
            return "";
        }
        // Resolve effective tenantId using TenantConfig as the single source of truth.
        // In single-tenant mode (tenant.enabled=false) context.getTenantId() is always null;
        // fall back to TenantConfig.defaultTenantId (default 1L).
        // In multi-tenant mode a null tenantId means upstream didn't populate the context —
        // log an error but still use the default rather than silently emitting a 0-tenant URL.
        Long effectiveTenantId = context.getTenantId() != null ? context.getTenantId()
                : tenantConfig.getDefaultTenantId();
        if (tenantConfig.isEnabled() && context.getTenantId() == null) {
            log.error("tenantId is null for scheduleId={} in multi-tenant mode — "
                    + "falling back to defaultTenantId={}. Check ReportExecutionContext population.",
                    context.getScheduleId(), effectiveTenantId);
        }
        long expiresAt = ReportDownloadTokenUtils
                .expiresAtEpochSeconds(downloadProperties.getTokenTtlSeconds());
        String token = ReportDownloadTokenUtils.createToken(downloadProperties.getSigningSecret(),
                context.getScheduleId(), context.getExecutionId(), expiresAt, effectiveTenantId);
        if (StringUtils.isBlank(token)) {
            // 早期实现会退到 /api/v1/ 认证端点，但飞书客户端/浏览器点按钮时不携带 S2 登录态 → 401。
            // 与其在卡片里渲染一个点了必挂的按钮，不如直接不出按钮，让 signingSecret 未配置的部署显式暴露问题。
            log.warn(
                    "Report download signing secret is not configured, skipping Feishu download button. "
                            + "Set s2.report-download.signing-secret to enable signed URLs.");
            return "";
        }
        return UriComponentsBuilder
                .fromUriString(StringUtils.stripEnd(apiBaseUrl, "/")
                        + "/api/public/reportSchedules/" + context.getScheduleId() + "/executions/"
                        + context.getExecutionId() + ":download")
                .queryParam("tenantId", effectiveTenantId).queryParam("expires", expiresAt)
                .queryParam("token", token).build().toUriString();
    }

    private String buildTemplateDownloadUrl(FeishuConfig config, DeliveryContext context) {
        if (StringUtils.isBlank(config.getDownloadUrl())) {
            return "";
        }
        return buildDownloadUrl(config, context);
    }

    private String generateSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[] {});
        return Base64.getEncoder().encodeToString(signData);
    }

    private void validateFeishuResponse(JSONObject respBody) {
        if (respBody == null) {
            return;
        }
        Integer code = respBody.getInteger("code");
        String message = respBody.getString("msg");
        if (code == null) {
            code = respBody.getInteger("StatusCode");
            message = respBody.getString("StatusMessage");
        }
        if (code != null && code != 0) {
            throw new DeliveryException("Feishu API error: " + message);
        }
    }

    @Override
    public boolean validateConfig(String configJson) {
        FeishuConfig config = parseConfig(configJson);
        if (StringUtils.isBlank(config.getWebhookUrl())) {
            throw new IllegalArgumentException("Feishu webhook URL is required");
        }
        if (!isValidBotWebhookUrl(config.getWebhookUrl())) {
            throw new IllegalArgumentException(
                    "Invalid Feishu webhook URL. Use a custom bot webhook like "
                            + "https://open.feishu.cn/open-apis/bot/v2/hook/xxx");
        }
        return true;
    }

    private void validateWebhookUrl(String webhookUrl) {
        if (StringUtils.isBlank(webhookUrl)) {
            throw new DeliveryException("Feishu webhook URL is required", false);
        }
        if (!isValidBotWebhookUrl(webhookUrl)) {
            throw new DeliveryException("Invalid Feishu webhook URL. Use a custom bot webhook like "
                    + "https://open.feishu.cn/open-apis/bot/v2/hook/xxx", false);
        }
    }

    private boolean isValidBotWebhookUrl(String webhookUrl) {
        try {
            URI uri = URI.create(webhookUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && ("open.feishu.cn".equalsIgnoreCase(host)
                            || "open.larksuite.com".equalsIgnoreCase(host))
                    && path != null && path.startsWith("/open-apis/bot/v2/hook/");
        } catch (Exception e) {
            return false;
        }
    }

    private FeishuConfig parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            throw new IllegalArgumentException("Feishu config is required");
        }
        try {
            return JSON.parseObject(configJson, FeishuConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Feishu config JSON: " + e.getMessage());
        }
    }

    @Data
    public static class FeishuConfig {
        private String webhookUrl;
        private String secret;
        private String msgType; // text, post, interactive
        private String title;
        private String content; // User-defined message template with ${variable} placeholders
        private String downloadUrl; // Base URL for download links
    }
}
