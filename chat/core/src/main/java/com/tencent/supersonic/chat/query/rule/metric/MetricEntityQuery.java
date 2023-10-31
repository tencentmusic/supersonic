package com.tencent.supersonic.chat.query.rule.metric;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.pojo.enums.FilterType;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Slf4j
@Component
public class MetricEntityQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_ENTITY";

    public MetricEntityQuery() {
        super();
        queryMatcher.addOption(ID, REQUIRED, AT_LEAST, 1)
                .addOption(ENTITY, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        if (!isMultiStructQuery()) {
            QueryResult queryResult = super.execute(user);
            fillAggregateInfo(user, queryResult);
            return queryResult;
        }
        return super.multiStructExecute(user);
    }

    protected boolean isMultiStructQuery() {
        Set<String> filterBizName = new HashSet<>();
        parseInfo.getDimensionFilters().stream()
                .filter(filter -> filter.getElementID() != null)
                .forEach(filter -> filterBizName.add(filter.getBizName()));
        return FilterType.UNION.equals(parseInfo.getFilterType()) && filterBizName.size() > 1;
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
                }
            });
            queryStructReq.setGroups(dimensions);
            log.info("addDimension after [{}]", queryStructReq.getGroups());
        }
    }

}
