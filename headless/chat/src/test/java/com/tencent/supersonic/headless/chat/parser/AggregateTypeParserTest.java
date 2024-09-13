package com.tencent.supersonic.headless.chat.parser;

import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.headless.chat.parser.rule.AggregateTypeParser;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class AggregateTypeParserTest {

    @Test
    void getAggregateParser() {
        AggregateTypeParser aggregateParser = new AggregateTypeParser();
        AggregateTypeEnum aggregateType =
                aggregateParser.resolveAggregateType("supsersonic产品访问次数最大值");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.MAX);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品pv");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.COUNT);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品uv");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.DISTINCT);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数最大值");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.MAX);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数最小值");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.MIN);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数平均值");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.AVG);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数topN");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.TOPN);

        aggregateType = aggregateParser.resolveAggregateType("supsersonic产品访问次数汇总");
        Assert.assertEquals(aggregateType, AggregateTypeEnum.SUM);
    }
}
