package com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter;

import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.Constants;

public class ClickHouseAdaptor extends EngineAdaptor {

    @Override
    public String getDateFormat(String dateType, String dateFormat, String column) {
        if (dateFormat.equalsIgnoreCase(Constants.DAY_FORMAT_INT)) {
            if (TimeDimensionEnum.MONTH.name().equalsIgnoreCase(dateType)) {
                return "formatDateTime(toDate(parseDateTimeBestEffort(toString(%s))),'%Y-%m')".replace("%s", column);
            } else if (TimeDimensionEnum.WEEK.name().equalsIgnoreCase(dateType)) {
                return "toMonday(toDate(parseDateTimeBestEffort(toString(%s))))".replace("%s", column);
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
    public String getDbMetaQueryTpl() {
        return " "
                + " select "
                + " name from system.databases "
                + " where name not in('_temporary_and_external_tables','benchmark','default','system');";
    }

    @Override
    public String getTableMetaQueryTpl() {
        return "select name from system.tables where database = '%s';";
    }

    @Override
    public String getColumnMetaQueryTpl() {
        return "select name,type as dataType, comment from system.columns where database = '%s' and table='%s'";
    }

}
