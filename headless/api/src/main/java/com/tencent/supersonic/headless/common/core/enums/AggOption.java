package com.tencent.supersonic.headless.common.core.enums;

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
