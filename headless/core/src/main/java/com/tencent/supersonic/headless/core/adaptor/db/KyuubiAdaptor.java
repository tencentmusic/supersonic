package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

public class KyuubiAdaptor extends BaseDbAdaptor {

    /** transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY) */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(%s, 'yyyy-MM')", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(date_sub(%s, (dayofweek(%s) - 2)), 'yyyy-MM-dd')",
                        column, column);
            } else {
                return String.format(
                        "date_format(to_date(cast(%s as string), 'yyyyMMdd'), 'yyyy-MM-dd')",
                        column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(%s, 'yyyy-MM')", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return String.format("date_format(date_sub(%s, (dayofweek(%s) - 2)), 'yyyy-MM-dd')",
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
