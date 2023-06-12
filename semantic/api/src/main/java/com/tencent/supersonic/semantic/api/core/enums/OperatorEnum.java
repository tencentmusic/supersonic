package com.tencent.supersonic.semantic.api.core.enums;

public enum OperatorEnum {

    MAX("MAX"),

    MIN("MIN"),

    AVG("AVG"),

    SUM("SUM"),

    DISTINCT("DISTINCT"),

    TOPN("TOPN"),

    PERCENTILE("PERCENTILE"),

    UNKNOWN("UNKNOWN");


    OperatorEnum(String operator) {
        this.operator = operator;
    }

    private String operator;

    public String getOperator() {
        return operator;
    }


}
