package com.tencent.supersonic.chat.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;
import com.tencent.supersonic.chat.service.RecommendService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * recommend controller
 */
@RestController
@RequestMapping("/api/chat/")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("recommend/{modelId}")
    public RecommendResp recommend(@PathVariable("modelId") Long modelId,
            @RequestParam(value = "limit", required = false) Long limit,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryReq queryCtx = new QueryReq();
        queryCtx.setUser(UserHolder.findUser(request, response));
        queryCtx.setModelId(modelId);
        return recommendService.recommend(queryCtx, limit);
    }

    @GetMapping("recommend/metric/{modelId}")
    public RecommendResp recommendMetricMode(@PathVariable("modelId") Long modelId,
            @RequestParam(value = "limit", required = false) Long limit,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryReq queryCtx = new QueryReq();
        queryCtx.setUser(UserHolder.findUser(request, response));
        queryCtx.setModelId(modelId);
        return recommendService.recommendMetricMode(queryCtx, limit);
    }

    @GetMapping("recommend/question")
    public List<RecommendQuestionResp> recommendQuestion(
            @RequestParam(value = "modelId", required = false) Long modelId,
            HttpServletRequest request,
            HttpServletResponse response) {
        return recommendService.recommendQuestion(modelId);
    }
}
