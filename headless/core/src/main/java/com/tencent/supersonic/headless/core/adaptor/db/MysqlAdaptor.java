package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.Constants;


public class MysqlAdaptor extends DbAdaptor {


    /**
     * transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY)
     */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(DATE_SUB(%s, INTERVAL (DAYOFWEEK(%s) - 2) DAY), '%Y-%m-%d')".replace("%s", column);
            } else {
                return "date_format(str_to_date(%s, '%Y%m%d'),'%Y-%m-%d')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m') ".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(DATE_SUB(%s, INTERVAL (DAYOFWEEK(%s) - 2) DAY), '%Y-%m-%d')".replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String getDbMetaQueryTpl() {
        return "select distinct TABLE_SCHEMA as name from information_schema.tables "
                + "where TABLE_SCHEMA not in ('information_schema','mysql','performance_schema','sys');";
    }

    @Override
    public String getTableMetaQueryTpl() {
        return "select TABLE_NAME as name from information_schema.tables where TABLE_SCHEMA = '%s';";
    }

    @Override
    public String functionNameCorrector(String sql) {
        return sql;
    }

    @Override
    public String getColumnMetaQueryTpl() {
        return "SELECT COLUMN_NAME as name, DATA_TYPE as dataType, COLUMN_COMMENT as comment "
                + "FROM information_schema.columns WHERE table_schema ='%s' AND  table_name = '%s'";
    }

}
