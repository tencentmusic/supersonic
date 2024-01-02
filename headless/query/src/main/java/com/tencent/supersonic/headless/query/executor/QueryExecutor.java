package com.tencent.supersonic.headless.query.executor;

import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;

public interface QueryExecutor {

    boolean accept(QueryStatement queryStatement);

    QueryResultWithSchemaResp execute(Catalog catalog, QueryStatement queryStatement);
}
