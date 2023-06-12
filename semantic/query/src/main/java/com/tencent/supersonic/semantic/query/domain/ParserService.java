package com.tencent.supersonic.semantic.query.domain;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;

public interface ParserService {


    SqlParserResp physicalSql(ParseSqlReq sqlCommend) throws Exception;

    SqlParserResp physicalSql(MetricReq metricCommand) throws Exception;
}
