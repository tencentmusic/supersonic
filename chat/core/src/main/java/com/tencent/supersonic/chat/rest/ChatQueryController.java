package com.tencent.supersonic.chat.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.SearchService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * query controller
 */
@RestController
@RequestMapping({"/api/chat/query", "/openapi/chat/query"})
public class ChatQueryController {

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    @Autowired
    private SearchService searchService;


    @PostMapping("search")
    public Object search(@RequestBody QueryReq queryCtx, HttpServletRequest request,
                         HttpServletResponse response) {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return searchService.search(queryCtx);
    }

    @PostMapping("parse")
    public Object parse(@RequestBody QueryReq queryCtx, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return queryService.performParsing(queryCtx);
    }

    @PostMapping("execute")
    public Object execute(@RequestBody ExecuteQueryReq queryReq,
                          HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        queryReq.setUser(UserHolder.findUser(request, response));
        return queryService.performExecution(queryReq);
    }

    @PostMapping("queryContext")
    public Object queryContext(@RequestBody QueryReq queryCtx, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return queryService.queryContext(queryCtx);
    }

    @PostMapping("queryData")
    public Object queryData(@RequestBody QueryDataReq queryData,
                            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        queryData.setUser(UserHolder.findUser(request, response));
        return queryService.executeDirectQuery(queryData, UserHolder.findUser(request, response));
    }

    @PostMapping("queryDimensionValue")
    public Object queryDimensionValue(@RequestBody @Valid DimensionValueReq dimensionValueReq,
                                      HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        return queryService.queryDimensionValue(dimensionValueReq, UserHolder.findUser(request, response));
    }

    @RequestMapping("/getEntityInfo")
    public Object getEntityInfo(Long queryId, Integer parseId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryService.getEntityInfo(queryId, parseId, user);
    }
}

