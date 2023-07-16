package com.tencent.supersonic.chat.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigBaseReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigFilter;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichResp;
import com.tencent.supersonic.chat.domain.service.ConfigService;

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


    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();


    @PostMapping
    public Long addChatConfig(@RequestBody ChatConfigBaseReq extendBaseCmd,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.addConfig(extendBaseCmd, user);
    }

    @PutMapping
    public Long editDomainExtend(@RequestBody ChatConfigEditReqReq extendEditCmd,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.editConfig(extendEditCmd, user);
    }


    @PostMapping("/search")
    public List<ChatConfigResp> search(@RequestBody ChatConfigFilter filter,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.search(filter, user);
    }


    @GetMapping("/richDesc/{domainId}")
    public ChatConfigRichResp getDomainExtendRichInfo(@PathVariable("domainId") Long domainId) {
        return configService.getConfigRichInfo(domainId);
    }

    @GetMapping("/richDesc/all")
    public List<ChatConfigRichResp> getAllChatRichConfig() {
        return configService.getAllChatRichConfig();
    }


    /**
     * get domain list
     *
     * @param
     */
    @GetMapping("/domainList")
    public List<DomainResp> getDomainList() {

        return semanticLayer.getDomainListForAdmin();
        //return defaultSemanticUtils.getDomainListForAdmin();
    }

    @GetMapping("/domainList/view")
    public List<DomainResp> getDomainListForViewer() {
        return semanticLayer.getDomainListForViewer();
        //return defaultSemanticUtils.getDomainListForViewer();
    }

    @PostMapping("/dimension/page")
    public PageInfo<DimensionResp> queryDimension(@RequestBody PageDimensionReq pageDimensionCmd,
            HttpServletRequest request,
            HttpServletResponse response) {
        return semanticLayer.queryDimensionPage(pageDimensionCmd);
    }

    @PostMapping("/metric/page")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetrricCmd,
            HttpServletRequest request,
            HttpServletResponse response) {
        return semanticLayer.queryMetricPage(pageMetrricCmd);
    }


}
