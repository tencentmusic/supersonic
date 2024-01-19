package com.tencent.supersonic.headless.core.planner;

import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QueryPlanner {

    public QueryExecutor plan(QueryStatement queryStatement) {
        optimizer(queryStatement);
        return route(queryStatement);
    }

    public void optimizer(QueryStatement queryStatement) {
        for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
            queryOptimizer.rewrite(queryStatement.getQueryStructReq(), queryStatement);
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
}
