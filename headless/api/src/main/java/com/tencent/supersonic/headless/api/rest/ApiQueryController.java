package com.tencent.supersonic.headless.api.rest;

import com.tencent.supersonic.headless.api.service.ApiQueryService;
import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.common.query.request.MetaQueryApiReq;
import com.tencent.supersonic.headless.common.query.request.QueryApiReq;
import com.tencent.supersonic.headless.common.query.request.QueryStructReq;
import com.tencent.supersonic.headless.common.query.response.ApiQueryResultResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/semantic/apiQuery")
@Slf4j
public class ApiQueryController {

    @Autowired
    private ApiQueryService apiQueryService;

    @PostMapping("/metricDataQueryById")
    public ApiQueryResultResp metricDataQueryById(@RequestBody QueryApiReq queryApiReq,
                                            HttpServletRequest request) throws Exception {
        return apiQueryService.metricDataQueryById(queryApiReq, request);
    }

    @PostMapping("/metaQuery")
    public Object metaQuery(@RequestBody MetaQueryApiReq metaQueryApiReq, HttpServletRequest request) {
        return apiQueryService.metaQuery(metaQueryApiReq, request);
    }

    @PostMapping("/dataQueryByStruct")
    public QueryResultWithSchemaResp dataQueryByStruct(QueryStructReq queryStructReq,
                                                       HttpServletRequest request) throws Exception {
        return apiQueryService.dataQueryByStruct(queryStructReq, request);
    }

}
