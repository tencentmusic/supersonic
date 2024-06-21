package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query optimizer rewrites physical SQL by following a set of
 * optimization rules, trying to derive the most efficient query.
 */
public interface QueryOptimizer {
    void rewrite(QueryStatement queryStatement);
}
