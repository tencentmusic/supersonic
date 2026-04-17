package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.render.FeishuCardTemplate;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.api.service.ReportScheduleService;
import com.tencent.supersonic.headless.api.util.ReportDownloadTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
@Slf4j
public class CardActionHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuMessageSender messageSender;
    private final ReportScheduleService reportScheduleService;
    private final FeishuProperties feishuProperties;
    private final TenantConfig tenantConfig;

    @Value("${s2.report-download.signing-secret:${s2.report.download.signing-secret:${s2.encryption.aes-key:}}}")
    private String downloadSigningSecret;

    @Value("${s2.report-download.token-ttl-seconds:604800}")
    private long downloadTokenTtlSeconds;

    /**
     * Handle a card action button click.
     *
     * @param actionValue the value map from the button (contains "action", "ruleId", "eventIds")
     * @param user resolved SuperSonic user
     * @param operatorOpenId the operator's open_id for sending confirmation reply
     */
    public void handle(Map<String, Object> actionValue, User user, String operatorOpenId) {
        String action = String.valueOf(actionValue.get("action"));
        if ("report_download".equals(action)) {
            sendReportDownload(actionValue, user, operatorOpenId);
            return;
        }

        String eventIdsStr = String.valueOf(actionValue.getOrDefault("eventIds", ""));
        List<Long> eventIds = Arrays.stream(eventIdsStr.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(Long::valueOf).collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            log.warn("Card action with no event IDs: {}", actionValue);
            return;
        }

        String targetStatus;
        Long assigneeId = null;
        String actionLabel;

        switch (action) {
            case "alert_confirm":
                targetStatus = "CONFIRMED";
                actionLabel = "已确认知悉";
                break;
            case "alert_assign":
                targetStatus = "ASSIGNED";
                assigneeId = user.getId();
                actionLabel = "已接手处理";
                break;
            default:
                log.warn("Unknown card action: {}", action);
                return;
        }

        int success = 0;
        int failed = 0;
        for (Long eventId : eventIds) {
            try {
                apiClient.transitionAlertEvent(eventId, targetStatus, assigneeId,
                        "via Feishu by " + user.getName(), user);
                success++;
            } catch (Exception e) {
                log.warn("Failed to transition event {}: {}", eventId, e.getMessage());
                failed++;
            }
        }

        // Send confirmation reply
        String resultText;
        if (failed == 0) {
            resultText = String.format("%s：共 %d 条异常事件", actionLabel, success);
        } else {
            resultText = String.format("%s：成功 %d 条，失败 %d 条", actionLabel, success, failed);
        }

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader(actionLabel, "green"));
        card.put("elements", List.of(FeishuCardTemplate.buildMarkdown(resultText)));

        try {
            messageSender.sendCard(operatorOpenId, card);
        } catch (Exception e) {
            log.error("Failed to send confirmation card to {}: {}", operatorOpenId, e.getMessage());
        }
    }

    private void sendReportDownload(Map<String, Object> actionValue, User user,
            String operatorOpenId) {
        String customDownloadUrl = Objects.toString(actionValue.get("customDownloadUrl"), "");
        Long scheduleId = parseLong(actionValue.get("scheduleId"));
        Long executionId = parseLong(actionValue.get("executionId"));
        Long tenantId = parseLong(actionValue.get("tenantId"));

        log.info("[feishu] report download callback openId={} user={} schedule={} execution={}",
                operatorOpenId, user != null ? user.getId() : null, scheduleId, executionId);

        Map<String, Object> card = new HashMap<>();
        if (scheduleId == null || executionId == null) {
            card.put("header", FeishuCardTemplate.buildHeader("暂不能下载", "orange"));
            card.put("elements", List.of(FeishuCardTemplate.buildMarkdown("报表文件还没有生成或下载地址已失效。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }

        // 未绑定账号的用户 user == null，直接拒绝并引导绑定，不走后续权限查询。
        if (user == null) {
            log.warn("[feishu] unbound user attempted report download: openId={} schedule={}",
                    operatorOpenId, scheduleId);
            card.put("header", FeishuCardTemplate.buildHeader("请先绑定账号", "orange"));
            card.put("elements", List.of(FeishuCardTemplate
                    .buildMarkdown("你的飞书账号尚未绑定平台账号，无法下载报表。\n请在此对话中发送任意消息，按提示完成账号绑定后重试。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }

        // ReportScheduleService.getScheduleById 已经内置 checkReadPermission(schedule, user)，
        // 非 owner 且非 super admin 会抛 InvalidPermissionException —— 捕获后给操作者发一张拒绝卡片。
        try {
            ReportScheduleResp schedule = reportScheduleService.getScheduleById(scheduleId, user);
            if (schedule == null) {
                card.put("header", FeishuCardTemplate.buildHeader("调度已删除", "orange"));
                card.put("elements",
                        List.of(FeishuCardTemplate.buildMarkdown("对应的报表调度已被删除，无法下载。")));
                sendCardSafely(operatorOpenId, card);
                return;
            }
            ReportExecutionResp execution =
                    reportScheduleService.getExecutionById(scheduleId, executionId, user);
            if (execution == null || execution.getResultLocation() == null) {
                card.put("header", FeishuCardTemplate.buildHeader("暂不能下载", "orange"));
                card.put("elements",
                        List.of(FeishuCardTemplate.buildMarkdown("报表文件还没有生成或下载地址已失效。")));
                sendCardSafely(operatorOpenId, card);
                return;
            }
        } catch (InvalidPermissionException e) {
            log.warn("[feishu] user={} denied download for schedule={}: {}", user.getId(),
                    scheduleId, e.getMessage());
            card.put("header", FeishuCardTemplate.buildHeader("无权限下载", "red"));
            card.put("elements",
                    List.of(FeishuCardTemplate.buildMarkdown("你没有该报表的查看权限，请联系报表创建人开通权限。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }

        Long resolvedTenantId =
                (user.getTenantId() != null && user.getTenantId() > 0) ? user.getTenantId()
                        : tenantConfig.getDefaultTenantId();
        if (tenantId != null && tenantId > 0 && !tenantId.equals(resolvedTenantId)) {
            log.warn(
                    "[feishu] blocked report download callback with mismatched tenant: "
                            + "payloadTenant={} userTenant={} user={} schedule={}",
                    tenantId, resolvedTenantId, user.getId(), scheduleId);
            card.put("header", FeishuCardTemplate.buildHeader("下载地址无效", "red"));
            card.put("elements",
                    List.of(FeishuCardTemplate.buildMarkdown("检测到非法下载请求，已拒绝。请重新打开最新的报表卡片。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }
        String downloadUrl =
                buildDownloadUrl(customDownloadUrl, scheduleId, executionId, resolvedTenantId);
        if (StringUtils.isBlank(downloadUrl)) {
            card.put("header", FeishuCardTemplate.buildHeader("暂不能下载", "orange"));
            card.put("elements",
                    List.of(FeishuCardTemplate.buildMarkdown("下载地址生成失败，请联系管理员检查飞书渠道配置。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }

        // 只放行白名单域名，防止卡片 value 被改写成钓鱼链接。
        if (!isAllowedDownloadUrl(downloadUrl)) {
            log.warn("[feishu] blocked download URL not in allowed origin: {}", downloadUrl);
            card.put("header", FeishuCardTemplate.buildHeader("下载地址无效", "red"));
            card.put("elements",
                    List.of(FeishuCardTemplate.buildMarkdown("检测到非法下载地址，已拒绝。请联系管理员核查飞书渠道配置。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }

        card.put("header", FeishuCardTemplate.buildHeader("报表下载", "blue"));
        card.put("elements", List.of(FeishuCardTemplate.buildMarkdown("已为你生成下载入口。"),
                FeishuCardTemplate.buildUrlButton("下载报表", downloadUrl, "primary")));
        sendCardSafely(operatorOpenId, card);
    }

    private void sendCardSafely(String operatorOpenId, Map<String, Object> card) {
        try {
            messageSender.sendCard(operatorOpenId, card);
        } catch (Exception e) {
            log.error("Failed to send report download card to {}: {}", operatorOpenId,
                    e.getMessage());
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            String s = value.toString().trim();
            return s.isEmpty() ? null : Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Restrict download URLs to the configured {@code apiBaseUrl}. This guards against a
     * compromised card value being used as an open-redirect / phishing vector through our bot.
     */
    private boolean isAllowedDownloadUrl(String url) {
        String apiBaseUrl = feishuProperties != null ? feishuProperties.getApiBaseUrl() : null;
        if (StringUtils.isBlank(apiBaseUrl)) {
            // Without an explicit public base URL there is no trusted absolute origin to compare
            // against. Keep relative in-app API paths only; reject arbitrary HTTPS URLs from the
            // callback payload.
            return url.startsWith("/api/public/") || url.startsWith("/api/v1/");
        }
        String stripped = StringUtils.stripEnd(apiBaseUrl, "/");
        return url.startsWith(stripped + "/api/public/") || url.startsWith(stripped + "/api/v1/");
    }

    private String buildDownloadUrl(String customDownloadUrl, Long scheduleId, Long executionId,
            Long tenantId) {
        if (StringUtils.isNotBlank(customDownloadUrl)) {
            return UriComponentsBuilder.fromUriString(customDownloadUrl)
                    .queryParam("executionId", executionId).build().toUriString();
        }
        String apiBaseUrl = feishuProperties != null ? feishuProperties.getApiBaseUrl() : null;
        if (StringUtils.isAnyBlank(apiBaseUrl, downloadSigningSecret) || tenantId == null) {
            return "";
        }
        long expiresAt = ReportDownloadTokenUtils.expiresAtEpochSeconds(downloadTokenTtlSeconds);
        String token = ReportDownloadTokenUtils.createToken(downloadSigningSecret, scheduleId,
                executionId, expiresAt, tenantId);
        if (StringUtils.isBlank(token)) {
            return "";
        }
        return UriComponentsBuilder
                .fromUriString(
                        StringUtils.stripEnd(apiBaseUrl, "/") + "/api/public/reportSchedules/"
                                + scheduleId + "/executions/" + executionId + ":download")
                .queryParam("tenantId", tenantId).queryParam("expires", expiresAt)
                .queryParam("token", token).build().toUriString();
    }
}
