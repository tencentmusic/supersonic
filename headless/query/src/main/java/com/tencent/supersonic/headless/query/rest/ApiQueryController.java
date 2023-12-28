package com.tencent.supersonic.headless.query.rest;

import com.tencent.supersonic.headless.api.query.request.QueryApiPreviewReq;
import com.tencent.supersonic.headless.api.query.request.QueryApiReq;
import com.tencent.supersonic.headless.api.query.response.ApiQueryResultResp;
import com.tencent.supersonic.headless.query.service.ApiQueryService;
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

    @PostMapping("/preview")
    public ApiQueryResultResp preview(@RequestBody QueryApiPreviewReq queryApiReq) throws Exception {
        return apiQueryService.preview(queryApiReq);
    }

    @PostMapping("/query")
    public ApiQueryResultResp query(@RequestBody QueryApiReq queryApiReq, HttpServletRequest request) throws Exception {
        return apiQueryService.query(queryApiReq, request);
    }

}
