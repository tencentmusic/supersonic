package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.server.render.FeishuCardTemplate;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardActionHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuMessageSender messageSender;

    /**
     * Handle a card action button click.
     *
     * @param actionValue the value map from the button (contains "action", "ruleId", "eventIds")
     * @param user resolved SuperSonic user
     * @param operatorOpenId the operator's open_id for sending confirmation reply
     */
    public void handle(Map<String, Object> actionValue, User user, String operatorOpenId) {
        String action = String.valueOf(actionValue.get("action"));
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
}
