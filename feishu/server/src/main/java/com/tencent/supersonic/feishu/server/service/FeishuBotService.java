package com.tencent.supersonic.feishu.server.service;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.handler.CardActionHandler;
import com.tencent.supersonic.feishu.server.handler.FeishuMessageRouter;
import com.tencent.supersonic.feishu.server.handler.MessageHandler;
import com.tencent.supersonic.feishu.server.metrics.FeishuMeterBinder;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
public class FeishuBotService {

    private static final String RATE_LIMIT_PREFIX = "rateLimit:";

    private final FeishuMessageRouter router;
    private final FeishuUserMappingService userMappingService;
    private final FeishuMessageSender messageSender;
    private final FeishuCardRenderer cardRenderer;
    private final FeishuProperties properties;
    private final FeishuCacheService cacheService;
    private final ThreadPoolTaskExecutor feishuExecutor;
    private final FeishuMeterBinder meterBinder;
    private final FeishuBindTokenService bindTokenService;
    private final CardActionHandler cardActionHandler;
    private final TenantConfig tenantConfig;

    public FeishuBotService(FeishuMessageRouter router, FeishuUserMappingService userMappingService,
            FeishuMessageSender messageSender, FeishuCardRenderer cardRenderer,
            FeishuProperties properties, FeishuCacheService cacheService,
            @Qualifier("feishuExecutor") ThreadPoolTaskExecutor feishuExecutor,
            FeishuMeterBinder meterBinder, FeishuBindTokenService bindTokenService,
            CardActionHandler cardActionHandler, TenantConfig tenantConfig) {
        this.router = router;
        this.userMappingService = userMappingService;
        this.messageSender = messageSender;
        this.cardRenderer = cardRenderer;
        this.properties = properties;
        this.cacheService = cacheService;
        this.feishuExecutor = feishuExecutor;
        this.meterBinder = meterBinder;
        this.bindTokenService = bindTokenService;
        this.cardActionHandler = cardActionHandler;
        this.tenantConfig = tenantConfig;
    }

