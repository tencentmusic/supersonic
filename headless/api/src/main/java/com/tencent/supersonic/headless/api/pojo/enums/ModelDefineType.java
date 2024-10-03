package com.tencent.supersonic.headless.api.pojo.enums;

/**
 * model datasource define type: sql_query : dataSet sql begin as select table_query:
 * dbName.tableName
 */
public enum ModelDefineType {
    SQL_QUERY("sql_query"), TABLE_QUERY("table_query");

    private String name;

    ModelDefineType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
