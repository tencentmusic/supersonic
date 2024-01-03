package com.tencent.supersonic.headless.core.optimizer;

import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

public interface QueryOptimizer {
    void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement);
}
