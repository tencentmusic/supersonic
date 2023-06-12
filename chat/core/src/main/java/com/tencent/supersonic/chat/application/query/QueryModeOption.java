package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.Filter;
import com.tencent.supersonic.chat.api.pojo.SchemaElementCount;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.chat.application.parser.resolver.AggregateTypeResolver;
import com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.common.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryModeOption {

    private QueryModeElementOption domain;
    private QueryModeElementOption entity;
    private QueryModeElementOption metric;
    private QueryModeElementOption dimension;
    private QueryModeElementOption filter;
    private QueryModeElementOption date;

    private QueryModeElementOption aggregation;
    private boolean supportCompare = false;
    private boolean supportOrderBy = false;
    List<AggregateTypeEnum> orderByTypes = Arrays.asList(AggregateTypeEnum.MAX, AggregateTypeEnum.MIN,
            AggregateTypeEnum.TOPN);

    public static QueryModeOption build() {
        return new QueryModeOption();
    }

    public QueryModeOption setDimension(SchemaElementOption dimension,
            QueryModeElementOption.RequireNumberType requireNumberType, Integer requireNumber) {
        this.dimension = QueryModeElementOption.build(dimension, requireNumberType, requireNumber);
        return this;
    }

    public QueryModeOption setDomain(SchemaElementOption domain,
            QueryModeElementOption.RequireNumberType requireNumberType, Integer requireNumber) {
        this.domain = QueryModeElementOption.build(domain, requireNumberType, requireNumber);
        return this;
    }

    public QueryModeOption setEntity(SchemaElementOption entity,
            QueryModeElementOption.RequireNumberType requireNumberType, Integer requireNumber) {
        this.entity = QueryModeElementOption.build(entity, requireNumberType, requireNumber);
        return this;
    }

    public QueryModeOption setFilter(SchemaElementOption filter,
            QueryModeElementOption.RequireNumberType requireNumberType, Integer requireNumber) {
        this.filter = QueryModeElementOption.build(filter, requireNumberType, requireNumber);
        return this;
    }

    public QueryModeOption setMetric(SchemaElementOption metric,
            QueryModeElementOption.RequireNumberType requireNumberType, Integer requireNumber) {
        this.metric = QueryModeElementOption.build(metric, requireNumberType, requireNumber);
        return this;
    }

    /**
     * add query semantic parse info
     * @param schemaElementMatches
     * @param domainSchemaDesc
     * @param chaConfigRichDesc
     * @param semanticParseInfo
     */
    public void addQuerySemanticParseInfo(List<SchemaElementMatch> schemaElementMatches,
            DomainSchemaResp domainSchemaDesc, ChatConfigRichInfo chaConfigRichDesc,
            SemanticParseInfo semanticParseInfo) {
        Map<Long, DimSchemaResp> dimensionDescMap = domainSchemaDesc.getDimensions().stream()
                .collect(Collectors.toMap(DimSchemaResp::getId, Function.identity()));
        Map<Long, MetricSchemaResp> metricDescMap = domainSchemaDesc.getMetrics().stream()
                .collect(Collectors.toMap(MetricSchemaResp::getId, Function.identity()));

        Map<Long, List<SchemaElementMatch>> values = getLinkSchemaElementMatch(schemaElementMatches, dimensionDescMap,
                semanticParseInfo, metricDescMap);
        if (!values.isEmpty()) {
            for (Map.Entry<Long, List<SchemaElementMatch>> entry : values.entrySet()) {
                DimSchemaResp dimensionDesc = dimensionDescMap.get(entry.getKey());
                if (entry.getValue().size() == 1) {
                    SchemaElementMatch schemaElementMatch = entry.getValue().get(0);
                    Filter chatFilter = new Filter();
                    chatFilter.setValue(schemaElementMatch.getWord());
                    chatFilter.setBizName(dimensionDesc.getBizName());
                    chatFilter.setName(dimensionDesc.getName());
                    chatFilter.setOperator(FilterOperatorEnum.EQUALS);
                    chatFilter.setElementID(Long.valueOf(schemaElementMatch.getElementID()));
                    semanticParseInfo.getDimensionFilters().add(chatFilter);
                    ContextHelper.setEntityId(entry.getKey(), schemaElementMatch.getWord(), chaConfigRichDesc,
                            semanticParseInfo);
                } else {
                    Filter chatFilter = new Filter();
                    List<String> vals = new ArrayList<>();
                    entry.getValue().stream().forEach(i -> vals.add(i.getWord()));
                    chatFilter.setValue(vals);
                    chatFilter.setBizName(dimensionDesc.getBizName());
                    chatFilter.setName(dimensionDesc.getName());
                    chatFilter.setOperator(FilterOperatorEnum.IN);
                    chatFilter.setElementID(entry.getKey());
                    semanticParseInfo.getDimensionFilters().add(chatFilter);
                }
            }
        }
    }

    private Map<Long, List<SchemaElementMatch>> getLinkSchemaElementMatch(List<SchemaElementMatch> schemaElementMatches,
            Map<Long, DimSchemaResp> dimensionDescMap, SemanticParseInfo semanticParseInfo,
            Map<Long, MetricSchemaResp> metricDescMap) {
        Map<Long, List<SchemaElementMatch>> values = new HashMap<>();
        for (SchemaElementMatch schemaElementMatch : schemaElementMatches) {
            Long elementID = Long.valueOf(schemaElementMatch.getElementID());
            switch (schemaElementMatch.getElementType()) {
                case DATE:
                case DOMAIN:
                case ENTITY:
                    break;
                case ID:
                case VALUE:
                case DIMENSION:
                    if (dimensionDescMap.containsKey(elementID)) {
                        DimSchemaResp dimensionDesc = dimensionDescMap.get(elementID);
                        SchemaItem dimensionParseInfo = new SchemaItem();
                        dimensionParseInfo.setBizName(dimensionDesc.getBizName());
                        dimensionParseInfo.setName(dimensionDesc.getName());
                        dimensionParseInfo.setId(dimensionDesc.getId());
                        if (!dimension.getSchemaElementOption().equals(SchemaElementOption.UNUSED)
                                && schemaElementMatch.getElementType().equals(SchemaElementType.DIMENSION)) {
                            semanticParseInfo.getDimensions().add(dimensionParseInfo);
                        }
                        if (!filter.getSchemaElementOption().equals(SchemaElementOption.UNUSED) && (
                                schemaElementMatch.getElementType().equals(SchemaElementType.VALUE)
                                        || schemaElementMatch.getElementType().equals(SchemaElementType.ID))) {
                            if (values.containsKey(elementID)) {
                                values.get(elementID).add(schemaElementMatch);
                            } else {
                                values.put(elementID, new ArrayList<>(Arrays.asList(schemaElementMatch)));
                            }
                        }
                    }
                    break;
                case METRIC:
                    if (!metric.getSchemaElementOption().equals(SchemaElementOption.UNUSED)) {
                        if (metricDescMap.containsKey(elementID)) {
                            MetricSchemaResp metricDesc = metricDescMap.get(elementID);
                            SchemaItem metric = new SchemaItem();
                            metric.setBizName(metricDesc.getBizName());
                            metric.setName(metricDesc.getName());
                            metric.setId(metricDesc.getId());
                            semanticParseInfo.getMetrics().add(metric);
                        }
                    }
                    break;
                default:
            }
        }
        return values;
    }

    /**
     * math
     * @param elementMatches
     * @param queryCtx
     * @return
     */
    public SchemaElementCount match(List<SchemaElementMatch> elementMatches, QueryContextReq queryCtx) {
        AggregateTypeResolver aggregateTypeResolver = ContextUtils.getBean(AggregateTypeResolver.class);

        boolean isCompareType = aggregateTypeResolver.hasCompareIntentionalWords(queryCtx.getQueryText());
        boolean isOrderByType = orderByTypes.contains(aggregateTypeResolver.resolve(queryCtx.getQueryText()));

        if ((isOrderByType && !supportOrderBy) || (isCompareType && !supportCompare)) {
            return new SchemaElementCount();
        }

        SchemaElementCount schemaElementCount = new SchemaElementCount();
        schemaElementCount.setCount(0);
        schemaElementCount.setMaxSimilarity(0);
        HashMap<SchemaElementType, Integer> schemaElementTypeCount = new HashMap<>();
        for (SchemaElementMatch schemaElementMatch : elementMatches) {
            SchemaElementType schemaElementType = schemaElementMatch.getElementType();
            if (schemaElementTypeCount.containsKey(schemaElementType)) {
                schemaElementTypeCount.put(schemaElementType, schemaElementTypeCount.get(schemaElementType) + 1);
            } else {
                schemaElementTypeCount.put(schemaElementType, 1);
            }
        }
        // test each element
        if (!isMatch(domain, getCount(schemaElementTypeCount, SchemaElementType.DOMAIN))) {
            return schemaElementCount;
        }
        if (!isMatch(dimension, getCount(schemaElementTypeCount, SchemaElementType.DIMENSION))) {
            return schemaElementCount;
        }
        if (!isMatch(metric, getCount(schemaElementTypeCount, SchemaElementType.METRIC))) {
            return schemaElementCount;
        }
        int filterCount = getCount(schemaElementTypeCount, SchemaElementType.VALUE) + getCount(schemaElementTypeCount,
                SchemaElementType.ID);
        if (!isMatch(filter, filterCount)) {
            return schemaElementCount;
        }
        if (!isMatch(entity, getCount(schemaElementTypeCount, SchemaElementType.ENTITY))) {
            return schemaElementCount;
        }
        if (!isMatch(date, getCount(schemaElementTypeCount, SchemaElementType.DATE))) {
            return schemaElementCount;
        }
        // count the max similarity
        double similarity = 0;
        Set<SchemaElementType> schemaElementTypeSet = new HashSet<>();
        for (SchemaElementMatch schemaElementMatch : elementMatches) {
            double schemaElementMatchSimilarity = getSimilarity(schemaElementMatch);
            if (schemaElementMatchSimilarity > similarity) {
                similarity = schemaElementMatchSimilarity;
            }
            schemaElementTypeSet.add(schemaElementMatch.getElementType());
        }
        schemaElementCount.setCount(schemaElementTypeSet.size());
        schemaElementCount.setMaxSimilarity(similarity);
        return schemaElementCount;
    }

    private int getCount(HashMap<SchemaElementType, Integer> schemaElementTypeCount,
            SchemaElementType schemaElementType) {
        if (schemaElementTypeCount.containsKey(schemaElementType)) {
            return schemaElementTypeCount.get(schemaElementType);
        }
        return 0;
    }

    private double getSimilarity(SchemaElementMatch schemaElementMatch) {
        switch (schemaElementMatch.getElementType()) {
            case DATE:
                return getSimilarity(date, schemaElementMatch.getSimilarity());
            case DOMAIN:
                return getSimilarity(domain, schemaElementMatch.getSimilarity());
            case ENTITY:
                return getSimilarity(entity, schemaElementMatch.getSimilarity());
            case DIMENSION:
                return getSimilarity(dimension, schemaElementMatch.getSimilarity());
            case METRIC:
                return getSimilarity(metric, schemaElementMatch.getSimilarity());
            case ID:
            case VALUE:
                return getSimilarity(filter, schemaElementMatch.getSimilarity());
            default:
                return 0;
        }
    }

    private double getSimilarity(QueryModeElementOption queryModeElementOption, double similarity) {
        if (queryModeElementOption.getSchemaElementOption().equals(SchemaElementOption.REQUIRED)) {
            return similarity;
        }
        return 0;
    }

    private boolean isMatch(QueryModeElementOption queryModeElementOption, int count) {
        // first find if unused but not empty
        if (queryModeElementOption.getSchemaElementOption().equals(SchemaElementOption.UNUSED) && count > 0) {
            return false;
        }
        // find if required but empty
        if (queryModeElementOption.getSchemaElementOption().equals(SchemaElementOption.REQUIRED) && count <= 0) {
            return false;
        }
        // find if count no satisfy
        if (queryModeElementOption.getRequireNumberType().equals(QueryModeElementOption.RequireNumberType.EQUAL)
                && queryModeElementOption.getRequireNumber() != count) {
            return false;
        }
        if (queryModeElementOption.getRequireNumberType().equals(QueryModeElementOption.RequireNumberType.AT_LEAST)
                && count < queryModeElementOption.getRequireNumber()) {
            return false;
        }
        if (queryModeElementOption.getRequireNumberType().equals(QueryModeElementOption.RequireNumberType.AT_MOST)
                && count > queryModeElementOption.getRequireNumber()) {
            return false;
        }
        // here default satisfy
        return true;
    }

}
