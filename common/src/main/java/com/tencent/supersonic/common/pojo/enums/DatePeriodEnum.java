package com.tencent.supersonic.common.pojo.enums;

public enum DatePeriodEnum {
    DAY("天"), WEEK("周"), MONTH("月"), QUARTER("季度"), YEAR("年");

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

    public static DatePeriodEnum fromChName(String chName) {
        for (DatePeriodEnum value : values()) {
            if (value.chName.equals(chName)) {
                return value;
            }
        }
        return null;
    }
}
