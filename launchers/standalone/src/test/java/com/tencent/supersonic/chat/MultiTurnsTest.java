package com.tencent.supersonic.chat;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.core.chat.query.rule.metric.MetricFilterQuery;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;

public class MultiTurnsTest extends BaseTest {

    @Test
    @Order(1)
    public void queryTest_01() throws Exception {
        QueryResult actualResult = submitMultiTurnChat("alice的访问次数",
                DataUtils.metricAgentId, DataUtils.MULTI_TURNS_CHAT_ID);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("人均访问次数"));
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问用户数"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(2)
    public void queryTest_02() throws Exception {
        QueryResult actualResult = submitMultiTurnChat("停留时长呢", DataUtils.metricAgentId,
                DataUtils.MULTI_TURNS_CHAT_ID);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @Order(3)
    public void queryTest_03() throws Exception {
        QueryResult actualResult = submitMultiTurnChat("lucy的如何", DataUtils.metricAgentId,
                DataUtils.MULTI_TURNS_CHAT_ID);

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "lucy", "用户", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.RECENT, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.METRIC);

        assertQueryResult(expectedResult, actualResult);
    }

}
