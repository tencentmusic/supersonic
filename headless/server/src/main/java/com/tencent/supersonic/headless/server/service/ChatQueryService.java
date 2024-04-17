package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;

/***
 * QueryService for query and search
 */
public interface ChatQueryService {

    MapResp performMapping(QueryReq queryReq);

    ParseResp performParsing(QueryReq queryReq);

    QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception;

    SemanticParseInfo queryContext(Integer chatId);

    QueryResult executeDirectQuery(QueryDataReq queryData, User user) throws Exception;

    EntityInfo getEntityInfo(SemanticParseInfo parseInfo, User user);

    Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception;

    void correct(QuerySqlReq querySqlReq, User user);
}

