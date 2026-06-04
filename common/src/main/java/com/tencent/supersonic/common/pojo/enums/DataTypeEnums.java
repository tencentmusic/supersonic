package com.tencent.supersonic.common.pojo.enums;

public enum DataTypeEnums {
    ARRAY("ARRAY"),

    MAP("MAP"),

    JSON("JSON"),

    VARCHAR("VARCHAR"),

    DATE("DATE"),

    TIMESTAMP("TIMESTAMP"),

    BIGINT("BIGINT"),

    INT("INT"),

    DOUBLE("DOUBLE"),

    FLOAT("FLOAT"),

    DECIMAL("DECIMAL"),

    UNKNOWN("unknown");

    private String type;

    DataTypeEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static DataTypeEnums of(String type) {
        for (DataTypeEnums typeEnum : DataTypeEnums.values()) {
            if (typeEnum.getType().equalsIgnoreCase(type)) {
                return typeEnum;
            }
        }
        if (type != null && type.toUpperCase().contains("INT")) {
            return DataTypeEnums.INT;
        }
        return DataTypeEnums.UNKNOWN;
    }
}
