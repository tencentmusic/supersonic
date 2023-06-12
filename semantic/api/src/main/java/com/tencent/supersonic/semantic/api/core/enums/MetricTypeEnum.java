package com.tencent.supersonic.semantic.api.core.enums;


public enum MetricTypeEnum {

    EXPR("expr");

    private String name;

    MetricTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
