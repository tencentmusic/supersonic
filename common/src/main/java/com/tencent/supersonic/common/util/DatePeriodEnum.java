package com.tencent.supersonic.common.util;

public enum DatePeriodEnum {
    DAY("日"),
    WEEK("周"),
    MONTH("月"),
    QUARTER("季度"),
    YEAR("年");
    private String chName;

    DatePeriodEnum(String chName) {
        this.chName = chName;
    }

    public String getChName() {
        return chName;
    }

    public static DatePeriodEnum get(String period) {
        for (DatePeriodEnum value : values()) {
            if (value.name().equalsIgnoreCase(period)) {
                return value;
            }
        }
        return null;
    }

}
