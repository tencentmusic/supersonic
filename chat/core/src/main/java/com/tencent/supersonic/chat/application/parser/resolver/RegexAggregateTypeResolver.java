package com.tencent.supersonic.chat.application.parser.resolver;

import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Primary
public class RegexAggregateTypeResolver implements AggregateTypeResolver {

    private static Map<AggregateTypeEnum, Pattern> aggregateRegexMap = new HashMap<>();
    private static Pattern compareIntentionalWord = Pattern.compile("(?i)(比较|对比)");

    static {
        aggregateRegexMap.put(AggregateTypeEnum.MAX, Pattern.compile("(?i)(最大值|最大|max|峰值|最高)"));
        aggregateRegexMap.put(AggregateTypeEnum.MIN, Pattern.compile("(?i)(最小值|最小|min|最低)"));
        aggregateRegexMap.put(AggregateTypeEnum.SUM, Pattern.compile("(?i)(汇总|总和|sum)"));
        aggregateRegexMap.put(AggregateTypeEnum.AVG, Pattern.compile("(?i)(平均值|平均|avg)"));
        aggregateRegexMap.put(AggregateTypeEnum.TOPN, Pattern.compile("(?i)(top)"));
        aggregateRegexMap.put(AggregateTypeEnum.DISTINCT, Pattern.compile("(?i)(uv)"));
        aggregateRegexMap.put(AggregateTypeEnum.COUNT, Pattern.compile("(?i)(总数|pv)"));
        aggregateRegexMap.put(AggregateTypeEnum.NONE, Pattern.compile("(?i)(明细)"));
    }

    @Override
    public AggregateTypeEnum resolve(String text) {

        Map<AggregateTypeEnum, Integer> aggregateCount = new HashMap<>(aggregateRegexMap.size());
        for (Entry<AggregateTypeEnum, Pattern> entry : aggregateRegexMap.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > 0) {
                aggregateCount.put(entry.getKey(), count);
            }
        }
        return aggregateCount.entrySet().stream().max(Map.Entry.comparingByValue()).map(entry -> entry.getKey())
                .orElse(null);
    }

    @Override
    public boolean hasCompareIntentionalWords(String queryText) {
        return compareIntentionalWord.matcher(queryText).find();
    }

}
