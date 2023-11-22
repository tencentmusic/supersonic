package com.tencent.supersonic.chat.postprocessor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.RelateSchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.common.pojo.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserRemoveHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MetricCheckPostProcessor used to verify whether the dimensions
 * involved in the query in metric mode can drill down on the metric.
 */
public class MetricCheckPostProcessor implements PostProcessor {

    @Override
    public void process(QueryContext queryContext) {
        List<SemanticQuery> semanticQueries = queryContext.getCandidateQueries();
        Map<Long, ModelSchema> modelSchemaMap = new HashMap<>();
        for (SemanticQuery semanticQuery : semanticQueries) {
            SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
            if (!QueryType.METRIC.equals(parseInfo.getQueryType())) {
                continue;
            }
            SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
            ModelSchema modelSchema = schemaService.getModelSchema(parseInfo.getModelId());
            String processedSql = processCorrectSql(parseInfo.getSqlInfo().getCorrectS2SQL(), modelSchema);
            parseInfo.getSqlInfo().setCorrectS2SQL(processedSql);
            modelSchemaMap.put(modelSchema.getModel().getModel(), modelSchema);
        }
        semanticQueries.removeIf(semanticQuery -> {
            if (!QueryType.METRIC.equals(semanticQuery.getParseInfo().getQueryType())) {
                return false;
            }
            String correctSql = semanticQuery.getParseInfo().getSqlInfo().getCorrectS2SQL();
            if (StringUtils.isBlank(correctSql)) {
                return false;
            }
            return !checkHasMetric(correctSql, modelSchemaMap.get(semanticQuery.getParseInfo().getModelId()));
        });
    }

    public String processCorrectSql(String correctSql, ModelSchema modelSchema) {
        List<String> groupByFields = SqlParserSelectHelper.getGroupByFields(correctSql);
        List<String> metricFields = SqlParserSelectHelper.getAggregateFields(correctSql)
                .stream().filter(metricField -> !metricField.equals("*")).collect(Collectors.toList());
        List<String> whereFields = SqlParserSelectHelper.getWhereFields(correctSql);
        List<String> dimensionFields = getDimensionFields(groupByFields, whereFields);
        if (CollectionUtils.isEmpty(metricFields) || StringUtils.isBlank(correctSql)) {
            return correctSql;
        }
        Set<String> metricToRemove = Sets.newHashSet();
        Set<String> groupByToRemove = Sets.newHashSet();
        Set<String> whereFieldsToRemove = Sets.newHashSet();
        for (String metricName : metricFields) {
            SchemaElement metricElement = modelSchema.getElement(SchemaElementType.METRIC, metricName);
            if (metricElement == null) {
                metricToRemove.add(metricName);
            }
            if (!checkNecessaryDimension(metricElement, modelSchema, dimensionFields)) {
                metricToRemove.add(metricName);
            }
        }
        for (String dimensionName : whereFields) {
            if (TimeDimensionEnum.getNameList().contains(dimensionName)
                    || TimeDimensionEnum.getChNameList().contains(dimensionName)) {
                continue;
            }
            if (!checkInModelSchema(dimensionName, SchemaElementType.DIMENSION, modelSchema)) {
                whereFieldsToRemove.add(dimensionName);
            }
            if (!checkDrillDownDimension(dimensionName, metricFields, modelSchema)) {
                whereFieldsToRemove.add(dimensionName);
            }
        }
        for (String dimensionName : groupByFields) {
            if (TimeDimensionEnum.getNameList().contains(dimensionName)
                    || TimeDimensionEnum.getChNameList().contains(dimensionName)) {
                continue;
            }
            if (!checkInModelSchema(dimensionName, SchemaElementType.DIMENSION, modelSchema)) {
                groupByToRemove.add(dimensionName);
            }
            if (!checkDrillDownDimension(dimensionName, metricFields, modelSchema)) {
                groupByToRemove.add(dimensionName);
            }
        }
        return removeFieldInSql(correctSql, metricToRemove, groupByToRemove, whereFieldsToRemove);
    }


    /**
     * To check whether the dimension bound to the metric exists,
     * eg: metric like UV is calculated in a certain dimension, it cannot be used on other dimensions.
     */
    private boolean checkNecessaryDimension(SchemaElement metric, ModelSchema modelSchema,
                                            List<String> dimensionFields) {
        List<String> necessaryDimensions = getNecessaryDimensionNames(metric, modelSchema);
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
                                            ModelSchema modelSchema) {
        List<SchemaElement> metricElements = modelSchema.getMetrics().stream()
                .filter(schemaElement -> metrics.contains(schemaElement.getName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(metricElements)) {
            return false;
        }
        List<String> relateDimensions = metricElements.stream()
                .filter(schemaElement -> !CollectionUtils.isEmpty(schemaElement.getRelateSchemaElements()))
                .map(schemaElement -> schemaElement.getRelateSchemaElements().stream()
                        .map(RelateSchemaElement::getDimensionId).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .map(id -> convertDimensionIdToName(id, modelSchema))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //if no metric has drill down dimension, return true
        if (CollectionUtils.isEmpty(relateDimensions)) {
            return true;
        }
        //if this dimension not in relate drill-down dimensions, return false
        return relateDimensions.contains(dimensionName);
    }

    private List<String> getNecessaryDimensionNames(SchemaElement metric, ModelSchema modelSchema) {
        List<Long> necessaryDimensionIds = getNecessaryDimensions(metric);
        return necessaryDimensionIds.stream().map(id -> convertDimensionIdToName(id, modelSchema))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Long> getNecessaryDimensions(SchemaElement metric) {
        if (metric == null) {
            return Lists.newArrayList();
        }
        List<RelateSchemaElement> relateSchemaElements = metric.getRelateSchemaElements();
        if (CollectionUtils.isEmpty(relateSchemaElements)) {
            return Lists.newArrayList();
        }
        return relateSchemaElements.stream()
                .filter(RelateSchemaElement::isNecessary).map(RelateSchemaElement::getDimensionId)
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

    private String convertDimensionIdToName(Long id, ModelSchema modelSchema) {
        SchemaElement schemaElement = modelSchema.getElement(SchemaElementType.DIMENSION, id);
        if (schemaElement == null) {
            return null;
        }
        return schemaElement.getName();
    }

    private boolean checkInModelSchema(String name, SchemaElementType type, ModelSchema modelSchema) {
        SchemaElement schemaElement = modelSchema.getElement(type, name);
        return schemaElement != null;
    }

    private boolean checkHasMetric(String correctSql, ModelSchema modelSchema) {
        List<String> selectFields = SqlParserSelectHelper.getSelectFields(correctSql);
        List<String> aggFields = SqlParserSelectHelper.getAggregateFields(correctSql);
        List<String> collect = modelSchema.getMetrics().stream()
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
