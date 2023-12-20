package com.tencent.supersonic.headless.query.parser;

import com.tencent.supersonic.headless.api.query.request.MetricReq;
import com.tencent.supersonic.headless.api.query.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.query.request.QueryStructReq;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;

public interface HeadlessConverter {

    boolean accept(QueryStatement queryStatement);

    void converter(Catalog catalog, QueryStructReq queryStructCmd, ParseSqlReq sqlCommend, MetricReq metricCommand)
            throws Exception;

}
