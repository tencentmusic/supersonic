package com.tencent.supersonic.semantic.model.domain.pojo;


public enum EngineTypeEnum {

    TDW(0, "tdw"),
    MYSQL(1, "mysql"),
    DORIS(2, "doris"),
    CLICKHOUSE(3, "clickhouse"),
    KAFKA(4, "kafka"),
    H2(5, "h2");


    private Integer code;

    private String name;

    EngineTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
