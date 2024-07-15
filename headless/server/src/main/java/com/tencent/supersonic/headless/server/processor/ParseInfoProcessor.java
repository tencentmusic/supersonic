package com.tencent.supersonic.headless.server.processor;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.server.web.service.SchemaService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

/**
 * ParseInfoProcessor extracts structured info from S2SQL so that
 * users get to know the details.
 **/
@Slf4j
public class ParseInfoProcessor implements ResultProcessor {

    @Override
    public void process(ParseResp parseResp, ChatQueryContext chatQueryContext, ChatContext chatContext) {
        List<SemanticQuery> candidateQueries = chatQueryContext.getCandidateQueries();
        if (CollectionUtils.isEmpty(candidateQueries)) {
            return;
        }
        List<SemanticParseInfo> candidateParses = candidateQueries.stream()
                .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
        if (!candidateQueries.isEmpty()) {
            candidateQueries.get(0).getParseInfo().setRecommendParse(true);
        }
        candidateParses.forEach(this::updateParseInfo);
    }

    public void updateParseInfo(SemanticParseInfo parseInfo) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        String correctS2SQL = sqlInfo.getCorrectedS2SQL();
        if (StringUtils.isBlank(correctS2SQL)) {
            return;
        }
        List<FieldExpression> expressions = SqlSelectHelper.getFilterExpression(correctS2SQL);
        //set dataInfo
        setDataInfo(parseInfo, expressions);
        //set filter
        setFilter(parseInfo, expressions);
        //set dimensions
        setDimensions(parseInfo, sqlInfo);
        //set metrics
        setMetrics(parseInfo, sqlInfo);
        //set limit
        setLimit(parseInfo, sqlInfo);
    }

    private void setLimit(SemanticParseInfo parseInfo, SqlInfo sqlInfo) {
        Limit limit = SqlSelectHelper.getLimit(sqlInfo.getCorrectedS2SQL());
        if (!Objects.isNull(limit)) {
            parseInfo.setLimit(Long.parseLong(limit.getRowCount().toString()));
        }
    }

    private void setDimensions(SemanticParseInfo parseInfo, SqlInfo sqlInfo) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        if (Objects.isNull(semanticSchema)) {
            return;
        }

        List<String> groupByFields = SqlSelectHelper.getGroupByFields(sqlInfo.getCorrectedS2SQL());
        List<String> groupByDimensions = getFieldsExceptDate(groupByFields);
        List<SchemaElement> dimensions = getAllFields(semanticSchema, parseInfo.getDataSetId());
        parseInfo.setDimensions(getDimensionsElements(groupByDimensions, dimensions));
    }

    private void setMetrics(SemanticParseInfo parseInfo, SqlInfo sqlInfo) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        if (Objects.isNull(semanticSchema)) {
            return;
        }
        Select selectStatement = SqlSelectHelper.getSelect(sqlInfo.getCorrectedS2SQL());
        if (!(selectStatement instanceof PlainSelect)) {
            return;
        }

        List<SelectItem<?>> selectItems = ((PlainSelect) selectStatement).getSelectItems();
        List<SelectItem<?>> functionList = selectItems.stream()
                .filter(s -> s.getExpression() instanceof Function)
                .collect(Collectors.toList());
        List<SchemaElement> allFields = getAllFields(semanticSchema, parseInfo.getDataSetId());
        Set<SchemaElement> metrics = getMetricsElements(functionList, allFields);
        parseInfo.setMetrics(metrics);
    }

    private static List<SchemaElement> getAllFields(SemanticSchema semanticSchema, Long dataSetId) {
        ArrayList<SchemaElement> schemaElements = new ArrayList<>();
        schemaElements.addAll(semanticSchema.getMetrics());
        schemaElements.addAll(semanticSchema.getDimensions());
        return schemaElements.stream()
                .filter(e -> Objects.equals(e.getDataSet(), dataSetId) || e.getDataSet() == 0L)
                .collect(Collectors.toList());
    }

    private void setFilter(SemanticParseInfo parseInfo, List<FieldExpression> expressions) {
        try {
            Map<String, SchemaElement> fieldNameToElement = getNameToElement(parseInfo.getDataSetId());
            List<QueryFilter> result = getDimensionFilter(fieldNameToElement, expressions);
            parseInfo.getDimensionFilters().addAll(result);
        } catch (Exception e) {
            log.error("set dimensionFilter error :", e);
        }
    }

    private void setDataInfo(SemanticParseInfo parseInfo, List<FieldExpression> expressions) {
        try {
            if (!org.apache.commons.collections.CollectionUtils.isEmpty(expressions)) {
                DateConf dateInfo = getDateInfo(expressions);
                if (dateInfo != null && parseInfo.getDateInfo() == null) {
                    parseInfo.setDateInfo(dateInfo);
                }
            }
        } catch (Exception e) {
            log.error("set dateInfo error :", e);
        }
    }

    private Set<SchemaElement> getMetricsElements(List<SelectItem<?>> functionList, List<SchemaElement> elements) {
        HashSet<SchemaElement> metricsElements = new HashSet<>();
        for (SelectItem<?> selectItem : functionList) {
            Function function = (Function) selectItem.getExpression();
            for (SchemaElement element : elements) {
                if (element.getName().equals(function.getParameters().get(0).toString())
                        || element.getAlias().contains(function.getParameters().get(0).toString())) {
                    element.setAggregator(AggOperatorEnum.of(function.getName()).name());
                    metricsElements.add(element);
                    break;
                }
            }
        }
        return metricsElements;
    }

    private Set<SchemaElement> getDimensionsElements(List<String> allFields, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> {
                            if (CollectionUtils.isEmpty(schemaElement.getAlias())) {
                                return allFields.contains(schemaElement.getName());
                            }
                            Set<String> allFieldsSet = new HashSet<>(allFields);
                            Set<String> aliasSet = new HashSet<>(schemaElement.getAlias());
                            List<String> intersection = allFieldsSet.stream()
                                    .filter(aliasSet::contains).collect(Collectors.toList());
                            return (allFields.contains(schemaElement.getName())
                                    || !CollectionUtils.isEmpty(intersection));
                        }
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
            List<FieldExpression> fieldExpressions) {
        List<QueryFilter> result = Lists.newArrayList();
        for (FieldExpression expression : fieldExpressions) {
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

    private DateConf getDateInfo(List<FieldExpression> fieldExpressions) {
        List<FieldExpression> dateExpressions = fieldExpressions.stream()
                .filter(expression -> TimeDimensionEnum.DAY.getChName().equalsIgnoreCase(expression.getFieldName()))
                .collect(Collectors.toList());
        if (org.apache.commons.collections.CollectionUtils.isEmpty(dateExpressions)) {
            return null;
        }
        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
        FieldExpression firstExpression = dateExpressions.get(0);

        FilterOperatorEnum firstOperator = FilterOperatorEnum.getSqlOperator(firstExpression.getOperator());
        if (FilterOperatorEnum.EQUALS.equals(firstOperator) && Objects.nonNull(firstExpression.getFieldValue())) {
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

    private boolean containOperators(FieldExpression expression, FilterOperatorEnum firstOperator,
            FilterOperatorEnum... operatorEnums) {
        return (Arrays.asList(operatorEnums).contains(firstOperator) && Objects.nonNull(
                expression.getFieldValue()));
    }

    private boolean hasSecondDate(List<FieldExpression> dateExpressions) {
        return dateExpressions.size() > 1 && Objects.nonNull(dateExpressions.get(1).getFieldValue());
    }

    protected Map<String, SchemaElement> getNameToElement(Long dataSetId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        List<SchemaElement> dimensions = semanticSchema.getDimensions(dataSetId);
        List<SchemaElement> metrics = semanticSchema.getMetrics(dataSetId);

        List<SchemaElement> allElements = Lists.newArrayList();
        allElements.addAll(dimensions);
        allElements.addAll(metrics);
        //support alias
        return allElements.stream()
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
                .collect(Collectors.toMap(pair -> pair.getLeft(), pair -> pair.getRight(),
                        (value1, value2) -> value2));
    }

}