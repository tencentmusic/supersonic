package com.tencent.supersonic.common.pojo.enums;

public enum DataFormatTypeEnum {

    PERCENT("percent"),

    DECIMAL("decimal");

    private String name;

    DataFormatTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}