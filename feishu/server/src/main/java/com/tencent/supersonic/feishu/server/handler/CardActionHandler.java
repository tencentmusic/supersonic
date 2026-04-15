package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.render.FeishuCardTemplate;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.api.service.ReportScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardActionHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuMessageSender messageSender;
    private final ReportScheduleService reportScheduleService;
    private final FeishuProperties feishuProperties;

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
        String downloadUrl = Objects.toString(actionValue.get("downloadUrl"), "");
        Long scheduleId = parseLong(actionValue.get("scheduleId"));
        Long executionId = parseLong(actionValue.get("executionId"));

        log.info("[feishu] report download callback openId={} user={} schedule={} execution={}",
                operatorOpenId, user != null ? user.getId() : null, scheduleId, executionId);

        Map<String, Object> card = new HashMap<>();
        if (StringUtils.isBlank(downloadUrl) || scheduleId == null) {
            card.put("header", FeishuCardTemplate.buildHeader("暂不能下载", "orange"));
            card.put("elements", List.of(FeishuCardTemplate.buildMarkdown("报表文件还没有生成或下载地址已失效。")));
            sendCardSafely(operatorOpenId, card);
            return;
        }

        // 只放行白名单域名，防止卡片 value 被改写成钓鱼链接（签名校验虽然已经兜底过一次，这里是最后一道防线）。
        if (!isAllowedDownloadUrl(downloadUrl)) {
            log.warn("[feishu] blocked download URL not in allowed origin: {}", downloadUrl);
            card.put("header", FeishuCardTemplate.buildHeader("下载地址无效", "red"));
            card.put("elements",
                    List.of(FeishuCardTemplate.buildMarkdown("检测到非法下载地址，已拒绝。请联系管理员核查飞书渠道配置。")));
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
        } catch (InvalidPermissionException e) {
            log.warn("[feishu] user={} denied download for schedule={}: {}",
                    user != null ? user.getId() : null, scheduleId, e.getMessage());
            card.put("header", FeishuCardTemplate.buildHeader("无权限下载", "red"));
            card.put("elements", List.of(FeishuCardTemplate
                    .buildMarkdown("你没有该报表的查看权限，请联系创建人（" + e.getMessage() + "）。")));
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
            // No base URL configured → only accept relative paths under /api/public or /api/v1
            return url.startsWith("/api/public/") || url.startsWith("/api/v1/");
        }
        String stripped = StringUtils.stripEnd(apiBaseUrl, "/");
        return url.startsWith(stripped + "/api/public/") || url.startsWith(stripped + "/api/v1/");
    }
}
