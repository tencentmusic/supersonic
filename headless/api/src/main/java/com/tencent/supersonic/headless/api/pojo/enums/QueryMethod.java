package com.tencent.supersonic.headless.api.pojo.enums;

public enum QueryMethod {
    SQL("SQL"),

    STRUCT("STRUCT");

    private String value;

    QueryMethod(String value) {
        this.value = value;
    }

    public static QueryMethod of(String src) {
        for (QueryMethod operatorEnum : QueryMethod.values()) {
            if (src.toUpperCase().contains(operatorEnum.value)) {
                return operatorEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }
}
