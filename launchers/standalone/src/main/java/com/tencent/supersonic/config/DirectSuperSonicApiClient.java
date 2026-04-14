package com.tencent.supersonic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.server.service.HttpSuperSonicApiClient;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.pojo.AlertEventTransitionReq;
import com.tencent.supersonic.headless.server.pojo.AlertResolutionStatus;
import com.tencent.supersonic.headless.server.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

/**
 * Direct (in-process) implementation of {@link SuperSonicApiClient} for standalone deployment.
 * Calls Service-layer beans directly, bypassing HTTP loopback, JWT token generation and ResultData
 * wrapping/unwrapping.
 * <p>
 * Activated only when chat-server is on the classpath (standalone) AND feishu is enabled.
 * Registered as an auto-configuration via {@code META-INF/spring.factories}. Uses {@code @Primary}
 * to override the default {@link HttpSuperSonicApiClient}.
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.tencent.supersonic.chat.server.service.ChatQueryService")
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Primary
public class DirectSuperSonicApiClient implements SuperSonicApiClient {

    private static final String FEISHU_CHAT_NAME = "飞书助手";
    private static final String CHAT_CACHE_PREFIX = "chatId:";

    private final ChatQueryService chatQueryService;
    private final ChatManageService chatManageService;
    private final AgentService agentService;
    private final UserService userService;
    private final SemanticLayerService semanticLayerService;
    private final ChatLayerService chatLayerService;
    private final AlertRuleService alertRuleService;
    private final FeishuCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Override
    public QueryResult query(String queryText, int agentId, User user, Long dataSetId) {
        Integer chatId = getOrCreateChat(agentId, user);

        try {
            return doQuery(queryText, agentId, chatId, user, dataSetId);
        } catch (Exception e) {
            Integer freshChatId = evictAndRefreshChat(agentId, user);
            if (freshChatId != null && !freshChatId.equals(chatId)) {
                log.info("Retrying query with fresh chatId={} (was {})", freshChatId, chatId);
                try {
                    return doQuery(queryText, agentId, freshChatId, user, dataSetId);
                } catch (Exception retryEx) {
                    log.error("Query retry also failed: {}", retryEx.getMessage());
                    throw new RuntimeException("查询服务调用失败: " + retryEx.getMessage(), retryEx);
                }
            }
            throw new RuntimeException("查询服务调用失败: " + e.getMessage(), e);
        }
    }

    private QueryResult doQuery(String queryText, int agentId, int chatId, User user,
            Long dataSetId) {
        ChatParseReq req = ChatParseReq.builder().queryText(queryText).agentId(agentId)
                .chatId(chatId).dataSetId(dataSetId).user(user).build();
        return chatQueryService.parseAndExecute(req);
    }

    @Override
    public ChatParseResp parse(String queryText, int agentId, User user) {
        Integer chatId = getOrCreateChat(agentId, user);

        try {
            return doParse(queryText, agentId, chatId, user);
        } catch (Exception e) {
            Integer freshChatId = evictAndRefreshChat(agentId, user);
            if (freshChatId != null && !freshChatId.equals(chatId)) {
                log.info("Retrying parse with fresh chatId={} (was {})", freshChatId, chatId);
                try {
                    return doParse(queryText, agentId, freshChatId, user);
                } catch (Exception retryEx) {
                    log.error("Parse retry also failed: {}", retryEx.getMessage());
                    throw new RuntimeException("SQL预览失败: " + retryEx.getMessage(), retryEx);
                }
            }
            throw new RuntimeException("SQL预览失败: " + e.getMessage(), e);
        }
    }

    private ChatParseResp doParse(String queryText, int agentId, int chatId, User user) {
        ChatParseReq req = ChatParseReq.builder().queryText(queryText).agentId(agentId)
                .chatId(chatId).user(user).build();
        return chatQueryService.parse(req);
    }

    @Override
    public SemanticQueryResp queryBySql(String sql, Long dataSetId, User user) {
        QuerySqlReq req = new QuerySqlReq();
        req.setSql(StringUtil.replaceBackticks(sql));
        req.setDataSetId(dataSetId);
        req.setLimit(0);

        try {
            chatLayerService.correct(req, user);
            return semanticLayerService.queryByReq(req, user);
        } catch (Exception e) {
            log.error("Direct SQL query failed: {}", e.getMessage(), e);
            throw new RuntimeException("SQL查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public User getUserById(Long userId, Long tenantId) {
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            return userService.getUserById(userId);
        } catch (Exception e) {
            log.error("Failed to get user by id={}: {}", userId, e.getMessage(), e);
            return null;
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    public List<User> getUserList(Long tenantId) {
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            List<User> users = userService.getUserList();
            return users != null ? users : List.of();
        } catch (Exception e) {
            log.error("Failed to get user list: {}", e.getMessage(), e);
            return List.of();
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    public User login(String username, String password, Long tenantId) {
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            com.tencent.supersonic.auth.api.authentication.request.UserReq req =
                    new com.tencent.supersonic.auth.api.authentication.request.UserReq();
            req.setName(username);
            req.setPassword(password);
            // login() returns a JWT token on success, throws on failure
            userService.login(req, "feishu-bind");
            // Credentials valid — look up the User object by name
            return userService.getUserList().stream().filter(u -> username.equals(u.getName()))
                    .findFirst().orElse(null);
        } catch (Exception e) {
            log.debug("Login failed for user={}: {}", username, e.getMessage());
            return null;
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAgentList(User user) {
        try {
            List<Agent> agents = agentService.getAgents(user, null);
            return agents.stream().map(agent -> objectMapper.convertValue(agent, Map.class))
                    .map(map -> (Map<String, Object>) map).toList();
        } catch (Exception e) {
            log.error("Failed to get agent list: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public void transitionAlertEvent(Long eventId, String targetStatus, Long assigneeId,
            String notes, User user) {
        AlertResolutionStatus status;
        try {
            status = AlertResolutionStatus.valueOf(targetStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的告警状态: " + targetStatus, e);
        }
        try {
            AlertEventTransitionReq req = new AlertEventTransitionReq();
            req.setTargetStatus(status);
            req.setAssigneeId(assigneeId);
            req.setNotes(notes);
            alertRuleService.transitionEvent(eventId, req, user);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to transition alert event {}: {}", eventId, e.getMessage(), e);
            throw new RuntimeException("告警事件状态转换失败: " + e.getMessage(), e);
        }
    }

    // ── chatId management (same key format as HttpSuperSonicApiClient) ──

    private Integer getOrCreateChat(int agentId, User user) {
        String cacheKey = CHAT_CACHE_PREFIX + user.getId() + ":" + agentId;
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            return Integer.valueOf(cached);
        }
        Integer chatId = findOrCreateChat(agentId, user);
        if (chatId != null) {
            cacheService.put(cacheKey, String.valueOf(chatId));
        }
        return chatId;
    }

    private Integer findOrCreateChat(int agentId, User user) {
        // 1. Try to find an existing Feishu chat session
        try {
            List<ChatDO> chats = chatManageService.getAll(user, agentId, FEISHU_CHAT_NAME);
            if (chats != null && !chats.isEmpty()) {
                Long existingId = chats.getFirst().getChatId();
                if (existingId != null) {
                    log.info("Reusing Feishu chat session: chatId={}, agentId={}, userId={}",
                            existingId, agentId, user.getId());
                    return existingId.intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query existing chats, will create new: {}", e.getMessage());
        }

        // 2. No existing chat found — create a new one
        Long chatId = chatManageService.addChat(user, FEISHU_CHAT_NAME, agentId);
        log.info("Created new chat session: chatId={}, agentId={}, userId={}", chatId, agentId,
                user.getId());
        return chatId != null ? chatId.intValue() : null;
    }

    private Integer evictAndRefreshChat(int agentId, User user) {
        String cacheKey = CHAT_CACHE_PREFIX + user.getId() + ":" + agentId;
        cacheService.remove(cacheKey);
        log.info("Evicted stale chatId cache: key={}", cacheKey);
        return getOrCreateChat(agentId, user);
    }
}
