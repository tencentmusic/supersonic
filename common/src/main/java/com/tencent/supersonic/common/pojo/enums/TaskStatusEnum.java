package com.tencent.supersonic.common.pojo.enums;

public enum TaskStatusEnum {
    INITIAL("initial", -2),

    ERROR("error", -1),

    PENDING("pending", 0),

    RUNNING("running", 1),

    SUCCESS("success", 2),

    UNKNOWN("unknown", 3);

    private String status;
    private Integer code;

    TaskStatusEnum(String status, Integer code) {
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public static TaskStatusEnum of(String status) {
        for (TaskStatusEnum statusEnum : TaskStatusEnum.values()) {
            if (statusEnum.status.equalsIgnoreCase(status)) {
                return statusEnum;
            }
        }
        return TaskStatusEnum.UNKNOWN;
    }

    public static TaskStatusEnum of(Integer code) {
        for (TaskStatusEnum statusEnum : TaskStatusEnum.values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum;
            }
        }
        return TaskStatusEnum.UNKNOWN;
    }
}
