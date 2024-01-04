package com.tencent.supersonic.headless.api.enums;


public enum DatasourceQuery {

    SQL_QUERY("sql_query"),
    TABLE_QUERY("table_query");

    private String name;


    DatasourceQuery(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
