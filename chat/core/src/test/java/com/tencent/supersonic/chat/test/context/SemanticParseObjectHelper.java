package com.tencent.supersonic.chat.test.context;

import com.google.gson.Gson;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.DateConf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        Long model = semanticParseJson.getModel();
        Set<SchemaElement> dimensionList = new LinkedHashSet();
        Set<SchemaElement> metricList = new LinkedHashSet();
        Set<QueryFilter> chatFilters = new LinkedHashSet();

        if (semanticParseJson.getFilter() != null && semanticParseJson.getFilter().size() > 0) {
            for (List<String> filter : semanticParseJson.getFilter()) {
                chatFilters.add(getChatFilter(filter));
            }
        }

        for (String dim : semanticParseJson.getDimensions()) {
            dimensionList.add(getDimension(dim, model));
        }
        for (String metric : semanticParseJson.getMetrics()) {
            metricList.add(getMetric(metric, model));
        }

        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();

        semanticParseInfo.setDimensionFilters(chatFilters);
        semanticParseInfo.setAggType(semanticParseJson.getAggregateType());
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
            dateInfo.setDateMode(DateConf.DateMode.RECENT);
            return dateInfo;
        }
        return null;
    }

    private static QueryFilter getChatFilter(List<String> filters) {
        if (filters.size() > 1) {
            QueryFilter chatFilter = new QueryFilter();

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

    private static SchemaElement getMetric(String bizName, Long modelId) {
        SchemaElement metric = new SchemaElement();
        metric.setBizName(bizName);
        return metric;
    }

    private static SchemaElement getDimension(String bizName, Long modelId) {
        SchemaElement dimension = new SchemaElement();
        dimension.setBizName(bizName);
        return dimension;
    }

    @Data
    public static class SemanticParseJson {

        private Long model;
        private String queryMode;
        private AggregateTypeEnum aggregateType;
        private Integer day;
        private List<String> dimensions;
        private List<String> metrics;
        private List<List<String>> filter;

    }
}
