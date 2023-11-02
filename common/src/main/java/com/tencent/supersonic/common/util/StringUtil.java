package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


@Slf4j
public class StringUtil {

    public static final String COMMA_WRAPPER = "'%s'";
    public static final String SPACE_WRAPPER = " %s ";

    public static String getCommaWrap(String value) {
        return String.format(COMMA_WRAPPER, value);
    }

    public static String getSpaceWrap(String value) {
        return String.format(SPACE_WRAPPER, value);
    }

    public static String formatSqlQuota(String where) {
        if (StringUtils.isEmpty(where)) {
            return where;
        }
        return where.replace("\"", "\\\\\"");
    }

    /**
     *
     * @param v1
     * @param v2
     * @return  value 0 if v1 equal to v2; less than 0 if v1 is less than v2; greater than 0 if v1 is greater than v2
     */
    public static int compareVersion(String v1, String v2) {
        String[] v1s = v1.split("\\.");
        String[] v2s = v2.split("\\.");
        int length = Math.min(v1s.length, v2s.length);
        for (int i = 0; i < length; i++) {
            Integer vv1 = Integer.parseInt(v1s[i]);
            Integer vv2 = Integer.parseInt(v2s[i]);
            int compare = vv1.compareTo(vv2);
            if (compare != 0) {
                return compare;
            }
        }
        return v1s.length - v2s.length;
    }

}