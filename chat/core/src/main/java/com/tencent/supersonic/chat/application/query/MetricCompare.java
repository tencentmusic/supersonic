package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_MOST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.OPTIONAL;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricCompare extends MetricSemanticQuery {

    public static String QUERY_MODE = "METRIC_COMPARE";
    public static Pattern intentWordPattern = Pattern.compile("(?i)(比较|对比)");

    public MetricCompare() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 2)
                .addOption(ENTITY, OPTIONAL, AT_MOST, 1);

        queryMatcher.setSupportCompare(true);
        queryMatcher.setSupportOrderBy(true);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches, QueryContextReq queryCtx) {
        if (intentWordPattern.matcher(queryCtx.getQueryText()).find()) {
            return super.match(candidateElementMatches, queryCtx);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void inheritContext(ChatContext chatContext) {
        SemanticParseInfo chatParseInfo = chatContext.getParseInfo();
        ContextHelper.updateTimeIfEmpty(chatParseInfo, parseInfo);
        ContextHelper.addIfEmpty(chatParseInfo.getMetrics(), parseInfo.getMetrics());
        mergeAppend(chatParseInfo.getDimensionFilters(), parseInfo.getDimensionFilters());
        addCompareDimension(parseInfo);
        parseInfo.setBonus(2 * 1.0);
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
