package com.tencent.supersonic.headless.api.service;

import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.query.request.MetaQueryApiReq;
import com.tencent.supersonic.headless.common.query.request.QueryApiReq;
import com.tencent.supersonic.headless.common.query.request.QueryStructReq;
import com.tencent.supersonic.headless.common.query.response.ApiQueryResultResp;
import com.tencent.supersonic.headless.api.annotation.ApiHeaderCheck;

import javax.servlet.http.HttpServletRequest;
/**
 * Api service for other apps to query meta info and data
 */
public interface ApiQueryService {

    /**
     * Query the metric data based on the metric id.
     * The data will be drilled down based on the information configured when applying for the APP.
     * @param queryApiReq
     * @param request
     * @return
     * @throws Exception
     */
    @ApiHeaderCheck
    ApiQueryResultResp metricDataQueryById(QueryApiReq queryApiReq, HttpServletRequest request) throws Exception;

    /**
     * Query data based on structure
     * @param queryStructReq
     * @param request
     * @return
     * @throws Exception
     */
    @ApiHeaderCheck
    QueryResultWithSchemaResp dataQueryByStruct(QueryStructReq queryStructReq,
                                                HttpServletRequest request) throws Exception;

    /**
     * Query the meta information of the metric, dimension and tag
     * @param metaQueryApiReq
     * @param request
     * @return
     */
    @ApiHeaderCheck
    Object metaQuery(MetaQueryApiReq metaQueryApiReq, HttpServletRequest request);

}
