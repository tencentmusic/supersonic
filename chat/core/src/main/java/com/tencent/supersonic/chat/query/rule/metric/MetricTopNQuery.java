package com.tencent.supersonic.chat.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.DIMENSION;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.common.pojo.Constants.DESC_UPPER;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MetricTopNQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_ORDERBY";
    private static final Long ORDERBY_MAX_RESULTS = 3L;
    private static final Pattern INTENT_PATTERN = Pattern.compile("(.*)(最大|最高|最多)(.*)");

    public MetricTopNQuery() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1);
        queryMatcher.addOption(VALUE, OPTIONAL, AT_LEAST, 0);
        queryMatcher.setSupportOrderBy(true);
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
                                          QueryContext queryCtx) {
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
    public void fillParseInfo(Long modelId, QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(modelId, queryContext, chatContext);

        parseInfo.setLimit(ORDERBY_MAX_RESULTS);
        parseInfo.setScore(parseInfo.getScore() + 2.0);
        parseInfo.setAggType(AggregateTypeEnum.SUM);

        SchemaElement metric = parseInfo.getMetrics().iterator().next();
        parseInfo.getOrders().add(new Order(metric.getBizName(), DESC_UPPER));
    }

}
