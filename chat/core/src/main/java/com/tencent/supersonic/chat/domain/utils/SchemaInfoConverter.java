package com.tencent.supersonic.chat.domain.utils;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.application.query.MetricCompare;
import com.tencent.supersonic.chat.application.query.MetricDomain;
import com.tencent.supersonic.chat.application.query.MetricFilter;
import com.tencent.supersonic.chat.application.query.MetricGroupBy;
import com.tencent.supersonic.chat.application.query.MetricOrderBy;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.enums.AggOperatorEnum;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.core.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

public class SchemaInfoConverter {

    /***
     * convert to queryStructReq
     * @param parseInfo
     * @return
     */
    public static QueryStructReq convertTo(SemanticParseInfo parseInfo) {
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

    private static List<Aggregator> getAggregatorByMetric(Set<SchemaItem> metrics, AggregateTypeEnum aggregateType) {
        List<Aggregator> aggregators = new ArrayList<>();
        String agg = (aggregateType == null || aggregateType.equals(AggregateTypeEnum.NONE)) ? ""
                : aggregateType.name();
        for (SchemaItem metric : metrics) {
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
            if (MetricCompare.QUERY_MODE.equals(queryMode)) {
                if (parseInfo.getAggType() != null && !parseInfo.getAggType().equals(AggregateTypeEnum.NONE)) {
                    return;
                }
            }
            if (parseInfo.getAggType() != null && (parseInfo.getAggType().equals(AggregateTypeEnum.MAX)
                    || parseInfo.getAggType().equals(AggregateTypeEnum.MIN)) && !CollectionUtils.isEmpty(
                    parseInfo.getDimensions())) {
                return;
            }
            DateConf dateInfo = parseInfo.getDateInfo();
            String dateField = TimeDimensionEnum.DAY.getName();
            if (Constants.MONTH.equals(dateInfo.getPeriod())) {
                dateField = TimeDimensionEnum.MONTH.getName();
            }
            if (Constants.WEEK.equals(dateInfo.getPeriod())) {
                dateField = TimeDimensionEnum.WEEK.getName();
            }
            for (SchemaItem dimension : parseInfo.getDimensions()) {
                if (dimension.getBizName().equalsIgnoreCase(dateField)) {
                    return;
                }
            }
            SchemaItem dimension = new SchemaItem();
            dimension.setBizName(dateField);

            if (MetricDomain.QUERY_MODE.equals(queryMode)
                    || MetricGroupBy.QUERY_MODE.equals(queryMode)
                    || MetricFilter.QUERY_MODE.equals(queryMode)
                    || MetricCompare.QUERY_MODE.equals(queryMode)
                    || MetricOrderBy.QUERY_MODE.equals(queryMode)) {
                parseInfo.getDimensions().add(dimension);
            }
        }
    }

    public static DomainInfos convert(List<DomainSchemaResp> domainSchemaInfos) {
        DomainInfos result = new DomainInfos();
        if (CollectionUtils.isEmpty(domainSchemaInfos)) {
            return result;
        }
        for (DomainSchemaResp domainSchemaDesc : domainSchemaInfos) {
            int domain = Math.toIntExact(domainSchemaDesc.getId());
            // domain
            ItemDO domainDO = new ItemDO();
            domainDO.setDomain(domain);
            domainDO.setName(domainSchemaDesc.getName());
            domainDO.setItemId(domain);
            result.getDomains().add(domainDO);
            // entity
            List<String> entityNames = domainSchemaDesc.getEntityNames();
            if (!CollectionUtils.isEmpty(entityNames)) {
                for (String entityName : entityNames) {
                    ItemDO entity = new ItemDO();
                    entity.setDomain(domain);
                    entity.setName(entityName);
                    entity.setItemId(domain);
                    result.getEntities().add(entity);
                }
            }
            // metric
            for (MetricSchemaResp metric : domainSchemaDesc.getMetrics()) {
                ItemDO metricDO = new ItemDO();
                metricDO.setDomain(domain);
                metricDO.setName(metric.getName());
                metricDO.setItemId(Math.toIntExact(metric.getId()));
                metricDO.setUseCnt(metric.getUseCnt());
                result.getMetrics().add(metricDO);
            }
            // dimension
            for (DimSchemaResp dimension : domainSchemaDesc.getDimensions()) {
                ItemDO dimensionDO = new ItemDO();
                dimensionDO.setDomain(domain);
                dimensionDO.setName(dimension.getName());
                dimensionDO.setItemId(Math.toIntExact(dimension.getId()));
                dimensionDO.setUseCnt(dimension.getUseCnt());
                result.getDimensions().add(dimensionDO);
            }
        }
        return result;
    }

    public static Set<Order> getOrder(Set<Order> parseOrder, AggregateTypeEnum aggregator, Set<SchemaItem> metrics) {
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
            for (SchemaItem metric : metrics) {
                Order order = new Order();
                order.setColumn(metric.getBizName());
                order.setDirection("desc");
                orders.add(order);
            }
        }
        return orders;
    }
}
