package com.tencent.supersonic.headless.api.pojo.enums;

public enum DimensionType {
    categorical, time, partition_time, primary_key, foreign_key;

    public static DimensionType fromIdentify(String identify) {
        if (IdentifyType.foreign.name().equalsIgnoreCase(identify)) {
            return DimensionType.foreign_key;
        } else if (IdentifyType.primary.name().equalsIgnoreCase(identify)) {
            return DimensionType.primary_key;
        }
        return DimensionType.categorical;
    }

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

    public static boolean isPrimaryKey(DimensionType type) {
        return type == primary_key;
    }
}
