package com.tencent.supersonic.test;

import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.response.QueryResultResp;
import com.tencent.supersonic.chat.application.query.MetricCompare;
import com.tencent.supersonic.chat.application.query.MetricDomain;
import com.tencent.supersonic.chat.application.query.MetricFilter;
import com.tencent.supersonic.chat.application.query.MetricGroupBy;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
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
public class QueryIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryIntegrationTest.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    //case:alice的访问次数,queryMode:METRIC_FILTER,
    @Test
    public void queryTest1() {
        QueryContextReq queryContextReq = getQueryContextReq("alice的访问次数");
        try {
            QueryResultResp queryResultResp = queryService.executeQuery(queryContextReq);
            LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
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
            SchemaItem schemaItemMetric = new SchemaItem();
            schemaItemMetric.setId(2L);
            schemaItemMetric.setName("访问次数");
            schemaItemMetric.setBizName("pv");
            Boolean metricExist = false;
            for (SchemaItem schemaItem : metrics) {
                if (schemaItem.getId().equals(schemaItemMetric.getId()) &&
                        schemaItem.getName().equals(schemaItemMetric.getName()) &&
                        schemaItem.getBizName().equals(schemaItemMetric.getBizName())) {
                    metricExist = true;
                }
            }
            assertThat(metricExist).isEqualTo(true);

            //assert 维度
            Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
            SchemaItem schemaItemDimension = new SchemaItem();
            schemaItemDimension.setBizName("sys_imp_date");
            Boolean dimensionExist = false;
            for (SchemaItem schemaItem : dimensions) {
                if (schemaItem.getBizName().equals(schemaItemDimension.getBizName())) {
                    dimensionExist = true;
                }
            }
            assertThat(dimensionExist).isEqualTo(true);

            //assert 维度filter
            Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
            Filter dimensionFilter = new Filter();
            dimensionFilter.setBizName("user_name");
            dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
            dimensionFilter.setValue("alice");
            dimensionFilter.setName("用户名");
            dimensionFilter.setElementID(2L);
            Boolean dimensionFilterExist = false;
            for (Filter filter : dimensionFilters) {
                if (filter.getBizName().equals(dimensionFilter.getBizName()) &&
                        filter.getOperator().equals(dimensionFilter.getOperator()) &&
                        filter.getValue().equals(dimensionFilter.getValue()) &&
                        filter.getElementID().equals(dimensionFilter.getElementID()) &&
                        filter.getName().equals(dimensionFilter.getName())) {
                    dimensionFilterExist = true;
                }
            }
            assertThat(dimensionFilterExist).isEqualTo(true);

            //assert 时间filter
            DateConf dateInfo = new DateConf();
            dateInfo.setUnit(7);
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setPeriod("DAY");
            Boolean timeFilterExist =
                    queryResultResp.getChatContext().getDateInfo().getUnit().equals(dateInfo.getUnit()) &&
                            queryResultResp.getChatContext().getDateInfo().getDateMode().equals(dateInfo.getDateMode())
                            &&
                            queryResultResp.getChatContext().getDateInfo().getPeriod().equals(dateInfo.getPeriod());
            assertThat(timeFilterExist).isEqualTo(true);

            //assert nativeQuery
            assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

        } catch (Exception e) {

        }
    }

    //case:超音数的访问次数,queryMode:METRIC_DOMAIN
    @Test
    public void queryTest2() {
        QueryContextReq queryContextReq = getQueryContextReq("超音数的访问次数");
        try {
            QueryResultResp queryResultResp = queryService.executeQuery(queryContextReq);
            LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
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
            SchemaItem schemaItemMetric = new SchemaItem();
            schemaItemMetric.setId(2L);
            schemaItemMetric.setName("访问次数");
            schemaItemMetric.setBizName("pv");
            Boolean metricExist = false;
            for (SchemaItem schemaItem : metrics) {
                if (schemaItem.getId().equals(schemaItemMetric.getId()) &&
                        schemaItem.getName().equals(schemaItemMetric.getName()) &&
                        schemaItem.getBizName().equals(schemaItemMetric.getBizName())) {
                    metricExist = true;
                }
            }
            assertThat(metricExist).isEqualTo(true);

            //assert 维度
            Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
            SchemaItem schemaItemDimension = new SchemaItem();
            schemaItemDimension.setBizName("sys_imp_date");
            Boolean dimensionExist = false;
            for (SchemaItem schemaItem : dimensions) {
                if (schemaItem.getBizName().equals(schemaItemDimension.getBizName())) {
                    dimensionExist = true;
                }
            }
            assertThat(dimensionExist).isEqualTo(true);

            //assert 时间filter
            DateConf dateInfo = new DateConf();
            dateInfo.setUnit(7);
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setPeriod("DAY");
            Boolean timeFilterExist =
                    queryResultResp.getChatContext().getDateInfo().getUnit().equals(dateInfo.getUnit()) &&
                            queryResultResp.getChatContext().getDateInfo().getDateMode().equals(dateInfo.getDateMode())
                            &&
                            queryResultResp.getChatContext().getDateInfo().getPeriod().equals(dateInfo.getPeriod());
            assertThat(timeFilterExist).isEqualTo(true);

            //assert nativeQuery
            assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

        } catch (Exception e) {

        }
    }


    //case:超音数各部门的访问次数,queryMode:METRIC_GROUPBY
    @Test
    public void queryTest3() {
        QueryContextReq queryContextReq = getQueryContextReq("超音数各部门的访问次数");
        try {
            QueryResultResp queryResultResp = queryService.executeQuery(queryContextReq);
            LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
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
            SchemaItem schemaItemMetric = new SchemaItem();
            schemaItemMetric.setId(2L);
            schemaItemMetric.setName("访问次数");
            schemaItemMetric.setBizName("pv");
            Boolean metricExist = false;
            for (SchemaItem schemaItem : metrics) {
                if (schemaItem.getId().equals(schemaItemMetric.getId()) &&
                        schemaItem.getName().equals(schemaItemMetric.getName()) &&
                        schemaItem.getBizName().equals(schemaItemMetric.getBizName())) {
                    metricExist = true;
                }
            }
            assertThat(metricExist).isEqualTo(true);

            //assert 维度
            Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
            SchemaItem schemaItemDimension = new SchemaItem();
            schemaItemDimension.setBizName("sys_imp_date");
            Boolean dimensionExist = false;
            for (SchemaItem schemaItem : dimensions) {
                if (schemaItem.getBizName().equals(schemaItemDimension.getBizName())) {
                    dimensionExist = true;
                }
            }
            assertThat(dimensionExist).isEqualTo(true);

            SchemaItem schemaItemDimension1 = new SchemaItem();
            schemaItemDimension1.setBizName("department");
            schemaItemDimension1.setName("部门");
            schemaItemDimension1.setId(1L);
            Boolean dimensionExist1 = false;
            for (SchemaItem schemaItem : dimensions) {
                if (schemaItem.getBizName().equals(schemaItemDimension1.getBizName()) &&
                        schemaItem.getId().equals(schemaItemDimension1.getId()) &&
                        schemaItem.getName().equals(schemaItemDimension1.getName())) {
                    dimensionExist1 = true;
                }
            }
            assertThat(dimensionExist1).isEqualTo(true);

            //assert 时间filter
            DateConf dateInfo = new DateConf();
            dateInfo.setUnit(7);
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setPeriod("DAY");
            Boolean timeFilterExist =
                    queryResultResp.getChatContext().getDateInfo().getUnit().equals(dateInfo.getUnit()) &&
                            queryResultResp.getChatContext().getDateInfo().getDateMode().equals(dateInfo.getDateMode())
                            &&
                            queryResultResp.getChatContext().getDateInfo().getPeriod().equals(dateInfo.getPeriod());
            assertThat(timeFilterExist).isEqualTo(true);

            //assert nativeQuery
            assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

        } catch (Exception e) {

        }
    }


    //case:对比alice和lucy的访问次数,queryMode:METRIC_COMPARE
    @Test
    public void queryTest4() {
        QueryContextReq queryContextReq = getQueryContextReq("对比alice和lucy的访问次数");
        try {
            QueryResultResp queryResultResp = queryService.executeQuery(queryContextReq);
            LOGGER.info("QueryResultResp queryResultResp:{}", queryResultResp);
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
            SchemaItem schemaItemMetric = new SchemaItem();
            schemaItemMetric.setId(2L);
            schemaItemMetric.setName("访问次数");
            schemaItemMetric.setBizName("pv");
            Boolean metricExist = false;
            for (SchemaItem schemaItem : metrics) {
                if (schemaItem.getId().equals(schemaItemMetric.getId()) &&
                        schemaItem.getName().equals(schemaItemMetric.getName()) &&
                        schemaItem.getBizName().equals(schemaItemMetric.getBizName())) {
                    metricExist = true;
                }
            }
            assertThat(metricExist).isEqualTo(true);

            //assert 维度
            Set<SchemaItem> dimensions = queryResultResp.getChatContext().getDimensions();
            SchemaItem schemaItemDimension = new SchemaItem();
            schemaItemDimension.setBizName("sys_imp_date");
            Boolean dimensionExist = false;
            for (SchemaItem schemaItem : dimensions) {
                if (schemaItem.getBizName().equals(schemaItemDimension.getBizName())) {
                    dimensionExist = true;
                }
            }
            assertThat(dimensionExist).isEqualTo(true);

            //assert 维度filter
            Set<Filter> dimensionFilters = queryResultResp.getChatContext().getDimensionFilters();
            Filter dimensionFilter = new Filter();
            dimensionFilter.setBizName("user_name");
            dimensionFilter.setOperator(FilterOperatorEnum.IN);
            List<String> list = new ArrayList<>();
            list.add("alice");
            list.add("lucy");
            dimensionFilter.setValue(list);
            dimensionFilter.setName("用户名");
            dimensionFilter.setElementID(2L);
            Boolean dimensionFilterExist = false;
            for (Filter filter : dimensionFilters) {
                if (filter.getBizName().equals(dimensionFilter.getBizName()) &&
                        filter.getOperator().equals(dimensionFilter.getOperator()) &&
                        filter.getValue().toString().equals(dimensionFilter.getValue().toString()) &&
                        filter.getElementID().equals(dimensionFilter.getElementID()) &&
                        filter.getName().equals(dimensionFilter.getName())) {
                    dimensionFilterExist = true;
                }
            }
            assertThat(dimensionFilterExist).isEqualTo(true);

            //assert 时间filter
            DateConf dateInfo = new DateConf();
            dateInfo.setUnit(7);
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setPeriod("DAY");
            Boolean timeFilterExist =
                    queryResultResp.getChatContext().getDateInfo().getUnit().equals(dateInfo.getUnit()) &&
                            queryResultResp.getChatContext().getDateInfo().getDateMode().equals(dateInfo.getDateMode())
                            &&
                            queryResultResp.getChatContext().getDateInfo().getPeriod().equals(dateInfo.getPeriod());
            assertThat(timeFilterExist).isEqualTo(true);

            //assert nativeQuery
            assertThat(queryResultResp.getChatContext().getNativeQuery()).isEqualTo(false);

        } catch (Exception e) {

        }
    }

    public QueryContextReq getQueryContextReq(String query) {
        QueryContextReq queryContextReq = new QueryContextReq();
        queryContextReq.setQueryText(query);//"alice的访问次数"
        queryContextReq.setChatId(1);
        queryContextReq.setUser(new User(1L, "admin", "admin", "admin@email"));
        return queryContextReq;
    }

}
