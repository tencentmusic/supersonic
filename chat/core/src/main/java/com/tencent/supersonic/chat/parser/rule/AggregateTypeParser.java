package com.tencent.supersonic.chat.parser.rule;

import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.AVG;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.COUNT;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.DISTINCT;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.MAX;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.MIN;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.NONE;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.SUM;
import static com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum.TOPN;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregateTypeParser implements SemanticParser {

    private static final Map<AggregateTypeEnum, Pattern> REGX_MAP = Stream.of(
            new AbstractMap.SimpleEntry<>(MAX, Pattern.compile("(?i)(最大值|最大|max|峰值|最高|最多)")),
            new AbstractMap.SimpleEntry<>(MIN, Pattern.compile("(?i)(最小值|最小|min|最低|最少)")),
            new AbstractMap.SimpleEntry<>(SUM, Pattern.compile("(?i)(汇总|总和|sum)")),
            new AbstractMap.SimpleEntry<>(AVG, Pattern.compile("(?i)(平均值|日均|平均|avg)")),
            new AbstractMap.SimpleEntry<>(TOPN, Pattern.compile("(?i)(top)")),
            new AbstractMap.SimpleEntry<>(DISTINCT, Pattern.compile("(?i)(uv)")),
            new AbstractMap.SimpleEntry<>(COUNT, Pattern.compile("(?i)(总数|pv)")),
            new AbstractMap.SimpleEntry<>(NONE, Pattern.compile("(?i)(明细)"))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(k1,k2)->k2));

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        for (SemanticQuery semanticQuery : queryContext.getCandidateQueries()) {
            if (!AggregateTypeEnum.NONE.equals(semanticQuery.getParseInfo().getAggType())) {
                continue;
            }

            String queryText = queryContext.getRequest().getQueryText();
            semanticQuery.getParseInfo().setAggType(resolveAggregateType(queryText));
        }
    }

    public static AggregateTypeEnum resolveAggregateType(String queryText) {
        Map<AggregateTypeEnum, Integer> aggregateCount = new HashMap<>(REGX_MAP.size());

        for (Map.Entry<AggregateTypeEnum, Pattern> entry : REGX_MAP.entrySet()) {
            Matcher matcher = entry.getValue().matcher(queryText);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                aggregateCount.put(entry.getKey(), count);
            }
        }

        return aggregateCount.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey()).orElse(NONE);
    }

}
