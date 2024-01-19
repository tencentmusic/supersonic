package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.parser.QueryParser;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.planner.QueryOptimizer;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.manager.SemanticSchemaManager;
import com.tencent.supersonic.headless.server.service.SemantciQueryEngine;
import com.tencent.supersonic.headless.server.utils.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class SemantciQueryEngineImpl implements SemantciQueryEngine {

    private final QueryParser queryParser;
    private final QueryUtils queryUtils;
    private final SemanticSchemaManager semanticSchemaManager;

    public SemantciQueryEngineImpl(QueryParser queryParser,
            QueryUtils queryUtils, SemanticSchemaManager semanticSchemaManager) {
        this.queryParser = queryParser;
        this.queryUtils = queryUtils;
        this.semanticSchemaManager = semanticSchemaManager;
    }

    public SemanticQueryResp execute(QueryStatement queryStatement) {
        SemanticQueryResp queryResultWithColumns = null;
        QueryExecutor queryExecutor = route(queryStatement);
        if (queryExecutor != null) {
            queryResultWithColumns = queryExecutor.execute(queryStatement);
            queryResultWithColumns.setSql(queryStatement.getSql());
            if (!CollectionUtils.isEmpty(queryStatement.getModelIds())) {
                queryUtils.fillItemNameInfo(queryResultWithColumns, queryStatement.getModelIds());
            }
        }
        return queryResultWithColumns;
    }

    public QueryStatement plan(QueryStatement queryStatement) throws Exception {
        queryStatement.setEnableOptimize(queryUtils.enableOptimize());
        queryStatement.setSemanticModel(getSemanticModel(queryStatement));
        queryStatement = queryParser.parse(queryStatement);
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
        queryStatement.setSql(sqlCommend.getSql());
        queryStatement.setIsS2SQL(true);
        queryStatement.setSemanticModel(getSemanticModel(queryStatement));
        return optimize(queryStructCmd, queryParser.parser(sqlCommend, queryStatement));
    }

    private SemanticModel getSemanticModel(QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructReq = queryStatement.getQueryStructReq();
        return semanticSchemaManager.get(queryStructReq.getModelIdStr());
    }

}
