package com.tencent.supersonic.common.pojo.enums;

public enum AggOperatorEnum {

    MAX("MAX"),

    MIN("MIN"),

    AVG("AVG"),

    SUM("SUM"),

    COUNT_DISTINCT("COUNT_DISTINCT"),
    DISTINCT("DISTINCT"),

    TOPN("TOPN"),

    PERCENTILE("PERCENTILE"),

    RATIO_ROLL("RATIO_ROLL"),
    RATIO_OVER("RATIO_OVER"),

    UNKNOWN("UNKNOWN");

    private String operator;

    AggOperatorEnum(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public static AggOperatorEnum of(String agg) {
        for (AggOperatorEnum aggOperatorEnum : AggOperatorEnum.values()) {
            if (aggOperatorEnum.getOperator().equalsIgnoreCase(agg)) {
                return aggOperatorEnum;
            }
        }
        return AggOperatorEnum.UNKNOWN;
    }


}