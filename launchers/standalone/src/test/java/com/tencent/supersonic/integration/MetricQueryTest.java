package com.tencent.supersonic.integration;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.config.ChatConfigEditReqReq;
import com.tencent.supersonic.chat.config.ChatConfigResp;
import com.tencent.supersonic.chat.config.ItemVisibility;
import com.tencent.supersonic.chat.query.rule.metric.MetricDomainQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricFilterQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricGroupByQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricTopNQuery;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.*;


public class MetricQueryTest extends BaseQueryTest {

    @Test
    public void queryTest_METRIC_FILTER() throws Exception {
        QueryResult actualResult = submitNewChat("alice的访问次数");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户名", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_METRIC_DOMAIN() throws Exception {
        QueryResult actualResult = submitNewChat("超音数的访问次数");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricDomainQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_METRIC_GROUPBY() throws Exception {
        QueryResult actualResult = submitNewChat("超音数各部门的访问次数");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_METRIC_FILTER_COMPARE() throws Exception {
        QueryResult actualResult = submitNewChat("对比alice和lucy的访问次数");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        List<String> list = new ArrayList<>();
        list.add("alice");
        list.add("lucy");
        QueryFilter dimensionFilter = DataUtils.getFilter("user_name", FilterOperatorEnum.IN, list, "用户名", 2L);
        expectedParseInfo.getDimensionFilters().add(dimensionFilter);

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_METRIC_TOPN() throws Exception {
        QueryResult actualResult = submitNewChat("近3天访问次数最多的用户");

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricTopNQuery.QUERY_MODE);
        expectedParseInfo.setAggType(SUM);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("用户名"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(3, DateConf.DateMode.RECENT_UNITS, "DAY"));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_METRIC_GROUPBY_SUM() throws Exception {
        QueryResult actualResult = submitNewChat("超音数各部门的访问次数总和");
        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricGroupByQuery.QUERY_MODE);
        expectedParseInfo.setAggType(SUM);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getDimensions().add(DataUtils.getSchemaElement("部门"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_METRIC_FILTER_TIME() throws Exception {
        DateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        DateFormat textFormat = new SimpleDateFormat("yyyy年mm月dd日");
        String dateStr = textFormat.format(format.parse(startDay));

        QueryResult actualResult = submitNewChat(String.format("想知道{}alice的访问次数", dateStr));

        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricFilterQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        expectedParseInfo.getDimensionFilters().add(DataUtils.getFilter("user_name",
                FilterOperatorEnum.EQUALS, "alice", "用户名", 2L));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, startDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);
    }

    @Test
    public void queryTest_CONFIG_VISIBILITY() throws Exception {
        // 1. round_1 use blacklist
        ChatConfigResp chatConfig = configService.fetchConfigByDomainId(1L);
        ChatConfigEditReqReq extendEditCmd = new ChatConfigEditReqReq();
        BeanUtils.copyProperties(chatConfig, extendEditCmd);
        // add blacklist
        List<Long> blackMetrics = Arrays.asList(3L);
        extendEditCmd.getChatAggConfig().getVisibility().setBlackMetricIdList(blackMetrics);
        configService.editConfig(extendEditCmd, User.getFakeUser());

        QueryResult actualResult = submitNewChat("超音数访问人数、访问次数");
        QueryResult expectedResult = new QueryResult();
        SemanticParseInfo expectedParseInfo = new SemanticParseInfo();
        expectedResult.setChatContext(expectedParseInfo);

        expectedResult.setQueryMode(MetricDomainQuery.QUERY_MODE);
        expectedParseInfo.setAggType(NONE);

        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));

        expectedParseInfo.setDateInfo(DataUtils.getDateConf(DateConf.DateMode.BETWEEN_CONTINUOUS, startDay, endDay));
        expectedParseInfo.setNativeQuery(false);

        assertQueryResult(expectedResult, actualResult);

        // 2. round_2 no blacklist
        // remove blacklist
        extendEditCmd.getChatAggConfig().setVisibility(new ItemVisibility());
        configService.editConfig(extendEditCmd, User.getFakeUser());

        actualResult = submitNewChat("超音数访问人数、访问次数");
        expectedParseInfo.getMetrics().clear();
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问次数"));
        expectedParseInfo.getMetrics().add(DataUtils.getSchemaElement("访问人数"));

        assertQueryResult(expectedResult, actualResult);

    }

}
