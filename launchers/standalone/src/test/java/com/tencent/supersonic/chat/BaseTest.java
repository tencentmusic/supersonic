package com.tencent.supersonic.chat;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.util.DataUtils;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.chat.server.service.ConfigService;
import com.tencent.supersonic.chat.server.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

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
    @Qualifier("chatQueryService")
    protected QueryService queryService;
    @Autowired
    protected ChatService chatService;
    @Autowired
    protected ConfigService configService;
    @MockBean
    protected AgentService agentService;

    protected QueryResult submitMultiTurnChat(String queryText, Integer agentId, Integer chatId) throws Exception {
        ParseResp parseResp = submitParse(queryText, agentId, chatId);

        SemanticParseInfo semanticParseInfo = parseResp.getSelectedParses().get(0);
        ExecuteQueryReq request = ExecuteQueryReq.builder()
                .agentId(agentId)
                .queryId(parseResp.getQueryId())
                .parseId(semanticParseInfo.getId())
                .chatId(parseResp.getChatId())
                .queryText(parseResp.getQueryText())
                .user(DataUtils.getUser())
                .parseInfo(semanticParseInfo)
                .saveAnswer(true)
                .build();
        QueryResult queryResult = queryService.performExecution(request);
        queryResult.setChatContext(semanticParseInfo);
        return queryResult;
    }

    protected QueryResult submitNewChat(String queryText, Integer agentId) throws Exception {
        ParseResp parseResp = submitParse(queryText, agentId);

        SemanticParseInfo parseInfo = parseResp.getSelectedParses().get(0);
        ExecuteQueryReq request = ExecuteQueryReq.builder()
                .agentId(agentId)
                .queryId(parseResp.getQueryId())
                .parseId(parseInfo.getId())
                .chatId(parseResp.getChatId())
                .queryText(parseResp.getQueryText())
                .user(DataUtils.getUser())
                .parseInfo(parseInfo)
                .saveAnswer(true)
                .build();

        QueryResult result = queryService.performExecution(request);

        ChatContext chatContext = chatService.getOrCreateContext(parseResp.getChatId());
        chatContext.setParseInfo(new SemanticParseInfo());
        chatService.updateContext(chatContext);
        result.setChatContext(parseInfo);
        return result;
    }

    protected ParseResp submitParse(String queryText, Integer agentId, Integer chatId) {
        if (Objects.isNull(chatId)) {
            chatId = 10;
        }
        QueryReq queryContextReq = DataUtils.getQueryContextReq(chatId, queryText);
        queryContextReq.setAgentId(agentId);
        return queryService.performParsing(queryContextReq);
    }

    protected ParseResp submitParse(String queryText, Integer agentId) {
        return submitParse(queryText, agentId, 10);
    }

    protected ParseResp submitParseWithAgent(String queryText, Integer agentId) {
        QueryReq queryContextReq = DataUtils.getQueryReqWithAgent(10, queryText, agentId);
        return queryService.performParsing(queryContextReq);
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
