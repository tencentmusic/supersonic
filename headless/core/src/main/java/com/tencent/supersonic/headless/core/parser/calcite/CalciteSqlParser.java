package com.tencent.supersonic.headless.core.parser.calcite;

import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.calcite.planner.AggPlanner;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.HeadlessModel;
import com.tencent.supersonic.headless.core.parser.calcite.schema.HeadlessSchema;
import com.tencent.supersonic.headless.core.parser.calcite.schema.RuntimeOptions;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import org.springframework.stereotype.Component;

@Component("CalciteSqlParser")
public class CalciteSqlParser implements SqlParser {

    @Override
    public QueryStatement explain(QueryStatement queryStatement, AggOption isAgg) throws Exception {
        MetricQueryReq metricReq = queryStatement.getMetricReq();
        HeadlessModel headlessModel = queryStatement.getHeadlessModel();
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
