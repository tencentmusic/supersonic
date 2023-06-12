package com.tencent.supersonic.chat.domain.service;

import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.pojo.search.SearchResult;
import java.util.List;

/**
 * search service
 */
public interface SearchService {

    List<SearchResult> search(QueryContextReq queryCtx);

}
