package com.tencent.supersonic.semantic.query.parser;

import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface SqlParser {
    QueryStatement explain(MetricReq metricReq, AggOption aggOption, Catalog catalog) throws Exception;
}
