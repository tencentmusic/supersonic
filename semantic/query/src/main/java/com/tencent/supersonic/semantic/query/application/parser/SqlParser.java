package com.tencent.supersonic.semantic.query.application.parser;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.core.domain.Catalog;
import com.tencent.supersonic.semantic.query.domain.pojo.QueryStatement;

public interface SqlParser {
    QueryStatement explain(MetricReq metricReq, boolean isAgg, Catalog catalog) throws Exception;
}
