package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueryMessageHandler implements MessageHandler {

    private static final String CONTEXT_CACHE_PREFIX = "queryCtx:";

    private final SuperSonicApiClient apiClient;
    private final FeishuCardRenderer cardRenderer;
    private final FeishuMessageSender messageSender;
    private final FeishuQuerySessionMapper sessionMapper;
    private final FeishuCacheService cacheService;

    @Override
    public void handle(FeishuMessage msg, User user) {
        // 1. Create session record (status=PENDING)
        FeishuQuerySessionDO session = new FeishuQuerySessionDO();
        session.setFeishuOpenId(msg.getOpenId());
        session.setFeishuMessageId(msg.getMessageId());
        session.setQueryText(msg.getContent());
        session.setAgentId(msg.getAgentId());
        session.setStatus("PENDING");
        sessionMapper.insert(session);

        try {
            // 2. Read cached dataSetId from last successful query (follow-up context)
            Long lastDataSetId = getCachedDataSetId(msg.getOpenId(), msg.getAgentId());

            // 3. Call SuperSonic API via HTTP
            QueryResult result =
                    apiClient.query(msg.getContent(), msg.getAgentId(), user, lastDataSetId);

            // 4. Update session
            session.setStatus("SUCCESS");
            Long currentDataSetId = null;
            if (result != null) {
                session.setQueryResultId(result.getQueryId());
                session.setSqlText(result.getQuerySql());
                if (result.getQueryResults() != null) {
                    session.setRowCount(result.getQueryResults().size());
                }
                if (result.getChatContext() != null
                        && result.getChatContext().getDataSetId() != null) {
                    currentDataSetId = result.getChatContext().getDataSetId();
                    session.setDatasetId(currentDataSetId);
                }
            }
            sessionMapper.updateById(session);

            // 5. Cache dataSetId for follow-up queries
            if (currentDataSetId != null) {
                cacheDataSetId(msg.getOpenId(), msg.getAgentId(), currentDataSetId);
            }

            // 6. Render card and reply (pass parseInfo for dynamic follow-up hints)
            SemanticParseInfo parseInfo = result != null ? result.getChatContext() : null;
            Map<String, Object> card = cardRenderer.renderQueryResult(result, parseInfo);
            messageSender.replyCard(msg.getMessageId(), card);

        } catch (Exception e) {
            log.error("Query handler error for openId={}", msg.getOpenId(), e);
            session.setStatus("ERROR");
            session.setErrorMessage(e.getMessage());
            sessionMapper.updateById(session);

            Map<String, Object> errorCard = cardRenderer.renderErrorCard("查询失败: " + e.getMessage());
            messageSender.replyCard(msg.getMessageId(), errorCard);
        }
    }

    private Long getCachedDataSetId(String openId, int agentId) {
        String cached = cacheService.get(CONTEXT_CACHE_PREFIX + openId + ":" + agentId);
        return cached != null ? Long.valueOf(cached) : null;
    }

    private void cacheDataSetId(String openId, int agentId, Long dataSetId) {
        cacheService.put(CONTEXT_CACHE_PREFIX + openId + ":" + agentId, String.valueOf(dataSetId));
    }
}
