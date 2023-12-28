package com.tencent.supersonic.headless.query.service;

import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.query.request.MetricReq;
import com.tencent.supersonic.headless.common.query.request.ParseSqlReq;
import com.tencent.supersonic.headless.common.query.request.QueryStructReq;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.query.executor.QueryExecutor;
import com.tencent.supersonic.headless.query.optimizer.QueryOptimizer;
import com.tencent.supersonic.headless.query.parser.QueryParser;
import com.tencent.supersonic.headless.query.utils.ComponentFactory;
import com.tencent.supersonic.headless.query.utils.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class HeadlessQueryEngineImpl implements HeadlessQueryEngine {

    private final QueryParser queryParser;
    private final Catalog catalog;
    private final QueryUtils queryUtils;

    public HeadlessQueryEngineImpl(QueryParser queryParser, Catalog catalog,
            QueryUtils queryUtils) {
        this.queryParser = queryParser;
        this.catalog = catalog;
        this.queryUtils = queryUtils;
    }

    public QueryResultWithSchemaResp execute(QueryStatement queryStatement) {
        QueryResultWithSchemaResp queryResultWithColumns = null;
        QueryExecutor queryExecutor = route(queryStatement);
        if (queryExecutor != null) {
            queryResultWithColumns = queryExecutor.execute(catalog, queryStatement);
            queryResultWithColumns.setSql(queryStatement.getSql());
            if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                queryUtils.fillItemNameInfo(queryResultWithColumns, queryStatement.getModelIds());
            }
        }
        return queryResultWithColumns;
    }

    public QueryStatement plan(QueryStatement queryStatement) throws Exception {
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement = queryParser.logicSql(queryStatement);
        queryUtils.checkSqlParse(queryStatement);
        queryStatement.setModelIds(queryStatement.getQueryStructReq().getModelIds());
        log.info("queryStatement:{}", queryStatement);
        return optimize(queryStatement.getQueryStructReq(), queryStatement);
    }

    public QueryStatement optimize(QueryStructReq queryStructCmd, QueryStatement queryStatement) {
        for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
            queryOptimizer.rewrite(queryStructCmd, queryStatement);
        }
        return queryStatement;
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
    public QueryStatement physicalSql(QueryStructReq queryStructCmd, ParseSqlReq sqlCommend) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        queryStatement.setQueryStructReq(queryStructCmd);
        queryStatement.setParseSqlReq(sqlCommend);
        queryStatement.setIsS2SQL(true);
        return optimize(queryStructCmd, queryParser.parser(sqlCommend, queryStatement));
    }

    public QueryStatement physicalSql(QueryStructReq queryStructCmd, MetricReq metricCommand) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        queryStatement.setQueryStructReq(queryStructCmd);
        queryStatement.setMetricReq(metricCommand);
        queryStatement.setIsS2SQL(false);
        return queryParser.parser(queryStatement);
    }

}
