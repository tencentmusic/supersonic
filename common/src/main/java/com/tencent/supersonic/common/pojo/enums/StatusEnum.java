package com.tencent.supersonic.common.pojo.enums;

public enum StatusEnum {
    INITIALIZED("INITIALIZED", 0),
    ONLINE("ONLINE", 1),
    OFFLINE("OFFLINE", 2),
    DELETED("DELETED", 3),
    UNAVAILABLE("UNAVAILABLE", 4),
    UNKNOWN("UNKNOWN", -1);

    private String status;
    private Integer code;

    StatusEnum(String status, Integer code) {
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public static StatusEnum of(String status) {
        for (StatusEnum statusEnum : StatusEnum.values()) {
            if (statusEnum.status.equalsIgnoreCase(status)) {
                return statusEnum;
            }
        }
        return StatusEnum.UNKNOWN;
    }

    public static StatusEnum of(Integer code) {
        for (StatusEnum statusEnum : StatusEnum.values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum;
            }
        }
        return StatusEnum.UNKNOWN;
    }
}
