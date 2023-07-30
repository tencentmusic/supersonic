package com.tencent.supersonic.integration;

import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.util.DataUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
public class BaseQueryTest {

    protected final int unit = 7;
    protected final String startDay = LocalDate.now().plusDays(-unit).toString();
    protected final String endDay = LocalDate.now().plusDays(-1).toString();

    @Autowired
    @Qualifier("chatQueryService")
    protected QueryService queryService;
    @Autowired
    protected ChatService chatService;
    @Autowired
    protected ConfigService configService;

    protected Integer getNewChat(String chatName) {
        chatService.addChat(User.getFakeUser(), chatName);
        Optional<Long> chatId = chatService.getAll(User.getFakeUser().getName()).stream().map(c -> c.getChatId()).sorted(Comparator.reverseOrder()).findFirst();
        if (chatId.isPresent()) {
            return chatId.get().intValue();
        }
        return 1;
    }

    protected QueryResult submitMultiTurnChat(String queryText) throws Exception {
        QueryRequest queryContextReq = DataUtils.getQueryContextReq(20, queryText);
        return queryService.executeQuery(queryContextReq);
    }

    protected QueryResult submitNewChat(String queryText) throws Exception {
        chatService.addChat(User.getFakeUser(), RandomStringUtils.random(5));

        ChatContext chatContext = chatService.getOrCreateContext(10);
        chatContext.setParseInfo(new SemanticParseInfo());
        chatService.updateContext(chatContext);

        QueryRequest queryContextReq = DataUtils.getQueryContextReq(10, queryText);
        return queryService.executeQuery(queryContextReq);
    }

    protected void assertSchemaElements(Set<SchemaElement> expected, Set<SchemaElement> actual) {
        Set<String> expectedNames = expected.stream().map(s -> s.getName())
                .filter(s -> s != null).collect(Collectors.toSet());
        Set<String> actualNames = actual.stream().map(s -> s.getName())
                .filter(s -> s != null).collect(Collectors.toSet());

        assertEquals(expectedNames, actualNames);
    }

    protected void assertDateConf(DateConf expected, DateConf actual) {
        Boolean timeFilterExist = expected.getStartDate().equals(actual.getStartDate())
                && expected.getEndDate().equals(actual.getEndDate())
                && expected.getDateMode().equals(actual.getDateMode())
                || expected.getUnit().equals(actual.getUnit()) &&
                expected.getDateMode().equals(actual.getDateMode()) &&
                expected.getPeriod().equals(actual.getPeriod());

        assertTrue(timeFilterExist);
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

        assertDateConf(expectedParseInfo.getDateInfo(), actualParseInfo.getDateInfo());
    }

}
