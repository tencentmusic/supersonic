package com.tencent.supersonic.semantic.api.query.enums;

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
