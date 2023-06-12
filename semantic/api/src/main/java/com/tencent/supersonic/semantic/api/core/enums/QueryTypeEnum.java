package com.tencent.supersonic.semantic.api.core.enums;


public enum QueryTypeEnum {
    SQL("SQL"),

    STRUCT("STRUCT");

    private String value;

    QueryTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static QueryTypeEnum of(String src) {
        for (QueryTypeEnum operatorEnum : QueryTypeEnum.values()) {
            if (src.toUpperCase().contains(operatorEnum.value)) {
                return operatorEnum;
            }
        }
        return null;
    }


}
