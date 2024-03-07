package com.tencent.supersonic.headless.server.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaItemQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.DataSetFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.DataSetSchemaResp;
import com.tencent.supersonic.headless.server.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @PostMapping
    public List<DataSetSchemaResp> fetchDataSetSchema(@RequestBody DataSetFilterReq filter) {
        return schemaService.fetchDataSetSchema(filter);
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

    @PostMapping("/schemaItem/list")
    public List querySchemaItem(@RequestBody SchemaItemQueryReq schemaItemQueryReq) {
        return schemaService.querySchemaItem(schemaItemQueryReq);
    }

}
