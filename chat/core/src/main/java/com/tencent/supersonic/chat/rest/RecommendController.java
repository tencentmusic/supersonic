package com.tencent.supersonic.chat.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.RecommendQuestionResp;
import com.tencent.supersonic.chat.api.pojo.response.RecommendResp;
import com.tencent.supersonic.chat.service.RecommendService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * recommend controller
 */
@RestController
@RequestMapping("/api/chat/")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("recommend/{domainId}")
    public RecommendResp recommend(@PathVariable("domainId") Long domainId,
                                   @RequestParam(value = "limit", required = false) Long limit,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        QueryReq queryCtx = new QueryReq();
        queryCtx.setUser(UserHolder.findUser(request, response));
        queryCtx.setDomainId(domainId);
        return recommendService.recommend(queryCtx, limit);
    }

    @GetMapping("recommend/metric/{domainId}")
    public RecommendResp recommendMetricMode(@PathVariable("domainId") Long domainId,
                                             @RequestParam(value = "limit", required = false) Long limit,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        QueryReq queryCtx = new QueryReq();
        queryCtx.setUser(UserHolder.findUser(request, response));
        queryCtx.setDomainId(domainId);
        return recommendService.recommendMetricMode(queryCtx, limit);
    }

    @GetMapping("recommend/question")
    public List<RecommendQuestionResp> recommendQuestion(@RequestParam(value = "domainId", required = false) Long domainId,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        return recommendService.recommendQuestion(domainId);
    }
}
