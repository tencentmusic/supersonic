package com.tencent.supersonic.headless.common.server.enums;


public enum QueryType {
    SQL("SQL"),

    STRUCT("STRUCT");

    private String value;

    QueryType(String value) {
        this.value = value;
    }

    public static QueryType of(String src) {
        for (QueryType operatorEnum : QueryType.values()) {
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
