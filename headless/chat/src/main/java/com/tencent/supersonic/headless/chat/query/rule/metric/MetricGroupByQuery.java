package com.tencent.supersonic.headless.chat.query.rule.metric;

import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.DIMENSION;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Component
public class MetricGroupByQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_GROUPBY";

    public MetricGroupByQuery() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1);
        queryMatcher.addOption(VALUE, OPTIONAL, AT_LEAST, 0);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }
}
