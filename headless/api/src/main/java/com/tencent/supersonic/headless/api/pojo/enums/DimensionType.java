package com.tencent.supersonic.headless.api.pojo.enums;

public enum DimensionType {
    categorical, time, partition_time, identify;

    public static boolean isTimeDimension(String type) {
        try {
            return isTimeDimension(DimensionType.valueOf(type.toLowerCase()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isTimeDimension(DimensionType type) {
        return type == time || type == partition_time;
    }

    public static boolean isPartitionTime(DimensionType type) {
        return type == partition_time;
    }

    public static boolean isIdentity(DimensionType type) {
        return type == identify;
    }
}
