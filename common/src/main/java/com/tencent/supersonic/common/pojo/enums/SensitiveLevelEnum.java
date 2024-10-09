package com.tencent.supersonic.common.pojo.enums;

public enum SensitiveLevelEnum {
    LOW(0), MID(1), HIGH(2);

    private Integer code;

    SensitiveLevelEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
