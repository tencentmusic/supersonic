package com.tencent.supersonic.headless.query.parser.calcite;

import com.tencent.supersonic.headless.api.query.enums.AggOption;
import com.tencent.supersonic.headless.api.query.request.MetricReq;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.parser.SqlParser;
import com.tencent.supersonic.headless.query.parser.calcite.planner.AggPlanner;
import com.tencent.supersonic.headless.query.parser.calcite.s2sql.HeadlessModel;
import com.tencent.supersonic.headless.query.parser.calcite.schema.HeadlessSchema;
import com.tencent.supersonic.headless.query.parser.calcite.schema.RuntimeOptions;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;
import org.springframework.stereotype.Component;

@Component("CalciteSqlParser")
public class CalciteSqlParser implements SqlParser {

    private final HeadlessSchemaManager headlessSchemaManager;

    public CalciteSqlParser(
            HeadlessSchemaManager headlessSchemaManager) {
        this.headlessSchemaManager = headlessSchemaManager;
    }

    @Override
    public QueryStatement explain(QueryStatement queryStatement, AggOption isAgg, Catalog catalog) throws Exception {
        MetricReq metricReq = queryStatement.getMetricReq();
        HeadlessModel headlessModel = headlessSchemaManager.get(metricReq.getRootPath());
        if (headlessModel == null) {
            queryStatement.setErrMsg("semanticSchema not found");
            return queryStatement;
        }
        queryStatement.setMetricReq(metricReq);
        HeadlessSchema headlessSchema = getSemanticSchema(headlessModel, queryStatement);
        AggPlanner aggBuilder = new AggPlanner(headlessSchema);
        aggBuilder.explain(queryStatement, isAgg);
        queryStatement.setSql(aggBuilder.getSql());
        queryStatement.setSourceId(aggBuilder.getSourceId());
        return queryStatement;
    }

    private HeadlessSchema getSemanticSchema(HeadlessModel headlessModel, QueryStatement queryStatement) {
        HeadlessSchema headlessSchema = HeadlessSchema.newBuilder(headlessModel.getRootPath()).build();
        headlessSchema.setDatasource(headlessModel.getDatasourceMap());
        headlessSchema.setDimension(headlessModel.getDimensionMap());
        headlessSchema.setMetric(headlessModel.getMetrics());
        headlessSchema.setJoinRelations(headlessModel.getJoinRelations());
        headlessSchema.setRuntimeOptions(RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                .enableOptimize(queryStatement.getEnableOptimize()).build());
        return headlessSchema;
    }
}
