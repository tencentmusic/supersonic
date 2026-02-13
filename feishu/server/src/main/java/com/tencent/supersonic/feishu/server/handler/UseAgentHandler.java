package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.FeishuUserMappingService;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class UseAgentHandler implements MessageHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuUserMappingService userMappingService;
    private final FeishuMessageSender messageSender;

    @Override
    public void handle(FeishuMessage msg, User user) {
        String text = msg.getContent().replaceFirst("(?i)/use\\s*", "").trim();

        if (text.isEmpty()) {
            messageSender.replyText(msg.getMessageId(),
                    "请指定 Agent 编号，例如: /use 3\n发送 /template 查看可用列表");
            return;
        }

        int targetAgentId;
        try {
            targetAgentId = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            messageSender.replyText(msg.getMessageId(), "Agent 编号格式错误，请输入数字，例如: /use 3");
            return;
        }

        // Validate: check the agent exists and is active
        List<Map<String, Object>> agents = apiClient.getAgentList(user);
        Map<String, Object> targetAgent = agents.stream().filter(a -> {
            Object id = a.get("id");
            int agentId = id instanceof Number ? ((Number) id).intValue() : -1;
            return agentId == targetAgentId && Integer.valueOf(1).equals(a.get("status"));
        }).findFirst().orElse(null);

        if (targetAgent == null) {
            messageSender.replyText(msg.getMessageId(),
                    "未找到编号为 " + targetAgentId + " 的在线 Agent，发送 /template 查看可用列表");
            return;
        }

        // Update the user's default agent
        boolean updated = userMappingService.updateDefaultAgent(msg.getOpenId(), targetAgentId);
        if (!updated) {
            messageSender.replyText(msg.getMessageId(), "切换失败：未找到您的账号映射记录");
            return;
        }

        String agentName = targetAgent.get("name") != null ? targetAgent.get("name").toString()
                : "Agent " + targetAgentId;
        messageSender.replyText(msg.getMessageId(), "已切换到 [" + agentName + "]，后续查询将使用此数据域");
    }
}
