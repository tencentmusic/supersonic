package com.tencent.supersonic.chat.api.pojo;

/***
 * Query Type
 */
public enum QueryType {
    /**
     * queries with metrics included in the select statement
     */
    METRIC,
    /**
     * queries with entity unique key included in the select statement
     */
    ENTITY,
    /**
     * the other queries
     */
    OTHER
}
