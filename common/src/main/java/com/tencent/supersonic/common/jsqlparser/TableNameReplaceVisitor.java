package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;

public class TableNameReplaceVisitor extends FromItemVisitorAdapter {

    private String tableName;

    public TableNameReplaceVisitor(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void visit(Table table) {
        table.setName(tableName);
    }
}