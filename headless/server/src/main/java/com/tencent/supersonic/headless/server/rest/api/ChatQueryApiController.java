package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class ChatQueryApiController {

    @Autowired
    private ChatQueryService chatQueryService;
    @Autowired
    private SearchService searchService;

    @PostMapping("/chat/search")
    public Object search(@RequestBody QueryReq queryReq,
                        HttpServletRequest request,
                        HttpServletResponse response) throws Exception {
        queryReq.setUser(UserHolder.findUser(request, response));
        return searchService.search(queryReq);
    }

    @PostMapping("/chat/map")
    public MapResp map(@RequestBody QueryReq queryReq,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        queryReq.setUser(UserHolder.findUser(request, response));
        return chatQueryService.performMapping(queryReq);
    }

    @PostMapping("/chat/parse")
    public Object parse(@RequestBody QueryReq queryReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        queryReq.setUser(UserHolder.findUser(request, response));
        return chatQueryService.performParsing(queryReq);
    }

    @PostMapping("/chat/execute")
    public Object execute(@RequestBody ExecuteQueryReq executeQueryReq,
                             HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
        executeQueryReq.setUser(UserHolder.findUser(request, response));
        return chatQueryService.performExecution(executeQueryReq);
    }

}
