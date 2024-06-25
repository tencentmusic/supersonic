package com.tencent.supersonic.common.jsqlparser;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum AggregateEnum {
    MOST("最多", "max"),
    HIGHEST("最高", "max"),
    MAXIMUN("最大", "max"),
    LEAST("最少", "min"),
    SMALLEST("最小", "min"),
    LOWEST("最低", "min"),
    AVERAGE("平均", "avg");
    private String aggregateCh;
    private String aggregateEN;

    AggregateEnum(String aggregateCh, String aggregateEN) {
        this.aggregateCh = aggregateCh;
        this.aggregateEN = aggregateEN;
    }

    public String getAggregateCh() {
        return aggregateCh;
    }

    public String getAggregateEN() {
        return aggregateEN;
    }

    public static Map<String, String> getAggregateEnum() {
        Map<String, String> aggregateMap = Arrays.stream(AggregateEnum.values())
                .collect(Collectors.toMap(AggregateEnum::getAggregateCh, AggregateEnum::getAggregateEN));
        return aggregateMap;
    }
}
