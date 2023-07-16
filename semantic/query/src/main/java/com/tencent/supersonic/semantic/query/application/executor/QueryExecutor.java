package com.tencent.supersonic.semantic.query.application.executor;

import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.core.domain.Catalog;
import com.tencent.supersonic.semantic.query.domain.pojo.QueryStatement;

public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(Catalog catalog,QueryStatement queryStatement);
}
