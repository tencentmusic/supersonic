package com.tencent.supersonic.headless.core.parser.calcite;

import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.request.MetricQueryReq;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.calcite.planner.AggPlanner;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.parser.calcite.schema.RuntimeOptions;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * the calcite parse implements
 */
@Component("CalciteSqlParser")
@Slf4j
public class CalciteSqlParser implements SqlParser {

    @Override
    public QueryStatement explain(QueryStatement queryStatement, AggOption isAgg) throws Exception {
        MetricQueryReq metricReq = queryStatement.getMetricReq();
        SemanticModel semanticModel = queryStatement.getSemanticModel();
        if (semanticModel == null) {
            queryStatement.setErrMsg("semanticSchema not found");
            return queryStatement;
        }
        queryStatement.setMetricReq(metricReq);
        SemanticSchema semanticSchema = getSemanticSchema(semanticModel, queryStatement);
        AggPlanner aggBuilder = new AggPlanner(semanticSchema);
        aggBuilder.explain(queryStatement, isAgg);
        EngineType engineType = EngineType.fromString(semanticSchema.getSemanticModel().getDatabase().getType());
        queryStatement.setSql(aggBuilder.getSql(engineType));
        queryStatement.setSourceId(aggBuilder.getSourceId());
        if (Objects.nonNull(queryStatement.getEnableOptimize()) && queryStatement.getEnableOptimize()
                && Objects.nonNull(queryStatement.getViewAlias()) && !queryStatement.getViewAlias().isEmpty()) {
            // simplify model sql with query sql
            String simplifySql = aggBuilder.simplify(
                    getSqlByView(aggBuilder.getSql(engineType), queryStatement.getViewSql(),
                            queryStatement.getViewAlias()), engineType);
            if (Objects.nonNull(simplifySql) && !simplifySql.isEmpty()) {
                log.debug("simplifySql [{}]", simplifySql);
                queryStatement.setViewSimplifySql(simplifySql);
            }
        }
        return queryStatement;
    }

    private SemanticSchema getSemanticSchema(SemanticModel semanticModel, QueryStatement queryStatement) {
        SemanticSchema semanticSchema = SemanticSchema.newBuilder(semanticModel.getSchemaKey()).build();
        semanticSchema.setSemanticModel(semanticModel);
        semanticSchema.setDatasource(semanticModel.getDatasourceMap());
        semanticSchema.setDimension(semanticModel.getDimensionMap());
        semanticSchema.setMetric(semanticModel.getMetrics());
        semanticSchema.setJoinRelations(semanticModel.getJoinRelations());
        semanticSchema.setRuntimeOptions(RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                .enableOptimize(queryStatement.getEnableOptimize()).build());
        return semanticSchema;
    }

    private String getSqlByView(String sql, String viewSql, String viewAlias) {
        return String.format("with %s as (%s) %s", viewAlias, sql, viewSql);
    }
}
