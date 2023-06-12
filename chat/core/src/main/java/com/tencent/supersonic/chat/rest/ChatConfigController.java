package com.tencent.supersonic.chat.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigBase;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigEditReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.service.ConfigService;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/chat/conf")
public class ChatConfigController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private DefaultSemanticInternalUtils defaultSemanticUtils;


    @PostMapping
    public Long addChatConfig(@RequestBody ChatConfigBase extendBaseCmd,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.addConfig(extendBaseCmd, user);
    }

    @PutMapping
    public Long editDomainExtend(@RequestBody ChatConfigEditReq extendEditCmd,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.editConfig(extendEditCmd, user);
    }


    @PostMapping("/search")
    public List<ChatConfigInfo> search(@RequestBody ChatConfigFilter filter,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.search(filter, user);
    }


    @GetMapping("/richDesc/{domainId}")
    public ChatConfigRichInfo getDomainExtendRichInfo(@PathVariable("domainId") Long domainId) {
        return configService.getConfigRichInfo(domainId);
    }


    /**
     * get domain list
     *
     * @param
     */
    @GetMapping("/domainList")
    public List<DomainResp> getDomainList(HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return defaultSemanticUtils.getDomainListForUser(user);
    }

    @PostMapping("/dimension/page")
    public PageInfo<DimensionResp> queryDimension(@RequestBody PageDimensionReq pageDimensionCmd,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return defaultSemanticUtils.queryDimensionPage(pageDimensionCmd, user);
    }

    @PostMapping("/metric/page")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetrricCmd,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return defaultSemanticUtils.queryMetricPage(pageMetrricCmd, user);
    }


}
