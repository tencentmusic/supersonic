package com.tencent.supersonic.common.pojo.enums;


public enum TypeEnums {

    DATASOURCE("datasource"),
    METRIC("metric"),
    DIMENSION("dimension"),
    DOMAIN("domain"),
    ENTITY("entity"),
    UNKNOWN("unknown");


    private String name;

    TypeEnums(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TypeEnums of(String type) {
        for (TypeEnums typeEnum : TypeEnums.values()) {
            if (typeEnum.name.equalsIgnoreCase(type)) {
                return typeEnum;
            }
        }
        return TypeEnums.UNKNOWN;
    }
}
