package com.tencent.supersonic.semantic.query.executor;

import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(Catalog catalog, QueryStatement queryStatement);
}
