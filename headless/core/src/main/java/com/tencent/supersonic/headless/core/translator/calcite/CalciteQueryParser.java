package com.tencent.supersonic.headless.core.translator.calcite;

import com.tencent.supersonic.common.calcite.SqlMergeWithUtils;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.QueryParser;
import com.tencent.supersonic.headless.core.translator.calcite.planner.AggPlanner;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.SemanticModel;
import com.tencent.supersonic.headless.core.translator.calcite.schema.RuntimeOptions;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;

/** the calcite parse implements */
@Component("CalciteQueryParser")
@Slf4j
public class CalciteQueryParser implements QueryParser {

    @Override
    public void parse(QueryStatement queryStatement, AggOption isAgg) throws Exception {
        MetricQueryParam metricReq = queryStatement.getMetricQueryParam();
        SemanticModel semanticModel = queryStatement.getSemanticModel();
        if (semanticModel == null) {
            queryStatement.setErrMsg("semanticSchema not found");
            return;
        }
        queryStatement.setMetricQueryParam(metricReq);
        SemanticSchema semanticSchema = getSemanticSchema(semanticModel, queryStatement);
        AggPlanner aggBuilder = new AggPlanner(semanticSchema);
        aggBuilder.explain(queryStatement, isAgg);
        EngineType engineType = EngineType.fromString(semanticModel.getDatabase().getType());
        queryStatement.setSql(aggBuilder.getSql(engineType));
        if (Objects.nonNull(queryStatement.getEnableOptimize())
                && queryStatement.getEnableOptimize()
                && Objects.nonNull(queryStatement.getDataSetAlias())
                && !queryStatement.getDataSetAlias().isEmpty()) {
            // simplify model sql with query sql
            String simplifySql = aggBuilder.simplify(
                    getSqlByDataSet(engineType, aggBuilder.getSql(engineType),
                            queryStatement.getDataSetSql(), queryStatement.getDataSetAlias()),
                    engineType);
            if (Objects.nonNull(simplifySql) && !simplifySql.isEmpty()) {
                log.debug("simplifySql [{}]", simplifySql);
                queryStatement.setDataSetSimplifySql(simplifySql);
            }
        }
    }

    private SemanticSchema getSemanticSchema(SemanticModel semanticModel,
            QueryStatement queryStatement) {
        SemanticSchema semanticSchema =
                SemanticSchema.newBuilder(semanticModel.getSchemaKey()).build();
        semanticSchema.setSemanticModel(semanticModel);
        semanticSchema.setDatasource(semanticModel.getDatasourceMap());
        semanticSchema.setDimension(semanticModel.getDimensionMap());
        semanticSchema.setMetric(semanticModel.getMetrics());
        semanticSchema.setJoinRelations(semanticModel.getJoinRelations());
        semanticSchema.setRuntimeOptions(
                RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                        .enableOptimize(queryStatement.getEnableOptimize()).build());
        return semanticSchema;
    }

    private String getSqlByDataSet(EngineType engineType, String parentSql, String dataSetSql,
            String parentAlias) throws SqlParseException {
        return SqlMergeWithUtils.mergeWith(engineType, dataSetSql,
                Collections.singletonList(parentSql), Collections.singletonList(parentAlias));
    }
}
