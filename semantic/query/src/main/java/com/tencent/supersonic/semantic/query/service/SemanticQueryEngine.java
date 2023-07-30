package com.tencent.supersonic.semantic.query.service;

import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.executor.QueryExecutor;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface SemanticQueryEngine {

    QueryStatement plan(QueryStructReq queryStructCmd) throws Exception;

    QueryExecutor route(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(QueryStatement queryStatement);

    QueryStatement physicalSql(ParseSqlReq sqlCommend) throws Exception;
}
