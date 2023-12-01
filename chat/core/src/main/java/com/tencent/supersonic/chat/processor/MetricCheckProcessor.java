package com.tencent.supersonic.chat.processor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.RelatedSchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserRemoveHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MetricCheckProcessor verifies whether the dimensions
 * involved in the query in metric mode can drill down on the metric.
 */
public class MetricCheckProcessor implements ParseResultProcessor {

    @Override
    public void process(ParseResp parseResp, QueryContext queryContext, ChatContext chatContext) {
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        for (SemanticQuery semanticQuery : semanticQueries) {
            SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
            if (!QueryType.METRIC.equals(parseInfo.getQueryType())) {
                continue;
            }
            String correctSqlProcessed = processCorrectSql(parseInfo, semanticSchema);
            parseInfo.getSqlInfo().setCorrectS2SQL(correctSqlProcessed);
        }
        semanticQueries.removeIf(semanticQuery -> {
            if (!QueryType.METRIC.equals(semanticQuery.getParseInfo().getQueryType())) {
                return false;
            }
            String correctSql = semanticQuery.getParseInfo().getSqlInfo().getCorrectS2SQL();
            if (StringUtils.isBlank(correctSql)) {
                return false;
            }
            return !checkHasMetric(correctSql, semanticSchema);
        });
    }

    public String processCorrectSql(SemanticParseInfo parseInfo, SemanticSchema semanticSchema) {
        String correctSql = parseInfo.getSqlInfo().getCorrectS2SQL();
        List<String> groupByFields = SqlParserSelectHelper.getGroupByFields(correctSql);
        List<String> metricFields = SqlParserSelectHelper.getAggregateFields(correctSql);
        List<String> whereFields = SqlParserSelectHelper.getWhereFields(correctSql);
        List<String> dimensionFields = getDimensionFields(groupByFields, whereFields);
        if (CollectionUtils.isEmpty(metricFields) || StringUtils.isBlank(correctSql)) {
            return correctSql;
        }
        Set<String> metricToRemove = Sets.newHashSet();
        Set<String> groupByToRemove = Sets.newHashSet();
        Set<String> whereFieldsToRemove = Sets.newHashSet();
        for (String metricName : metricFields) {
            SchemaElement metricElement = semanticSchema.getElementByName(SchemaElementType.METRIC, metricName);
            if (metricElement == null) {
                metricToRemove.add(metricName);
            }
            if (!checkNecessaryDimension(metricElement, semanticSchema, dimensionFields)) {
                metricToRemove.add(metricName);
            }
        }
        for (String dimensionName : whereFields) {
            if (TimeDimensionEnum.containsTimeDimension(dimensionName)) {
                continue;
            }
            if (!checkInModelSchema(dimensionName, SchemaElementType.DIMENSION, semanticSchema)) {
                whereFieldsToRemove.add(dimensionName);
            }
            if (!checkDrillDownDimension(dimensionName, metricFields, semanticSchema)) {
                whereFieldsToRemove.add(dimensionName);
            }
        }
        for (String dimensionName : groupByFields) {
            if (TimeDimensionEnum.containsTimeDimension(dimensionName)) {
                continue;
            }
            if (!checkInModelSchema(dimensionName, SchemaElementType.DIMENSION, semanticSchema)) {
                groupByToRemove.add(dimensionName);
            }
            if (!checkDrillDownDimension(dimensionName, metricFields, semanticSchema)) {
                groupByToRemove.add(dimensionName);
            }
        }
        return removeFieldInSql(correctSql, metricToRemove, groupByToRemove, whereFieldsToRemove);
    }

    /**
     * To check whether the dimension bound to the metric exists,
     * eg: metric like UV is calculated in a certain dimension, it cannot be used on other dimensions.
     */
    private boolean checkNecessaryDimension(SchemaElement metric, SemanticSchema semanticSchema,
            List<String> dimensionFields) {
        List<String> necessaryDimensions = getNecessaryDimensionNames(metric, semanticSchema);
        if (CollectionUtils.isEmpty(necessaryDimensions)) {
            return true;
        }
        for (String dimension : necessaryDimensions) {
            if (!dimensionFields.contains(dimension)) {
                return false;
            }
        }
        return true;
    }

