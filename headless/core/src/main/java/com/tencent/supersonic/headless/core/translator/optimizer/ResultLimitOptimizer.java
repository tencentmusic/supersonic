package com.tencent.supersonic.headless.core.translator.optimizer;

import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("ResultLimitOptimizer")
public class ResultLimitOptimizer implements QueryOptimizer {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return !SqlSelectHelper.hasLimit(queryStatement.getSql());
    }

    @Override
    public void rewrite(QueryStatement queryStatement) {
        queryStatement.setSql(queryStatement.getSql() + " LIMIT " + queryStatement.getLimit());
    }
}
