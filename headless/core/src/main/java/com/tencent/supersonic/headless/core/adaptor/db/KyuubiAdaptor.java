package com.tencent.supersonic.headless.core.adaptor.db;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.DBColumn;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import com.tencent.supersonic.headless.core.pojo.ConnectInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
    public List<String> getTables(ConnectInfo connectionInfo, String schemaName)
            throws SQLException {
        List<String> tablesAndViews = new ArrayList<>();
        DatabaseMetaData metaData = getDatabaseMetaData(connectionInfo);

        try {
            ResultSet resultSet = metaData.getTables(null, schemaName, null, new String[] {"TABLE", "VIEW"});;
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                tablesAndViews.add(name);
            }
        } catch (SQLException e) {
            log.error("Failed to get tables and views", e);
        }
        return tablesAndViews;
    }

    @Override
    public String rewriteSql(String sql) {
        return sql;
    }
}
