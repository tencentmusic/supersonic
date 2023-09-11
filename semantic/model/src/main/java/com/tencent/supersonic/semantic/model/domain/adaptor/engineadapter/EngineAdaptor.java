package com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter;


public abstract class EngineAdaptor {


    public abstract String getDateFormat(String dateType, String dateFormat, String column);


    public abstract String getColumnMetaQueryTpl();

    public abstract String getDbMetaQueryTpl();

    public abstract String getTableMetaQueryTpl();

    public abstract String functionNameCorrector(String sql);
}
