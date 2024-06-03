package com.tencent.supersonic.headless.core.planner;

import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.executor.accelerator.QueryAccelerator;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultQueryPlanner implements QueryPlanner {

    public QueryExecutor plan(QueryStatement queryStatement) {
        optimize(queryStatement);
        return route(queryStatement);
    }

    private void optimize(QueryStatement queryStatement) {
        for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
            queryOptimizer.rewrite(queryStatement);
        }
    }

    public QueryExecutor route(QueryStatement queryStatement) {
        for (QueryExecutor queryExecutor : ComponentFactory.getQueryExecutors()) {
            if (queryExecutor.accept(queryStatement)) {
                return queryExecutor;
            }
        }
        return null;
    }

    @Override
    public QueryAccelerator accelerate(QueryStatement queryStatement) {
        for (QueryAccelerator queryAccelerator : ComponentFactory.getQueryAccelerators()) {
            if (queryAccelerator.check(queryStatement)) {
                return queryAccelerator;
            }
        }
        return null;
    }
}