    public void handleEventAsync(String eventType, Map<String, Object> event) {
        if ("card.action.trigger".equals(eventType)) {
            feishuExecutor.execute(() -> {
                try {
                    handleCardAction(event);
                } catch (Exception e) {
                    log.error("Failed to handle card action", e);
                }
            });
            return;
        }

        if (!"im.message.receive_v1".equals(eventType) && !"message".equals(eventType)) {
            log.debug("Ignoring event type: {}", eventType);
            return;
        }

        try {
            feishuExecutor.execute(() -> {
                try {
                    handleMessage(event);
                } catch (Exception e) {
                    log.error("Failed to handle event: {}", eventType, e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Feishu executor queue full, rejecting event: {}", eventType);
            meterBinder.incrementExecutorRejection();
            replyBusy(event);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCardAction(Map<String, Object> event) {
        // Feishu card action event structure:
        // { "event": { "operator": { "open_id": "..." }, "action": { "value": {...}, "tag":
        // "button" } } }
        Map<String, Object> eventBody = (Map<String, Object>) event.get("event");
        if (eventBody == null) {
            log.warn("Card action event missing 'event' body");
            return;
        }

        Map<String, Object> operator = (Map<String, Object>) eventBody.get("operator");
        String openId = operator != null ? String.valueOf(operator.get("open_id")) : null;
        if (openId == null) {
            log.warn("Card action event missing operator open_id");
            return;
        }

        Map<String, Object> action = (Map<String, Object>) eventBody.get("action");
        if (action == null) {
            log.warn("Card action event missing action");
            return;
        }

        Map<String, Object> actionValue = (Map<String, Object>) action.get("value");
        if (actionValue == null) {
            log.warn("Card action missing value payload");
            return;
        }

        try {
            // Async thread has no inherited TenantContext; set a default so any downstream SQL has
            // a consistent tenant filter during mapping lookup. Overwrite with the real tenant
            // once the user has been resolved.
            TenantContext.setTenantId(getDefaultTenantId());

            FeishuUserMappingService.ResolvedMapping mapping =
                    userMappingService.resolveMapping(openId);
            if (mapping == null || mapping.user() == null) {
                log.warn("Cannot resolve Feishu user for open_id: {}", openId);
                sendUnmappedActionPrompt(openId);
                return;
            }

            TenantContext.setTenantId(mapping.user().getTenantId());
            // Send confirmation to operator's open_id (sendCard uses receive_id_type=open_id)
            cardActionHandler.handle(actionValue, mapping.user(), openId);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extract messageId from event and reply with a "busy" message when the executor queue is full.
     */
    @SuppressWarnings("unchecked")
    private void replyBusy(Map<String, Object> event) {
        try {
            String messageId = null;
            Map<String, Object> message = (Map<String, Object>) event.get("message");
            if (message != null) {
                messageId = (String) message.get("message_id");
            } else {
                messageId = (String) event.get("open_message_id");
            }
            if (messageId != null) {
                Map<String, Object> card = cardRenderer.renderErrorCard("系统繁忙，请稍后重试");
                messageSender.replyCard(messageId, card);
            }
        } catch (Exception ex) {
            log.error("Failed to send busy reply", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(Map<String, Object> event) {
        // Parse message from event
        FeishuMessage msg = parseMessage(event);
        if (msg == null) {
            log.warn("Failed to parse message from event");
            return;
        }

        try {
            // Set default tenant for user mapping lookup (async thread has no tenant context)
            TenantContext.setTenantId(getDefaultTenantId());

            // Resolve user and mapping
            FeishuUserMappingService.ResolvedMapping resolved =
                    userMappingService.resolveMapping(msg.getOpenId());
            if (resolved == null) {
                replyUnmappedUser(msg);
                return;
            }
            User user = resolved.user();

            // Override with user's actual tenant
            TenantContext.setTenantId(user.getTenantId());

            // Set agentId: user-level override > global default
            Integer agentId = resolved.agentId() != null ? resolved.agentId()
                    : properties.getDefaultAgentId();
            msg.setAgentId(agentId);

            // Rate limit check
            if (isRateLimited(msg.getOpenId())) {
                meterBinder.incrementRateLimitHit();
                messageSender.replyText(msg.getMessageId(),
                        "查询过于频繁，请稍后再试（每分钟限" + properties.getRateLimit().getMaxRequests() + "次）");
                return;
            }

            // Route and handle
            MessageHandler handler = router.route(msg.getContent());
            handler.handle(msg, user);
        } catch (Exception e) {
            log.error("Error handling message from {}", msg.getOpenId(), e);
            try {
                Map<String, Object> errorCard =
                        cardRenderer.renderErrorCard("处理消息时发生错误: " + e.getMessage());
                messageSender.replyCard(msg.getMessageId(), errorCard);
            } catch (Exception sendErr) {
                log.error("Failed to send error reply", sendErr);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void replyUnmappedUser(FeishuMessage msg) {
        FeishuUserMappingDO pending = findBindablePending(msg.getOpenId(), /* crossTenant */ false);
        if (pending != null) {
            Map<String, Object> card = buildBindGuideCard(msg.getOpenId(), pending);
            if (card != null) {
                messageSender.replyCard(msg.getMessageId(), card);
                return;
            }
        }
        messageSender.replyText(msg.getMessageId(),
                "未找到您的账号映射，已自动提交映射申请，请联系管理员在用户映射页面审核并关联您的平台账号。");
    }

    private void sendUnmappedActionPrompt(String openId) {
        // Card callback runs on an async thread with only an openId — we do not know which tenant
        // the user belongs to until we find their pending mapping. Use the cross-tenant lookup so
        // that a tenant-2 user whose callback was set up before tenant resolution will still get
        // the correct bind link signed with THEIR tenantId.
        FeishuUserMappingDO pending = findBindablePending(openId, /* crossTenant */ true);
        if (pending != null) {
            Map<String, Object> card = buildBindGuideCard(openId, pending);
            if (card != null) {
                try {
                    messageSender.sendCard(openId, card);
                } catch (Exception e) {
                    log.warn("Failed to send bind guide card to {}: {}", openId, e.getMessage());
                }
                return;
            }
        }
        try {
            messageSender.sendText(openId, "未找到您的账号映射，暂不能下载报表。请先在飞书机器人对话中发送任意消息完成账号绑定。");
        } catch (Exception e) {
            log.warn("Failed to send unmapped prompt to {}: {}", openId, e.getMessage());
        }
    }

    /**
     * Find a PENDING mapping row usable for signing a self-service bind token. Returns null when
     * OAuth self-service binding is disabled.
     *
     * @param crossTenant when true, bypass the current TenantContext and look across all tenants
     *        (card-action callbacks know only an openId, so this is required to recover the owning
     *        tenant before signing the bind token)
     */
    private FeishuUserMappingDO findBindablePending(String openId, boolean crossTenant) {
        if (!properties.getOauth().isEnabled()) {
            return null;
        }
        return crossTenant ? userMappingService.findLatestPendingAcrossTenants(openId)
                : userMappingService.findPendingByOpenId(openId);
    }

    /**
     * Build a bind-guide card for the given pending mapping. Switches TenantContext to the pending
     * mapping's tenant so that both {@code bindTokenService.generateToken} and any downstream SQL
     * observe the correct tenant; the caller is responsible for restoring or clearing TenantContext
     * afterwards (e.g. via a {@code finally} in {@code handleCardAction}).
     */
    private Map<String, Object> buildBindGuideCard(String openId, FeishuUserMappingDO pending) {
        if (pending.getTenantId() == null) {
            log.warn("Pending mapping id={} has null tenantId, refusing to sign bind token",
                    pending.getId());
            return null;
        }
        TenantContext.setTenantId(pending.getTenantId());
        String bindToken = bindTokenService.generateToken(openId, pending.getId(),
                pending.getFeishuUserName(), pending.getTenantId());
        String bindUrl = properties.getApiBaseUrl() + "/api/feishu/bindPage?token=" + bindToken;
        return cardRenderer.renderBindGuideCard(
                pending.getFeishuUserName() != null ? pending.getFeishuUserName() : "用户", bindUrl);
    }

    private boolean isRateLimited(String openId) {
        FeishuProperties.RateLimitConfig config = properties.getRateLimit();
        if (!config.isEnabled()) {
            return false;
        }
        long count = cacheService.incrementCounter(RATE_LIMIT_PREFIX + openId);
        return count > config.getMaxRequests();
    }

    private Long getDefaultTenantId() {
        return tenantConfig != null && tenantConfig.getDefaultTenantId() != null
                ? tenantConfig.getDefaultTenantId()
                : 1L;
    }

    @SuppressWarnings("unchecked")
    private FeishuMessage parseMessage(Map<String, Object> event) {
        try {
            // Detect v1.0 vs v2.0 by checking for v2.0-specific nested structure
            Map<String, Object> message = (Map<String, Object>) event.get("message");
            if (message != null) {
                return parseMessageV2(event, message);
            }
            // v1.0: fields are flat in the event object
            return parseMessageV1(event);
        } catch (Exception e) {
            log.error("Error parsing message", e);
            return null;
        }
    }

    /**
     * Parse v2.0 event: {sender: {sender_id: {open_id}}, message: {chat_id, message_id, ...}}
     */
    @SuppressWarnings("unchecked")
    private FeishuMessage parseMessageV2(Map<String, Object> event, Map<String, Object> message) {
        Map<String, Object> sender = (Map<String, Object>) event.get("sender");
        Map<String, Object> senderId =
                sender != null ? (Map<String, Object>) sender.get("sender_id") : null;
        if (senderId == null)
            return null;

        String openId = (String) senderId.get("open_id");
        String chatId = (String) message.get("chat_id");
        String messageId = (String) message.get("message_id");
        String chatType = (String) message.get("chat_type");
        String msgType = (String) message.get("message_type");

        if (!"text".equals(msgType)) {
            log.debug("Ignoring non-text message type: {}", msgType);
            return null;
        }

        // Parse content - JSON string like {"text":"hello"}
        String contentStr = (String) message.get("content");
        String text = "";
        if (contentStr != null) {
            Map<String, Object> contentMap =
                    com.alibaba.fastjson.JSON.parseObject(contentStr, Map.class);
            text = (String) contentMap.getOrDefault("text", "");
        }

        // For group chats, check if bot is mentioned (strip @mention from text)
        List<FeishuMessage.Mention> mentions = new ArrayList<>();
        Object mentionList = message.get("mentions");
        if (mentionList instanceof List) {
            for (Object m : (List<?>) mentionList) {
                Map<String, Object> mentionMap = (Map<String, Object>) m;
                FeishuMessage.Mention mention =
                        FeishuMessage.Mention.builder().id((String) mentionMap.get("key"))
                                .name((String) mentionMap.get("name")).build();
                Map<String, Object> mId = (Map<String, Object>) mentionMap.get("id");
                if (mId != null) {
                    mention.setOpenId((String) mId.get("open_id"));
                }
                mentions.add(mention);
                if (mention.getId() != null) {
                    text = text.replace("@_user_" + mention.getId(), "").trim();
                }
            }
        }

        if ("group".equals(chatType) && mentions.isEmpty()) {
            return null;
        }

        return FeishuMessage.builder().openId(openId).chatId(chatId).messageId(messageId)
                .chatType(chatType).content(text.trim()).mentions(mentions).build();
    }

    /**
     * Parse v1.0 event: {type, open_id, open_chat_id, open_message_id, msg_type, text, ...}
     */
    private FeishuMessage parseMessageV1(Map<String, Object> event) {
        String openId = (String) event.get("open_id");
        String chatId = (String) event.get("open_chat_id");
        String messageId = (String) event.get("open_message_id");
        String msgType = (String) event.get("msg_type");
        // v1.0 uses "private" instead of "p2p"
        String chatType = (String) event.get("chat_type");

        if (openId == null || messageId == null)
            return null;

        if (!"text".equals(msgType)) {
            log.debug("Ignoring non-text message type (v1.0): {}", msgType);
            return null;
        }

        // v1.0: text is a plain string at event root (may include @mention prefix)
        String text = (String) event.get("text");
        if (text == null)
            text = "";

        // v1.0: "text_without_at_bot" provides clean text in group chats
        String textWithoutAt = (String) event.get("text_without_at_bot");
        if (textWithoutAt != null) {
            text = textWithoutAt;
        }

        // Normalize chat_type: v1.0 "private" -> "p2p"
        if ("private".equals(chatType)) {
            chatType = "p2p";
        }

        // For group chat without text_without_at_bot, skip if not mentioned
        if ("group".equals(chatType) && textWithoutAt == null) {
            return null;
        }

        log.debug("Parsed v1.0 message: openId={}, text={}", openId, text);
        return FeishuMessage.builder().openId(openId).chatId(chatId).messageId(messageId)
                .chatType(chatType).content(text.trim()).mentions(new ArrayList<>()).build();
    }
}
