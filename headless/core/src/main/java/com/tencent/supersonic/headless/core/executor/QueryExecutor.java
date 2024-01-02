package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.headless.common.server.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.service.Catalog;

public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(Catalog catalog, QueryStatement queryStatement);
}
