
package com.tencent.supersonic.chat.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.service.ParseInfoService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.FilterExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ParserInfoServiceImpl implements ParseInfoService {

    @Value("${candidate.top.size:5}")
    private int candidateTopSize;

    public List<SemanticParseInfo> getTopCandidateParseInfo(List<SemanticParseInfo> selectedParses,
            List<SemanticParseInfo> candidateParses) {
        if (CollectionUtils.isEmpty(selectedParses) || CollectionUtils.isEmpty(candidateParses)) {
            return candidateParses;
        }
        int selectParseSize = selectedParses.size();
        Set<Double> selectParseScoreSet = selectedParses.stream()
                .map(SemanticParseInfo::getScore).collect(Collectors.toSet());
        int candidateParseSize = candidateTopSize - selectParseSize;
        candidateParses = candidateParses.stream()
                .filter(candidateParse -> !selectParseScoreSet.contains(candidateParse.getScore()))
                .collect(Collectors.toList());
        SemanticParseInfo semanticParseInfo = selectedParses.get(0);
        Long modelId = semanticParseInfo.getModelId();
        if (modelId == null || modelId <= 0) {
            return candidateParses;
        }
        return candidateParses.stream()
                .sorted(Comparator.comparing(parse -> !parse.getModelId().equals(modelId)))
                .limit(candidateParseSize)
                .collect(Collectors.toList());
    }

    public List<SemanticParseInfo> sortParseInfo(List<SemanticQuery> semanticQueries) {
        return semanticQueries.stream()
                .map(SemanticQuery::getParseInfo)
                .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                .collect(Collectors.toList());
    }

    public void updateParseInfo(SemanticParseInfo parseInfo) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        String logicSql = sqlInfo.getCorrectS2SQL();
        if (StringUtils.isBlank(logicSql)) {
            return;
        }

        List<FilterExpression> expressions = SqlParserSelectHelper.getFilterExpression(logicSql);
        //set dataInfo
        try {
            if (!org.springframework.util.CollectionUtils.isEmpty(expressions)) {
                DateConf dateInfo = getDateInfo(expressions);
                parseInfo.setDateInfo(dateInfo);
            }
        } catch (Exception e) {
            log.error("set dateInfo error :", e);
        }

        //set filter
        try {
            Map<String, SchemaElement> fieldNameToElement = getNameToElement(parseInfo.getModelId());
            List<QueryFilter> result = getDimensionFilter(fieldNameToElement, expressions);
            parseInfo.getDimensionFilters().addAll(result);
        } catch (Exception e) {
            log.error("set dimensionFilter error :", e);
        }

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        if (Objects.isNull(semanticSchema)) {
            return;
        }
        List<String> allFields = getFieldsExceptDate(SqlParserSelectHelper.getAllFields(sqlInfo.getCorrectS2SQL()));

        Set<SchemaElement> metrics = getElements(parseInfo.getModelId(), allFields, semanticSchema.getMetrics());
        parseInfo.setMetrics(metrics);

        if (SqlParserSelectFunctionHelper.hasAggregateFunction(sqlInfo.getCorrectS2SQL())) {
            parseInfo.setNativeQuery(false);
            List<String> groupByFields = SqlParserSelectHelper.getGroupByFields(sqlInfo.getCorrectS2SQL());
            List<String> groupByDimensions = getFieldsExceptDate(groupByFields);
            parseInfo.setDimensions(
                    getElements(parseInfo.getModelId(), groupByDimensions, semanticSchema.getDimensions()));
        } else {
            parseInfo.setNativeQuery(true);
            List<String> selectFields = SqlParserSelectHelper.getSelectFields(sqlInfo.getCorrectS2SQL());
            List<String> selectDimensions = getFieldsExceptDate(selectFields);
            parseInfo.setDimensions(
                    getElements(parseInfo.getModelId(), selectDimensions, semanticSchema.getDimensions()));
        }
    }


    private Set<SchemaElement> getElements(Long modelId, List<String> allFields, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> modelId.equals(schemaElement.getModel())
                        && allFields.contains(schemaElement.getName())
                ).collect(Collectors.toSet());
    }

    private List<String> getFieldsExceptDate(List<String> allFields) {
        if (org.springframework.util.CollectionUtils.isEmpty(allFields)) {
            return new ArrayList<>();
        }
        return allFields.stream()
                .filter(entry -> !TimeDimensionEnum.DAY.getChName().equalsIgnoreCase(entry))
                .collect(Collectors.toList());
    }


    private List<QueryFilter> getDimensionFilter(Map<String, SchemaElement> fieldNameToElement,
            List<FilterExpression> filterExpressions) {
        List<QueryFilter> result = Lists.newArrayList();
        for (FilterExpression expression : filterExpressions) {
            QueryFilter dimensionFilter = new QueryFilter();
            dimensionFilter.setValue(expression.getFieldValue());
            SchemaElement schemaElement = fieldNameToElement.get(expression.getFieldName());
            if (Objects.isNull(schemaElement)) {
                continue;
            }
            dimensionFilter.setName(schemaElement.getName());
            dimensionFilter.setBizName(schemaElement.getBizName());
            dimensionFilter.setElementID(schemaElement.getId());

            FilterOperatorEnum operatorEnum = FilterOperatorEnum.getSqlOperator(expression.getOperator());
            dimensionFilter.setOperator(operatorEnum);
            dimensionFilter.setFunction(expression.getFunction());
            result.add(dimensionFilter);
        }
        return result;
    }

    private DateConf getDateInfo(List<FilterExpression> filterExpressions) {
        List<FilterExpression> dateExpressions = filterExpressions.stream()
                .filter(expression -> TimeDimensionEnum.DAY.getChName().equalsIgnoreCase(expression.getFieldName()))
                .collect(Collectors.toList());
        if (org.springframework.util.CollectionUtils.isEmpty(dateExpressions)) {
            return new DateConf();
        }
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateMode.BETWEEN);
        FilterExpression firstExpression = dateExpressions.get(0);

        FilterOperatorEnum firstOperator = FilterOperatorEnum.getSqlOperator(firstExpression.getOperator());
        if (FilterOperatorEnum.EQUALS.equals(firstOperator) && Objects.nonNull(firstExpression.getFieldValue())) {
            dateInfo.setStartDate(firstExpression.getFieldValue().toString());
            dateInfo.setEndDate(firstExpression.getFieldValue().toString());
            dateInfo.setDateMode(DateMode.BETWEEN);
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

    private boolean containOperators(FilterExpression expression, FilterOperatorEnum firstOperator,
            FilterOperatorEnum... operatorEnums) {
        return (Arrays.asList(operatorEnums).contains(firstOperator) && Objects.nonNull(expression.getFieldValue()));
    }

    private boolean hasSecondDate(List<FilterExpression> dateExpressions) {
        return dateExpressions.size() > 1 && Objects.nonNull(dateExpressions.get(1).getFieldValue());
    }

    protected Map<String, SchemaElement> getNameToElement(Long modelId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        List<SchemaElement> dimensions = semanticSchema.getDimensions();
        List<SchemaElement> metrics = semanticSchema.getMetrics();

        List<SchemaElement> allElements = Lists.newArrayList();
        allElements.addAll(dimensions);
        allElements.addAll(metrics);
        //support alias
        return allElements.stream()
                .filter(schemaElement -> schemaElement.getModel().equals(modelId))
                .flatMap(schemaElement -> {
                    Set<Pair<String, SchemaElement>> result = new HashSet<>();
                    result.add(Pair.of(schemaElement.getName(), schemaElement));
                    List<String> aliasList = schemaElement.getAlias();
                    if (!org.springframework.util.CollectionUtils.isEmpty(aliasList)) {
                        for (String alias : aliasList) {
                            result.add(Pair.of(alias, schemaElement));
                        }
                    }
                    return result.stream();
                })
                .collect(Collectors.toMap(pair -> pair.getLeft(), pair -> pair.getRight(), (value1, value2) -> value2));
    }
}
