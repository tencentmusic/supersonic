package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.chat.corrector.S2SqlDateHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class S2SqlDateHelperTest {

    @Test
    void testCurrentTimeMode() {
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.CURRENT);
        timeDefaultConfig.setPeriod(DatePeriodEnum.MONTH);

        Pair<String, String> dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2024-09-01");
        assert dateRange.getRight().equals("2024-09-21");

        timeDefaultConfig.setPeriod(DatePeriodEnum.YEAR);
        dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2024-01-01");
        assert dateRange.getRight().equals("2024-09-21");
    }

    @Test
    void testRecentTimeMode() {
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        timeDefaultConfig.setUnit(3);
        timeDefaultConfig.setPeriod(DatePeriodEnum.DAY);

        Pair<String, String> dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2024-09-18");
        assert dateRange.getRight().equals("2024-09-21");

        timeDefaultConfig.setPeriod(DatePeriodEnum.MONTH);
        dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2024-06-21");
        assert dateRange.getRight().equals("2024-09-21");

        timeDefaultConfig.setPeriod(DatePeriodEnum.YEAR);
        dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2021-09-21");
        assert dateRange.getRight().equals("2024-09-21");
    }

    @Test
    void testLastTimeMode() {
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setUnit(3);
        timeDefaultConfig.setPeriod(DatePeriodEnum.DAY);

        Pair<String, String> dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2024-09-18");
        assert dateRange.getRight().equals("2024-09-18");

        timeDefaultConfig.setPeriod(DatePeriodEnum.MONTH);
        dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2024-06-21");
        assert dateRange.getRight().equals("2024-06-21");

        timeDefaultConfig.setPeriod(DatePeriodEnum.YEAR);
        dateRange =
                S2SqlDateHelper.calculateDateRange("2024-09-21", timeDefaultConfig, "yyyy-MM-dd");
        assert dateRange.getLeft().equals("2021-09-21");
        assert dateRange.getRight().equals("2021-09-21");
    }
}
