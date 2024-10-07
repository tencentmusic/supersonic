package com.tencent.supersonic.headless.api.pojo.enums;

public enum AppStatus {
    INIT(0), ONLINE(1), OFFLINE(2), DELETED(3), UNKNOWN(4);

    private Integer code;

    AppStatus(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static AppStatus fromCode(Integer code) {
        for (AppStatus appStatusEnum : AppStatus.values()) {
            if (appStatusEnum.getCode().equals(code)) {
                return appStatusEnum;
            }
        }
        return AppStatus.UNKNOWN;
    }
}
