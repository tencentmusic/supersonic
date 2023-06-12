package com.tencent.supersonic.semantic.query.domain.parser;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.SemanticModel;

public interface SemanticSqlService {

    SqlParserResp explain(MetricReq metricReq, boolean isAgg, SemanticModel semanticModel) throws Exception;
}