    /**
     * To check whether the dimension can drill down the metric,
     * eg: some descriptive dimensions are not suitable as drill-down dimensions
     */
    private boolean checkDrillDownDimension(String dimensionName, List<String> metrics,
            SemanticSchema semanticSchema) {
        List<SchemaElement> metricElements = semanticSchema.getMetrics().stream()
                .filter(schemaElement -> metrics.contains(schemaElement.getName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(metricElements)) {
            return false;
        }
        List<String> relateDimensions = metricElements.stream()
                .filter(schemaElement -> !CollectionUtils.isEmpty(schemaElement.getRelatedSchemaElements()))
                .map(schemaElement -> schemaElement.getRelatedSchemaElements().stream()
                        .map(RelatedSchemaElement::getDimensionId).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .map(id -> convertDimensionIdToName(id, semanticSchema))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //if no metric has drill down dimension, return true
        if (CollectionUtils.isEmpty(relateDimensions)) {
            return true;
        }
        //if this dimension not in relate drill-down dimensions, return false
        return relateDimensions.contains(dimensionName);
    }

    private List<String> getNecessaryDimensionNames(SchemaElement metric, SemanticSchema semanticSchema) {
        List<Long> necessaryDimensionIds = getNecessaryDimensions(metric);
        return necessaryDimensionIds.stream().map(id -> convertDimensionIdToName(id, semanticSchema))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Long> getNecessaryDimensions(SchemaElement metric) {
        if (metric == null) {
            return Lists.newArrayList();
        }
        List<RelatedSchemaElement> relateSchemaElements = metric.getRelatedSchemaElements();
        if (CollectionUtils.isEmpty(relateSchemaElements)) {
            return Lists.newArrayList();
        }
        return relateSchemaElements.stream()
                .filter(RelatedSchemaElement::isNecessary).map(RelatedSchemaElement::getDimensionId)
                .collect(Collectors.toList());
    }

    private List<String> getDimensionFields(List<String> groupByFields, List<String> whereFields) {
        List<String> dimensionFields = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(groupByFields)) {
            dimensionFields.addAll(groupByFields);
        }
        if (!CollectionUtils.isEmpty(whereFields)) {
            dimensionFields.addAll(whereFields);
        }
        return dimensionFields;
    }

    private String convertDimensionIdToName(Long id, SemanticSchema semanticSchema) {
        SchemaElement schemaElement = semanticSchema.getElement(SchemaElementType.DIMENSION, id);
        if (schemaElement == null) {
            return null;
        }
        return schemaElement.getName();
    }

    private boolean checkInModelSchema(String name, SchemaElementType type, SemanticSchema semanticSchema) {
        SchemaElement schemaElement = semanticSchema.getElementByName(type, name);
        return schemaElement != null;
    }

    private boolean checkHasMetric(String correctSql, SemanticSchema semanticSchema) {
        List<String> selectFields = SqlParserSelectHelper.getSelectFields(correctSql);
        List<String> aggFields = SqlParserSelectHelper.getAggregateFields(correctSql);
        List<String> collect = semanticSchema.getMetrics().stream()
                .map(SchemaElement::getName).collect(Collectors.toList());
        for (String field : selectFields) {
            if (collect.contains(field)) {
                return true;
            }
        }
        return !CollectionUtils.isEmpty(aggFields);
    }

    private static String removeFieldInSql(String sql, Set<String> metricToRemove,
            Set<String> dimensionByToRemove, Set<String> whereFieldsToRemove) {
        sql = SqlParserRemoveHelper.removeWhereCondition(sql, whereFieldsToRemove);
        sql = SqlParserRemoveHelper.removeSelect(sql, metricToRemove);
        sql = SqlParserRemoveHelper.removeSelect(sql, dimensionByToRemove);
        sql = SqlParserRemoveHelper.removeGroupBy(sql, dimensionByToRemove);
        sql = SqlParserRemoveHelper.removeNumberCondition(sql);
        return sql;
    }

}
