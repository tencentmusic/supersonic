package com.tencent.supersonic.headless.api.pojo.enums;

/**
 * Aggregation type of metric when query metric without aggregation method
 * NATIVE: will not use Aggregation
 * DEFAULT: will use the aggregation method define in the model
 */
public enum AggOption {
    NATIVE,
    AGGREGATION,
    DEFAULT;

    public static AggOption getAggregation(boolean isNativeQuery) {
        return isNativeQuery ? NATIVE : AGGREGATION;
    }

    public static boolean isAgg(AggOption aggOption) {
        return NATIVE.equals(aggOption) ? false : true;
    }
}
