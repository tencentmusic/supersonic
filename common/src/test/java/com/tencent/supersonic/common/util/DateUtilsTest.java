package com.tencent.supersonic.common.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void getBeforeDate() {

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

        dateStr = DateUtils.getBeforeDate(0, DatePeriodEnum.DAY);
        //Assert.assertEquals(dateStr, "2023-09-08");

        dateStr = DateUtils.getBeforeDate(1, DatePeriodEnum.DAY);
        //Assert.assertEquals(dateStr, "2023-09-07");

        dateStr = DateUtils.getBeforeDate(1, DatePeriodEnum.WEEK);
        //Assert.assertEquals(dateStr, "2023-09-01");

        dateStr = DateUtils.getBeforeDate(1, DatePeriodEnum.MONTH);
        //Assert.assertEquals(dateStr, "2023-08-08");
    }
}