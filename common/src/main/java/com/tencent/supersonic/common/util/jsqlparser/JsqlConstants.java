package com.tencent.supersonic.common.util.jsqlparser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsqlConstants {

    public static final String DATE_FUNCTION = "datediff";
    public static final double HALF_YEAR = 0.5d;
    public static final int SIX_MONTH = 6;
    public static final String EQUAL = "=";
    public static final String MINOR_THAN_CONSTANT = " 1 < 2 ";
    public static final String MINOR_THAN_EQUALS_CONSTANT = " 1 <= 1 ";
    public static final String GREATER_THAN_CONSTANT = " 2 > 1 ";
    public static final String GREATER_THAN_EQUALS_CONSTANT = " 1 >= 1 ";
    public static final String EQUAL_CONSTANT = " 1 = 1 ";

    public static final String IN_CONSTANT = " 1 in (1) ";
    public static final String LIKE_CONSTANT = "'a' like 'a'";
    public static final String IN = "IN";

}
