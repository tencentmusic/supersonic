package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;

public abstract class MetricSemanticQuery extends RuleSemanticQuery {

    public MetricSemanticQuery() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1);
    }
}
