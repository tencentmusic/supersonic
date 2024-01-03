package com.tencent.supersonic.headless.core.service;

import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;

public interface HeadlessQueryEngine {

    QueryStatement plan(QueryStatement queryStatement) throws Exception;

    QueryExecutor route(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(QueryStatement queryStatement);

    QueryStatement physicalSql(QueryStructReq queryStructCmd, ParseSqlReq sqlCommend) throws Exception;

    QueryStatement physicalSql(QueryStructReq queryStructCmd, MetricQueryReq sqlCommend) throws Exception;
}
