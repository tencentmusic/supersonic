package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.headless.core.config.ExecutorConfig;
import com.tencent.supersonic.headless.core.pojo.StructQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class SqlGenerateUtilsTest {

    private SqlFilterUtils sqlFilterUtils;

    private ExecutorConfig executorConfig;

    private DateModeUtils dateModeUtils;

    private SqlGenerateUtils sqlGenerateUtils;

    @BeforeEach
    void setUp() {
        sqlFilterUtils = Mockito.mock(SqlFilterUtils.class);
        executorConfig = Mockito.mock(ExecutorConfig.class);
        dateModeUtils = new DateModeUtils();
        dateModeUtils.setSysZipperDateColBegin("start_");
        dateModeUtils.setSysZipperDateColEnd("end_");
        sqlGenerateUtils = new SqlGenerateUtils(sqlFilterUtils, dateModeUtils, executorConfig);
    }

    // ---------------------------------------------------------------
    // getSelect() tests
    // ---------------------------------------------------------------

    @Test
    void getSelect_emptyGroupsAndAggregators_returnsStar() {
        StructQuery query = new StructQuery();
        query.setGroups(new ArrayList<>());
        query.setAggregators(new ArrayList<>());

        String result = sqlGenerateUtils.getSelect(query);

        assertEquals("*", result);
    }

    @Test
    void getSelect_withGroupsOnly_returnsGroupColumns() {
        StructQuery query = new StructQuery();
        query.setGroups(Arrays.asList("city", "product"));
        query.setAggregators(new ArrayList<>());

        String result = sqlGenerateUtils.getSelect(query);

        assertEquals("city,product", result);
    }

    @Test
    void getSelect_withGroupsAndAggregators_returnsBoth() {
        StructQuery query = new StructQuery();
        query.setGroups(Collections.singletonList("city"));

        Aggregator agg = new Aggregator("revenue", AggOperatorEnum.SUM);
        query.setAggregators(Collections.singletonList(agg));

        String result = sqlGenerateUtils.getSelect(query);

        assertTrue(result.startsWith("city,"), "Should start with group column");
        assertTrue(result.contains("SUM"), "Should contain SUM aggregation");
        assertTrue(result.contains("revenue"), "Should contain aggregated column name");
    }

    @Test
    void getSelect_withAggregatorsOnly_returnsAggOnly() {
        StructQuery query = new StructQuery();
        query.setGroups(new ArrayList<>());

        Aggregator agg = new Aggregator("revenue", AggOperatorEnum.SUM);
        query.setAggregators(Collections.singletonList(agg));

        String result = sqlGenerateUtils.getSelect(query);

        assertNotEquals("*", result);
        assertTrue(result.contains("SUM"));
        assertTrue(result.contains("revenue"));
    }

    @Test
    void getSelect_countDistinctAggregator_producesCountDistinctSyntax() {
        StructQuery query = new StructQuery();
        query.setGroups(new ArrayList<>());

        Aggregator agg = new Aggregator("user_id", AggOperatorEnum.COUNT_DISTINCT);
        query.setAggregators(Collections.singletonList(agg));

        String result = sqlGenerateUtils.getSelect(query);

        assertTrue(result.contains("count(distinct user_id"),
                "COUNT_DISTINCT should produce count(distinct ...) syntax");
    }

    // ---------------------------------------------------------------
    // getDateWhereClause() tests
    // ---------------------------------------------------------------

    @Test
    void getDateWhereClause_betweenMode_nullDateDate_returnsBetweenDateStr() {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
        dateInfo.setStartDate("2025-03-04");
        dateInfo.setEndDate("2025-03-10");
        dateInfo.setDateField("workday");

        String result = sqlGenerateUtils.getDateWhereClause(dateInfo, null);

        assertTrue(result.contains("workday >= '2025-03-04'"),
                "Should contain start date condition, got: " + result);
        assertTrue(result.contains("workday <= '2025-03-10'"),
                "Should contain end date condition, got: " + result);
    }

    @Test
    void getDateWhereClause_recentMode_nullDateDate_returnsDefaultRecentDateInfo() {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.RECENT);
        dateInfo.setUnit(7);
        dateInfo.setPeriod(DatePeriodEnum.DAY);
        dateInfo.setDateField("workday");

        String result = sqlGenerateUtils.getDateWhereClause(dateInfo, null);

        // defaultRecentDateInfo for DAY: (workday >= '<now-1 - 6 days>' and workday <= '<now-1>')
        LocalDate expectedMax = LocalDate.now().minusDays(1);
        LocalDate expectedMin = expectedMax.minusDays(6);
        String fmt = "yyyy-MM-dd";

        assertTrue(
                result.contains("workday >= '"
                        + expectedMin.format(DateTimeFormatter.ofPattern(fmt)) + "'"),
                "Should contain min date, got: " + result);
        assertTrue(
                result.contains("workday <= '"
                        + expectedMax.format(DateTimeFormatter.ofPattern(fmt)) + "'"),
                "Should contain max date, got: " + result);
    }

    @Test
    void getDateWhereClause_listMode_nullDateDate_returnsListDateStr() {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.LIST);
        dateInfo.setDateField("workday");
        dateInfo.setDateList(Arrays.asList("2025-03-01", "2025-03-05", "2025-03-10"));

        String result = sqlGenerateUtils.getDateWhereClause(dateInfo, null);

        assertTrue(result.contains("workday in"), "Should use IN clause, got: " + result);
        assertTrue(result.contains("'2025-03-01'"), "Should contain first date");
        assertTrue(result.contains("'2025-03-05'"), "Should contain second date");
        assertTrue(result.contains("'2025-03-10'"), "Should contain third date");
    }

    // ---------------------------------------------------------------
    // generateWhere() tests
    // ---------------------------------------------------------------

    @Test
    void generateWhere_nullDateInfo_noDateCondition() {
        StructQuery query = new StructQuery();
        query.setDateInfo(null);
        query.setDimensionFilters(new ArrayList<>());

        when(sqlFilterUtils.getWhereClause(anyList())).thenReturn("");

        String result = sqlGenerateUtils.generateWhere(query, null);

        assertFalse(result.contains(">="), "Should not contain date range operator");
        assertFalse(result.contains("<="), "Should not contain date range operator");
        assertEquals("", result, "With null dateInfo and no filters, WHERE clause should be empty");
    }

    @Test
    void generateWhere_withDateInfoAndNoFilters_returnsDateWhereOnly() {
        StructQuery query = new StructQuery();

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.BETWEEN);
        dateConf.setStartDate("2025-03-04");
        dateConf.setEndDate("2025-03-10");
        dateConf.setDateField("workday");
        query.setDateInfo(dateConf);
        query.setDimensionFilters(new ArrayList<>());

        when(sqlFilterUtils.getWhereClause(anyList())).thenReturn("");

        String result = sqlGenerateUtils.generateWhere(query, null);

        assertTrue(result.startsWith("where "), "Should start with 'where', got: " + result);
        assertTrue(result.contains("workday >= '2025-03-04'"), "Should contain start date");
        assertTrue(result.contains("workday <= '2025-03-10'"), "Should contain end date");
    }

    @Test
    void generateWhere_withDateInfoAndFilters_combinesBothWithAnd() {
        StructQuery query = new StructQuery();

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.BETWEEN);
        dateConf.setStartDate("2025-01-01");
        dateConf.setEndDate("2025-01-31");
        dateConf.setDateField("dt");
        query.setDateInfo(dateConf);
        query.setDimensionFilters(new ArrayList<>());

        when(sqlFilterUtils.getWhereClause(anyList())).thenReturn("city = 'Beijing'");

        String result = sqlGenerateUtils.generateWhere(query, null);

        assertTrue(result.startsWith("where "), "Should start with 'where'");
        assertTrue(result.contains("AND"), "Should combine date and filter with AND");
        assertTrue(result.contains("dt >= '2025-01-01'"), "Should contain date condition");
        assertTrue(result.contains("city = 'Beijing'"), "Should contain filter condition");
    }

    // ---------------------------------------------------------------
    // getGroupBy() / getOrderBy() / getLimit() tests
    // ---------------------------------------------------------------

    @Test
    void getGroupBy_emptyGroups_returnsEmptyString() {
        StructQuery query = new StructQuery();
        query.setGroups(new ArrayList<>());

        assertEquals("", sqlGenerateUtils.getGroupBy(query));
    }

    @Test
    void getGroupBy_withGroups_returnsGroupByClause() {
        StructQuery query = new StructQuery();
        query.setGroups(Arrays.asList("city", "product"));

        String result = sqlGenerateUtils.getGroupBy(query);

        assertEquals("group by city,product", result);
    }

    @Test
    void getGroupBy_detailQueryWithGroups_returnsEmptyString() {
        StructQuery query = new StructQuery();
        query.setQueryType(QueryType.DETAIL);
        query.setGroups(Arrays.asList("city", "product"));

        String result = sqlGenerateUtils.getGroupBy(query);

        assertEquals("", result);
    }

    @Test
    void getOrderBy_emptyOrders_returnsEmptyString() {
        StructQuery query = new StructQuery();
        query.setOrders(new ArrayList<>());

        assertEquals("", sqlGenerateUtils.getOrderBy(query));
    }

    @Test
    void getLimit_positiveLimit_returnsLimitClause() {
        StructQuery query = new StructQuery();
        query.setLimit(100L);

        assertEquals(" limit 100", sqlGenerateUtils.getLimit(query));
    }

    @Test
    void getLimit_nullLimit_returnsEmptyString() {
        StructQuery query = new StructQuery();
        query.setLimit(null);

        assertEquals("", sqlGenerateUtils.getLimit(query));
    }

    @Test
    void getLimit_zeroLimit_returnsEmptyString() {
        StructQuery query = new StructQuery();
        query.setLimit(0L);

        assertEquals("", sqlGenerateUtils.getLimit(query));
    }

    // ---------------------------------------------------------------
    // isSupportWith() tests
    // ---------------------------------------------------------------

    @Test
    void isSupportWith_mysqlAboveLowVersion_returnsTrue() {
        when(executorConfig.getMysqlLowVersion()).thenReturn("8.0");

        assertTrue(sqlGenerateUtils
                .isSupportWith(com.tencent.supersonic.common.pojo.enums.EngineType.MYSQL, "8.1"));
    }

    @Test
    void isSupportWith_mysqlBelowLowVersion_returnsFalse() {
        when(executorConfig.getMysqlLowVersion()).thenReturn("8.0");

        assertFalse(sqlGenerateUtils
                .isSupportWith(com.tencent.supersonic.common.pojo.enums.EngineType.MYSQL, "5.7"));
    }

    @Test
    void isSupportWith_nullVersion_returnsTrue() {
        assertTrue(sqlGenerateUtils
                .isSupportWith(com.tencent.supersonic.common.pojo.enums.EngineType.MYSQL, null));
    }
}
