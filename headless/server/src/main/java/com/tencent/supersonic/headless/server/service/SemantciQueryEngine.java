package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

public interface SemantciQueryEngine {

    QueryStatement plan(QueryStatement queryStatement) throws Exception;

    QueryExecutor route(QueryStatement queryStatement);

    SemanticQueryResp execute(QueryStatement queryStatement);

    QueryStatement physicalSql(QueryStructReq queryStructCmd, ParseSqlReq sqlCommend) throws Exception;

}
