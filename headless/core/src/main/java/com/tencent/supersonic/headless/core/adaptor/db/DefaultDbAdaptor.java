package com.tencent.supersonic.headless.core.adaptor.db;

public class DefaultDbAdaptor extends BaseDbAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        return column;
    }

    @Override
    public String rewriteSql(String sql) {
        return sql;
    }
}
