package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DateModeUtilsTest {

    private DateModeUtils dateModeUtils;

    @BeforeEach
    void setUp() {
        dateModeUtils = new DateModeUtils();
        // @Value defaults are applied by Spring; set them manually for unit tests
        dateModeUtils.setSysZipperDateColBegin("start_");
        dateModeUtils.setSysZipperDateColEnd("end_");
    }

    // ---------------------------------------------------------------
    // getDateWhereStr — null / missing inputs
    // ---------------------------------------------------------------

    @Test
    void getDateWhereStr_nullDateInfo_returnsEmpty() {
        String result = dateModeUtils.getDateWhereStr(null);
        assertEquals("", result);
    }

    @Test
    void getDateWhereStr_nullDateField_returnsEmpty() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.BETWEEN);
        conf.setDateField(null);

        String result = dateModeUtils.getDateWhereStr(conf);
        assertEquals("", result);
    }

    // ---------------------------------------------------------------
    // getDateWhereStr — BETWEEN mode
    // ---------------------------------------------------------------

    @Test
    void getDateWhereStr_betweenMode_producesRangeCondition() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.BETWEEN);
        conf.setStartDate("2025-03-04");
        conf.setEndDate("2025-03-10");
        conf.setDateField("workday");
        conf.setPeriod(DatePeriodEnum.DAY);

        String result = dateModeUtils.getDateWhereStr(conf);
        assertEquals("workday >= '2025-03-04' and workday <= '2025-03-10'", result);
    }

    @Test
    void getDateWhereStr_betweenMode_monthPeriodWithDash() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.BETWEEN);
        conf.setStartDate("2025-01-15");
        conf.setEndDate("2025-03-20");
        conf.setDateField("report_month");
        conf.setPeriod(DatePeriodEnum.MONTH);

        String result = dateModeUtils.getDateWhereStr(conf);
        // MONTH period with dash-containing dates → formatted to yyyy-MM
        assertEquals("report_month >= '2025-01' and report_month <= '2025-03'", result);
    }

    // ---------------------------------------------------------------
    // getDateWhereStr — LIST mode
    // ---------------------------------------------------------------

    @Test
    void getDateWhereStr_listMode_producesInClause() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.LIST);
        conf.setDateList(Arrays.asList("2025-03-04", "2025-03-05"));
        conf.setDateField("workday");
        conf.setPeriod(DatePeriodEnum.DAY);

        String result = dateModeUtils.getDateWhereStr(conf);
        assertEquals("(workday in ('2025-03-04','2025-03-05'))", result);
    }

    @Test
    void getDateWhereStr_listMode_singleDate() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.LIST);
        conf.setDateList(Arrays.asList("2025-06-01"));
        conf.setDateField("dt");
        conf.setPeriod(DatePeriodEnum.DAY);

        String result = dateModeUtils.getDateWhereStr(conf);
        assertEquals("(dt in ('2025-06-01'))", result);
    }

    // ---------------------------------------------------------------
    // getDateWhereStr — RECENT mode with null dateDate (core regression)
    // ---------------------------------------------------------------

    @Test
    void getDateWhereStr_recentMode_nullDateDate_fallsBackToDefaultRecent() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.RECENT);
        conf.setUnit(7);
        conf.setPeriod(DatePeriodEnum.DAY);
        conf.setDateField("workday");

        // dateDate is null → should invoke defaultRecentDateInfo, NOT return ""
        String result = dateModeUtils.getDateWhereStr(conf, null);

        assertNotNull(result);
        assertFalse(result.isEmpty(),
                "RECENT mode with null dateDate must not return empty string");
        assertTrue(result.contains("workday >= '"), "Result should contain date field range");
        assertTrue(result.contains("workday <= '"), "Result should contain date field range");

        // Verify the actual dates: unit=7, DAY → yesterday back 6 more days = 7 days total
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate sevenDaysAgo = yesterday.minusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String expected = String.format("(workday >= '%s' and workday <= '%s')",
                sevenDaysAgo.format(fmt), yesterday.format(fmt));
        assertEquals(expected, result);
    }

    @Test
    void getDateWhereStr_recentMode_noArg_usesDefaultRecent() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.RECENT);
        conf.setUnit(7);
        conf.setPeriod(DatePeriodEnum.DAY);
        conf.setDateField("workday");

        // single-arg overload delegates to two-arg with null dateDate
        String result = dateModeUtils.getDateWhereStr(conf);

        assertFalse(result.isEmpty());
        assertTrue(result.contains("workday"));
    }

    // ---------------------------------------------------------------
    // defaultRecentDateInfo — DAY period
    // ---------------------------------------------------------------

    @Test
    void defaultRecentDateInfo_dayUnit1_producesYesterday() {
        DateConf conf = new DateConf();
        conf.setUnit(1);
        conf.setPeriod(DatePeriodEnum.DAY);
        conf.setDateField("workday");

        String result = dateModeUtils.defaultRecentDateInfo(conf);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String expected = String.format("(workday >= '%s' and workday <= '%s')",
                yesterday.format(fmt), yesterday.format(fmt));
        assertEquals(expected, result);
    }

    @Test
    void defaultRecentDateInfo_dayUnit7_producesLast7Days() {
        DateConf conf = new DateConf();
        conf.setUnit(7);
        conf.setPeriod(DatePeriodEnum.DAY);
        conf.setDateField("workday");

        String result = dateModeUtils.defaultRecentDateInfo(conf);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate start = yesterday.minusDays(6); // 7 days total: start..yesterday
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String expected = String.format("(workday >= '%s' and workday <= '%s')", start.format(fmt),
                yesterday.format(fmt));
        assertEquals(expected, result);
    }

    @Test
    void defaultRecentDateInfo_nullDateInfo_returnsEmpty() {
        String result = dateModeUtils.defaultRecentDateInfo(null);
        assertEquals("", result);
    }

    // ---------------------------------------------------------------
    // defaultRecentDateInfo — WEEK period
    // ---------------------------------------------------------------

    @Test
    void defaultRecentDateInfo_weekUnit1_producesLastWeekRange() {
        DateConf conf = new DateConf();
        conf.setUnit(1);
        conf.setPeriod(DatePeriodEnum.WEEK);
        conf.setDateField("dt");

        String result = dateModeUtils.defaultRecentDateInfo(conf);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate start = yesterday.minusDays(7); // unit=1 week
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String expected = String.format("(dt >= '%s' and dt <= '%s')", start.format(fmt),
                yesterday.format(fmt));
        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------
    // defaultRecentDateInfo — MONTH period
    // ---------------------------------------------------------------

    @Test
    void defaultRecentDateInfo_monthUnit1_producesLastMonthRange() {
        DateConf conf = new DateConf();
        conf.setUnit(1);
        conf.setPeriod(DatePeriodEnum.MONTH);
        conf.setDateField("report_month");

        String result = dateModeUtils.defaultRecentDateInfo(conf);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate start = yesterday.minusMonths(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        String expected = String.format("(report_month >= '%s' and report_month <= '%s')",
                start.format(fmt), yesterday.format(fmt));
        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------
    // betweenDateStr — direct method tests
    // ---------------------------------------------------------------

    @Test
    void betweenDateStr_dayPeriod() {
        DateConf conf = new DateConf();
        conf.setStartDate("2025-01-01");
        conf.setEndDate("2025-01-31");
        conf.setDateField("dt");
        conf.setPeriod(DatePeriodEnum.DAY);

        String result = dateModeUtils.betweenDateStr(conf);
        assertEquals("dt >= '2025-01-01' and dt <= '2025-01-31'", result);
    }

    @Test
    void betweenDateStr_weekPeriod() {
        DateConf conf = new DateConf();
        conf.setStartDate("2025-01-06");
        conf.setEndDate("2025-01-20");
        conf.setDateField("week_start");
        conf.setPeriod(DatePeriodEnum.WEEK);

        String result = dateModeUtils.betweenDateStr(conf);
        assertEquals("week_start >= '2025-01-06' and week_start <= '2025-01-20'", result);
    }

    // ---------------------------------------------------------------
    // listDateStr — direct method tests
    // ---------------------------------------------------------------

    @Test
    void listDateStr_multipleDates() {
        DateConf conf = new DateConf();
        conf.setDateList(Arrays.asList("2025-01-01", "2025-01-15", "2025-02-01"));
        conf.setDateField("dt");
        conf.setPeriod(DatePeriodEnum.DAY);

        String result = dateModeUtils.listDateStr(conf);
        assertEquals("(dt in ('2025-01-01','2025-01-15','2025-02-01'))", result);
    }

    // ---------------------------------------------------------------
    // hasAvailableDataMode
    // ---------------------------------------------------------------

    @Test
    void hasAvailableDataMode_availableMode_returnsTrue() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.AVAILABLE);
        assertTrue(dateModeUtils.hasAvailableDataMode(conf));
    }

    @Test
    void hasAvailableDataMode_recentMode_returnsFalse() {
        DateConf conf = new DateConf();
        conf.setDateMode(DateConf.DateMode.RECENT);
        assertFalse(dateModeUtils.hasAvailableDataMode(conf));
    }

    @Test
    void hasAvailableDataMode_null_returnsFalse() {
        assertFalse(dateModeUtils.hasAvailableDataMode(null));
    }
}
