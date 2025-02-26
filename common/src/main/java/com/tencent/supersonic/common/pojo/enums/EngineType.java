package com.tencent.supersonic.common.pojo.enums;

public enum EngineType {
    TDW(0, "TDW"),
    MYSQL(1, "MYSQL"),
    DORIS(2, "DORIS"),
    CLICKHOUSE(3, "CLICKHOUSE"),
    H2(5, "H2"),
    POSTGRESQL(6, "POSTGRESQL"),
    OTHER(7, "OTHER"),
    DUCKDB(8, "DUCKDB"),
    HANADB(9, "HANADB"),
    STARROCKS(10, "STARROCKS"),
    KYUUBI(11, "KYUUBI"),
    PRESTO(12, "PRESTO"),
    TRINO(13, "TRINO"),;

    private Integer code;

    private String name;

    EngineType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static EngineType fromString(String value) {
        for (EngineType engineType : EngineType.values()) {
            if (engineType.name().equalsIgnoreCase(value)) {
                return engineType;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }
}
