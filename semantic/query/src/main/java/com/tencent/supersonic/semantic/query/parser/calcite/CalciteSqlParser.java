package com.tencent.supersonic.semantic.query.parser.calcite;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.parser.SqlParser;
import com.tencent.supersonic.semantic.query.parser.calcite.planner.AggPlanner;
import com.tencent.supersonic.semantic.query.parser.calcite.dsl.SemanticModel;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import org.springframework.stereotype.Component;

@Component("CalciteSqlParser")
public class CalciteSqlParser implements SqlParser {
    private final SemanticSchemaManager semanticSchemaManager;

    public CalciteSqlParser(
            SemanticSchemaManager semanticSchemaManager) {
        this.semanticSchemaManager = semanticSchemaManager;
    }

    @Override
    public QueryStatement explain(MetricReq metricReq, boolean isAgg, Catalog catalog) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        SemanticModel semanticModel = semanticSchemaManager.get(metricReq.getRootPath());
        if (semanticModel == null) {
            queryStatement.setErrMsg("semanticSchema not found");
            return queryStatement;
        }
        SemanticSchema semanticSchema = getSemanticSchema(semanticModel);
        AggPlanner aggBuilder = new AggPlanner(semanticSchema);
        aggBuilder.explain(metricReq, isAgg);
        queryStatement.setSql(aggBuilder.getSql());
        queryStatement.setSourceId(aggBuilder.getSourceId());
        return queryStatement;
    }

    private SemanticSchema getSemanticSchema(SemanticModel semanticModel) {
        SemanticSchema semanticSchema = SemanticSchema.newBuilder(semanticModel.getRootPath()).build();
        semanticSchema.setDatasource(semanticModel.getDatasourceMap());
        semanticSchema.setDimension(semanticModel.getDimensionMap());
        semanticSchema.setMetric(semanticModel.getMetrics());
        return semanticSchema;
    }
}
