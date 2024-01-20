package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.SemanticQuery;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * RespBuildProcessor fill response object with parsing results.
 **/
@Slf4j
public class RespBuildProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        parseResp.setChatId(queryContext.getChatId());
        parseResp.setQueryText(queryContext.getQueryText());
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        ChatService chatService = ContextUtils.getBean(ChatService.class);
        if (candidateQueries.size() > 0) {
            List<SemanticParseInfo> candidateParses = candidateQueries.stream()
                    .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
            parseResp.setSelectedParses(candidateParses);
            parseResp.setState(ParseResp.ParseState.COMPLETED);
        } else {
            parseResp.setState(ParseResp.ParseState.FAILED);
        }
        chatService.batchAddParse(chatContext, queryContext, parseResp);
    }

}
