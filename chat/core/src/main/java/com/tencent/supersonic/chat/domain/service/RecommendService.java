package com.tencent.supersonic.chat.domain.service;


import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.pojo.chat.RecommendResponse;

/***
 * Recommend Service
 */
public interface RecommendService {

    RecommendResponse recommend(QueryContextReq queryCtx);

}
