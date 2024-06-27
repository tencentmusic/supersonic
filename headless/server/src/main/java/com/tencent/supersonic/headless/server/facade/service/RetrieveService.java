package com.tencent.supersonic.headless.server.facade.service;

import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;

public interface RetrieveService {

    List<SearchResult> retrieve(QueryReq queryCtx);

}