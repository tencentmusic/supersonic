package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataReq;
import org.apache.calcite.sql.parser.SqlParseException;

/***
 * QueryService for query and search
 */
public interface QueryService {

    ParseResp performParsing(QueryReq queryReq);

    QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception;

    SemanticParseInfo queryContext(QueryReq queryReq);

    QueryResult executeDirectQuery(QueryDataReq queryData, User user) throws SqlParseException;

    EntityInfo getEntityInfo(Long queryId, Integer parseId, User user);

    Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception;
}

