package com.tencent.supersonic.feishu.server.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class TemplateListHandler implements MessageHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuCardRenderer cardRenderer;
    private final FeishuMessageSender messageSender;
    private final FeishuQuerySessionMapper sessionMapper;

    @Override
    public void handle(FeishuMessage msg, User user) {
        List<Map<String, Object>> agents = apiClient.getAgentList(user);

        // Filter to online agents only (status=1)
        List<Map<String, Object>> activeAgents =
                agents.stream().filter(a -> Integer.valueOf(1).equals(a.get("status")))
                        .collect(Collectors.toList());

        if (activeAgents.isEmpty()) {
            messageSender.replyText(msg.getMessageId(), "暂无可用的查询模板，请联系管理员配置。");
            return;
        }

        // Count per-agent usage for this user, sort by frequency descending
        Map<Integer, Long> agentUsageCount = countAgentUsage(msg.getOpenId());
        activeAgents.sort(Comparator.<Map<String, Object>, Long>comparing(a -> {
            Object id = a.get("id");
            int agentId = id instanceof Number ? ((Number) id).intValue() : 0;
            return agentUsageCount.getOrDefault(agentId, 0L);
        }).reversed());

        Map<String, Object> card = cardRenderer.renderAgentListCard(activeAgents, msg.getAgentId());
        messageSender.replyCard(msg.getMessageId(), card);
    }

    /**
     * Count SUCCESS query sessions grouped by agentId for a given user.
     */
    private Map<Integer, Long> countAgentUsage(String openId) {
        LambdaQueryWrapper<FeishuQuerySessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuQuerySessionDO::getFeishuOpenId, openId)
                .eq(FeishuQuerySessionDO::getStatus, "SUCCESS")
                .isNotNull(FeishuQuerySessionDO::getAgentId)
                .select(FeishuQuerySessionDO::getAgentId);
        List<FeishuQuerySessionDO> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream().collect(
                Collectors.groupingBy(FeishuQuerySessionDO::getAgentId, Collectors.counting()));
    }
}
