package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;

public interface RetrieveService {

    MapInfoResp map(QueryMapReq queryMapReq);

    List<SearchResult> search(QueryReq queryCtx);

}