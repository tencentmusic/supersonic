package com.tencent.supersonic.chat.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.DOMAIN;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_MOST;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import org.springframework.stereotype.Component;

@Component
public class MetricDomainQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_DOMAIN";

    public MetricDomainQuery() {
        super();
        queryMatcher.addOption(DOMAIN, OPTIONAL, AT_MOST, 1);
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
