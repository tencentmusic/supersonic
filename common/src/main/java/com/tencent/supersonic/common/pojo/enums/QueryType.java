package com.tencent.supersonic.common.pojo.enums;

/**
 * Enumerate query types supported by SuperSonic.
 */
public enum QueryType {
    /**
     * queries with metric calculation (optionally slice and dice by dimensions)
     */
    METRIC,
    /**
     * queries with tag-based entity targeting
     */
    TAG,
    /**
     * queries with ID-based entity selection
     */
    ID;

    public boolean isNativeAggQuery() {
        return TAG.equals(this);
    }
}
