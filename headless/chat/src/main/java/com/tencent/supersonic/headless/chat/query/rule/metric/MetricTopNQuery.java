package com.tencent.supersonic.headless.chat.query.rule.metric;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.DIMENSION;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Component
public class MetricTopNQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_ORDERBY";
    private static final Pattern INTENT_PATTERN = Pattern.compile("(.*)(最大|最高|最多)(.*)");

    public MetricTopNQuery() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1);
        queryMatcher.addOption(VALUE, OPTIONAL, AT_LEAST, 0);
        queryMatcher.setSupportOrderBy(true);
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
            ChatQueryContext queryCtx) {
        Matcher matcher = INTENT_PATTERN.matcher(queryCtx.getRequest().getQueryText());
        if (matcher.matches()) {
            return super.match(candidateElementMatches, queryCtx);
        }
        return new ArrayList<>();
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public void fillParseInfo(ChatQueryContext chatQueryContext, Long dataSetId) {
        super.fillParseInfo(chatQueryContext, dataSetId);

        parseInfo.setScore(parseInfo.getScore() + 2.0);
        SchemaElement metric = parseInfo.getMetrics().iterator().next();
        parseInfo.getOrders().add(new Order(metric.getBizName(), Constants.DESC_UPPER));
    }
}
