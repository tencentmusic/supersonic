package com.tencent.supersonic.chat.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.query.rule.metric.MetricDomainQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricFilterQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricGroupByQuery;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public class QueryReqBuilder {

    public static QueryStructReq buildStructReq(SemanticParseInfo parseInfo) {
        QueryStructReq queryStructCmd = new QueryStructReq();
        queryStructCmd.setDomainId(parseInfo.getDomainId());
        queryStructCmd.setNativeQuery(parseInfo.getNativeQuery());
        queryStructCmd.setDateInfo(parseInfo.getDateInfo());

        List<Filter> dimensionFilters = parseInfo.getDimensionFilters().stream()
                .filter(chatFilter -> Strings.isNotEmpty(chatFilter.getBizName()))
                .map(chatFilter -> new Filter(chatFilter.getBizName(), chatFilter.getOperator(), chatFilter.getValue()))
                .collect(Collectors.toList());
        queryStructCmd.setDimensionFilters(dimensionFilters);

        List<Filter> metricFilters = parseInfo.getMetricFilters().stream()
                .map(chatFilter -> new Filter(chatFilter.getBizName(), chatFilter.getOperator(), chatFilter.getValue()))
                .collect(Collectors.toList());
        queryStructCmd.setMetricFilters(metricFilters);

        addDateDimension(parseInfo);
        List<String> dimensions = parseInfo.getDimensions().stream().map(entry -> entry.getBizName())
                .collect(Collectors.toList());
        queryStructCmd.setGroups(dimensions);
        queryStructCmd.setLimit(parseInfo.getLimit());
        Set<Order> order = getOrder(parseInfo.getOrders(), parseInfo.getAggType(), parseInfo.getMetrics());
        queryStructCmd.setOrders(new ArrayList<>(order));
        queryStructCmd.setAggregators(getAggregatorByMetric(parseInfo.getMetrics(), parseInfo.getAggType()));
        return queryStructCmd;
    }

    public static QueryMultiStructReq buildMultiStructReq(SemanticParseInfo parseInfo) {
        QueryStructReq queryStructReq = buildStructReq(parseInfo);
        QueryMultiStructReq queryMultiStructReq = new QueryMultiStructReq();
        List<QueryStructReq> queryStructReqs = Lists.newArrayList();
        for (Filter dimensionFilter : queryStructReq.getDimensionFilters()) {
            QueryStructReq req = new QueryStructReq();
            BeanUtils.copyProperties(queryStructReq, req);
            req.setDimensionFilters(Lists.newArrayList(dimensionFilter));
            queryStructReqs.add(req);
        }
        queryMultiStructReq.setQueryStructReqs(queryStructReqs);
        return queryMultiStructReq;
    }

    /**
     * convert to QueryDslReq
     * @param querySql
     * @param domainId
     * @return
     */
    public static QueryDslReq buildDslReq(String querySql,Long domainId) {
        QueryDslReq queryDslReq = new QueryDslReq();
        if (Objects.nonNull(querySql)) {
            queryDslReq.setSql(querySql);
        }
        queryDslReq.setDomainId(domainId);
        return queryDslReq;
    }


    private static List<Aggregator> getAggregatorByMetric(Set<SchemaElement> metrics, AggregateTypeEnum aggregateType) {
        List<Aggregator> aggregators = new ArrayList<>();
        String agg = (aggregateType == null || aggregateType.equals(AggregateTypeEnum.NONE)) ? ""
                : aggregateType.name();
        for (SchemaElement metric : metrics) {
            aggregators.add(new Aggregator(metric.getBizName(), AggOperatorEnum.of(agg)));
        }
        return aggregators;
    }

    private static void addDateDimension(SemanticParseInfo parseInfo) {
        if (parseInfo != null) {
            String queryMode = parseInfo.getQueryMode();
            if (parseInfo.getDateInfo() == null) {
                return;
            }
            if (parseInfo.getAggType() != null && (parseInfo.getAggType().equals(AggregateTypeEnum.MAX)
                    || parseInfo.getAggType().equals(AggregateTypeEnum.MIN)) && !CollectionUtils.isEmpty(
                    parseInfo.getDimensions())) {
                return;
            }
            DateConf dateInfo = parseInfo.getDateInfo();
            String dateField = getDateField(dateInfo);

            for (SchemaElement dimension : parseInfo.getDimensions()) {
                if (dimension.getBizName().equalsIgnoreCase(dateField)) {
                    return;
                }
            }
            SchemaElement dimension = new SchemaElement();
            dimension.setBizName(dateField);

            if (MetricDomainQuery.QUERY_MODE.equals(queryMode)
                    || MetricGroupByQuery.QUERY_MODE.equals(queryMode)
                    || MetricFilterQuery.QUERY_MODE.equals(queryMode)
            ) {
                parseInfo.getDimensions().add(dimension);
            }
        }
    }

    public static Set<Order> getOrder(Set<Order> parseOrder, AggregateTypeEnum aggregator, Set<SchemaElement> metrics) {
        if (!CollectionUtils.isEmpty(parseOrder)) {
            return parseOrder;
        }
        Set<Order> orders = new LinkedHashSet();
        if (CollectionUtils.isEmpty(metrics)) {
            return orders;
        }
        if ((AggregateTypeEnum.TOPN.equals(aggregator) || AggregateTypeEnum.MAX.equals(aggregator)
                || AggregateTypeEnum.MIN.equals(
                aggregator))) {
            for (SchemaElement metric : metrics) {
                Order order = new Order();
                order.setColumn(metric.getBizName());
                order.setDirection("desc");
                orders.add(order);
            }
        }
        return orders;
    }

    public static String getDateField(DateConf dateConf) {
        if(Objects.isNull(dateConf)) {
            return "";
        }
        String dateField = TimeDimensionEnum.DAY.getName();
        if (Constants.MONTH.equals(dateConf.getPeriod())) {
            dateField = TimeDimensionEnum.MONTH.getName();
        }
        if (Constants.WEEK.equals(dateConf.getPeriod())) {
            dateField = TimeDimensionEnum.WEEK.getName();
        }
        return dateField;
    }

    public static QueryStructReq buildStructRatioReq(SemanticParseInfo parseInfo,SchemaElement metric,AggOperatorEnum aggOperatorEnum) {
        QueryStructReq queryStructCmd = buildStructReq(parseInfo);
        queryStructCmd.setNativeQuery(false);
        queryStructCmd.setOrders(new ArrayList<>());
        List<Aggregator> aggregators = new ArrayList<>();
        Aggregator ratioRoll = new Aggregator(metric.getBizName(), aggOperatorEnum);
        aggregators.add(ratioRoll);
        queryStructCmd.setAggregators(aggregators);
        return queryStructCmd;
    }
}
