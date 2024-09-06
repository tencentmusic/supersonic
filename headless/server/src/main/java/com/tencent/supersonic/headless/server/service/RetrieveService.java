package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;

public interface RetrieveService {

    List<SearchResult> retrieve(QueryNLReq queryNLReq);
}
