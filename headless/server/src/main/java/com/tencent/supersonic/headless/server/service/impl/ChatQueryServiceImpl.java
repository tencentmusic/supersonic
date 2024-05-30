package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.enums.CostType;
import com.tencent.supersonic.headless.api.pojo.enums.QueryMethod;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.ExplainSqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ExplainResp;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.chat.corrector.GrammarCorrector;
import com.tencent.supersonic.headless.core.chat.corrector.SchemaCorrector;
import com.tencent.supersonic.headless.core.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.core.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.core.chat.knowledge.SearchService;
import com.tencent.supersonic.headless.core.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.core.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.core.chat.query.QueryManager;
import com.tencent.supersonic.headless.core.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.core.pojo.ChatContext;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.server.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.headless.server.service.ChatContextService;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.QueryService;
import com.tencent.supersonic.headless.server.service.WorkflowService;
import com.tencent.supersonic.headless.server.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatQueryServiceImpl implements ChatQueryService {

    @Autowired
    private SemanticService semanticService;
    @Autowired
    private ChatContextService chatContextService;
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private DataSetService dataSetService;
    @Autowired
    private WorkflowService workflowService;

    @Override
    public MapResp performMapping(QueryReq queryReq) {
        MapResp mapResp = new MapResp();
        QueryContext queryCtx = buildQueryContext(queryReq);
        ComponentFactory.getSchemaMappers().forEach(mapper -> {
            mapper.map(queryCtx);
        });
        SchemaMapInfo mapInfo = queryCtx.getMapInfo();
        mapResp.setMapInfo(mapInfo);
        mapResp.setQueryText(queryReq.getQueryText());
        return mapResp;
    }

    @Override
    public ParseResp performParsing(QueryReq queryReq) {
        ParseResp parseResult = new ParseResp(queryReq.getChatId(), queryReq.getQueryText());
        // build queryContext and chatContext
        QueryContext queryCtx = buildQueryContext(queryReq);

        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatContextService.getOrCreateContext(queryReq.getChatId());

        workflowService.startWorkflow(queryCtx, chatCtx, parseResult);

        List<SemanticParseInfo> parseInfos = queryCtx.getCandidateQueries().stream()
                .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
        parseResult.setSelectedParses(parseInfos);
        return parseResult;
    }

    public QueryContext buildQueryContext(QueryReq queryReq) {

        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        Map<Long, List<Long>> modelIdToDataSetIds = dataSetService.getModelIdToDataSetIds();
        QueryContext queryCtx = QueryContext.builder()
                .queryFilters(queryReq.getQueryFilters())
                .semanticSchema(semanticSchema)
                .candidateQueries(new ArrayList<>())
                .mapInfo(new SchemaMapInfo())
                .modelIdToDataSetIds(modelIdToDataSetIds)
                .text2SQLType(queryReq.getText2SQLType())
                .mapModeEnum(queryReq.getMapModeEnum())
                .dataSetIds(queryReq.getDataSetIds())
                .build();
        BeanUtils.copyProperties(queryReq, queryCtx);
        return queryCtx;
    }

    @Override
    public QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception {
        List<StatisticsDO> timeCostDOList = new ArrayList<>();
        SemanticParseInfo parseInfo = queryReq.getParseInfo();
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (semanticQuery == null) {
            return null;
        }
        semanticQuery.setParseInfo(parseInfo);

        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatContextService.getOrCreateContext(queryReq.getChatId());
        long startTime = System.currentTimeMillis();
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        QueryResult queryResult = doExecution(semanticQueryReq, parseInfo, queryReq.getUser());
        timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                .interfaceName(semanticQuery.getClass().getSimpleName()).type(CostType.QUERY.getType()).build());
        queryResult.setQueryTimeCost(timeCostDOList.get(0).getCost().longValue());
        queryResult.setChatContext(parseInfo);
        // update chat context after a successful semantic query
        if (QueryState.SUCCESS.equals(queryResult.getQueryState()) && queryReq.isSaveAnswer()) {
            chatCtx.setParseInfo(parseInfo);
            chatContextService.updateContext(chatCtx);
        }
        chatCtx.setQueryText(queryReq.getQueryText());
        chatCtx.setUser(queryReq.getUser().getName());
        return queryResult;
    }

    private QueryResult doExecution(SemanticQueryReq semanticQueryReq,
                                    SemanticParseInfo parseInfo, User user) throws Exception {
        SemanticQueryResp queryResp = queryService.queryByReq(semanticQueryReq, user);
        QueryResult queryResult = new QueryResult();
        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }

        String sql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>()
                : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(sql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(parseInfo.getQueryMode());
        queryResult.setQueryState(QueryState.SUCCESS);

        return queryResult;
    }

    @Override
    public SemanticParseInfo queryContext(Integer chatId) {
        ChatContext context = chatContextService.getOrCreateContext(chatId);
        return context.getParseInfo();
    }

    //mainly used for executing after revising filters,for example:"fans_cnt>=100000"->"fans_cnt>500000",
    //"style='流行'"->"style in ['流行','爱国']"
    @Override
    public QueryResult executeDirectQuery(QueryDataReq queryData, User user) throws Exception {
        SemanticParseInfo parseInfo = getSemanticParseInfo(queryData);
        SemanticSchema semanticSchema = semanticService.getSemanticSchema();

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        semanticQuery.setParseInfo(parseInfo);

        List<String> fields = new ArrayList<>();
        if (Objects.nonNull(parseInfo.getSqlInfo())
                && StringUtils.isNotBlank(parseInfo.getSqlInfo().getCorrectS2SQL())) {
            String correctorSql = parseInfo.getSqlInfo().getCorrectS2SQL();
            fields = SqlSelectHelper.getAllFields(correctorSql);
        }
        if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())
                && checkMetricReplace(fields, queryData.getMetrics())) {
            //replace metrics
            log.info("llm begin replace metrics!");
            SchemaElement metricToReplace = queryData.getMetrics().iterator().next();
            replaceMetrics(parseInfo, metricToReplace);
        } else if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())) {
            log.info("llm begin revise filters!");
            String correctorSql = reviseCorrectS2SQL(queryData, parseInfo);
            parseInfo.getSqlInfo().setCorrectS2SQL(correctorSql);
            semanticQuery.setParseInfo(parseInfo);
            SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
            ExplainSqlReq<Object> explainSqlReq = ExplainSqlReq.builder().queryReq(semanticQueryReq)
                    .queryTypeEnum(QueryMethod.SQL).build();
            ExplainResp explain = queryService.explain(explainSqlReq, user);
            if (StringUtils.isNotBlank(explain.getSql())) {
                parseInfo.getSqlInfo().setQuerySQL(explain.getSql());
            }
        } else {
            log.info("rule begin replace metrics and revise filters!");
            //remove unvalid filters
            validFilter(semanticQuery.getParseInfo().getDimensionFilters());
            validFilter(semanticQuery.getParseInfo().getMetricFilters());
            //init s2sql
            semanticQuery.initS2Sql(semanticSchema, user);
            QueryReq queryReq = new QueryReq();
            queryReq.setQueryFilters(new QueryFilters());
            queryReq.setUser(user);
        }
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        QueryResult queryResult = doExecution(semanticQueryReq, semanticQuery.getParseInfo(), user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        DataSetSchema dataSetSchema = semanticSchema.getDataSetSchemaMap().get(parseInfo.getDataSetId());
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, dataSetSchema, user);
        queryResult.setEntityInfo(entityInfo);
        return queryResult;
    }

    private boolean checkMetricReplace(List<String> oriFields, Set<SchemaElement> metrics) {
        if (CollectionUtils.isEmpty(oriFields)) {
            return false;
        }
        if (CollectionUtils.isEmpty(metrics)) {
            return false;
        }
        List<String> metricNames = metrics.stream().map(SchemaElement::getName).collect(Collectors.toList());
        return !oriFields.containsAll(metricNames);
    }

    public String reviseCorrectS2SQL(QueryDataReq queryData, SemanticParseInfo parseInfo) {
        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
        Map<String, Map<String, String>> havingFiledNameToValueMap = new HashMap<>();

        String correctorSql = parseInfo.getSqlInfo().getCorrectS2SQL();
        log.info("correctorSql before replacing:{}", correctorSql);
        // get where filter and having filter
        List<FieldExpression> whereExpressionList = SqlSelectHelper.getWhereExpressions(correctorSql);
        List<FieldExpression> havingExpressionList = SqlSelectHelper.getHavingExpressions(correctorSql);
        List<Expression> addWhereConditions = new ArrayList<>();
        List<Expression> addHavingConditions = new ArrayList<>();
        Set<String> removeWhereFieldNames = new HashSet<>();
        Set<String> removeHavingFieldNames = new HashSet<>();
        // replace where filter
        updateFilters(whereExpressionList, queryData.getDimensionFilters(),
                parseInfo.getDimensionFilters(), addWhereConditions, removeWhereFieldNames);
        updateDateInfo(queryData, parseInfo, filedNameToValueMap,
                whereExpressionList, addWhereConditions, removeWhereFieldNames);
        correctorSql = SqlReplaceHelper.replaceValue(correctorSql, filedNameToValueMap);
        correctorSql = SqlRemoveHelper.removeWhereCondition(correctorSql, removeWhereFieldNames);
        // replace having filter
        updateFilters(havingExpressionList, queryData.getDimensionFilters(),
                parseInfo.getDimensionFilters(), addHavingConditions, removeHavingFieldNames);
        correctorSql = SqlReplaceHelper.replaceHavingValue(correctorSql, havingFiledNameToValueMap);
        correctorSql = SqlRemoveHelper.removeHavingCondition(correctorSql, removeHavingFieldNames);

        correctorSql = SqlAddHelper.addWhere(correctorSql, addWhereConditions);
        correctorSql = SqlAddHelper.addHaving(correctorSql, addHavingConditions);
        log.info("correctorSql after replacing:{}", correctorSql);
        return correctorSql;
    }

    private void replaceMetrics(SemanticParseInfo parseInfo, SchemaElement metric) {
        List<String> oriMetrics = parseInfo.getMetrics().stream()
                .map(SchemaElement::getName).collect(Collectors.toList());
        String correctorSql = parseInfo.getSqlInfo().getCorrectS2SQL();
        log.info("before replaceMetrics:{}", correctorSql);
        log.info("filteredMetrics:{},metrics:{}", oriMetrics, metric);
        Map<String, Pair<String, String>> fieldMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(oriMetrics) && !oriMetrics.contains(metric.getName())) {
            fieldMap.put(oriMetrics.get(0), Pair.of(metric.getName(), metric.getDefaultAgg()));
            correctorSql = SqlReplaceHelper.replaceAggFields(correctorSql, fieldMap);
        }
        log.info("after replaceMetrics:{}", correctorSql);
        parseInfo.getSqlInfo().setCorrectS2SQL(correctorSql);
    }

    @Override
    public EntityInfo getEntityInfo(SemanticParseInfo parseInfo, User user) {
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        DataSetSchema dataSetSchema =
                semanticService.getSemanticSchema().getDataSetSchemaMap().get(parseInfo.getDataSetId());
        return semanticService.getEntityInfo(parseInfo, dataSetSchema, user);
    }

    private void updateDateInfo(QueryDataReq queryData, SemanticParseInfo parseInfo,
                                Map<String, Map<String, String>> filedNameToValueMap,
                                List<FieldExpression> fieldExpressionList,
                                List<Expression> addConditions,
                                Set<String> removeFieldNames) {
        if (Objects.isNull(queryData.getDateInfo())) {
            return;
        }
        if (queryData.getDateInfo().getUnit() > 1) {
            queryData.getDateInfo().setStartDate(DateUtils.getBeforeDate(queryData.getDateInfo().getUnit() + 1));
            queryData.getDateInfo().setEndDate(DateUtils.getBeforeDate(1));
        }
        // startDate equals to endDate
        for (FieldExpression fieldExpression : fieldExpressionList) {
            if (TimeDimensionEnum.DAY.getChName().equals(fieldExpression.getFieldName())) {
                // first remove,then add
                removeFieldNames.add(TimeDimensionEnum.DAY.getChName());
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                addTimeFilters(queryData.getDateInfo().getStartDate(), greaterThanEquals, addConditions);
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                addTimeFilters(queryData.getDateInfo().getEndDate(), minorThanEquals, addConditions);
                break;
            }
        }
        for (FieldExpression fieldExpression : fieldExpressionList) {
            for (QueryFilter queryFilter : queryData.getDimensionFilters()) {
                if (queryFilter.getOperator().equals(FilterOperatorEnum.LIKE)
                        && FilterOperatorEnum.LIKE.getValue().toLowerCase().equals(
                                fieldExpression.getOperator().toLowerCase())) {
                    Map<String, String> replaceMap = new HashMap<>();
                    String preValue = fieldExpression.getFieldValue().toString();
                    String curValue = queryFilter.getValue().toString();
                    if (preValue.startsWith("%")) {
                        curValue = "%" + curValue;
                    }
                    if (preValue.endsWith("%")) {
                        curValue = curValue + "%";
                    }
                    replaceMap.put(preValue, curValue);
                    filedNameToValueMap.put(fieldExpression.getFieldName(), replaceMap);
                    break;
                }
            }
        }
        parseInfo.setDateInfo(queryData.getDateInfo());
    }

    private <T extends ComparisonOperator> void addTimeFilters(String date,
                                                               T comparisonExpression,
                                                               List<Expression> addConditions) {
        Column column = new Column(TimeDimensionEnum.DAY.getChName());
        StringValue stringValue = new StringValue(date);
        comparisonExpression.setLeftExpression(column);
        comparisonExpression.setRightExpression(stringValue);
        addConditions.add(comparisonExpression);
    }

    private void updateFilters(List<FieldExpression> fieldExpressionList,
                               Set<QueryFilter> metricFilters,
                               Set<QueryFilter> contextMetricFilters,
                               List<Expression> addConditions,
                               Set<String> removeFieldNames) {
        if (CollectionUtils.isEmpty(metricFilters)) {
            return;
        }
        for (QueryFilter dslQueryFilter : metricFilters) {
            for (FieldExpression fieldExpression : fieldExpressionList) {
                if (fieldExpression.getFieldName() != null
                        && fieldExpression.getFieldName().contains(dslQueryFilter.getName())) {
                    removeFieldNames.add(dslQueryFilter.getName());
                    if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                        EqualsTo equalsTo = new EqualsTo();
                        addWhereFilters(dslQueryFilter, equalsTo, contextMetricFilters, addConditions);
                    } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.GREATER_THAN_EQUALS)) {
                        GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                        addWhereFilters(dslQueryFilter, greaterThanEquals, contextMetricFilters, addConditions);
                    } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.GREATER_THAN)) {
                        GreaterThan greaterThan = new GreaterThan();
                        addWhereFilters(dslQueryFilter, greaterThan, contextMetricFilters, addConditions);
                    } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.MINOR_THAN_EQUALS)) {
                        MinorThanEquals minorThanEquals = new MinorThanEquals();
                        addWhereFilters(dslQueryFilter, minorThanEquals, contextMetricFilters, addConditions);
                    } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.MINOR_THAN)) {
                        MinorThan minorThan = new MinorThan();
                        addWhereFilters(dslQueryFilter, minorThan, contextMetricFilters, addConditions);
                    } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.IN)) {
                        InExpression inExpression = new InExpression();
                        addWhereInFilters(dslQueryFilter, inExpression, contextMetricFilters, addConditions);
                    }
                    break;
                }
            }
        }
    }

    // add in condition to sql where  condition
    private void addWhereInFilters(QueryFilter dslQueryFilter,
                                   InExpression inExpression,
                                   Set<QueryFilter> contextMetricFilters,
                                   List<Expression> addConditions) {
        Column column = new Column(dslQueryFilter.getName());
        ParenthesedExpressionList parenthesedExpressionList = new ParenthesedExpressionList<>();
        List<String> valueList = JsonUtil.toList(
                JsonUtil.toString(dslQueryFilter.getValue()), String.class);
        if (CollectionUtils.isEmpty(valueList)) {
            return;
        }
        valueList.stream().forEach(o -> {
            StringValue stringValue = new StringValue(o);
            parenthesedExpressionList.add(stringValue);
        });
        inExpression.setLeftExpression(column);
        inExpression.setRightExpression(parenthesedExpressionList);
        addConditions.add(inExpression);
        contextMetricFilters.stream().forEach(o -> {
            if (o.getName().equals(dslQueryFilter.getName())) {
                o.setValue(dslQueryFilter.getValue());
                o.setOperator(dslQueryFilter.getOperator());
            }
        });
    }

    // add where filter
    private <T extends ComparisonOperator> void addWhereFilters(QueryFilter dslQueryFilter,
                                                                T comparisonExpression,
                                                                Set<QueryFilter> contextMetricFilters,
                                                                List<Expression> addConditions) {
        String columnName = dslQueryFilter.getName();
        if (StringUtils.isNotBlank(dslQueryFilter.getFunction())) {
            columnName = dslQueryFilter.getFunction() + "(" + dslQueryFilter.getName() + ")";
        }
        if (Objects.isNull(dslQueryFilter.getValue())) {
            return;
        }
        Column column = new Column(columnName);
        comparisonExpression.setLeftExpression(column);
        if (StringUtils.isNumeric(dslQueryFilter.getValue().toString())) {
            LongValue longValue = new LongValue(Long.parseLong(dslQueryFilter.getValue().toString()));
            comparisonExpression.setRightExpression(longValue);
        } else {
            StringValue stringValue = new StringValue(dslQueryFilter.getValue().toString());
            comparisonExpression.setRightExpression(stringValue);
        }
        addConditions.add(comparisonExpression);
        contextMetricFilters.stream().forEach(o -> {
            if (o.getName().equals(dslQueryFilter.getName())) {
                o.setValue(dslQueryFilter.getValue());
                o.setOperator(dslQueryFilter.getOperator());
            }
        });
    }

    private SemanticParseInfo getSemanticParseInfo(QueryDataReq queryData) {
        SemanticParseInfo parseInfo = queryData.getParseInfo();
        if (LLMSqlQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            return parseInfo;
        }
        if (CollectionUtils.isNotEmpty(queryData.getDimensions())) {
            parseInfo.setDimensions(queryData.getDimensions());
        }
        if (CollectionUtils.isNotEmpty(queryData.getMetrics())) {
            parseInfo.setMetrics(queryData.getMetrics());
        }
        if (CollectionUtils.isNotEmpty(queryData.getDimensionFilters())) {
            parseInfo.setDimensionFilters(queryData.getDimensionFilters());
        }
        if (CollectionUtils.isNotEmpty(queryData.getMetricFilters())) {
            parseInfo.setMetricFilters(queryData.getMetricFilters());
        }
        if (Objects.nonNull(queryData.getDateInfo())) {
            parseInfo.setDateInfo(queryData.getDateInfo());
        }
        return parseInfo;
    }

    private void validFilter(Set<QueryFilter> filters) {
        for (QueryFilter queryFilter : filters) {
            if (Objects.isNull(queryFilter.getValue())) {
                filters.remove(queryFilter);
            }
            if (queryFilter.getOperator().equals(FilterOperatorEnum.IN) && CollectionUtils.isEmpty(
                    JsonUtil.toList(JsonUtil.toString(queryFilter.getValue()), String.class))) {
                filters.remove(queryFilter);
            }
        }
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception {
        SemanticQueryResp semanticQueryResp = new SemanticQueryResp();
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        SchemaElement schemaElement = semanticSchema.getDimension(dimensionValueReq.getElementID());
        Set<Long> detectDataSetIds = new HashSet<>();
        detectDataSetIds.add(schemaElement.getDataSet());
        dimensionValueReq.setModelId(schemaElement.getModel());
        List<String> dimensionValues = getDimensionValues(dimensionValueReq, detectDataSetIds);
        // if the search results is null,search dimensionValue from database
        if (CollectionUtils.isEmpty(dimensionValues)) {
            semanticQueryResp = queryDatabase(dimensionValueReq, user);
            return semanticQueryResp;
        }
        List<QueryColumn> columns = new ArrayList<>();
        QueryColumn queryColumn = new QueryColumn();
        queryColumn.setNameEn(dimensionValueReq.getBizName());
        queryColumn.setShowType("CATEGORY");
        queryColumn.setAuthorized(true);
        queryColumn.setType("CHAR");
        columns.add(queryColumn);
        List<Map<String, Object>> resultList = new ArrayList<>();
        dimensionValues.stream().forEach(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put(dimensionValueReq.getBizName(), o);
            resultList.add(map);
        });
        semanticQueryResp.setColumns(columns);
        semanticQueryResp.setResultList(resultList);
        return semanticQueryResp;
    }

    private List<String> getDimensionValues(DimensionValueReq dimensionValueReq, Set<Long> dataSetIds) {
        //if value is null ,then search from NATURE_TO_VALUES
        if (StringUtils.isBlank(dimensionValueReq.getValue())) {
            return SearchService.getDimensionValue(dimensionValueReq);
        }
        Map<Long, List<Long>> modelIdToDataSetIds = new HashMap<>();
        modelIdToDataSetIds.put(dimensionValueReq.getModelId(), new ArrayList<>(dataSetIds));
        //search from prefixSearch
        List<HanlpMapResult> hanlpMapResultList = knowledgeBaseService.prefixSearch(dimensionValueReq.getValue(),
                2000, modelIdToDataSetIds, dataSetIds);
        HanlpHelper.transLetterOriginal(hanlpMapResultList);
        return hanlpMapResultList.stream()
                .filter(o -> {
                    for (String nature : o.getNatures()) {
                        Long elementID = NatureHelper.getElementID(nature);
                        if (dimensionValueReq.getElementID().equals(elementID)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(mapResult -> mapResult.getName())
                .collect(Collectors.toList());
    }

    private SemanticQueryResp queryDatabase(DimensionValueReq dimensionValueReq, User user) throws Exception {
        QueryStructReq queryStructReq = new QueryStructReq();

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.RECENT);
        dateConf.setUnit(1);
        dateConf.setPeriod("DAY");
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setLimit(20L);
        queryStructReq.setDataSetId(dimensionValueReq.getModelId());
        queryStructReq.setQueryType(QueryType.ID);
        List<String> groups = new ArrayList<>();
        groups.add(dimensionValueReq.getBizName());
        queryStructReq.setGroups(groups);
        return queryService.queryByReq(queryStructReq, user);
    }

    public void correct(QuerySqlReq querySqlReq, User user) {
        QueryContext queryCtx = new QueryContext();
        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        queryCtx.setSemanticSchema(semanticSchema);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SqlInfo sqlInfo = new SqlInfo();
        sqlInfo.setCorrectS2SQL(querySqlReq.getSql());
        sqlInfo.setS2SQL(querySqlReq.getSql());
        semanticParseInfo.setSqlInfo(sqlInfo);
        semanticParseInfo.setQueryType(QueryType.DETAIL);

        Long dataSetId = querySqlReq.getDataSetId();
        if (Objects.isNull(dataSetId)) {
            dataSetId = dataSetService.getDataSetIdFromSql(querySqlReq.getSql(), user);
        }
        SchemaElement dataSet = semanticSchema.getDataSet(dataSetId);
        semanticParseInfo.setDataSet(dataSet);

        ComponentFactory.getSemanticCorrectors().forEach(corrector -> {
            if (!(corrector instanceof GrammarCorrector || (corrector instanceof SchemaCorrector))) {
                corrector.correct(queryCtx, semanticParseInfo);
            }
        });
        querySqlReq.setSql(sqlInfo.getCorrectS2SQL());
    }

}
