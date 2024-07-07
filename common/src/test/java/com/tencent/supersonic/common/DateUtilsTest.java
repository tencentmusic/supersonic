package com.tencent.supersonic.common;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DateUtilsTest {

    @Test
    void testGetBeforeDate() {

        String dateStr = DateUtils.getBeforeDate("2023-08-10", 1, DatePeriodEnum.DAY);
        Assert.assertEquals(dateStr, "2023-08-09");

        dateStr = DateUtils.getBeforeDate("2023-08-10", 8, DatePeriodEnum.DAY);
        Assert.assertEquals(dateStr, "2023-08-02");

        dateStr = DateUtils.getBeforeDate("2023-08-10", 0, DatePeriodEnum.DAY);
        Assert.assertEquals(dateStr, "2023-08-10");

        dateStr = DateUtils.getBeforeDate("2023-08-10", 1, DatePeriodEnum.WEEK);
        Assert.assertEquals(dateStr, "2023-08-03");

        dateStr = DateUtils.getBeforeDate("2023-08-10", 0, DatePeriodEnum.WEEK);
        Assert.assertEquals(dateStr, "2023-08-07");

        dateStr = DateUtils.getBeforeDate("2023-08-01", 1, DatePeriodEnum.MONTH);
        Assert.assertEquals(dateStr, "2023-07-01");

        dateStr = DateUtils.getBeforeDate("2023-08-10", 0, DatePeriodEnum.MONTH);
        Assert.assertEquals(dateStr, "2023-08-01");

        dateStr = DateUtils.getBeforeDate("2023-08-01", 1, DatePeriodEnum.YEAR);
        Assert.assertEquals(dateStr, "2022-08-01");

        dateStr = DateUtils.getBeforeDate("2023-08-10", 0, DatePeriodEnum.YEAR);
        Assert.assertEquals(dateStr, "2023-01-01");
    }

    @Test
    void testDayDateList() {
        String startDate = "2023-07-29";
        String endDate = "2023-08-03";
        List<String> actualDateList = DateUtils.getDateList(startDate, endDate, Constants.DAY);
        List<String> expectedDateList = Lists.newArrayList("2023-07-29", "2023-07-30",
                "2023-07-31", "2023-08-01", "2023-08-02", "2023-08-03");
        Assertions.assertEquals(actualDateList, expectedDateList);
    }

    @Test
    void testWeekDateList() {
        String startDate = "2023-10-30";
        String endDate = "2023-11-13";
        List<String> actualDateList = DateUtils.getDateList(startDate, endDate, Constants.WEEK);
        List<String> expectedDateList = Lists.newArrayList("2023-10-30", "2023-11-06",
                "2023-11-13");
        Assertions.assertEquals(actualDateList, expectedDateList);
    }

    @Test
    void testMonthDateList() {
        String startDate = "2023-07-01";
        String endDate = "2023-10-01";
        List<String> actualDateList = DateUtils.getDateList(startDate, endDate, Constants.MONTH);
        List<String> expectedDateList = Lists.newArrayList("2023-07", "2023-08", "2023-09", "2023-10");
        Assertions.assertEquals(actualDateList, expectedDateList);
    }
}