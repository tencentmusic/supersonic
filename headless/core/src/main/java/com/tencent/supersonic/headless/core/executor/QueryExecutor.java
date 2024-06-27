package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * QueryExecutor submits SQL to the database engine and performs acceleration if necessary.
 */
public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    SemanticQueryResp execute(QueryStatement queryStatement);
}
