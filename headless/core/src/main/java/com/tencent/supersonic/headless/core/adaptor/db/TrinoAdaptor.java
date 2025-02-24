package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

public class TrinoAdaptor extends BaseDbAdaptor {

    /** transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY) */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(%s, '%%Y-%%m')", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return String.format(
                        "date_format(date_add('day', - (day_of_week(%s) - 2), %s), '%%Y-%%m-%%d')",
                        column, column);
            } else {
                return String.format("date_format(date_parse(%s, '%%Y%%m%%d'), '%%Y-%%m-%%d')",
                        column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(%s, '%%Y-%%m')", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return String.format(
                        "date_format(date_add('day', - (day_of_week(%s) - 2), %s), '%%Y-%%m-%%d')",
                        column, column);
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
