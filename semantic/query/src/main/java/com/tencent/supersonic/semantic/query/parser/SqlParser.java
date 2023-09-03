package com.tencent.supersonic.semantic.query.parser;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface SqlParser {
    QueryStatement explain(MetricReq metricReq, boolean isAgg, Catalog catalog) throws Exception;
}
