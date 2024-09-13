package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.request.RecommendReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;

import java.util.List;

/** * Recommend Service */
public interface RecommendService {

    RecommendResp recommend(RecommendReq recommendReq, Long limit);

    RecommendResp recommendMetricMode(RecommendReq recommendReq, Long limit);

    List<RecommendQuestionResp> recommendQuestion(Long modelId);
}
