package com.tencent.supersonic.integration;

import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.application.query.*;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
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
import com.tencent.supersonic.chat.domain.service.QueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
public class QueryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTest.class);

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    //case:alice的访问次数,queryMode:METRIC_FILTER,
    @Test
    public void queryTest_01() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1, "alice的访问次数");
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

    }

    //case:超音数的访问次数,queryMode:METRIC_DOMAIN
    @Test
    public void queryTest_02() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(2, "超音数的访问次数");
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

    }


    //case:超音数各部门的访问次数,queryMode:METRIC_GROUPBY
    @Test
    public void queryTest_03() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1, "超音数各部门的访问次数");
        QueryResultResp queryResultResp = new QueryResultResp();
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
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        Boolean dimensionExist = DataUtils.compareDateDimension(dimensions);
        assertThat(dimensionExist).isEqualTo(true);

        SchemaItem schemaItemDimension1 = DataUtils.getSchemaItem(1L, "部门", "department");
        Boolean dimensionExist1 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension1);
        assertThat(dimensionExist1).isEqualTo(true);


        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

    }


    //case:对比alice和lucy的访问次数,queryMode:METRIC_COMPARE
    @Test
    public void queryTest_04() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1, "对比alice和lucy的访问次数");
        QueryResultResp queryResultResp = new QueryResultResp();
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
        List<String> list = new ArrayList<>();
        list.add("alice");
        list.add("lucy");
        Filter dimensionFilter = DataUtils.getFilter("user_name", FilterOperatorEnum.IN, list, "用户名", 2L);
        Boolean dimensionFilterExist = DataUtils.compareDimensionFilter(dimensionFilters, dimensionFilter);
        assertThat(dimensionFilterExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

    }

    //case:近3天访问次数最多的用户,queryMode:ENTITY_detail
    @Test
    public void queryTest_05() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(2, "艺人周杰伦的代表作");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), EntityDetail.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(2L);


        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        SchemaItem schemaItemDimension = DataUtils.getSchemaItem(5L, "代表作", "song_name");
        Boolean dimensionExist = DataUtils.compareSchemaItem(dimensions, schemaItemDimension);
        assertThat(dimensionExist).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter = DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS, "周杰伦", "歌手名", 7L);
        Boolean dimensionFilterExist = DataUtils.compareDimensionFilter(dimensionFilters, dimensionFilter);
        assertThat(dimensionFilterExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(1, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(true);

    }

    //case:近3天访问次数最多的用户名,queryMode:METRIC_ORDERBY
    @Test
    public void queryTest_06() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(2, "近3天访问次数最多的用户名");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), MetricOrderBy.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), AggregateTypeEnum.MAX);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);

        //assert 指标
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(2L, "访问次数", "pv");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        SchemaItem schemaItemDimension = DataUtils.getSchemaItem(2L, "用户名", "user_name");
        Boolean dimensionExist = DataUtils.compareSchemaItem(dimensions, schemaItemDimension);
        assertThat(dimensionExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(3, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

    }


    //case:播放量最多的艺人,queryMode:ENTITY_LIST_TOPN
    @Test
    public void queryTest_07() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(2, "播放量最多的艺人");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), EntityListTopN.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), AggregateTypeEnum.MAX);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(2L);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        SchemaItem schemaItemDimension = DataUtils.getSchemaItem(7L, "歌手名", "singer_name");
        Boolean dimensionExist = DataUtils.compareSchemaItem(dimensions, schemaItemDimension);
        assertThat(dimensionExist).isEqualTo(true);

        //assert 指标
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(4L, "播放量", "js_play_cnt");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(1, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(true);

    }

    //case:超音数各部门的访问次数总和,queryMode:METRIC_GROUPBY,aggType:sum
    @Test
    public void queryTest_09() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1, "超音数各部门的访问次数总和");
        QueryResultResp queryResultResp = new QueryResultResp();
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
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), AggregateTypeEnum.SUM);
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

        SchemaItem schemaItemDimension1 = DataUtils.getSchemaItem(1L, "部门", "department");
        Boolean dimensionExist1 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension1);
        assertThat(dimensionExist1).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        ;
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

    }

    @Test
    public void queryTest_10() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(2, "爱情、流行类型的艺人");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), EntityListFilter.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(2L);

        //assert 指标
        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(4L, "播放量", "js_play_cnt");
        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
        assertThat(metricExist).isEqualTo(true);

        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        SchemaItem schemaItemDimension = DataUtils.getSchemaItem(7L, "歌手名", "singer_name");
        Boolean dimensionExist = DataUtils.compareSchemaItem(dimensions, schemaItemDimension);
        assertThat(dimensionExist).isEqualTo(true);

        SchemaItem schemaItemDimension1 = DataUtils.getSchemaItem(6L, "风格", "genre");
        Boolean dimensionExist1 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension1);
        assertThat(dimensionExist1).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
        List<String> list = new ArrayList<>();
        list.add("爱情");
        list.add("流行");
        Filter dimensionFilter = DataUtils.getFilter("genre", FilterOperatorEnum.IN, list, "风格", 6L);
        Boolean dimensionFilterExist = DataUtils.compareDimensionFilter(dimensionFilters, dimensionFilter);
        assertThat(dimensionFilterExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(1, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(true);

    }

    //case:近3天访问次数最多的用户,queryMode:ENTITY_detail
    @Test
    public void queryTest_11() {
        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(1, "艺人周杰伦的代表作、风格、活跃区域");
        QueryResultResp queryResultResp = new QueryResultResp();
        try {
            queryResultResp = queryService.executeQuery(queryContextReq);
        } catch (Exception e) {

        }
        LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
        //assert queryState
        Assert.assertEquals(queryResultResp.getQueryState(), 0);
        //assert queryMode
        Assert.assertEquals(queryResultResp.getQueryMode(), EntityDetail.QUERY_MODE);
        //assert aggType
        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
        //assert 主题域Id
        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(2L);


        //assert 维度
        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
        SchemaItem schemaItemDimension = DataUtils.getSchemaItem(5L, "代表作", "song_name");
        Boolean dimensionExist = DataUtils.compareSchemaItem(dimensions, schemaItemDimension);
        assertThat(dimensionExist).isEqualTo(true);

        SchemaItem schemaItemDimension1 = DataUtils.getSchemaItem(4L, "活跃区域", "act_area");
        Boolean dimensionExist1 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension1);
        assertThat(dimensionExist1).isEqualTo(true);

        SchemaItem schemaItemDimension2 = DataUtils.getSchemaItem(6L, "风格", "genre");
        Boolean dimensionExist2 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension2);
        assertThat(dimensionExist2).isEqualTo(true);

        //assert 维度filter
        Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
        Filter dimensionFilter = DataUtils.getFilter("singer_name", FilterOperatorEnum.EQUALS, "周杰伦", "歌手名", 7L);
        Boolean dimensionFilterExist = DataUtils.compareDimensionFilter(dimensionFilters, dimensionFilter);
        assertThat(dimensionFilterExist).isEqualTo(true);

        //assert 时间filter
        DateConf dateInfo = DataUtils.getDateConf(1, DateConf.DateMode.RECENT_UNITS, "DAY");
        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
        assertThat(timeFilterExist).isEqualTo(true);

        //assert nativeQuery
        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(true);

    }

    //    @Test
