package com.tencent.supersonic.semantic.api.model.enums;


public enum QueryTypeEnum {
    SQL("SQL"),

    STRUCT("STRUCT");

    private String value;

    QueryTypeEnum(String value) {
        this.value = value;
    }

    public static QueryTypeEnum of(String src) {
        for (QueryTypeEnum operatorEnum : QueryTypeEnum.values()) {
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
