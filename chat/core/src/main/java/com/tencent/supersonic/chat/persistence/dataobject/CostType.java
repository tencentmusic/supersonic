package com.tencent.supersonic.chat.persistence.dataobject;

public enum CostType {
    MAPPER(1, "mapper"),
    PARSER(2, "parser"),
    QUERY(3, "query"),
    PARSERRESPONDER(4, "responder");

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
