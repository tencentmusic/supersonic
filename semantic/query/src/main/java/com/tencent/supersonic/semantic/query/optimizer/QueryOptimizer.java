package com.tencent.supersonic.semantic.query.optimizer;

import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface QueryOptimizer {
    void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement);
}
