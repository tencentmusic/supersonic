package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@Slf4j
@RequiredArgsConstructor
public class HelpMessageHandler implements MessageHandler {

    private final FeishuCardRenderer cardRenderer;
    private final FeishuMessageSender messageSender;
    private final SuperSonicApiClient apiClient;

    @SuppressWarnings("unchecked")
    @Override
    public void handle(FeishuMessage msg, User user) {
        // Fetch example questions from current agent
        List<String> examples = List.of();
        String agentName = null;
        try {
            List<Map<String, Object>> agents = apiClient.getAgentList(user);
            for (Map<String, Object> agent : agents) {
                Object id = agent.get("id");
                int agentId = id instanceof Number ? ((Number) id).intValue() : -1;
                if (agentId == msg.getAgentId()) {
                    agentName = agent.get("name") != null ? agent.get("name").toString() : null;
                    Object exObj = agent.get("examples");
                    if (exObj instanceof List) {
                        examples = (List<String>) exObj;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch agent examples for help card: {}", e.getMessage());
        }

        Map<String, Object> card = cardRenderer.renderHelpCard(agentName, examples);
        messageSender.replyCard(msg.getMessageId(), card);
    }
}
