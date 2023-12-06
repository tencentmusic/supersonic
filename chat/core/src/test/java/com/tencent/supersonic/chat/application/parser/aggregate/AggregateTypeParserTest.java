package com.tencent.supersonic.chat.application.parser.aggregate;

import static org.junit.Assert.assertEquals;

import com.tencent.supersonic.chat.parser.sql.rule.AggregateTypeParser;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import org.junit.jupiter.api.Test;

class AggregateTypeParserTest {

    @Test
    void getAggregateParser() {
        AggregateTypeParser aggregateParser = new AggregateTypeParser();
        AggregateTypeEnum aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数最大值");
        assertEquals(aggregateType, AggregateTypeEnum.MAX);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品pv");
        assertEquals(aggregateType, AggregateTypeEnum.COUNT);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品uv");
        assertEquals(aggregateType, AggregateTypeEnum.DISTINCT);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数最大值");
        assertEquals(aggregateType, AggregateTypeEnum.MAX);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数最小值");
        assertEquals(aggregateType, AggregateTypeEnum.MIN);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数平均值");
        assertEquals(aggregateType, AggregateTypeEnum.AVG);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数topN");
        assertEquals(aggregateType, AggregateTypeEnum.TOPN);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数汇总");
        assertEquals(aggregateType, AggregateTypeEnum.SUM);
    }
}