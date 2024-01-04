package com.tencent.supersonic.headless.core.adaptor.db;


public abstract class DbAdaptor {

    public abstract String getDateFormat(String dateType, String dateFormat, String column);

    public abstract String getColumnMetaQueryTpl();

    public abstract String getDbMetaQueryTpl();

    public abstract String getTableMetaQueryTpl();

    public abstract String functionNameCorrector(String sql);
}
