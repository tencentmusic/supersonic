package com.tencent.supersonic.semantic.api.core.enums;


public enum QueryTypeBackEnum {
    NORMAL("NORMAL", 0),

    PRE_FLUSH("PRE_FLUSH", 1);

    private String value;
    private Integer state;

    QueryTypeBackEnum(String value, Integer state) {
        this.value = value;
        this.state = state;
    }

    public String getValue() {
        return value;
    }

    public Integer getState() {
        return state;
    }

    public static QueryTypeBackEnum of(String src) {
        for (QueryTypeBackEnum operatorEnum : QueryTypeBackEnum.values()) {
            if (src.toUpperCase().contains(operatorEnum.value)) {
                return operatorEnum;
            }
        }
        return null;
    }


}
