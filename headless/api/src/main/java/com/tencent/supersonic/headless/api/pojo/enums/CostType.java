package com.tencent.supersonic.headless.api.pojo.enums;

public enum CostType {
    MAPPER(1, "mapper"),
    PARSER(2, "parser"),
    QUERY(3, "query"),
    PROCESSOR(4, "processor");

    private Integer type;
    private String name;

    CostType(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
