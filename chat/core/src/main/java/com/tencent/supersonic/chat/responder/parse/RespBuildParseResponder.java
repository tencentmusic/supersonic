package com.tencent.supersonic.chat.responder.parse;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RespBuildParseResponder implements ParseResponder {

    @Override
    public void fillResponse(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        QueryReq queryReq = queryContext.getRequest();
        parseResp.setChatId(queryReq.getChatId());
        parseResp.setQueryText(queryReq.getQueryText());
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        if (candidateQueries.size() > 0) {
            List<SemanticParseInfo> candidateParses = candidateQueries.stream()
                    .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
            parseResp.setCandidateParses(candidateParses);
            parseResp.setState(ParseResp.ParseState.COMPLETED);
            parseResp.setCandidateParses(candidateParses);
            ChatService chatService = ContextUtils.getBean(ChatService.class);
            chatService.batchAddParse(chatContext, queryReq, parseResp);
        } else {
            parseResp.setState(ParseResp.ParseState.FAILED);
        }
    }

}
