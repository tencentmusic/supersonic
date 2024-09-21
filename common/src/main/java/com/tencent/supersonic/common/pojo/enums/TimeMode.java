package com.tencent.supersonic.common.pojo.enums;

public enum TimeMode {

    // a specific date at N days ago
    LAST,
    // a period of time from N days ago to today
    RECENT,
    // a period of time from the first day of current month/year to today
    CURRENT
}
