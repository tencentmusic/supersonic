package com.tencent.supersonic.chat.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.service.RecommendService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * recommend controller
 */
@RestController
@RequestMapping("/api/chat/")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("recommend/{domainId}")
    public Object recommend(@PathVariable("domainId") Integer domainId, HttpServletRequest request,
            HttpServletResponse response) {
        QueryContextReq queryCtx = new QueryContextReq();
        queryCtx.setUser(UserHolder.findUser(request, response));
        queryCtx.setDomainId(domainId);
        return recommendService.recommend(queryCtx);
    }
}
