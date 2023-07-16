package com.tencent.supersonic.semantic.query.domain;

import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.application.executor.QueryExecutor;
import com.tencent.supersonic.semantic.query.domain.pojo.QueryStatement;

public interface SemanticQueryEngine {

    QueryStatement plan(QueryStructReq queryStructCmd) throws Exception;

    QueryExecutor route(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(QueryStatement queryStatement);

    QueryStatement physicalSql(ParseSqlReq sqlCommend) throws Exception;
}
