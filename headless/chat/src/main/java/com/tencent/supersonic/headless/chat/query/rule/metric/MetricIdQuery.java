package com.tencent.supersonic.headless.chat.query.rule.metric;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.FilterType;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricIdQuery extends MetricSemanticQuery {

    public static final String QUERY_MODE = "METRIC_ID";

    public MetricIdQuery() {
        super();
        queryMatcher.addOption(ID, REQUIRED, AT_LEAST, 1)
                .addOption(ENTITY, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public SemanticQueryReq buildSemanticQueryReq() {
        if (!isMultiStructQuery()) {
            return super.buildSemanticQueryReq();
        }
        return super.multiStructExecute();
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
