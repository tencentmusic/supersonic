package com.tencent.supersonic.chat.query.rule.metric;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.AggregateInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;

@Slf4j
@Component
public class MetricFilterQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_FILTER";

    public MetricFilterQuery() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1)
                .addOption(ENTITY, OPTIONAL, AT_MOST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        if (!isMultiStructQuery()) {
            QueryResult queryResult = super.execute(user);
            if (Objects.nonNull(queryResult)) {
                QueryResultWithSchemaResp queryResp = new QueryResultWithSchemaResp();
                queryResp.setColumns(queryResult.getQueryColumns());
                queryResp.setResultList(queryResult.getQueryResults());
                AggregateInfo aggregateInfo = ContextUtils.getBean(SemanticService.class)
                        .getAggregateInfo(user,parseInfo,queryResp);
                queryResult.setAggregateInfo(aggregateInfo);
            }
            return queryResult;
        }
        return super.multiStructExecute(user);
    }

    protected boolean isMultiStructQuery() {
        Set<String> filterBizName = new HashSet<>();
        parseInfo.getDimensionFilters().forEach(filter ->
                filterBizName.add(filter.getBizName()));
        return filterBizName.size() > 1;
    }

    @Override
    protected QueryStructReq convertQueryStruct() {
        QueryStructReq queryStructReq = super.convertQueryStruct();
        addDimension(queryStructReq, true);
        return queryStructReq;
    }

    @Override
    protected QueryMultiStructReq convertQueryMultiStruct() {
        QueryMultiStructReq queryMultiStructReq = super.convertQueryMultiStruct();
        for (QueryStructReq queryStructReq : queryMultiStructReq.getQueryStructReqs()) {
            addDimension(queryStructReq, false);
        }
        return queryMultiStructReq;
    }

    private void addDimension(QueryStructReq queryStructReq, boolean onlyOperateInFilter) {
        if (!queryStructReq.getDimensionFilters().isEmpty()) {
            List<String> dimensions = queryStructReq.getGroups();
            log.info("addDimension before [{}]", queryStructReq.getGroups());
            List<Filter> filters = new ArrayList<>(queryStructReq.getDimensionFilters());
            if (onlyOperateInFilter) {
                filters = filters.stream().filter(filter
                        -> filter.getOperator().equals(FilterOperatorEnum.IN)).collect(Collectors.toList());
            }
            filters.forEach(d -> {
                if (!dimensions.contains(d.getBizName())) {
                    dimensions.add(d.getBizName());
                }});
            queryStructReq.setGroups(dimensions);
            log.info("addDimension after [{}]", queryStructReq.getGroups());
        }
    }

}
