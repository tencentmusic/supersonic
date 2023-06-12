package com.tencent.supersonic.common.enums;

public enum AggOperatorEnum {

    MAX("MAX"),

    MIN("MIN"),

    AVG("AVG"),

    SUM("SUM"),

    DISTINCT("DISTINCT"),

    TOPN("TOPN"),

    PERCENTILE("PERCENTILE"),

    UNKNOWN("UNKNOWN");


    AggOperatorEnum(String operator) {
        this.operator = operator;
    }

    private String operator;

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