package com.tencent.supersonic.chat.rest;


import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataRequest;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.SearchService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/chat/query")
public class ChatQueryController {

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    @Autowired
    private SearchService searchService;


    @PostMapping("search")
    public Object search(@RequestBody QueryRequest queryCtx, HttpServletRequest request,
                         HttpServletResponse response) {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return searchService.search(queryCtx);
    }

    @PostMapping("query")
    public Object query(@RequestBody QueryRequest queryCtx, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return queryService.executeQuery(queryCtx);
    }

    @PostMapping("queryContext")
    public Object queryContext(@RequestBody QueryRequest queryCtx, HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return queryService.queryContext(queryCtx);
    }

    @PostMapping("queryData")
    public Object queryData(@RequestBody QueryDataRequest queryData, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        return queryService.executeDirectQuery(queryData, UserHolder.findUser(request, response));
    }

}
