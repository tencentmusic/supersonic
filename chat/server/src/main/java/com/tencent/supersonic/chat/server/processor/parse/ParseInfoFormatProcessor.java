package com.tencent.supersonic.chat.server.processor.parse;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ParseInfoFormatProcessor formats parse info to make it more readable to the users.
 **/
@Slf4j
public class ParseInfoFormatProcessor implements ParseResultProcessor {

    @Override
    public boolean accept(ParseContext parseContext) {
        return !parseContext.getResponse().getSelectedParses().isEmpty();
    }

    @Override
    public void process(ParseContext parseContext) {
        parseContext.getResponse().getSelectedParses().forEach(p -> {
            if (Objects.isNull(p.getDataSet()) || Objects.isNull(p.getSqlInfo().getParsedS2SQL())) {
                return;
            }

            buildParseInfoFromSQL(p);
            buildTextInfo(p);
        });
    }

    private void buildTextInfo(SemanticParseInfo parseInfo) {
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("**数据集:** ").append(parseInfo.getDataSet().getName()).append(" ");
        List<String> metricNames = parseInfo.getMetrics().stream().map(SchemaElement::getName)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(metricNames)) {
            textBuilder.append(" **指标:** ").append(String.join(",", metricNames));
        }
        List<String> dimensionNames = parseInfo.getDimensions().stream().map(SchemaElement::getName)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(dimensionNames)) {
            textBuilder.append(" **维度:** ").append(String.join(",", dimensionNames));
        }
        if (parseInfo.getDateInfo() != null) {
            textBuilder.append(" **数据时间:** ").append(parseInfo.getDateInfo().getStartDate())
                    .append("~").append(parseInfo.getDateInfo().getEndDate()).append(" ");
        }
        if (!CollectionUtils.isEmpty(parseInfo.getDimensionFilters())
                || CollectionUtils.isEmpty(parseInfo.getMetricFilters())) {
            Set<QueryFilter> queryFilters = parseInfo.getDimensionFilters();
            queryFilters.addAll(parseInfo.getMetricFilters());
            if (!queryFilters.isEmpty()) {
                textBuilder.append(" **筛选条件:** ");
            }
            for (QueryFilter queryFilter : queryFilters) {
                textBuilder.append(queryFilter.getName()).append(" ")
                        .append(queryFilter.getOperator().getValue()).append(" ")
                        .append(queryFilter.getValue()).append(" ");
            }
        }
        parseInfo.setTextInfo(textBuilder.toString());
    }


    private void buildParseInfoFromSQL(SemanticParseInfo parseInfo) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        String s2SQL = sqlInfo.getCorrectedS2SQL();
        if (StringUtils.isBlank(s2SQL)) {
            return;
        }

        parseQueryType(parseInfo);
        List<FieldExpression> expressions = SqlSelectHelper.getFilterExpression(s2SQL);
        Long dataSetId = parseInfo.getDataSetId();
        SemanticLayerService semanticLayerService =
                ContextUtils.getBean(SemanticLayerService.class);
        DataSetSchema dsSchema = semanticLayerService.getDataSetSchema(dataSetId);

        // extract date filter from S2SQL
        try {
            if (!CollectionUtils.isEmpty(expressions)) {
                parseInfo.setDateInfo(extractDateFilter(expressions, dsSchema));
            }
        } catch (Exception e) {
            log.error("failed to extract date range:", e);
        }

        // extract dimension filters from S2SQL
        try {
            List<QueryFilter> queryFilters = extractDimensionFilter(dsSchema, expressions);
            parseInfo.getDimensionFilters().addAll(queryFilters);
        } catch (Exception e) {
            log.error("failed to extract dimension filters:", e);
        }

        // extract metrics from S2SQL
        List<String> allFields =
                filterDateField(dsSchema, SqlSelectHelper.getAllSelectFields(s2SQL));
        Set<SchemaElement> metrics = matchSchemaElements(allFields, dsSchema.getMetrics());
        parseInfo.setMetrics(metrics);

        // extract dimensions from S2SQL
        if (QueryType.AGGREGATE.equals(parseInfo.getQueryType())) {
            List<String> groupByFields = SqlSelectHelper.getGroupByFields(s2SQL);
            List<String> groupByDimensions = filterDateField(dsSchema, groupByFields);
            parseInfo.setDimensions(
                    matchSchemaElements(groupByDimensions, dsSchema.getDimensions()));
        } else if (QueryType.DETAIL.equals(parseInfo.getQueryType())) {
            List<String> selectFields = SqlSelectHelper.getSelectFields(s2SQL);
            List<String> selectDimensions = filterDateField(dsSchema, selectFields);
            parseInfo
                    .setDimensions(matchSchemaElements(selectDimensions, dsSchema.getDimensions()));
        }
    }

    private Set<SchemaElement> matchSchemaElements(List<String> allFields,
            Set<SchemaElement> elements) {
        return elements.stream().filter(schemaElement -> {
            if (CollectionUtils.isEmpty(schemaElement.getAlias())) {
                return allFields.contains(schemaElement.getName());
            }
            Set<String> allFieldsSet = new HashSet<>(allFields);
            Set<String> aliasSet = new HashSet<>(schemaElement.getAlias());
            List<String> intersection =
                    allFieldsSet.stream().filter(aliasSet::contains).collect(Collectors.toList());
            return allFields.contains(schemaElement.getName())
                    || !CollectionUtils.isEmpty(intersection);
        }).collect(Collectors.toSet());
    }

    private List<String> filterDateField(DataSetSchema dataSetSchema, List<String> allFields) {
        return allFields.stream().filter(entry -> !isPartitionDimension(dataSetSchema, entry))
                .collect(Collectors.toList());
    }

    private List<QueryFilter> extractDimensionFilter(DataSetSchema dsSchema,
            List<FieldExpression> fieldExpressions) {

        Map<String, SchemaElement> fieldNameToElement = getNameToElement(dsSchema);
        List<QueryFilter> result = Lists.newArrayList();
        for (FieldExpression expression : fieldExpressions) {
            QueryFilter dimensionFilter = new QueryFilter();
            dimensionFilter.setValue(expression.getFieldValue());
            SchemaElement schemaElement = fieldNameToElement.get(expression.getFieldName());
            if (Objects.isNull(schemaElement)
                    || isPartitionDimension(dsSchema, schemaElement.getName())) {
                continue;
            }
            dimensionFilter.setName(schemaElement.getName());
            dimensionFilter.setBizName(schemaElement.getBizName());
            dimensionFilter.setElementID(schemaElement.getId());

            FilterOperatorEnum operatorEnum =
                    FilterOperatorEnum.getSqlOperator(expression.getOperator());
            dimensionFilter.setOperator(operatorEnum);
            dimensionFilter.setFunction(expression.getFunction());
            result.add(dimensionFilter);
        }
        return result;
    }

    private DateConf extractDateFilter(List<FieldExpression> fieldExpressions,
            DataSetSchema dataSetSchema) {
        List<FieldExpression> dateExpressions = fieldExpressions.stream().filter(
                expression -> isPartitionDimension(dataSetSchema, expression.getFieldName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dateExpressions)) {
            return null;
        }
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
        FieldExpression firstExpression = dateExpressions.get(0);

        FilterOperatorEnum firstOperator =
                FilterOperatorEnum.getSqlOperator(firstExpression.getOperator());
        if (FilterOperatorEnum.EQUALS.equals(firstOperator)
                && Objects.nonNull(firstExpression.getFieldValue())) {
            dateInfo.setStartDate(firstExpression.getFieldValue().toString());
            dateInfo.setEndDate(firstExpression.getFieldValue().toString());
            dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            return dateInfo;
        }
        if (containOperators(firstExpression, firstOperator, FilterOperatorEnum.GREATER_THAN,
                FilterOperatorEnum.GREATER_THAN_EQUALS)) {
            dateInfo.setStartDate(firstExpression.getFieldValue().toString());
            if (hasSecondDate(dateExpressions)) {
                dateInfo.setEndDate(dateExpressions.get(1).getFieldValue().toString());
            }
        }
        if (containOperators(firstExpression, firstOperator, FilterOperatorEnum.MINOR_THAN,
                FilterOperatorEnum.MINOR_THAN_EQUALS)) {
            dateInfo.setEndDate(firstExpression.getFieldValue().toString());
            if (hasSecondDate(dateExpressions)) {
                dateInfo.setStartDate(dateExpressions.get(1).getFieldValue().toString());
            }
        }
        return dateInfo;
    }

    private static boolean isPartitionDimension(DataSetSchema dataSetSchema, String sqlFieldName) {
        if (Objects.isNull(dataSetSchema) || Objects.isNull(dataSetSchema.getPartitionDimension())
                || Objects.isNull(dataSetSchema.getPartitionDimension().getName())) {
            return false;
        }
        return sqlFieldName.equalsIgnoreCase(dataSetSchema.getPartitionDimension().getName());
    }

    private boolean containOperators(FieldExpression expression, FilterOperatorEnum firstOperator,
            FilterOperatorEnum... operatorEnums) {
        return (Arrays.asList(operatorEnums).contains(firstOperator)
                && Objects.nonNull(expression.getFieldValue()));
    }

    private boolean hasSecondDate(List<FieldExpression> dateExpressions) {
        return dateExpressions.size() > 1
                && Objects.nonNull(dateExpressions.get(1).getFieldValue());
    }

    private Map<String, SchemaElement> getNameToElement(DataSetSchema dsSchema) {
        Set<SchemaElement> dimensions = dsSchema.getDimensions();
        Set<SchemaElement> metrics = dsSchema.getMetrics();

        List<SchemaElement> allElements = Lists.newArrayList();
        allElements.addAll(dimensions);
        allElements.addAll(metrics);
        // support alias
        return allElements.stream().flatMap(schemaElement -> {
            Set<Pair<String, SchemaElement>> result = new HashSet<>();
            result.add(Pair.of(schemaElement.getName(), schemaElement));
            List<String> aliasList = schemaElement.getAlias();
            if (!org.springframework.util.CollectionUtils.isEmpty(aliasList)) {
                for (String alias : aliasList) {
                    result.add(Pair.of(alias, schemaElement));
                }
            }
            return result.stream();
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (value1, value2) -> value2));
    }

    private void parseQueryType(SemanticParseInfo parseInfo) {
        parseInfo.setQueryType(QueryType.DETAIL);
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isBlank(sqlInfo.getCorrectedS2SQL())) {
            parseInfo.setQueryType(QueryType.DETAIL);
        }

        // 2. AGG queryType
        if (Objects.nonNull(sqlInfo) && StringUtils.isNotBlank(sqlInfo.getParsedS2SQL())
                && SqlSelectFunctionHelper.hasAggregateFunction(sqlInfo.getCorrectedS2SQL())) {
            parseInfo.setQueryType(QueryType.AGGREGATE);
        }
    }

}
