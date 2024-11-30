package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

public class MysqlAdaptor extends BaseDbAdaptor {

    /** transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY) */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(DATE_SUB(%s, INTERVAL (DAYOFWEEK(%s) - 2) DAY), '%Y-%m-%d')"
                        .replace("%s", column);
            } else {
                return "date_format(str_to_date(%s, '%Y%m%d'),'%Y-%m-%d')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m') ".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(DATE_SUB(%s, INTERVAL (DAYOFWEEK(%s) - 2) DAY), '%Y-%m-%d')"
                        .replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String rewriteSql(String sql) {
        return sql;
    }
}
