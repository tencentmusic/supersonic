package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;

import java.util.HashMap;
import java.util.Map;

public class ClickHouseAdaptor extends BaseDbAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "toYYYYMM(toDate(parseDateTimeBestEffort(toString(%s))))".replace("%s",
                        column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "toMonday(toDate(parseDateTimeBestEffort(toString(%s))))".replace("%s",
                        column);
            } else {
                return "toDate(parseDateTimeBestEffort(toString(%s)))".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "formatDateTime(toDate(%s),'%Y-%m')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "toMonday(toDate(%s))".replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }

    @Override
    public String rewriteSql(String sql) {
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("MONTH".toLowerCase(), "toMonth");
        functionMap.put("DAY".toLowerCase(), "toDayOfMonth");
        functionMap.put("YEAR".toLowerCase(), "toYear");
        return SqlReplaceHelper.replaceFunction(sql, functionMap);
    }
}
