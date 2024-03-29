package com.tencent.supersonic.chat.server.rest;


import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * query controller
 */
@RestController
@RequestMapping({"/api/chat/query", "/openapi/chat/query"})
public class ChatQueryController {

    @Autowired
    private ChatService chatService;

    @PostMapping("search")
    public Object search(@RequestBody ChatParseReq chatParseReq, HttpServletRequest request,
                         HttpServletResponse response) {
        chatParseReq.setUser(UserHolder.findUser(request, response));
        return chatService.search(chatParseReq);
    }

    @PostMapping("map")
    public Object map(@RequestBody ChatParseReq chatParseReq,
                        HttpServletRequest request, HttpServletResponse response) throws Exception {
        chatParseReq.setUser(UserHolder.findUser(request, response));
        return chatService.performMapping(chatParseReq);
    }

    @PostMapping("parse")
    public Object parse(@RequestBody ChatParseReq chatParseReq,
                        HttpServletRequest request, HttpServletResponse response) throws Exception {
        chatParseReq.setUser(UserHolder.findUser(request, response));
        return chatService.performParsing(chatParseReq);
    }

    @PostMapping("execute")
    public Object execute(@RequestBody ChatExecuteReq chatExecuteReq,
                          HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        chatExecuteReq.setUser(UserHolder.findUser(request, response));
        return chatService.performExecution(chatExecuteReq);
    }

    @PostMapping("queryContext")
    public Object queryContext(@RequestBody QueryReq queryCtx,
                               HttpServletRequest request, HttpServletResponse response) {
        queryCtx.setUser(UserHolder.findUser(request, response));
        return chatService.queryContext(queryCtx.getChatId());
    }

    @PostMapping("queryData")
    public Object queryData(@RequestBody ChatQueryDataReq chatQueryDataReq,
                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        chatQueryDataReq.setUser(UserHolder.findUser(request, response));
        return chatService.queryData(chatQueryDataReq, UserHolder.findUser(request, response));
    }

    @PostMapping("queryDimensionValue")
    public Object queryDimensionValue(@RequestBody @Valid DimensionValueReq dimensionValueReq,
                                      HttpServletRequest request, HttpServletResponse response) throws Exception {
        return chatService.queryDimensionValue(dimensionValueReq, UserHolder.findUser(request, response));
    }

}

