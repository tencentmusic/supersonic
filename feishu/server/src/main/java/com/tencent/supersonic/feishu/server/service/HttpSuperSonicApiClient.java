package com.tencent.supersonic.feishu.server.service;

import com.tencent.supersonic.auth.api.authentication.service.InternalTokenGenerator;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP-based implementation of {@link SuperSonicApiClient}. Calls SuperSonic REST APIs via HTTP
 * loopback (localhost). Active only in microservice deployments where chat-server is NOT on the
 * classpath.
 */
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@Slf4j
public class HttpSuperSonicApiClient implements SuperSonicApiClient {

    private static final String FEISHU_CHAT_NAME = "飞书助手";
    private static final String CHAT_CACHE_PREFIX = "chatId:";

    private final InternalTokenGenerator tokenGenerator;
    private final FeishuProperties properties;
    private final RestTemplate restTemplate;
    private final FeishuCacheService cacheService;

    public HttpSuperSonicApiClient(InternalTokenGenerator tokenGenerator,
            FeishuProperties properties, FeishuCacheService cacheService) {
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.cacheService = cacheService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getQueryTimeoutMs());
        factory.setReadTimeout((int) properties.getQueryTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public QueryResult query(String queryText, int agentId, User user, Long dataSetId) {
        Integer chatId = getOrCreateChat(agentId, user);
        String url = properties.getApiBaseUrl() + "/api/chat/query/";

        try {
            return doQuery(url, queryText, agentId, chatId, user, dataSetId);
        } catch (Exception e) {
            // Retry once with a fresh chatId in case the cached one was stale
            Integer freshChatId = evictAndRefreshChat(agentId, user);
            if (freshChatId != null && !freshChatId.equals(chatId)) {
                log.info("Retrying query with fresh chatId={} (was {})", freshChatId, chatId);
                try {
                    return doQuery(url, queryText, agentId, freshChatId, user, dataSetId);
                } catch (Exception retryEx) {
                    log.error("Query retry also failed: {}", retryEx.getMessage());
                    throw new RuntimeException("查询服务调用失败: " + retryEx.getMessage(), retryEx);
                }
            }
            throw new RuntimeException("查询服务调用失败: " + e.getMessage(), e);
        }
    }

    private QueryResult doQuery(String url, String queryText, int agentId, int chatId, User user,
            Long dataSetId) {
        ChatParseReq req = ChatParseReq.builder().queryText(queryText).agentId(agentId)
                .chatId(chatId).dataSetId(dataSetId).build();
        HttpEntity<ChatParseReq> entity = new HttpEntity<>(req, buildHeaders(user));
        return exchangeWithRetry(url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {});
    }

    private Integer evictAndRefreshChat(int agentId, User user) {
        String cacheKey = CHAT_CACHE_PREFIX + user.getId() + ":" + agentId;
        cacheService.remove(cacheKey);
        log.info("Evicted stale chatId cache: key={}", cacheKey);
        return getOrCreateChat(agentId, user);
    }

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
        String baseUrl = properties.getApiBaseUrl();

        // 1. Try to find an existing Feishu chat session (by chatName) to avoid mixing with web
        // sessions
        String getAllUrl = baseUrl + "/api/chat/manage/getAll?agentId=" + agentId + "&chatName="
                + FEISHU_CHAT_NAME;
        try {
            ResponseEntity<ResultData<List<Map<String, Object>>>> response = restTemplate.exchange(
                    getAllUrl, HttpMethod.GET, new HttpEntity<>(buildHeaders(user)),
                    new ParameterizedTypeReference<ResultData<List<Map<String, Object>>>>() {});
            List<Map<String, Object>> chats = extractData(response);
            if (chats != null && !chats.isEmpty()) {
                Number existingId = (Number) chats.getFirst().get("chatId");
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
        String saveUrl = baseUrl + "/api/chat/manage/save?chatName=" + FEISHU_CHAT_NAME
                + "&agentId=" + agentId;
        ResponseEntity<ResultData<Long>> response = restTemplate.exchange(saveUrl, HttpMethod.POST,
                new HttpEntity<>(buildHeaders(user)),
                new ParameterizedTypeReference<ResultData<Long>>() {});
        Long chatId = extractData(response);
        log.info("Created new chat session: chatId={}, agentId={}, userId={}", chatId, agentId,
                user.getId());
        return chatId != null ? chatId.intValue() : null;
    }

    @Override
    public ChatParseResp parse(String queryText, int agentId, User user) {
        Integer chatId = getOrCreateChat(agentId, user);
        String url = properties.getApiBaseUrl() + "/api/chat/query/parse";

        try {
            return doParse(url, queryText, agentId, chatId, user);
        } catch (Exception e) {
            Integer freshChatId = evictAndRefreshChat(agentId, user);
            if (freshChatId != null && !freshChatId.equals(chatId)) {
                log.info("Retrying parse with fresh chatId={} (was {})", freshChatId, chatId);
                try {
                    return doParse(url, queryText, agentId, freshChatId, user);
                } catch (Exception retryEx) {
                    log.error("Parse retry also failed: {}", retryEx.getMessage());
                    throw new RuntimeException("SQL预览失败: " + retryEx.getMessage(), retryEx);
                }
            }
            throw new RuntimeException("SQL预览失败: " + e.getMessage(), e);
        }
    }

    private ChatParseResp doParse(String url, String queryText, int agentId, int chatId,
            User user) {
        ChatParseReq req =
                ChatParseReq.builder().queryText(queryText).agentId(agentId).chatId(chatId).build();
        HttpEntity<ChatParseReq> entity = new HttpEntity<>(req, buildHeaders(user));
        return exchangeWithRetry(url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {});
    }

    @Override
    public SemanticQueryResp queryBySql(String sql, Long dataSetId, User user) {
        String url = properties.getApiBaseUrl() + "/api/semantic/query/sql";
        QuerySqlReq req = new QuerySqlReq();
        req.setSql(sql);
        req.setDataSetId(dataSetId);
        req.setLimit(0);

        HttpEntity<QuerySqlReq> entity = new HttpEntity<>(req, buildHeaders(user));

        try {
            return exchangeWithRetry(url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("SQL query failed: POST {}, error: {}", url, e.getMessage(), e);
            throw new RuntimeException("SQL查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public User getUserById(Long userId, Long tenantId) {
        String url = properties.getApiBaseUrl() + "/api/auth/user/" + userId;
        HttpEntity<Void> entity = new HttpEntity<>(buildInternalHeaders(tenantId));

        try {
            ResponseEntity<ResultData<User>> response = restTemplate.exchange(url, HttpMethod.GET,
                    entity, new ParameterizedTypeReference<ResultData<User>>() {});
            return extractData(response);
        } catch (Exception e) {
            log.error("Failed to get user by id={}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<User> getUserList(Long tenantId) {
        String url = properties.getApiBaseUrl() + "/api/auth/user/getUserList";
        HttpEntity<Void> entity = new HttpEntity<>(buildInternalHeaders(tenantId));

        try {
            ResponseEntity<ResultData<List<User>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity,
                            new ParameterizedTypeReference<ResultData<List<User>>>() {});
            List<User> users = extractData(response);
            return users != null ? users : List.of();
        } catch (Exception e) {
            log.error("Failed to get user list: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public User login(String username, String password, Long tenantId) {
        String loginUrl = properties.getApiBaseUrl() + "/api/auth/user/login";
        Map<String, String> loginReq = Map.of("name", username, "password", password);
        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(loginReq, buildInternalHeaders(tenantId));

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(loginUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Login succeeded — find user by name
                List<User> users = getUserList(tenantId);
                return users.stream().filter(u -> username.equals(u.getName())).findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("Login failed for user={}: {}", username, e.getMessage());
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getAgentList(User user) {
        String url = properties.getApiBaseUrl() + "/api/chat/agent/getAgentList";

        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(user));

        try {
            ResponseEntity<ResultData<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<ResultData<List<Map<String, Object>>>>() {});
            return extractData(response);
        } catch (Exception e) {
            log.error("Failed to get agent list: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public void transitionAlertEvent(Long eventId, String targetStatus, Long assigneeId,
            String notes, User user) {
        String url =
                properties.getApiBaseUrl() + "/api/v1/alertRules/events/" + eventId + ":transition";
        HttpHeaders headers = buildHeaders(user);
        Map<String, Object> body = new HashMap<>();
        body.put("targetStatus", targetStatus);
        if (assigneeId != null) {
            body.put("assigneeId", assigneeId);
        }
        if (notes != null) {
            body.put("notes", notes);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, entity, String.class);
        } catch (Exception e) {
            log.error("Failed to transition alert event {}: {}", eventId, e.getMessage());
            throw e;
        }
    }

    private <T> T exchangeWithRetry(String url, HttpMethod method, HttpEntity<?> entity,
            ParameterizedTypeReference<ResultData<T>> responseType) {
        ResourceAccessException lastException = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ResponseEntity<ResultData<T>> response =
                        restTemplate.exchange(url, method, entity, responseType);
                return extractData(response);
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("Transient I/O error calling {} (attempt {}/2): {}", url, attempt,
                        e.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastException;
    }

    private <T> T extractData(ResponseEntity<ResultData<T>> response) {
        ResultData<T> body = response.getBody();
        if (body == null) {
            return null;
        }
        if (body.getCode() != 200) {
            throw new RuntimeException(
                    "API error: code=" + body.getCode() + ", msg=" + body.getMsg());
        }
        return body.getData();
    }

    private HttpHeaders buildInternalHeaders(Long tenantId) {
        User internal = User.builder().name("feishu-internal").tenantId(tenantId).build();
        return buildHeaders(internal);
    }

    private HttpHeaders buildHeaders(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String token = tokenGenerator.generateToken(user);
        headers.set("Authorization", "Bearer " + token);

        if (user.getTenantId() != null) {
            headers.set("X-Tenant-Id", String.valueOf(user.getTenantId()));
        }

        return headers;
    }
}
