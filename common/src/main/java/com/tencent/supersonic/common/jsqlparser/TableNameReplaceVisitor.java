package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;

import java.util.Set;

public class TableNameReplaceVisitor extends FromItemVisitorAdapter {

    private Set<String> notReplaceTables;
    private String tableName;

    public TableNameReplaceVisitor(String tableName, Set<String> notReplaceTables) {
        this.tableName = tableName;
        this.notReplaceTables = notReplaceTables;
    }

    @Override
    public void visit(Table table) {
        if (notReplaceTables.contains(table.getName())) {
            return;
        }
        table.setName(tableName);
    }
}
