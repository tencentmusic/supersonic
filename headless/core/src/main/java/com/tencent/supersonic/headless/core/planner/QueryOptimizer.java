package com.tencent.supersonic.headless.core.planner;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query optimizer rewrites QueryStatement with a set of optimization rules
 */
public interface QueryOptimizer {
    void rewrite(QueryStatement queryStatement);
}
