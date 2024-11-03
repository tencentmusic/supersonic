package com.tencent.supersonic.common.pojo.enums;

/** Enumerate query types supported by SuperSonic. */
public enum QueryType {
    /** queries with aggregation (optionally slice and dice by dimensions) */
    AGGREGATE,
    /** queries with field selection */
    DETAIL;

    public boolean isNativeAggQuery() {
        return DETAIL.equals(this);
    }
}
