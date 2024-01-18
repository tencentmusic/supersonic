package com.tencent.supersonic.headless.core.planner;

import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * the interface that rewrites QueryStatement with some optimization rules
 */
public interface QueryOptimizer {
    void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement);
}
