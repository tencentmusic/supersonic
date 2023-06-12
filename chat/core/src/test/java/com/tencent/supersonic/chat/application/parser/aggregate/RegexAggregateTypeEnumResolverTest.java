package com.tencent.supersonic.chat.application.parser.aggregate;

import static org.junit.Assert.assertEquals;

import com.tencent.supersonic.chat.application.parser.resolver.RegexAggregateTypeResolver;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import org.junit.jupiter.api.Test;

class RegexAggregateTypeEnumResolverTest {

    @Test
    void getAggregateParser() {
        RegexAggregateTypeResolver regexAggregateParser = new RegexAggregateTypeResolver();
        AggregateTypeEnum aggregateType = regexAggregateParser.resolve("supsersonic产品访问次数最大值");
        assertEquals(aggregateType, AggregateTypeEnum.MAX);

        aggregateType = regexAggregateParser.resolve("supsersonic产品pv");
        assertEquals(aggregateType, AggregateTypeEnum.COUNT);

        aggregateType = regexAggregateParser.resolve("supsersonic产品uv");
        assertEquals(aggregateType, AggregateTypeEnum.DISTINCT);

        aggregateType = regexAggregateParser.resolve("supsersonic产品访问次数最大值");
        assertEquals(aggregateType, AggregateTypeEnum.MAX);

        aggregateType = regexAggregateParser.resolve("supsersonic产品访问次数最小值");
        assertEquals(aggregateType, AggregateTypeEnum.MIN);

        aggregateType = regexAggregateParser.resolve("supsersonic产品访问次数平均值");
        assertEquals(aggregateType, AggregateTypeEnum.AVG);

        aggregateType = regexAggregateParser.resolve("supsersonic产品访问次数topN");
        assertEquals(aggregateType, AggregateTypeEnum.TOPN);

        aggregateType = regexAggregateParser.resolve("supsersonic产品访问次数汇总");
        assertEquals(aggregateType, AggregateTypeEnum.SUM);
    }
}