package com.tencent.supersonic.common.pojo.enums;

public enum FilterType {
    // filtering between different dimensions will directly splice the AND clause
    AND,
    // filtering between different dimensions will generate multiple sql clauses and splice them
    // together using union
    UNION
}
