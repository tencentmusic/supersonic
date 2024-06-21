package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * customize various query media ( like duckDb redis) to improved query performance
 * check ok and query successful , return SemanticQueryResp to interface immediately
 */
public interface QueryAccelerator {

    boolean reload();

    boolean check(QueryStatement queryStatement);

    SemanticQueryResp query(QueryStatement queryStatement);
}
