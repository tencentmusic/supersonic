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

        dateStr = DateUtils.getBeforeDate("2023-08-10", 1, DatePeriodEnum.WEEK);
        Assert.assertEquals(dateStr, "2023-08-03");

        dateStr = DateUtils.getBeforeDate("2023-08-01", 1, DatePeriodEnum.MONTH);
        Assert.assertEquals(dateStr, "2023-07-01");

        dateStr = DateUtils.getBeforeDate("2023-08-01", 1, DatePeriodEnum.YEAR);
        Assert.assertEquals(dateStr, "2022-08-01");
    }
}