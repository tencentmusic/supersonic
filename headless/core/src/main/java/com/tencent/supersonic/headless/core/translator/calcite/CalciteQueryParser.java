package com.tencent.supersonic.headless.core.translator.calcite;

import com.tencent.supersonic.common.calcite.SqlMergeWithUtils;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.QueryParser;
import com.tencent.supersonic.headless.core.translator.calcite.planner.AggPlanner;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Ontology;
import com.tencent.supersonic.headless.core.translator.calcite.schema.RuntimeOptions;
import com.tencent.supersonic.headless.core.translator.calcite.schema.S2SemanticSchema;
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
        Ontology ontology = queryStatement.getOntology();
        if (ontology == null) {
            queryStatement.setErrMsg("semanticSchema not found");
            return;
        }
        queryStatement.setMetricQueryParam(metricReq);
        S2SemanticSchema semanticSchema = getSemanticSchema(ontology, queryStatement);
        AggPlanner aggPlanner = new AggPlanner(semanticSchema);
        aggPlanner.plan(queryStatement, isAgg);
        EngineType engineType = EngineType.fromString(ontology.getDatabase().getType());
        queryStatement.setSql(aggPlanner.getSql(engineType));
        if (Objects.nonNull(queryStatement.getEnableOptimize())
                && queryStatement.getEnableOptimize()
                && Objects.nonNull(queryStatement.getDataSetAlias())
                && !queryStatement.getDataSetAlias().isEmpty()) {
            // simplify model sql with query sql
            String simplifySql = aggPlanner.simplify(
                    getSqlByDataSet(engineType, aggPlanner.getSql(engineType),
                            queryStatement.getDataSetSql(), queryStatement.getDataSetAlias()),
                    engineType);
            if (Objects.nonNull(simplifySql) && !simplifySql.isEmpty()) {
                log.debug("simplifySql [{}]", simplifySql);
                queryStatement.setDataSetSimplifySql(simplifySql);
            }
        }
    }

    private S2SemanticSchema getSemanticSchema(Ontology ontology, QueryStatement queryStatement) {
        S2SemanticSchema semanticSchema =
                S2SemanticSchema.newBuilder(ontology.getSchemaKey()).build();
        semanticSchema.setSemanticModel(ontology);
        semanticSchema.setDatasource(ontology.getDatasourceMap());
        semanticSchema.setDimension(ontology.getDimensionMap());
        semanticSchema.setMetric(ontology.getMetrics());
        semanticSchema.setJoinRelations(ontology.getJoinRelations());
        semanticSchema.setRuntimeOptions(
                RuntimeOptions.builder().minMaxTime(queryStatement.getMinMaxTime())
                        .enableOptimize(queryStatement.getEnableOptimize()).build());
        return semanticSchema;
    }

    private String getSqlByDataSet(EngineType engineType, String parentSql, String dataSetSql,
            String parentAlias) throws SqlParseException {
        if (!SqlMergeWithUtils.hasWith(engineType, dataSetSql)) {
            return String.format("with %s as (%s) %s", parentAlias, parentSql, dataSetSql);
        }
        return SqlMergeWithUtils.mergeWith(engineType, dataSetSql,
                Collections.singletonList(parentSql), Collections.singletonList(parentAlias));
    }
}
