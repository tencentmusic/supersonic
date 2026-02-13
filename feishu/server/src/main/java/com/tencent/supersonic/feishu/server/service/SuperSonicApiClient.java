package com.tencent.supersonic.feishu.server.service;

import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;

import java.util.List;
import java.util.Map;

/**
 * Client for calling SuperSonic core APIs (chat query, SQL execution, user/agent lookup).
 * <p>
 * Two implementations:
 * <ul>
 * <li>{@code HttpSuperSonicApiClient} — HTTP loopback via REST (microservice deployment)</li>
 * <li>{@code DirectSuperSonicApiClient} — direct Service-layer calls (standalone deployment)</li>
 * </ul>
 */
public interface SuperSonicApiClient {

    /**
     * Execute a natural language query (combined parse + execute).
     *
     * @param dataSetId optional dataSetId to narrow dataset routing for follow-up queries
     */
    QueryResult query(String queryText, int agentId, User user, Long dataSetId);

    /**
     * Parse a natural language query without executing. Returns the parse result with SQL preview.
     */
    ChatParseResp parse(String queryText, int agentId, User user);

    /**
     * Execute a SQL query directly.
     */
    SemanticQueryResp queryBySql(String sql, Long dataSetId, User user);

    /**
     * Get a user by ID, scoped to the given tenant.
     */
    User getUserById(Long userId, Long tenantId);

    /**
     * Get all users, scoped to the given tenant.
     */
    List<User> getUserList(Long tenantId);

    /**
     * Get the list of agents visible to the given user.
     */
    List<Map<String, Object>> getAgentList(User user);
}
