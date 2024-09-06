package com.tencent.supersonic.headless.api.pojo.enums;

public enum QueryTypeBack {
    NORMAL("NORMAL", 0),

    PRE_FLUSH("PRE_FLUSH", 1);

    private String value;
    private Integer state;

    QueryTypeBack(String value, Integer state) {
        this.value = value;
        this.state = state;
    }

    public static QueryTypeBack of(String src) {
        for (QueryTypeBack operatorEnum : QueryTypeBack.values()) {
            if (src.toUpperCase().contains(operatorEnum.value)) {
                return operatorEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public Integer getState() {
        return state;
    }
}
