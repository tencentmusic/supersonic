package com.tencent.supersonic.common.enums;

public enum TaskStatusEnum {

    RUNNING("running", 0),

    SUCCESS("success", 1),

    ERROR("error", -1),

    UNKNOWN("UNKNOWN", 2);

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