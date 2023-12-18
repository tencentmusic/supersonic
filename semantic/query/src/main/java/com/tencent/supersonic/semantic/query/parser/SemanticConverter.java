package com.tencent.supersonic.semantic.query.parser;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;

public interface SemanticConverter {

    boolean accept(QueryStatement queryStatement);

    void converter(Catalog catalog, QueryStructReq queryStructCmd, ParseSqlReq sqlCommend, MetricReq metricCommand)
            throws Exception;

}
