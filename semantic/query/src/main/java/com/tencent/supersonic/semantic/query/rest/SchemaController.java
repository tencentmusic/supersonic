package com.tencent.supersonic.semantic.query.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.core.request.DomainSchemaFilterReq;
import com.tencent.supersonic.semantic.api.core.request.PageDimensionReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.query.domain.SchemaService;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @PostMapping
    public List<DomainSchemaResp> fetchDomainSchema(@RequestBody DomainSchemaFilterReq filter,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.fetchDomainSchema(filter, user);
    }

    /**
     * get domain list
     *
     * @param
     */
    @GetMapping("/domain/list")
    public List<DomainResp> getDomainList(HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.getDomainListForAdmin(user);
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

}