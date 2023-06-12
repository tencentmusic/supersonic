package com.tencent.supersonic.semantic.api.core.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum TimeDimensionEnum {

    DAY("sys_imp_date"),
    WEEK("sys_imp_week"),

    MONTH("sys_imp_month");

    private String name;

    TimeDimensionEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static List<String> getNameList() {
        return Arrays.stream(TimeDimensionEnum.values()).map(TimeDimensionEnum::getName).collect(Collectors.toList());
    }
}
