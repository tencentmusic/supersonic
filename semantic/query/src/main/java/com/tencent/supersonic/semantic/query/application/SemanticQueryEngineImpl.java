package com.tencent.supersonic.semantic.query.application;

import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.core.domain.Catalog;
import com.tencent.supersonic.semantic.query.application.executor.QueryExecutor;
import com.tencent.supersonic.semantic.query.application.optimizer.QueryOptimizer;
import com.tencent.supersonic.semantic.query.application.parser.QueryParser;
import com.tencent.supersonic.semantic.query.domain.SemanticQueryEngine;
import com.tencent.supersonic.semantic.query.domain.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.domain.utils.ComponentFactory;
import com.tencent.supersonic.semantic.query.domain.utils.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SemanticQueryEngineImpl implements SemanticQueryEngine {

    private final QueryParser queryParser;
    private final Catalog catalog;
    private final QueryUtils queryUtils;

    public SemanticQueryEngineImpl(QueryParser queryParser, Catalog catalog,
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
            if (queryStatement.getDomainId() > 0) {
                queryUtils.fillItemNameInfo(queryResultWithColumns, queryStatement.getDomainId());
            }
        }
        return queryResultWithColumns;
    }

    public QueryStatement plan(QueryStructReq queryStructCmd) throws Exception {
        QueryStatement queryStatement = queryParser.logicSql(queryStructCmd);
        queryUtils.checkSqlParse(queryStatement);
        queryStatement.setDomainId(queryStructCmd.getDomainId());
        log.info("queryStatement:{}", queryStatement);
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
    public QueryStatement physicalSql(ParseSqlReq sqlCommend) throws Exception {
        return queryParser.parser(sqlCommend);
    }


    public QueryStatement physicalSql(MetricReq metricCommand) throws Exception {
        return queryParser.parser(metricCommand);
    }
}
