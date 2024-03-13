package com.tencent.supersonic.headless.core.chat.query.rule.metric;

import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.RequireNumberType.AT_MOST;
@Component
public class MetricModelQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_MODEL";

    public MetricModelQuery() {
        super();
        queryMatcher.addOption(SchemaElementType.DATASET, OPTIONAL, AT_MOST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
