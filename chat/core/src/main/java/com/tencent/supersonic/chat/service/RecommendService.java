package com.tencent.supersonic.chat.service;


import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestion;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResponse;

import java.util.List;

/***
 * Recommend Service
 */
public interface RecommendService {

    RecommendResponse recommend(QueryRequest queryCtx, Long limit);

    RecommendResponse recommendMetricMode(QueryRequest queryCtx, Long limit);

    List<RecommendQuestion> recommendQuestion(Long domainId);
}
