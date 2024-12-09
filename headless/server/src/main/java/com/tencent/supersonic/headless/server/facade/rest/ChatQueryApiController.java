package com.tencent.supersonic.headless.server.facade.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class ChatQueryApiController {

    @Autowired
    private ChatLayerService chatLayerService;

    @Autowired
    private SemanticLayerService semanticLayerService;

    @PostMapping("/chat/search")
    public Object search(@RequestBody QueryNLReq queryNLReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        queryNLReq.setUser(UserHolder.findUser(request, response));
        return chatLayerService.retrieve(queryNLReq);
    }

    @PostMapping("/chat/map")
    public Object map(@RequestBody QueryNLReq queryNLReq, HttpServletRequest request,
            HttpServletResponse response) {
        queryNLReq.setUser(UserHolder.findUser(request, response));
        return chatLayerService.map(queryNLReq);
    }

    @PostMapping("/chat/parse")
    public Object parse(@RequestBody QueryNLReq queryNLReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        queryNLReq.setUser(UserHolder.findUser(request, response));
        return chatLayerService.parse(queryNLReq);
    }

    @PostMapping("/chat")
    public Object queryByNL(@RequestBody QueryNLReq queryNLReq, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        ParseResp parseResp = chatLayerService.parse(queryNLReq);
        if (parseResp.getState().equals(ParseResp.ParseState.COMPLETED)) {
            SemanticParseInfo parseInfo = parseResp.getSelectedParses().get(0);
            QuerySqlReq sqlReq = new QuerySqlReq();
            sqlReq.setSql(parseInfo.getSqlInfo().getCorrectedS2SQL());
            sqlReq.setSqlInfo(parseInfo.getSqlInfo());
            return semanticLayerService.queryByReq(sqlReq, user);
        }

        throw new RuntimeException(
                "Failed to parse natural language query: " + queryNLReq.getQueryText());
    }
}
