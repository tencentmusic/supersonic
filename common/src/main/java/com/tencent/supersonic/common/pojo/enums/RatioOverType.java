package com.tencent.supersonic.common.pojo.enums;

public enum RatioOverType {
    DAY_ON_DAY("日环比"),
    WEEK_ON_DAY("周环比"),
    WEEK_ON_WEEK("周环比"),
    MONTH_ON_WEEK("月环比"),
    MONTH_ON_MONTH("月环比"),
    YEAR_ON_MONTH("年同比"),
    YEAR_ON_YEAR("年环比");

    private String showName;

    RatioOverType(String showName) {
        this.showName = showName;
    }

    public String getShowName() {
        return showName;
    }
}
