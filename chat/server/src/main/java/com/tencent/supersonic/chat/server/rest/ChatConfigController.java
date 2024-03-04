package com.tencent.supersonic.chat.server.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.core.query.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.server.service.ConfigService;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ItemResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


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

    @GetMapping("/domainList")
    public List<DomainResp> getDomainList(HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getDomainList(user);
    }

    //Compatible with front-end
    @GetMapping("/dataSetList")
    public List<DataSetResp> getDataSetList() {
        return semanticInterpreter.getDataSetList(null);
    }

    @GetMapping("/dataSetList/{domainId}")
    public List<DataSetResp> getDataSetList(@PathVariable("domainId") Long domainId) {
        return semanticInterpreter.getDataSetList(domainId);
    }

    @PostMapping("/dimension/page")
    public PageInfo<DimensionResp> getDimension(@RequestBody PageDimensionReq pageDimensionReq) {
        return semanticInterpreter.getDimensionPage(pageDimensionReq);
    }

    @PostMapping("/metric/page")
    public PageInfo<MetricResp> getMetric(@RequestBody PageMetricReq pageMetricReq,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return semanticInterpreter.getMetricPage(pageMetricReq, user);
    }

    @GetMapping("/getDomainDataSetTree")
    public List<ItemResp> getDomainDataSetTree() {
        return semanticInterpreter.getDomainDataSetTree();
    }

}
