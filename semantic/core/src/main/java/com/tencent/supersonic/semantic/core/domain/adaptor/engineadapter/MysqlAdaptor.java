package com.tencent.supersonic.semantic.core.domain.adaptor.engineadapter;

import com.tencent.supersonic.semantic.api.core.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.constant.Constants;


public class MysqlAdaptor extends EngineAdaptor {


    /**
     * transform YYYYMMDD to YYYY-MM-DD YYYY-MM YYYY-MM-DD(MONDAY)
     */
    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "to_monday(from_unixtime(unix_timestamp(%s), 'yyyy-MM-dd'))".replace("%s", column);
            } else {
                return "from_unixtime(unix_timestamp(%s), 'yyyy-MM-dd')".replace("%s", column);
            }
        } else if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "DATE_FORMAT(%s, '%Y-%m') ".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "to_monday(from_unixtime(unix_timestamp(%s), 'yyyy-MM-dd'))".replace("%s", column);
            } else {
                return column;
            }
        }
        return column;
    }


}
