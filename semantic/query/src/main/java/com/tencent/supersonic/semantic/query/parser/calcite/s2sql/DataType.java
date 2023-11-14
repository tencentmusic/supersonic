package com.tencent.supersonic.semantic.query.parser.calcite.s2sql;

import java.util.Arrays;

public enum DataType {

    ARRAY("ARRAY"),

    MAP("MAP"),

    JSON("JSON"),

    VARCHAR("VARCHAR"),

    DATE("DATE"),

    BIGINT("BIGINT"),

    INT("INT"),

    DOUBLE("DOUBLE"),

    FLOAT("FLOAT"),

    DECIMAL("DECIMAL"),

    UNKNOWN("unknown");

    private String type;

    DataType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static DataType of(String type) {
        for (DataType typeEnum : DataType.values()) {
            if (typeEnum.getType().equalsIgnoreCase(type)) {
                return typeEnum;
            }
        }
        return DataType.UNKNOWN;
    }

    public boolean isObject() {
        return Arrays.asList(ARRAY, MAP, JSON).contains(this);
    }

    public boolean isArray() {
        return ARRAY.equals(this);
    }
}