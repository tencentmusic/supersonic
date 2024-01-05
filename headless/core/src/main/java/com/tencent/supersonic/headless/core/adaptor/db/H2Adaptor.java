package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

public class H2Adaptor extends DbAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyyMMdd'),'yyyy-MM')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_TRUNC('week',%s)".replace("%s", column);
            } else {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyyMMdd'),'yyyy-MM-dd')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyy-MM-dd'),'yyyy-MM') ".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_TRUNC('week',%s)".replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String getColumnMetaQueryTpl() {
        return "SELECT COLUMN_NAME AS name, "
                + " case DATA_TYPE"
                + " when '12' then 'varchar'"
                + " when '-5' then 'integer'"
                + " when '8' then 'double'"
                + " end  AS dataType"
                + " FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA ='%s' AND  TABLE_NAME = '%s'";
    }

    @Override
    public String getDbMetaQueryTpl() {
        return "SELECT DISTINCT TABLE_SCHEMA as name  FROM INFORMATION_SCHEMA.TABLES WHERE STORAGE_TYPE = 'MEMORY'";
    }

    @Override
    public String getTableMetaQueryTpl() {
        return "SELECT TABLE_NAME as name FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE STORAGE_TYPE = 'MEMORY' AND TABLE_SCHEMA = '%s'";
    }

    @Override
    public String functionNameCorrector(String sql) {
        return sql;
    }
}
