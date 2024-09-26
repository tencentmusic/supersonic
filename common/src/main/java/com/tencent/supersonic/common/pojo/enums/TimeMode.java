package com.tencent.supersonic.common.pojo.enums;

public enum TimeMode {
    // a single date at N days ago
    LAST,
    // a period of date from N days ago to today
    RECENT,
    // a period of date from the first day of current month/year to today
    CURRENT
}
