package com.tencent.supersonic.headless.common.model.enums;

public enum AppStatusEnum {

    INIT(0),
    ONLINE(1),
    OFFLINE(2),
    DELETED(3),
    UNKNOWN(4);

    private Integer code;

    AppStatusEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static AppStatusEnum fromCode(Integer code) {
        for (AppStatusEnum appStatusEnum : AppStatusEnum.values()) {
            if (appStatusEnum.getCode().equals(code)) {
                return appStatusEnum;
            }
        }
        return AppStatusEnum.UNKNOWN;
    }
}
