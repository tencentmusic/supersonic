package com.tencent.supersonic.semantic.query.domain.utils.calculate;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;

public interface CalculateConverter {

    boolean accept(QueryStructReq queryStructCmd);

    SqlParserResp getSqlParser(QueryStructReq queryStructCmd) throws Exception;

}
