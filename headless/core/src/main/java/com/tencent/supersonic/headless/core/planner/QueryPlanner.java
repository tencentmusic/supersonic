package com.tencent.supersonic.headless.core.planner;

import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.executor.accelerator.QueryAccelerator;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;

/**
 * A query planner takes parsed QueryStatement and generates an optimized execution plan.
 * It interacts with the optimizer to determine the most efficient way to execute the query.
 */
public interface QueryPlanner {
    QueryExecutor plan(QueryStatement queryStatement);

    QueryExecutor route(QueryStatement queryStatement);

    QueryAccelerator accelerate(QueryStatement queryStatement);
}
