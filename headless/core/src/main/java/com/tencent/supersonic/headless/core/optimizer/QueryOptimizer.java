package com.tencent.supersonic.headless.core.optimizer;

import com.tencent.supersonic.headless.common.core.request.QueryStructReq;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;

public interface QueryOptimizer {
    void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement);
}
