package com.tencent.supersonic.common.pojo;

import java.util.regex.Pattern;


public class Constants {

    public static final String COMMA = ",";
    public static final String DOUBLE_SLASH = "//";
    public static final String EMPTY = "";
    public static final String AT_SYMBOL = "@";
    public static final String DOT = ".";
    public static String SPACE = " ";
    public static final String COLON = ":";
    public static final String MINUS = "-";
    public static final String UNDERLINE = "_";
    public static final String UNDERLINE_DOUBLE = "__";
    public static final String PARENTHESES_START = "(";
    public static final String PARENTHESES_END = ")";
    public static final String APOSTROPHE = "'";
    public static final String PERCENT_SIGN = "%";
    public static String LIMIT_UPPER = "LIMIT";
    public static String ORDER_UPPER = "ORDER";
    public static String DESC_UPPER = "DESC";
    public static String ASC_UPPER = "ASC";
    public static String GROUP_UPPER = "GROUP";
    public static String NULL_UPPER = "NULL";
    public static String AND_UPPER = "AND";
    public static String END_SUBQUERY = ") subq_";
    public static String SYS_VAR = "sys_var";
    public static final String NEW_LINE_CHAR = "\n";
    public static final String UNIONALL = " union all ";
    public static final String YAML_FILES_SUFFIX = ".yaml";
    public static final String JDBC_PREFIX_FORMATTER = "jdbc:%s:";
    public static final Pattern PATTERN_JDBC_TYPE = Pattern.compile("jdbc:\\w+");
    public static final String STATISTIC = "statistic";
    public static final String DORIS_LOWER = "doris";
    public static final String MYSQL_LOWER = "mysql";
    public static final String ADMIN_LOWER = "admin";

    public static final String DAY = "DAY";
    public static final String DAY_FORMAT = "yyyy-MM-dd";
    public static final String MONTH_FORMAT = "yyyy-MM";
    public static final String TIMES_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DAY_FORMAT_INT = "YYYYMMDD";
    public static final String MONTH_FORMAT_INT = "YYYYMM";
    public static final String MONTH = "MONTH";
    public static final String WEEK = "WEEK";
    public static final String YEAR = "YEAR";

    public static final String JOIN_UNDERLINE = "__";

    public static final String LIST_LOWER = "list";
    public static final String TOTAL_LOWER = "total";
    public static final String PAGESIZE_LOWER = "pageSize";
    public static final String TRUE_LOWER = "true";

    public static final String NULL = "null";
    public static final Double MAX_SIMILARITY = 1.0d;
    public static final String CONTEXT = "CONTEXT";
    public static final String BRACKETS_START = "[";
    public static final String BRACKETS_END = "]";

    public static final Long DEFAULT_FREQUENCY = 100000L;

    public static final String TABLE_PREFIX = "t_";

}
