package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * Query data or execute sql from a query engine
 */
public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(QueryStatement queryStatement);
}
