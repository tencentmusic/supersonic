package com.tencent.supersonic.chat.rest;

import com.tencent.supersonic.chat.api.pojo.request.RecommendReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;
import com.tencent.supersonic.chat.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

/**
 * recommend controller
 */
@RestController
@RequestMapping({"/api/chat/", "/openapi/chat/"})
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("recommend/{modelId}")
    public RecommendResp recommend(@PathVariable("modelId") Long modelId,
                                   @RequestParam(value = "limit", required = false) Long limit) {
        RecommendReq recommendReq = new RecommendReq();
        recommendReq.setModelId(modelId);
        return recommendService.recommend(recommendReq, limit);
    }

    @GetMapping("recommend/metric/{modelId}")
    public RecommendResp recommendMetricMode(@PathVariable("modelId") Long modelId,
                                             @RequestParam(value = "metricId", required = false) Long metricId,
                                             @RequestParam(value = "limit", required = false) Long limit) {
        RecommendReq recommendReq = new RecommendReq();
        recommendReq.setModelId(modelId);
        recommendReq.setMetricId(metricId);
        return recommendService.recommendMetricMode(recommendReq, limit);
    }

    @GetMapping("recommend/question")
    public List<RecommendQuestionResp> recommendQuestion(
            @RequestParam(value = "modelId", required = false) Long modelId) {
        return recommendService.recommendQuestion(modelId);
    }
}
