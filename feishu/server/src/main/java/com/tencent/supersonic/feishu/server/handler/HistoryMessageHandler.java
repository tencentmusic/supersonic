package com.tencent.supersonic.feishu.server.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryMessageHandler implements MessageHandler {

    private final FeishuQuerySessionMapper sessionMapper;
    private final FeishuCardRenderer cardRenderer;
    private final FeishuMessageSender messageSender;

    @Override
    public void handle(FeishuMessage msg, User user) {
        LambdaQueryWrapper<FeishuQuerySessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuQuerySessionDO::getFeishuOpenId, msg.getOpenId())
                .orderByDesc(FeishuQuerySessionDO::getCreatedAt).last("LIMIT 10");
        List<FeishuQuerySessionDO> sessions = sessionMapper.selectList(wrapper);

        if (sessions.isEmpty()) {
            messageSender.replyText(msg.getMessageId(), "暂无查询记录");
            return;
        }

        List<Map<String, String>> items = sessions.stream().map(s -> {
            Map<String, String> item = new HashMap<>();
            item.put("query", s.getQueryText());
            item.put("status", s.getStatus());
            item.put("time", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            return item;
        }).toList();

        Map<String, Object> card = cardRenderer.renderHistoryCard(items);
        messageSender.replyCard(msg.getMessageId(), card);
    }
}
