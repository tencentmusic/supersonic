package com.tencent.supersonic.chat.test.context;

import com.google.gson.Gson;
import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public class SemanticParseObjectHelper {

    public static SemanticParseInfo copy(SemanticParseInfo semanticParseInfo) {
        Gson g = new Gson();
        return g.fromJson(g.toJson(semanticParseInfo), SemanticParseInfo.class);
    }

    public static SemanticParseInfo getSemanticParseInfo(String json) {
        Gson gson = new Gson();
        SemanticParseJson semanticParseJson = gson.fromJson(json, SemanticParseJson.class);
        if (semanticParseJson != null) {
            return getSemanticParseInfo(semanticParseJson);
        }
        return null;
    }

    private static SemanticParseInfo getSemanticParseInfo(SemanticParseJson semanticParseJson) {
        Long domain = semanticParseJson.getDomain();
        List<SchemaItem> dimensionList = new ArrayList<>();
        List<SchemaItem> metricList = new ArrayList<>();
        List<Filter> chatFilters = new ArrayList<>();

        if (semanticParseJson.getFilter() != null && semanticParseJson.getFilter().size() > 0) {
            for (List<String> filter : semanticParseJson.getFilter()) {
                chatFilters.add(getChatFilter(filter));
            }
        }

        for (String dim : semanticParseJson.getDimensions()) {
            dimensionList.add(getDimension(dim, domain));
        }
        for (String metric : semanticParseJson.getMetrics()) {
            metricList.add(getMetric(metric, domain));
        }

        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();

        semanticParseInfo.setDimensionFilters(chatFilters);
        semanticParseInfo.setAggType(semanticParseJson.getAggregateType());
        semanticParseInfo.setDomainId(domain);
        semanticParseInfo.setQueryMode(semanticParseJson.getQueryMode());
        semanticParseInfo.setMetrics(metricList);
        semanticParseInfo.setDimensions(dimensionList);

        DateConf dateInfo = getDateInfoAgo(semanticParseJson.getDay());
        semanticParseInfo.setDateInfo(dateInfo);
        return semanticParseInfo;
    }

    private static DateConf getDateInfoAgo(int dayAgo) {
        if (dayAgo > 0) {
            DateConf dateInfo = new DateConf();
            dateInfo.setUnit(dayAgo);
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            return dateInfo;
        }
        return null;
    }

    private static Filter getChatFilter(List<String> filters) {
        if (filters.size() > 1) {
            Filter chatFilter = new Filter();

            chatFilter.setBizName(filters.get(1));
            chatFilter.setOperator(FilterOperatorEnum.getSqlOperator(filters.get(2)));
            if (filters.size() > 4) {
                List<String> valuse = new ArrayList<>();
                valuse.addAll(filters.subList(3, filters.size()));
                chatFilter.setValue(valuse);
            } else {
                chatFilter.setValue(filters.get(3));
            }

            return chatFilter;
        }
        return null;
    }


    private static SchemaItem getMetric(String bizName, Long domainId) {
        SchemaItem metric = new SchemaItem();
        metric.setBizName(bizName);
        //metric.set(domainId);
        return metric;
    }

    private static SchemaItem getDimension(String bizName, Long domainId) {
        SchemaItem dimension = new SchemaItem();
        dimension.setBizName(bizName);
        //dimension.setDomainId(domainId);
        return dimension;
    }

    @Data
    public static class SemanticParseJson {

        private Long domain;
        private String queryMode;
        private AggregateTypeEnum aggregateType;
        private Integer day;
        private List<String> dimensions;
        private List<String> metrics;
        private List<List<String>> filter;

    }
}
