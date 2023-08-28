package com.tencent.supersonic.common.util;

public enum DatePeriodEnum {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    public static DatePeriodEnum get(String period) {
        for (DatePeriodEnum value : values()) {
            if (value.name().equalsIgnoreCase(period)) {
                return value;
            }
        }
        return null;
    }
}
