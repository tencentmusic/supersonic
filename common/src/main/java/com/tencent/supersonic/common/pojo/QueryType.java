package com.tencent.supersonic.common.pojo;

/***
 * Query Type
 */
public enum QueryType {
    /**
     * queries with metrics included in the select statement
     */
    METRIC,
    /**
     * queries with only tag included in the select statement
     */
    TAG,
    /**
     * the other queries
     */
    OTHER;

    public boolean isNativeAggQuery() {
        return TAG.equals(this);
    }
}
