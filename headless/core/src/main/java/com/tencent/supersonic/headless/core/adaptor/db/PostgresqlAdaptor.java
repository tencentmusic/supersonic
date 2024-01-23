package com.tencent.supersonic.headless.core.adaptor.db;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import java.util.HashMap;
import java.util.Map;

public class PostgresqlAdaptor extends DbAdaptor {

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
        return " SELECT datname as name FROM pg_database WHERE datistemplate = false ;";
    }

    @Override
    public String getTableMetaQueryTpl() {
        return " SELECT table_name as name FROM information_schema.tables WHERE "
                + "table_schema = 'public'  AND table_catalog = '%s' ; ";
    }

    @Override
    public String functionNameCorrector(String sql) {
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("MONTH".toLowerCase(), "toMonth");
        functionMap.put("DAY".toLowerCase(), "toDayOfMonth");
        functionMap.put("YEAR".toLowerCase(), "toYear");
        return SqlParserReplaceHelper.replaceFunction(sql, functionMap);
    }

    @Override
    public String getColumnMetaQueryTpl() {
        return " SELECT column_name as name, data_type as dataType FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_catalog = '%s' AND table_name = '%s' ; ";
    }

}
