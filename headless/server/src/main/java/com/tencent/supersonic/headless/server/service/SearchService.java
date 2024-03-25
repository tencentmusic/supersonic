package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;

import java.util.List;


/**
 * search service
 */
public interface SearchService {

    List<SearchResult> search(QueryReq queryCtx);

}
