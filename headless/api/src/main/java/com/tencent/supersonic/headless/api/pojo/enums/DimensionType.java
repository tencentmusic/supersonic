package com.tencent.supersonic.headless.api.pojo.enums;


public enum DimensionType {

    categorical,
    time,
    partition_time,
    identify;

    public static Boolean isTimeDimension(String type) {
        return time.name().equals(type) || partition_time.name().equals(type);
    }

    public static Boolean isTimeDimension(DimensionType type) {
        return time.equals(type) || partition_time.equals(type);
    }

}
