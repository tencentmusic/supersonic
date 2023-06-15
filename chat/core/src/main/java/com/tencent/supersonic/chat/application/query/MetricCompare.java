package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MetricCompare extends BaseSemanticQuery {

    public static String QUERY_MODE = "METRIC_COMPARE";


    public MetricCompare() {
        queryModeOption.setAggregation(QueryModeElementOption.optional());
        queryModeOption.setDate(QueryModeElementOption.optional());
        queryModeOption.setDimension(QueryModeElementOption.unused());
        queryModeOption.setFilter(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setMetric(SchemaElementOption.REQUIRED, QueryModeElementOption.RequireNumberType.AT_LEAST, 1);
        queryModeOption.setEntity(QueryModeElementOption.unused());
        queryModeOption.setDomain(QueryModeElementOption.optional());
        queryModeOption.setSupportCompare(true);
        queryModeOption.setSupportOrderBy(true);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public SemanticParseInfo getParseInfo(QueryContextReq queryCtx, ChatContext chatCt) {
        SemanticParseInfo semanticParseInfo = chatCt.getParseInfo();
        ContextHelper.updateTime(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateDomain(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.updateSemanticQuery(queryCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.addIfEmpty(queryCtx.getParseInfo().getDimensionFilters(),
                semanticParseInfo.getDimensionFilters());
        ContextHelper.updateList(queryCtx.getParseInfo().getMetrics(), semanticParseInfo.getMetrics());
        ContextHelper.updateEntity(queryCtx.getParseInfo(), semanticParseInfo);
        return semanticParseInfo;
    }

    @Override
    public SemanticParseInfo getContext(ChatContext chatCtx, QueryContextReq queryCtx) {
        SemanticParseInfo semanticParseInfo = queryCtx.getParseInfo();
        ContextHelper.updateTimeIfEmpty(chatCtx.getParseInfo(), semanticParseInfo);
        ContextHelper.addIfEmpty(chatCtx.getParseInfo().getMetrics(), semanticParseInfo.getMetrics());
        mergeAppend(chatCtx.getParseInfo().getDimensionFilters(), semanticParseInfo.getDimensionFilters());
        addCompareDimension(semanticParseInfo);
        return semanticParseInfo;
    }

    private void addCompareDimension(SemanticParseInfo semanticParseInfo) {
        if (!semanticParseInfo.getDimensionFilters().isEmpty()) {
            Set<String> dimensions = semanticParseInfo.getDimensions().stream().map(d -> d.getBizName()).collect(
                    Collectors.toSet());
            log.info("addCompareDimension before [{}]", dimensions);
            semanticParseInfo.getDimensionFilters().stream().filter(d -> d.getOperator().equals(FilterOperatorEnum.IN))
                    .forEach(
                            d -> {
                                if (!dimensions.contains(d.getBizName())) {
                                    SchemaItem schemaItem = new SchemaItem();
                                    schemaItem.setBizName(d.getBizName());
                                    schemaItem.setId(d.getElementID());
                                    semanticParseInfo.getDimensions().add(schemaItem);
                                    dimensions.add(d.getBizName());
                                }
                            }
                    );
            log.info("addCompareDimension after [{}]", dimensions);
        }
    }

    private void mergeAppend(Set<Filter> from, Set<Filter> to) {
        if (!from.isEmpty()) {
            for (Filter filter : from) {
                if (FilterOperatorEnum.EQUALS.equals(filter.getOperator()) || FilterOperatorEnum.IN.equals(
                        filter.getOperator())) {
                    Optional<Filter> toAdd = to.stream()
                            .filter(t -> t.getBizName().equalsIgnoreCase(filter.getBizName())).findFirst();
                    if (toAdd.isPresent()) {
                        if (FilterOperatorEnum.EQUALS.equals(toAdd.get().getOperator()) || FilterOperatorEnum.IN.equals(
                                toAdd.get().getOperator())) {
                            List<Object> vals = new ArrayList<>();
                            if (toAdd.get().getOperator().equals(FilterOperatorEnum.IN)) {
                                vals.addAll((List<Object>) (toAdd.get().getValue()));
                            } else {
                                vals.add(toAdd.get().getValue());
                            }
                            if (filter.getOperator().equals(FilterOperatorEnum.IN)) {
                                vals.addAll((List<Object>) (filter.getValue()));
                            } else {
                                vals.add(filter.getValue());
                            }
                            toAdd.get().setValue(vals);
                            toAdd.get().setOperator(FilterOperatorEnum.IN);
                            continue;
                        }
                    }
                }
                to.add(filter);
            }
        }
    }
}
