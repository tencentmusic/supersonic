package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryRuleReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryRuleResp;

import java.util.List;

public interface QueryRuleService {

    QueryRuleResp addQueryRule(QueryRuleReq queryRuleReq, User user);

    QueryRuleResp updateQueryRule(QueryRuleReq queryRuleReq, User user);

    Boolean dropQueryRule(Long id, User user);

    QueryRuleResp getQueryRuleById(Long id, User user);

    List<QueryRuleResp> getQueryRuleList(QueryRuleFilter queryRuleFilter, User user);
}
