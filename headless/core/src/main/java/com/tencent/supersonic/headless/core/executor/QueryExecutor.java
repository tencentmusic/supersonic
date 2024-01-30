package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * Query data or execute sql from a query engine
 */
public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    SemanticQueryResp execute(QueryStatement queryStatement);
}
