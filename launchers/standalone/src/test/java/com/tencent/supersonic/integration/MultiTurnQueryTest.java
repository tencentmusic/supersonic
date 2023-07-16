package com.tencent.supersonic.integration;

import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.application.query.MetricCompare;
import com.tencent.supersonic.chat.application.query.MetricDomain;
import com.tencent.supersonic.chat.application.query.MetricFilter;
import com.tencent.supersonic.chat.application.query.MetricGroupBy;
import com.tencent.supersonic.chat.domain.service.QueryService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
public class MultiTurnQueryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTurnQueryTest.class);

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    //case:alice的访问次数->停留时长呢？->想知道lucy的,queryMode:METRIC_FILTER
    @Test
    public void queryTest1() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1,"alice的访问次数");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist = DataUtils.compareDateDimension(dimensions);
        assertThat(dimensionExist).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter = DataUtils.getFilter("user_name", FilterOperatorEnum.EQUALS, "alice", "用户名", 2L);
        Boolean dimensionFilterExist = DataUtils.compareDimensionFilter(dimensionFilters, dimensionFilter);
        assertThat(dimensionFilterExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


        queryContextReq = DataUtils.getQueryContextReq(1,"停留时长呢？");
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics1 = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric1 = DataUtils.getSchemaItem(1L, "停留时长", "stay_hours");
        Boolean metricExist1 = DataUtils.compareSchemaItem(metrics1, schemaItemMetric1);
        assertThat(metricExist1).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions1 = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist1 = DataUtils.compareDateDimension(dimensions1);
        assertThat(dimensionExist1).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters1 = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter1 = DataUtils.getFilter("user_name", FilterOperatorEnum.EQUALS, "alice", "用户名", 2L);
        Boolean dimensionFilterExist1 = DataUtils.compareDimensionFilter(dimensionFilters1, dimensionFilter1);
        assertThat(dimensionFilterExist1).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo1 = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist1 = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo1);
        assertThat(timeFilterExist1).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


        queryContextReq = DataUtils.getQueryContextReq(1,"想知道lucy的");
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics2 = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric2 = DataUtils.getSchemaItem(1L, "停留时长", "stay_hours");
        Boolean metricExist2 = DataUtils.compareSchemaItem(metrics2, schemaItemMetric2);
        assertThat(metricExist2).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions2 = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist2 = DataUtils.compareDateDimension(dimensions2);
        assertThat(dimensionExist2).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters2 = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter2 = DataUtils.getFilter("user_name", FilterOperatorEnum.EQUALS, "lucy", "用户名", 2L);
        Boolean dimensionFilterExist2 = DataUtils.compareDimensionFilter(dimensionFilters2, dimensionFilter2);
        ;
        assertThat(dimensionFilterExist2).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo2 = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist2 = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo2);
        assertThat(timeFilterExist2).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

    }


    //case:超音数的访问次数->按部门的呢->p2的呢
    @Test
    public void queryTest2() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(2,"超音数的访问次数");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricDomain.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist = DataUtils.compareDateDimension(dimensions);
        assertThat(dimensionExist).isEqualTo(true);


        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


        queryContextReq = DataUtils.getQueryContextReq(2,"按部门的呢");
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricGroupBy.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics1 = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric1 = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist1 = DataUtils.compareSchemaItem(metrics1, schemaItemMetric1);
        assertThat(metricExist1).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions1 = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist1 = DataUtils.compareDateDimension(dimensions1);
        assertThat(dimensionExist1).isEqualTo(true);

        SchemaItem schemaItemDimension2 = DataUtils.getSchemaItem(1L, "部门", "department");
        Boolean dimensionExist2 = DataUtils.compareSchemaItem(dimensions1, schemaItemDimension2);
        assertThat(dimensionExist2).isEqualTo(true);


        //assert 时间filter
        DateConf dateInfo1 = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist1 = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo1);
        assertThat(timeFilterExist1).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


        queryContextReq = DataUtils.getQueryContextReq(2,"p2的呢");
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics2 = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric2 = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist2 = DataUtils.compareSchemaItem(metrics2, schemaItemMetric2);
        assertThat(metricExist2).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions2 = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist3 = DataUtils.compareDateDimension(dimensions2);
        assertThat(dimensionExist3).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters2 = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter2 = DataUtils.getFilter("page", FilterOperatorEnum.EQUALS, "p2", "页面", 3L);
        Boolean dimensionFilterExist2 = DataUtils.compareDimensionFilter(dimensionFilters2, dimensionFilter2);
        assertThat(dimensionFilterExist2).isEqualTo(true);


        //assert 时间filter
        DateConf dateInfo2 = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist2 = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo2);
        assertThat(timeFilterExist2).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

    }

    //alice的访问次数->对比alice和lucy呢->他是哪个部门的呢
    @Test
    public void queryTest3() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1,"alice的访问次数");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist = DataUtils.compareDateDimension(dimensions);
        assertThat(dimensionExist).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter = DataUtils.getFilter("user_name", FilterOperatorEnum.EQUALS, "alice", "用户名", 2L);
        Boolean dimensionFilterExist = DataUtils.compareDimensionFilter(dimensionFilters, dimensionFilter);
        assertThat(dimensionFilterExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


        queryContextReq = DataUtils.getQueryContextReq(1,"对比alice和lucy呢");
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricCompare.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics1 = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric1 = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist1 = DataUtils.compareSchemaItem(metrics1, schemaItemMetric1);
        assertThat(metricExist1).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions1 = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist1 = DataUtils.compareDateDimension(dimensions1);
        assertThat(dimensionExist1).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters1 = queryResultResp.getChatContext().getDimensionFilters();
        List<String> list = new ArrayList<>();
        list.add("alice");
        list.add("lucy");
        Filter dimensionFilter1 = DataUtils.getFilter("user_name", FilterOperatorEnum.IN, list, "用户名", 2L);
        Boolean dimensionFilterExist1 = DataUtils.compareDimensionFilter(dimensionFilters1, dimensionFilter1);
        assertThat(dimensionFilterExist1).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo1 = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist1 = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo1);
        assertThat(timeFilterExist1).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


        queryContextReq = DataUtils.getQueryContextReq(1,"他是哪个部门的呢");
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics2 = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric2 = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist2 = DataUtils.compareSchemaItem(metrics2, schemaItemMetric2);
        assertThat(metricExist2).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions2 = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist2 = DataUtils.compareDateDimension(dimensions2);
        assertThat(dimensionExist2).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters2 = queryResultResp.getChatContext().getDimensionFilters();
        List<String> list1 = new ArrayList<>();
        list1.add("alice");
        list1.add("lucy");
        Filter dimensionFilter2 = DataUtils.getFilter("user_name", FilterOperatorEnum.IN, list1, "用户名", 2L);
        Boolean dimensionFilterExist2 = DataUtils.compareDimensionFilter(dimensionFilters2, dimensionFilter2);
        assertThat(dimensionFilterExist2).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo2 = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist2 = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo2);
        assertThat(timeFilterExist2).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);


    }


}
