package com.tencent.supersonic.common.pojo.enums;

import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    public static boolean containsTimeDimension(String fieldName) {
        if (getNameList().contains(fieldName) || getChNameList().contains(fieldName)) {
            return true;
        }
        return false;
    }

    public static List<String> getNameList() {
        return Arrays.stream(TimeDimensionEnum.values()).map(TimeDimensionEnum::getName).collect(Collectors.toList());
    }

    public static List<String> getChNameList() {
        return Arrays.stream(TimeDimensionEnum.values()).map(TimeDimensionEnum::getChName).collect(Collectors.toList());
    }

    public static Map<String, String> getChNameToNameMap() {
        return Arrays.stream(TimeDimensionEnum.values())
                .collect(Collectors.toMap(TimeDimensionEnum::getChName, TimeDimensionEnum::getName, (k1, k2) -> k1));
    }

    public static Map<String, String> getNameToNameMap() {
        return Arrays.stream(TimeDimensionEnum.values())
                .collect(Collectors.toMap(TimeDimensionEnum::getName, TimeDimensionEnum::getName, (k1, k2) -> k1));
    }

    public String getName() {
        return name;
    }

    public String getChName() {
        return chName;
    }

    /**
     * Determine if a time dimension field is included in a Chinese/English text field
     *
     * @param fields field
     * @return true/false
     */
    public static boolean containsZhTimeDimension(List<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return false;
        }
        return fields.stream().anyMatch(field -> containsTimeDimension(field));
    }
}
