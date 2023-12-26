package com.tencent.supersonic.common.pojo.enums;

public enum ReturnCode {
    SUCCESS(200, "success"),
    INVALID_REQUEST(400, "invalid request"),
    INVALID_PERMISSION(401, "invalid permission"),
    ACCESS_ERROR(403, "access denied"),
    SYSTEM_ERROR(500, "system error");

    private final int code;
    private final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
