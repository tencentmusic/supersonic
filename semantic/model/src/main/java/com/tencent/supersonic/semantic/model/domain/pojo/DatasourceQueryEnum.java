package com.tencent.supersonic.semantic.model.domain.pojo;


public enum DatasourceQueryEnum {

    SQL_QUERY("sql_query"),
    TABLE_QUERY("table_query");

    private String name;


    DatasourceQueryEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
