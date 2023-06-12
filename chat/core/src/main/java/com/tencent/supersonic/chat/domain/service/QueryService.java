package com.tencent.supersonic.chat.domain.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.domain.pojo.chat.QueryData;

/***
 * QueryService for query and search
 */
public interface QueryService {

    public QueryResultResp executeQuery(QueryContextReq queryCtx) throws Exception;

    public SemanticParseInfo queryContext(QueryContextReq queryCtx);

    QueryResultResp queryData(QueryData queryData, User user) throws Exception;
}
