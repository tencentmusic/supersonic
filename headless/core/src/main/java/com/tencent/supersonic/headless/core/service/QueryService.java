package com.tencent.supersonic.headless.core.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.common.core.request.QueryItemReq;
import com.tencent.supersonic.headless.common.core.response.ItemQueryResultResp;
import com.tencent.supersonic.headless.common.server.response.ExplainResp;
import com.tencent.supersonic.headless.common.server.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.core.request.ExplainSqlReq;
import com.tencent.supersonic.headless.common.core.request.ItemUseReq;
import com.tencent.supersonic.headless.common.core.request.MetricReq;
import com.tencent.supersonic.headless.common.core.request.QueryDimValueReq;
import com.tencent.supersonic.headless.common.core.request.QueryS2SQLReq;
import com.tencent.supersonic.headless.common.core.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.common.core.request.QueryStructReq;
import com.tencent.supersonic.headless.common.core.response.ItemUseResp;
import com.tencent.supersonic.headless.core.annotation.ApiHeaderCheck;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface QueryService {

    Object queryBySql(QueryS2SQLReq querySqlCmd, User user) throws Exception;

    QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception;

    QueryResultWithSchemaResp queryByStructWithAuth(QueryStructReq queryStructCmd, User user)
            throws Exception;

    QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructCmd, User user) throws Exception;

    QueryResultWithSchemaResp queryDimValue(QueryDimValueReq queryDimValueReq, User user);

    Object queryByQueryStatement(QueryStatement queryStatement);

    List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend);

    <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception;

    @ApiHeaderCheck
    ItemQueryResultResp metricDataQueryById(QueryItemReq queryApiReq,
                                            HttpServletRequest request) throws Exception;

    QueryStatement parseMetricReq(MetricReq metricReq) throws Exception;

}
