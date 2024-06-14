package com.tencent.supersonic.headless.server.utils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Pattern;

/**
 * tools for arrow flight sql
 */
public class FlightUtils {

    public static int resolveType(Object value) {
        if (value instanceof Long) {
            return Types.BIGINT;
        }
        if (value instanceof Integer) {
            return Types.INTEGER;
        }
        if (value instanceof Double) {
            return Types.DOUBLE;
        }
        if (value instanceof String) {
            String val = String.valueOf(value);
            if (Pattern.matches("^\\d+$", val)) {
                return Types.BIGINT;
            } else if (Pattern.matches("^\\d+\\.\\d+$", val)) {
                return Types.DECIMAL;
            } else if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2}$", val)) {
                return Types.DATE;
            } else if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", val)) {
                return Types.TIME;
            }
        }
        return Types.VARCHAR;
    }

    public static int isNullable(int sqlType) throws SQLException {
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.DECIMAL:
                return ResultSetMetaData.columnNullable;
            default:
                return ResultSetMetaData.columnNullableUnknown;
        }
    }
}
