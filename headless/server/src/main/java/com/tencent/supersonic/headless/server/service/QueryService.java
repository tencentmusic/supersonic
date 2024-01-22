package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDimValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryItemReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.annotation.ApiHeaderCheck;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public interface QueryService {

    SemanticQueryResp queryBySql(QuerySqlReq querySqlCmd, User user) throws Exception;

    SemanticQueryResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception;

    SemanticQueryResp queryBySemanticQuery(SemanticQueryReq semanticQueryReq, User user) throws Exception;

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
