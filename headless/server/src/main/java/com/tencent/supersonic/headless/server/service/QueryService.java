package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.request.ItemUseReq;
import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.request.QueryItemReq;
import com.tencent.supersonic.headless.api.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.ExplainResp;
import com.tencent.supersonic.headless.api.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.api.response.ItemUseResp;
import com.tencent.supersonic.headless.api.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.annotation.ApiHeaderCheck;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface QueryService {

    SemanticQueryResp queryBySql(QuerySqlReq querySqlCmd, User user) throws Exception;

    SemanticQueryResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception;

    SemanticQueryResp queryByStructWithAuth(QueryStructReq queryStructCmd, User user) throws Exception;

    SemanticQueryResp queryByMultiStruct(QueryMultiStructReq queryMultiStructCmd, User user) throws Exception;

    SemanticQueryResp queryDimValue(QueryDimValueReq queryDimValueReq, User user);

    SemanticQueryResp queryByQueryStatement(QueryStatement queryStatement);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend);

    <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception;

    QueryStatement explain(ParseSqlReq parseSqlReq) throws Exception;

    @ApiHeaderCheck
    ItemQueryResultResp queryMetricDataById(QueryItemReq queryApiReq,
            HttpServletRequest request) throws Exception;

}
