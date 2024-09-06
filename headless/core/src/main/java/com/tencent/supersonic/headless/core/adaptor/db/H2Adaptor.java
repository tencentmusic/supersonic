package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

public class H2Adaptor extends BaseDbAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyyMMdd'),'yyyy-MM')"
                        .replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_TRUNC('week',%s)".replace("%s", column);
            } else {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyyMMdd'),'yyyy-MM-dd')"
                        .replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "FORMATDATETIME(PARSEDATETIME(%s, 'yyyy-MM-dd'),'yyyy-MM') "
                        .replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "DATE_TRUNC('week',%s)".replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String functionNameCorrector(String sql) {
        return sql;
    }
}
