package com.tencent.supersonic.chat;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.util.DataUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class BaseTest extends BaseApplication {

    protected final int unit = 7;
    protected final String startDay = LocalDate.now().plusDays(-unit).toString();
    protected final String endDay = LocalDate.now().plusDays(-1).toString();
    protected final String period = "DAY";

    @Autowired
    protected ChatService chatService;
    @Autowired
    protected AgentService agentService;

    protected QueryResult submitMultiTurnChat(String queryText, Integer agentId, Integer chatId) throws Exception {
        ParseResp parseResp = submitParse(queryText, agentId, chatId);

        SemanticParseInfo semanticParseInfo = parseResp.getSelectedParses().get(0);
        ChatExecuteReq request = ChatExecuteReq.builder()
                .chatId(parseResp.getChatId())
                .queryText(parseResp.getQueryText())
                .user(DataUtils.getUser())
                .parseId(semanticParseInfo.getId())
                .queryId(parseResp.getQueryId())
                .saveAnswer(true)
                .build();
        QueryResult queryResult = chatService.performExecution(request);
        queryResult.setChatContext(semanticParseInfo);
        return queryResult;
    }

    protected QueryResult submitNewChat(String queryText, Integer agentId) throws Exception {
        ParseResp parseResp = submitParse(queryText, agentId);

        SemanticParseInfo parseInfo = parseResp.getSelectedParses().get(0);
        ChatExecuteReq request = ChatExecuteReq.builder()
                .chatId(parseResp.getChatId())
                .queryText(parseResp.getQueryText())
                .user(DataUtils.getUser())
                .parseId(parseInfo.getId())
                .agentId(agentId)
                .queryId(parseResp.getQueryId())
                .saveAnswer(false)
                .build();

        QueryResult result = chatService.performExecution(request);
        result.setChatContext(parseInfo);
        return result;
    }

    protected ParseResp submitParse(String queryText, Integer agentId, Integer chatId) {
        if (Objects.isNull(chatId)) {
            chatId = 10;
        }
        ChatParseReq chatParseReq = DataUtils.getChatParseReq(chatId, queryText);
        chatParseReq.setAgentId(agentId);
        return chatService.performParsing(chatParseReq);
    }

    protected ParseResp submitParse(String queryText, Integer agentId) {
        return submitParse(queryText, agentId, 10);
    }

    protected ParseResp submitParseWithAgent(String queryText, Integer agentId) {
        ChatParseReq chatParseReq = DataUtils.getChatParseReqWithAgent(10, queryText, agentId);
        return chatService.performParsing(chatParseReq);
    }

    protected void assertSchemaElements(Set<SchemaElement> expected, Set<SchemaElement> actual) {
        Set<String> expectedNames = expected.stream().map(s -> s.getName())
                .filter(s -> s != null).collect(Collectors.toSet());
        Set<String> actualNames = actual.stream().map(s -> s.getName())
                .filter(s -> s != null).collect(Collectors.toSet());

        assertEquals(expectedNames, actualNames);
    }

    protected void assertQueryResult(QueryResult expected, QueryResult actual) {
        SemanticParseInfo expectedParseInfo = expected.getChatContext();
        SemanticParseInfo actualParseInfo = actual.getChatContext();

        assertEquals(QueryState.SUCCESS, actual.getQueryState());
        assertEquals(expected.getQueryMode(), actual.getQueryMode());
        assertEquals(expectedParseInfo.getAggType(), actualParseInfo.getAggType());

        assertSchemaElements(expectedParseInfo.getMetrics(), actualParseInfo.getMetrics());
        assertSchemaElements(expectedParseInfo.getDimensions(), actualParseInfo.getDimensions());

        assertEquals(expectedParseInfo.getDimensionFilters(), actualParseInfo.getDimensionFilters());
        assertEquals(expectedParseInfo.getMetricFilters(), actualParseInfo.getMetricFilters());

        assertEquals(expectedParseInfo.getDateInfo(), actualParseInfo.getDateInfo());
    }

}
