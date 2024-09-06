package com.tencent.supersonic.headless.server.facade.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SqlEvaluation;
import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;

/** This interface adds natural language support to the semantic layer. */
public interface ChatLayerService {

    MapResp performMapping(QueryNLReq queryNLReq);

    ParseResp performParsing(QueryNLReq queryNLReq);

    MapInfoResp map(QueryMapReq queryMapReq);

    List<SearchResult> retrieve(QueryNLReq queryNLReq);

    void correct(QuerySqlReq querySqlReq, User user);

    SqlEvaluation validate(QuerySqlReq querySqlReq, User user);
}
