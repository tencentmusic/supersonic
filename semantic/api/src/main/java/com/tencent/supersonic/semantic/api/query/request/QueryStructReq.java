package com.tencent.supersonic.semantic.api.query.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.semantic.api.query.pojo.Cache;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.pojo.Param;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;


@Data
public class QueryStructReq {

    private Long modelId;

    private List<String> groups = new ArrayList<>();
    private List<Aggregator> aggregators = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private List<Filter> dimensionFilters = new ArrayList<>();
    private List<Filter> metricFilters = new ArrayList<>();
    private List<Param> params = new ArrayList<>();
    private DateConf dateInfo;
    private Long limit = 2000L;
    private Boolean nativeQuery = false;
    private Cache cacheInfo;

    public List<String> getGroups() {
        if (!CollectionUtils.isEmpty(this.groups)) {
            this.groups = groups.stream().filter(group -> !Strings.isEmpty(group)).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(this.groups)) {
            this.groups = Lists.newArrayList();
        }

        return this.groups;
    }

    public List<String> getMetrics() {
        List<String> metrics = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(this.aggregators)) {
            metrics = aggregators.stream().map(Aggregator::getColumn).collect(Collectors.toList());
        }
        return metrics;
    }

    public List<Order> getOrders() {
        if (orders == null) {
            return Lists.newArrayList();
        }
        return orders;
    }

    public List<Param> getParams() {
        if (params == null) {
            return Lists.newArrayList();
        }
        return params;
    }

    public String toCustomizedString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"modelId\":")
                .append(modelId);
        stringBuilder.append(",\"groups\":")
                .append(groups);
        stringBuilder.append(",\"aggregators\":")
                .append(aggregators);
        stringBuilder.append(",\"orders\":")
                .append(orders);
        stringBuilder.append(",\"filters\":")
                .append(dimensionFilters);
        stringBuilder.append(",\"dateInfo\":")
                .append(dateInfo);
        stringBuilder.append(",\"params\":")
                .append(params);
        stringBuilder.append(",\"limit\":")
                .append(limit);
        stringBuilder.append(",\"nativeQuery\":")
                .append(nativeQuery);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }


    public String generateCommandMd5() {
        return DigestUtils.md5Hex(this.toCustomizedString());
    }

    public List<Filter> getOriginalFilter() {
        return dimensionFilters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"modelId\":")
                .append(modelId);
        sb.append(",\"groups\":")
                .append(groups);
        sb.append(",\"aggregators\":")
                .append(aggregators);
        sb.append(",\"orders\":")
                .append(orders);
        sb.append(",\"dimensionFilters\":")
                .append(dimensionFilters);
        sb.append(",\"metricFilters\":")
                .append(metricFilters);
        sb.append(",\"params\":")
                .append(params);
        sb.append(",\"dateInfo\":")
                .append(dateInfo);
        sb.append(",\"limit\":")
                .append(limit);
        sb.append(",\"nativeQuery\":")
                .append(nativeQuery);
        sb.append(",\"cacheInfo\":")
                .append(cacheInfo);
        sb.append('}');
        return sb.toString();
    }
}
