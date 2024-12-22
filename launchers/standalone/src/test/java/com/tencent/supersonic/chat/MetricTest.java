package com.tencent.supersonic.chat;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.demo.S2VisitsDemo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricFilterQuery;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricGroupByQuery;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricModelQuery;
import com.tencent.supersonic.headless.chat.query.rule.metric.MetricTopNQuery;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.MAX;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.SUM;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetricTest extends BaseTest {

    @BeforeEach
    public void init() {
        agent = getAgentByName(S2VisitsDemo.AGENT_NAME);
        schema = schemaService.getSemanticSchema(agent.getDataSetIds());
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testMetricModel() throws Exception {
        QueryResult actualResult = submitNewChat("超音数访问次数", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricModelQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(DateConf.DateMode.BETWEEN, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 1;
        assert actualResult.getQuerySql().contains("s2_pv_uv_statis");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testDerivedMetricModel() throws Exception {
        QueryResult actualResult = submitNewChat("超音数 人均访问次数", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricModelQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("人均访问次数"));

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(DateConf.DateMode.BETWEEN, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 1;
        assert actualResult.getQuerySql().contains("s2_pv_uv_statis");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testMetricFilter() throws Exception {
        QueryResult actualResult = submitNewChat("alice的访问次数", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        SchemaElement userElement = getSchemaElementByName(schema.getDimensions(), "用户名");
        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户名", userElement.getId()));

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(DateConf.DateMode.BETWEEN, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 1;
        assert actualResult.getQuerySql().contains("s2_pv_uv_statis");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testMetricGroupBy() throws Exception {
        QueryResult actualResult = submitNewChat("近7天超音数各用户的访问次数", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("用户名"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, 7,
                DatePeriodEnum.DAY, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 6;
        assert actualResult.getQuerySql().contains("s2_pv_uv_statis");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testMetricGroupByAndJoin() throws Exception {
        QueryResult actualResult = submitNewChat("近7天超音数各部门的访问次数", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, 7,
                DatePeriodEnum.DAY, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 4;
        assert actualResult.getQuerySql().contains("s2_pv_uv_statis");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testMetricGroupByAndMultiJoin() throws Exception {
        QueryResult actualResult = submitNewChat("近7天超音数各部门的访问次数和停留时长", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("停留时长"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN, 7,
                DatePeriodEnum.DAY, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 4;
        assert actualResult.getQuerySql().contains("s2_pv_uv_statis");
        assert actualResult.getQuerySql().contains("s2_user_department");
        assert actualResult.getQuerySql().contains("s2_stay_time_statis");
    }

    @Test
    public void testMetricFilterCompare() throws Exception {
        QueryResult actualResult = submitNewChat("对比alice和lucy的访问次数", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("用户名"));
        List<String> list = new ArrayList<>();
        list.add("alice");
        list.add("lucy");

        SchemaElement userElement = getSchemaElementByName(schema.getDimensions(), "用户名");
        QueryFilter dimensionFilter = DataUtils.getFilter("user_name", FilterOperatorEnum.IN, list,
                "用户名", userElement.getId());
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(DateConf.DateMode.BETWEEN, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 2;
    }

    @Test
    @Order(3)
    public void testMetricTopN() throws Exception {
        QueryResult actualResult = submitNewChat("近3天访问次数最多的用户", agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricTopNQuery.QUERY_MODE);
        expectedParseInfo.setAggType(MAX);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("用户名"));

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(3, DateConf.DateMode.BETWEEN, DatePeriodEnum.DAY));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void testMetricGroupBySum() throws Exception {
        QueryResult actualResult = submitNewChat("近7天超音数各部门的访问次数总和", agent.getId());
        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(SUM);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(DateConf.DateMode.BETWEEN, unit, period, startDay, endDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
        assert actualResult.getQueryResults().size() == 4;
    }

    @Test
    public void testMetricFilterTime() throws Exception {
        DateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        DateFormat textFormat = new SimpleDateFormat("yyyy年mm月dd日");
        String dateStr = textFormat.format(format.parse(startDay));

        QueryResult actualResult =
                submitNewChat(String.format("alice在%s的访问次数", dateStr), agent.getId());

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        SchemaElement userElement = getSchemaElementByName(schema.getDimensions(), "用户名");
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户名", userElement.getId()));

        expectedParseInfo.setDateInfo(
                DataUtils.getDateConf(DateConf.DateMode.BETWEEN, 1, period, startDay, startDay));
        expectedParseInfo.setQueryType(QueryType.AGGREGATE);

        assertQueryResult(expectedResult, actualResult);
    }
}
