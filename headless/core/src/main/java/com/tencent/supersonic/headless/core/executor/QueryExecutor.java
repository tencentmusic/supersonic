package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(QueryStatement queryStatement);
}
