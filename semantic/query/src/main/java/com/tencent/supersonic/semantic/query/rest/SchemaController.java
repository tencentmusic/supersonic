package com.tencent.supersonic.semantic.query.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationSourceReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationSourceResp;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DomainResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.query.service.MaterializationService;
import com.tencent.supersonic.semantic.query.service.SchemaService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/semantic/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;
    @Autowired
    private MaterializationService materializationService;

    @PostMapping
    public List<ModelSchemaResp> fetchModelSchema(@RequestBody ModelSchemaFilterReq filter,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.fetchModelSchema(filter, user);
    }

    @GetMapping("/domain/list")
    public List<DomainResp> getDomainList(HttpServletRequest request,
                                         HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.getDomainList(user);
    }

    @GetMapping("/model/list")
    public List<ModelResp> getModelList(@RequestParam("domainId") Long domainId,
                                        @RequestParam("authType") String authType,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.getModelList(user, AuthType.valueOf(authType), domainId);
    }

    @PostMapping("/dimension/page")
    public PageInfo<DimensionResp> queryDimension(@RequestBody PageDimensionReq pageDimensionCmd,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.queryDimension(pageDimensionCmd, user);
    }

    @PostMapping("/metric/page")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetricCmd,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.queryMetric(pageMetricCmd, user);
    }

    /**
     * task api
     */

    @PostMapping("/materialization/source")
    MaterializationSourceResp queryDataSource(@RequestBody MaterializationSourceReq materializationSourceReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return materializationService.getMaterializationDataSource(materializationSourceReq, user);
    }

}
