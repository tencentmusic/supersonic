package com.tencent.supersonic.chat.persistence.dataobject;

public enum CostShowType {
    PARSER(1, "parser"),
    SQL(2, "sql"),
    QUERY(3, "query"),
    RECALL(4, "recall");

    private Integer type;
    private String name;

    CostShowType(Integer type, String name) {
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
