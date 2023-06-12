package com.tencent.supersonic.semantic.query.application;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.query.domain.parser.SemanticSqlService;
import com.tencent.supersonic.semantic.query.domain.parser.convertor.planner.AggPlanner;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.SemanticModel;
import com.tencent.supersonic.semantic.query.domain.parser.schema.SemanticSchema;
import org.springframework.stereotype.Service;

@Service("SemanticSqlService")
public class SemanticSqlServiceImpl implements SemanticSqlService {

    @Override
    public SqlParserResp explain(MetricReq metricReq, boolean isAgg, SemanticModel semanticModel) throws Exception {
        SqlParserResp sqlParserResp = new SqlParserResp();
        SemanticSchema semanticSchema = getSemanticSchema(semanticModel);
        AggPlanner aggBuilder = new AggPlanner(semanticSchema);
        aggBuilder.explain(metricReq, isAgg);
        sqlParserResp.setSql(aggBuilder.getSql());
        sqlParserResp.setSourceId(aggBuilder.getSourceId());
        return sqlParserResp;
    }

    private SemanticSchema getSemanticSchema(SemanticModel semanticModel) {
        SemanticSchema semanticSchema = SemanticSchema.newBuilder(semanticModel.getRootPath()).build();
        semanticSchema.setDatasource(semanticModel.getDatasourceMap());
        semanticSchema.setDimension(semanticModel.getDimensionMap());
        semanticSchema.setMetric(semanticModel.getMetrics());
        return semanticSchema;
    }
}