//    public void queryTest_11() {
//        QueryContextReq queryContextReq = DataUtils.getQueryContextReq("最近4天HR部门的访问人数");
//        try {
//            QueryResultResp queryResultResp = queryService.executeQuery(queryContextReq);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//case:各部门对各页面的访问次数,queryMode:METRIC_GROUPBY
//    @Test
//    public void queryTest_08() {
//        QueryContextReq queryContextReq = DataUtils.getQueryContextReq(3, "各部门对各页面的访问次数");
//        QueryResultResp queryResultResp = new QueryResultResp();
//        try {
//            queryResultResp = queryService.executeQuery(queryContextReq);
//        } catch (Exception e) {
//
//        }
//        LOGGER.info("QueryResultResp queryResultResp:{}", JsonUtil.toString(queryResultResp));
//        //assert queryState
//        Assert.assertEquals(queryResultResp.getQueryState(), 0);
//        //assert queryMode
//        Assert.assertEquals(queryResultResp.getQueryMode(), MetricGroupBy.QUERY_MODE);
//        //assert aggType
//        Assert.assertEquals(queryResultResp.getChatContext().getAggType(), null);
//        //assert 主题域Id
//        assertThat(queryResultResp.getChatContext().getDomainId()).isEqualTo(1L);
//
//        //assert 指标
//        Set<SchemaItem> metrics = queryResultResp.getChatContext().getMetrics();
//        SchemaItem schemaItemMetric = DataUtils.getSchemaItem(2L, "访问次数", "pv");
//        Boolean metricExist = DataUtils.compareSchemaItem(metrics, schemaItemMetric);
//        assertThat(metricExist).isEqualTo(true);
//
//        //assert 维度
//        Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
//        Boolean dimensionExist = DataUtils.compareDateDimension(dimensions);
//        assertThat(dimensionExist).isEqualTo(true);
//
//        SchemaItem schemaItemDimension1 = DataUtils.getSchemaItem(1L, "部门", "department");
//        Boolean dimensionExist1 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension1);
//        assertThat(dimensionExist1).isEqualTo(true);
//
//        SchemaItem schemaItemDimension2 = DataUtils.getSchemaItem(3L, "页面", "page");
//        Boolean dimensionExist2 = DataUtils.compareSchemaItem(dimensions, schemaItemDimension2);
//        assertThat(dimensionExist2).isEqualTo(true);
//
//
//        //assert 时间filter
//        DateConf dateInfo = DataUtils.getDateConf(7, DateConf.DateMode.RECENT_UNITS, "DAY");
//        Boolean timeFilterExist = DataUtils.compareDate(queryResultResp.getChatContext().getDateInfo(), dateInfo);
//        assertThat(timeFilterExist).isEqualTo(true);
//
//        //assert nativeQuery
//        assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);
//
//    }

}
