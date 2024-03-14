package com.tencent.supersonic.headless.server.processor;

import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.core.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RespBuildProcessor fill response object with parsing results.
 **/
@Slf4j
public class RespBuildProcessor implements ResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        parseResp.setChatId(queryContext.getChatId());
        parseResp.setQueryText(queryContext.getQueryText());
        List<SemanticQuery> candidateQueries = queryContext.getCandidateQueries();
        if (candidateQueries.size() > 0) {
            List<SemanticParseInfo> candidateParses = candidateQueries.stream()
                    .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
            parseResp.setSelectedParses(candidateParses);
            parseResp.setState(ParseResp.ParseState.COMPLETED);
        } else {
            parseResp.setState(ParseResp.ParseState.FAILED);
        }
    }

}
