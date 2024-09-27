package com.tencent.supersonic.common.pojo;

import java.util.regex.Pattern;

public class Constants {
    public static final String COMMA = ",";
    public static final String DOUBLE_SLASH = "//";
    public static final String EMPTY = "";
    public static final String AT_SYMBOL = "@";
    public static final String DOT = ".";
    public static final String SPACE = " ";
    public static final String POUND = "#";
    public static final String COLON = ":";
    public static final String MINUS = "-";
    public static final String UNDERLINE = "_";
    public static final String PARENTHESES_START = "(";
    public static final String PARENTHESES_END = ")";
    public static final String APOSTROPHE = "'";
    public static final String PERCENT_SIGN = "%";
    public static final String DESC_UPPER = "DESC";
    public static final String ASC_UPPER = "ASC";
    public static final String AND_UPPER = "AND";
    public static final String SYS_VAR = "sys_var";
    public static final String NEW_LINE_CHAR = "\n";
    public static final String UNIONALL = " union all ";
    public static final String JDBC_PREFIX_FORMATTER = "jdbc:%s:";
    public static final Pattern PATTERN_JDBC_TYPE = Pattern.compile("jdbc:\\w+");
    public static final String ADMIN_LOWER = "admin";
    public static final String DAY_FORMAT = "yyyy-MM-dd";
    public static final String MONTH_FORMAT = "yyyy-MM";
    public static final String TIMES_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DAY_FORMAT_INT = "YYYYMMDD";
    public static final String MONTH_FORMAT_INT = "YYYYMM";
    public static final String JOIN_UNDERLINE = "__";
    public static final String NULL = "null";
    public static final String CONTEXT = "CONTEXT";
    public static final Long DEFAULT_FREQUENCY = 100000L;
    public static final String TABLE_PREFIX = "t_";
    public static final long DEFAULT_DETAIL_LIMIT = 500;
    public static final long DEFAULT_METRIC_LIMIT = 200;
    public static final long DEFAULT_DOWNLOAD_LIMIT = 10000;
}
