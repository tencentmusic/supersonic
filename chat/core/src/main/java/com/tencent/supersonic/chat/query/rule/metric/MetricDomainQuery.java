package com.tencent.supersonic.chat.query.rule.metric;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.AggregateInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import java.util.Objects;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.*;

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
        if (!Objects.isNull(queryResult)) {
            QueryResultWithSchemaResp queryResp = new QueryResultWithSchemaResp();
            queryResp.setColumns(queryResult.getQueryColumns());
            queryResp.setResultList(queryResult.getQueryResults());
            AggregateInfo aggregateInfo = ContextUtils.getBean(SemanticService.class)
                    .getAggregateInfo(user, parseInfo, queryResp);
            queryResult.setAggregateInfo(aggregateInfo);
        }
        return queryResult;
    }

}
