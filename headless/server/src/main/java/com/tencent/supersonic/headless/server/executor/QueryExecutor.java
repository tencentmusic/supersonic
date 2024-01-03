package com.tencent.supersonic.headless.server.executor;

import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.service.Catalog;

public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(Catalog catalog, QueryStatement queryStatement);
}
