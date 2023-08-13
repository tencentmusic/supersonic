package com.tencent.supersonic.chat.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.MODEL;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_MOST;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import org.springframework.stereotype.Component;

@Component
public class MetricModelQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_MODEL";

    public MetricModelQuery() {
        super();
        queryMatcher.addOption(MODEL, OPTIONAL, AT_MOST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        QueryResult queryResult = super.execute(user);
        fillAggregateInfo(user, queryResult);
        return queryResult;
    }

}
