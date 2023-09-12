package com.tencent.supersonic.chat.service;


import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;

import java.util.List;

/***
 * Recommend Service
 */
public interface RecommendService {

    RecommendResp recommend(QueryReq queryCtx, Long limit);

    RecommendResp recommendMetricMode(QueryReq queryCtx, Long limit);

    List<RecommendQuestionResp> recommendQuestion(Long modelId);
}
