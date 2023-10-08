package com.tencent.supersonic.chat.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.chat.service.ConfigService;

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
@RequestMapping({"/api/chat/conf", "/openapi/chat/conf"})
public class ChatConfigController {

    @Autowired
    private ConfigService configService;


    private SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();


    @PostMapping
    public Long addChatConfig(@RequestBody ChatConfigBaseReq extendBaseCmd,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return configService.addConfig(extendBaseCmd, user);
    }

    @PutMapping
    public Long editModelExtend(@RequestBody ChatConfigEditReqReq extendEditCmd,
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


    @GetMapping("/richDesc/{modelId}")
    public ChatConfigRichResp getModelExtendRichInfo(@PathVariable("modelId") Long modelId) {
        return configService.getConfigRichInfo(modelId);
    }

    @GetMapping("/richDesc/all")
    public List<ChatConfigRichResp> getAllChatRichConfig() {
        return configService.getAllChatRichConfig();
    }

    @GetMapping("/modelList/{domainId}")
    public List<ModelResp> getModelList(@PathVariable("domainId") Long domainId,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getModelList(AuthType.ADMIN, domainId, user);
    }

    @GetMapping("/modelList")
    public List<ModelResp> getModelList(HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getModelList(AuthType.ADMIN, null, user);
    }

    @GetMapping("/domainList")
    public List<DomainResp> getDomainList(HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getDomainList(user);
    }

    @GetMapping("/modelList/view")
    public List<ModelResp> getModelListVisible(HttpServletRequest request,
                                               HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getModelList(AuthType.VISIBLE, null, user);
    }

    @PostMapping("/dimension/page")
    public PageInfo<DimensionResp> getDimension(@RequestBody PageDimensionReq pageDimensionReq,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        return semanticInterpreter.getDimensionPage(pageDimensionReq);
    }

    @PostMapping("/metric/page")
    public PageInfo<MetricResp> getMetric(@RequestBody PageMetricReq pageMetricReq,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getMetricPage(pageMetricReq, user);
    }


}
