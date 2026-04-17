package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@Slf4j
@RequiredArgsConstructor
public class PreviewMessageHandler implements MessageHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuCardRenderer cardRenderer;
    private final FeishuMessageSender messageSender;

    @Override
    public void handle(FeishuMessage msg, User user) {
        // Strip "/sql " prefix
        String queryText = msg.getContent().replaceFirst("(?i)/sql\\s+", "").trim();
        if (queryText.isBlank()) {
            messageSender.replyText(msg.getMessageId(), "请输入查询内容，例如: /sql 查昨天北京的GMV");
            return;
        }

        try {
            ChatParseResp resp = apiClient.parse(queryText, msg.getAgentId(), user);

            if (resp == null || resp.getState() == ParseResp.ParseState.FAILED
                    || resp.getSelectedParses().isEmpty()) {
                String errorMsg =
                        resp != null && resp.getErrorMsg() != null ? resp.getErrorMsg() : "无法解析查询";
                messageSender.replyCard(msg.getMessageId(),
                        cardRenderer.renderErrorCard("SQL 预览失败: " + errorMsg));
                return;
            }

            SemanticParseInfo topParse = resp.getSelectedParses().getFirst();
            messageSender.replyCard(msg.getMessageId(),
                    cardRenderer.renderSqlPreviewCard(queryText, topParse));
        } catch (Exception e) {
            log.error("Preview handler error for openId={}", msg.getOpenId(), e);
            messageSender.replyCard(msg.getMessageId(),
                    cardRenderer.renderErrorCard("SQL 预览失败: " + e.getMessage()));
        }
    }
}
