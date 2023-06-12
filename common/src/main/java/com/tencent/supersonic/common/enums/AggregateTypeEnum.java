package com.tencent.supersonic.common.enums;

public enum AggregateTypeEnum {
    SUM,
    AVG,
    MAX,
    MIN,
    TOPN,
    DISTINCT,
    COUNT,
    NONE;


    public static AggregateTypeEnum of(String agg) {
        for (AggregateTypeEnum aggEnum : AggregateTypeEnum.values()) {
            if (aggEnum.name().equalsIgnoreCase(agg)) {
                return aggEnum;
            }
        }
        return AggregateTypeEnum.NONE;
    }
}
