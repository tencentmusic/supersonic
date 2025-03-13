package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DbSchema {

    private String catalog;

    private String db;

    private String table;

    private String sql;

    private String ddl;

    private List<DBColumn> dbColumns;
}
