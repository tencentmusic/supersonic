package com.tencent.supersonic.common.pojo.enums;

import cn.hutool.core.collection.CollectionUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum TimeDimensionEnum {

    DAY("sys_imp_date", "数据日期"),

    WEEK("sys_imp_week", "数据日期_周"),

    MONTH("sys_imp_month", "数据日期_月");

    private String name;

    private String chName;

    TimeDimensionEnum(String name, String chName) {
        this.name = name;
        this.chName = chName;
    }

    public static List<String> getNameList() {
        return Arrays.stream(TimeDimensionEnum.values()).map(TimeDimensionEnum::getName).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public String getChName() {
        return chName;
    }

    /**
     * Determine if a time dimension field is included in a Chinese text field
     *
     * @param fields field
     * @return true/false
     */
    public static boolean containsZhTimeDimension(List<String> fields) {
        if (CollectionUtil.isEmpty(fields)) {
            return false;
        }
        return fields.contains(TimeDimensionEnum.DAY.getChName())
                || fields.contains(TimeDimensionEnum.WEEK.getChName())
                || fields.contains(TimeDimensionEnum.MONTH.getChName());
    }
}
