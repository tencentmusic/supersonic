package com.tencent.supersonic.headless.core.service;

import com.tencent.supersonic.headless.common.server.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.core.request.MetricReq;
import com.tencent.supersonic.headless.common.core.request.ParseSqlReq;
import com.tencent.supersonic.headless.common.core.request.QueryStructReq;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;

public interface HeadlessQueryEngine {

    QueryStatement plan(QueryStatement queryStatement) throws Exception;

    QueryExecutor route(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(QueryStatement queryStatement);

    QueryStatement physicalSql(QueryStructReq queryStructCmd, ParseSqlReq sqlCommend) throws Exception;

    QueryStatement physicalSql(QueryStructReq queryStructCmd, MetricReq sqlCommend) throws Exception;
}
