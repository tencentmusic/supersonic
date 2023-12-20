package com.tencent.supersonic.headless.query.service;

import com.tencent.supersonic.headless.api.query.request.QueryApiPreviewReq;
import com.tencent.supersonic.headless.api.query.request.QueryApiReq;
import com.tencent.supersonic.headless.api.query.response.ApiQueryResultResp;
import com.tencent.supersonic.headless.query.annotation.ApiDataPermission;

import javax.servlet.http.HttpServletRequest;

public interface ApiQueryService {

    ApiQueryResultResp preview(QueryApiPreviewReq queryApiReq) throws Exception;

    @ApiDataPermission
    ApiQueryResultResp query(QueryApiReq queryApiReq, HttpServletRequest request) throws Exception;
}
