package com.tencent.supersonic.chat.service.impl;


import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.request.SolvedQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.CostType;
import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.processor.ParseResultProcessor;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.query.QueryResponder;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.service.StatisticsService;
import com.tencent.supersonic.chat.service.TimeCost;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.SolvedQueryManager;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.QueryType;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserRemoveHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.dictionary.HanlpMapResult;
import com.tencent.supersonic.knowledge.dictionary.MultiCustomDictionary;
import com.tencent.supersonic.knowledge.service.SearchService;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import com.tencent.supersonic.knowledge.utils.NatureHelper;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Component("chatQueryService")
@Primary
@Slf4j
public class QueryServiceImpl implements QueryService {

    @Autowired
    private ChatService chatService;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private SolvedQueryManager solvedQueryManager;

    @Value("${time.threshold: 100}")
    private Integer timeThreshold;

    private List<SchemaMapper> schemaMappers = ComponentFactory.getSchemaMappers();
    private List<SemanticParser> semanticParsers = ComponentFactory.getSemanticParsers();
    private List<ParseResultProcessor> responseProcessors = ComponentFactory.getPostProcessors();
    private List<QueryResponder> executeResponders = ComponentFactory.getExecuteResponders();
    private List<SemanticCorrector> semanticCorrectors = ComponentFactory.getSemanticCorrectors();

    @Override
    public ParseResp performParsing(QueryReq queryReq) {
        ParseResp parseResult = new ParseResp();
        // build queryContext and chatContext
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());
        List<StatisticsDO> timeCostDOList = new ArrayList<>();

        // 1. mapper
        schemaMappers.forEach(mapper -> {
            long startTime = System.currentTimeMillis();
            mapper.map(queryCtx);
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(mapper.getClass().getSimpleName()).type(CostType.MAPPER.getType()).build());
        });

        // 2. parser
        semanticParsers.forEach(parser -> {
            long startTime = System.currentTimeMillis();
            parser.parse(queryCtx, chatCtx);
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(parser.getClass().getSimpleName()).type(CostType.PARSER.getType()).build());
            log.info("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        // 3. corrector
        List<SemanticQuery> candidateQueries = queryCtx.getCandidateQueries();
        if (CollectionUtils.isNotEmpty(candidateQueries)) {
            for (SemanticQuery semanticQuery : candidateQueries) {
                // the rules are not being corrected.
                if (semanticQuery instanceof RuleSemanticQuery) {
                    continue;
                }
                semanticCorrectors.forEach(correction -> {
                    correction.correct(queryReq, semanticQuery.getParseInfo());
                });
            }
        }

        // 4. processor
        responseProcessors.forEach(processor -> {
            long startTime = System.currentTimeMillis();
            processor.process(parseResult, queryCtx, chatCtx);
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(processor.getClass().getSimpleName())
                    .type(CostType.PROCESSOR.getType()).build());
            log.info("{} result:{}", processor.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        if (Objects.nonNull(parseResult.getQueryId()) && timeCostDOList.size() > 0) {
            saveTimeCostInfo(timeCostDOList, queryReq.getQueryText(), parseResult.getQueryId(),
                    queryReq.getUser().getName(), queryReq.getChatId().longValue());
        }
        return parseResult;
    }

    @Override
    @TimeCost
    public QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception {
        ChatParseDO chatParseDO = chatService.getParseInfo(queryReq.getQueryId(), queryReq.getParseId());
        ChatQueryDO chatQueryDO = chatService.getLastQuery(queryReq.getChatId());
        List<StatisticsDO> timeCostDOList = new ArrayList<>();
        SemanticParseInfo parseInfo = JsonUtil.toObject(chatParseDO.getParseInfo(), SemanticParseInfo.class);
        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        if (semanticQuery == null) {
            return null;
        }
        semanticQuery.setParseInfo(parseInfo);

        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());
        chatCtx.setAgentId(queryReq.getAgentId());
        Long startTime = System.currentTimeMillis();
        QueryResult queryResult = semanticQuery.execute(queryReq.getUser());

        if (queryResult != null) {
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(semanticQuery.getClass().getSimpleName()).type(CostType.QUERY.getType()).build());
            queryResult.setQueryTimeCost(timeCostDOList.get(0).getCost().longValue());
            saveTimeCostInfo(timeCostDOList, queryReq.getQueryText(), queryReq.getQueryId(),
                    queryReq.getUser().getName(), queryReq.getChatId().longValue());
            queryResult.setChatContext(parseInfo);
            // update chat context after a successful semantic query
            if (queryReq.isSaveAnswer() && QueryState.SUCCESS.equals(queryResult.getQueryState())) {
                chatCtx.setParseInfo(parseInfo);
                chatService.updateContext(chatCtx);
                saveSolvedQuery(queryReq, parseInfo, chatQueryDO, queryResult);
            }
            chatCtx.setQueryText(queryReq.getQueryText());
            chatCtx.setUser(queryReq.getUser().getName());
            for (QueryResponder executeResponder : executeResponders) {
                executeResponder.fillInfo(queryResult, parseInfo, queryReq);
            }
            chatService.updateQuery(queryReq.getQueryId(), queryResult, chatCtx);
        } else {
            chatService.deleteChatQuery(queryReq.getQueryId());
        }
        return queryResult;
    }

    // save time cost data
    private void saveTimeCostInfo(List<StatisticsDO> timeCostDOList,
            String queryText, Long queryId,
            String userName, Long chatId) {
        List<StatisticsDO> list = timeCostDOList.stream()
                .filter(o -> o.getCost() > timeThreshold).collect(Collectors.toList());
        list.forEach(o -> {
            o.setQueryText(queryText);
            o.setQuestionId(queryId);
            o.setUserName(userName);
            o.setChatId(chatId);
            o.setCreateTime(new java.util.Date());
        });
        if (list.size() > 0) {
            log.info("filterStatistics size:{},data:{}", list.size(), JsonUtil.toString(list));
            statisticsService.batchSaveStatistics(list);
        }
    }

    private void saveSolvedQuery(ExecuteQueryReq queryReq, SemanticParseInfo parseInfo,
            ChatQueryDO chatQueryDO, QueryResult queryResult) {
        if (queryResult.getResponse() == null && CollectionUtils.isEmpty(queryResult.getQueryResults())) {
            return;
        }
        solvedQueryManager.saveSolvedQuery(SolvedQueryReq.builder().parseId(queryReq.getParseId())
                .queryId(queryReq.getQueryId())
                .agentId(chatQueryDO.getAgentId())
                .modelId(parseInfo.getModelClusterKey())
                .queryText(queryReq.getQueryText()).build());
    }

    @Override
    public SemanticParseInfo queryContext(QueryReq queryCtx) {
        ChatContext context = chatService.getOrCreateContext(queryCtx.getChatId());
        return context.getParseInfo();
    }

    //mainly used for executing after revising filters,for example:"fans_cnt>=100000"->"fans_cnt>500000",
    //"style='流行'"->"style in ['流行','爱国']"
    @Override
    @TimeCost
    public QueryResult executeDirectQuery(QueryDataReq queryData, User user) throws SqlParseException {
        ChatParseDO chatParseDO = chatService.getParseInfo(queryData.getQueryId(),
                queryData.getParseId());
        SemanticParseInfo parseInfo = getSemanticParseInfo(queryData, chatParseDO);

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        semanticQuery.setParseInfo(parseInfo);
        List<String> metrics = queryData.getMetrics().stream().map(o -> o.getName()).collect(Collectors.toList());
        List<String> fields = new ArrayList<>();
        if (Objects.nonNull(parseInfo.getSqlInfo())
                && StringUtils.isNotBlank(parseInfo.getSqlInfo().getCorrectS2SQL())) {
            String correctorSql = parseInfo.getSqlInfo().getCorrectS2SQL();
            fields = SqlParserSelectHelper.getAllFields(correctorSql);
        }
        if (CollectionUtils.isNotEmpty(fields) && !fields.containsAll(metrics)
                && CollectionUtils.isNotEmpty(queryData.getMetrics())) {
            //replace metrics
            log.info("llm begin replace metrics!");
            replaceMetrics(parseInfo, metrics);
        } else if (LLMSqlQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            log.info("llm begin revise filters!");
            String correctorSql = reviseCorrectS2SQL(queryData, parseInfo);
            parseInfo.getSqlInfo().setCorrectS2SQL(correctorSql);
            semanticQuery.setParseInfo(parseInfo);
            String explainSql = semanticQuery.explain(user);
            if (StringUtils.isNotBlank(explainSql)) {
                parseInfo.getSqlInfo().setQuerySQL(explainSql);
            }
        } else {
            log.info("rule begin replace metrics and revise filters!");
            //remove unvalid filters
            validFilter(semanticQuery.getParseInfo().getDimensionFilters());
            validFilter(semanticQuery.getParseInfo().getMetricFilters());
            //init s2sql
            semanticQuery.initS2Sql(user);
            QueryReq queryReq = new QueryReq();
            queryReq.setQueryFilters(new QueryFilters());
            queryReq.setUser(user);
        }
        QueryResult queryResult = semanticQuery.execute(user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, user);
        queryResult.setEntityInfo(entityInfo);
        return queryResult;
    }

    public String reviseCorrectS2SQL(QueryDataReq queryData, SemanticParseInfo parseInfo) {
        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
        Map<String, Map<String, String>> havingFiledNameToValueMap = new HashMap<>();

        String correctorSql = parseInfo.getSqlInfo().getCorrectS2SQL();
        log.info("correctorSql before replacing:{}", correctorSql);
        // get where filter and having filter
        List<FieldExpression> whereExpressionList = SqlParserSelectHelper.getWhereExpressions(correctorSql);
        List<FieldExpression> havingExpressionList = SqlParserSelectHelper.getHavingExpressions(correctorSql);
        List<Expression> addWhereConditions = new ArrayList<>();
        List<Expression> addHavingConditions = new ArrayList<>();
        Set<String> removeWhereFieldNames = new HashSet<>();
        Set<String> removeHavingFieldNames = new HashSet<>();
        // replace where filter
        updateFilters(whereExpressionList, queryData.getDimensionFilters(),
                parseInfo.getDimensionFilters(), addWhereConditions, removeWhereFieldNames);
        updateDateInfo(queryData, parseInfo, filedNameToValueMap,
                whereExpressionList, addWhereConditions, removeWhereFieldNames);
        correctorSql = SqlParserReplaceHelper.replaceValue(correctorSql, filedNameToValueMap);
        correctorSql = SqlParserRemoveHelper.removeWhereCondition(correctorSql, removeWhereFieldNames);
        // replace having filter
        updateFilters(havingExpressionList, queryData.getDimensionFilters(),
                parseInfo.getDimensionFilters(), addHavingConditions, removeHavingFieldNames);
        correctorSql = SqlParserReplaceHelper.replaceHavingValue(correctorSql, havingFiledNameToValueMap);
        correctorSql = SqlParserRemoveHelper.removeHavingCondition(correctorSql, removeHavingFieldNames);

        correctorSql = SqlParserAddHelper.addWhere(correctorSql, addWhereConditions);
        correctorSql = SqlParserAddHelper.addHaving(correctorSql, addHavingConditions);
        log.info("correctorSql after replacing:{}", correctorSql);
        return correctorSql;
    }

    private void replaceMetrics(SemanticParseInfo parseInfo, List<String> metrics) {
        List<String> filteredMetrics = parseInfo.getMetrics().stream()
                .map(o -> o.getName()).collect(Collectors.toList());
        String correctorSql = parseInfo.getSqlInfo().getCorrectS2SQL();
        log.info("before replaceMetrics:{}", correctorSql);
        log.info("filteredMetrics:{},metrics:{}", filteredMetrics, metrics);
        Map<String, String> fieldMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filteredMetrics) && CollectionUtils.isNotEmpty(metrics)) {
            fieldMap.put(filteredMetrics.get(0), metrics.get(0));
            correctorSql = SqlParserReplaceHelper.replaceSelectFields(correctorSql, fieldMap);
        }
        log.info("after replaceMetrics:{}", correctorSql);
        parseInfo.getSqlInfo().setCorrectS2SQL(correctorSql);
    }

    @Override
    public EntityInfo getEntityInfo(Long queryId, Integer parseId, User user) {
        ChatParseDO chatParseDO = chatService.getParseInfo(queryId, parseId);
        SemanticParseInfo parseInfo = JsonUtil.toObject(chatParseDO.getParseInfo(), SemanticParseInfo.class);
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        return semanticService.getEntityInfo(parseInfo, user);
    }

    private void updateDateInfo(QueryDataReq queryData, SemanticParseInfo parseInfo,
            Map<String, Map<String, String>> filedNameToValueMap,
            List<FieldExpression> fieldExpressionList,
            List<Expression> addConditions,
            Set<String> removeFieldNames) {
        if (Objects.isNull(queryData.getDateInfo())) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        String dateField = TimeDimensionEnum.DAY.getChName();
        if (queryData.getDateInfo().getUnit() > 1) {
            queryData.getDateInfo().setStartDate(DateUtils.getBeforeDate(queryData.getDateInfo().getUnit() + 1));
            queryData.getDateInfo().setEndDate(DateUtils.getBeforeDate(1));
        }
        // startDate equals to endDate
        if (queryData.getDateInfo().getStartDate().equals(queryData.getDateInfo().getEndDate())) {
            for (FieldExpression fieldExpression : fieldExpressionList) {
                if (TimeDimensionEnum.DAY.getChName().equals(fieldExpression.getFieldName())) {
                    //sql where condition exists 'equals' operator about date,just replace
                    if (fieldExpression.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                        dateField = fieldExpression.getFieldName();
                        map.put(fieldExpression.getFieldValue().toString(),
                                queryData.getDateInfo().getStartDate());
                        filedNameToValueMap.put(dateField, map);
                    } else {
                        // first remove,then add
                        removeFieldNames.add(TimeDimensionEnum.DAY.getChName());
                        EqualsTo equalsTo = new EqualsTo();
                        Column column = new Column(TimeDimensionEnum.DAY.getChName());
                        StringValue stringValue = new StringValue(queryData.getDateInfo().getStartDate());
                        equalsTo.setLeftExpression(column);
                        equalsTo.setRightExpression(stringValue);
                        addConditions.add(equalsTo);
                    }
                    break;
                }
            }
        } else {
            for (FieldExpression fieldExpression : fieldExpressionList) {
                if (TimeDimensionEnum.DAY.getChName().equals(fieldExpression.getFieldName())) {
                    dateField = fieldExpression.getFieldName();
                    //just replace
                    if (FilterOperatorEnum.GREATER_THAN_EQUALS.getValue().equals(fieldExpression.getOperator())
                            || FilterOperatorEnum.GREATER_THAN.getValue().equals(fieldExpression.getOperator())) {
                        map.put(fieldExpression.getFieldValue().toString(),
                                queryData.getDateInfo().getStartDate());
                    }
                    if (FilterOperatorEnum.MINOR_THAN_EQUALS.getValue().equals(fieldExpression.getOperator())
                            || FilterOperatorEnum.MINOR_THAN.getValue().equals(fieldExpression.getOperator())) {
                        map.put(fieldExpression.getFieldValue().toString(),
                                queryData.getDateInfo().getEndDate());
                    }
                    filedNameToValueMap.put(dateField, map);
                    // first remove,then add
                    if (FilterOperatorEnum.EQUALS.getValue().equals(fieldExpression.getOperator())) {
                        removeFieldNames.add(TimeDimensionEnum.DAY.getChName());
                        GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                        addTimeFilters(queryData.getDateInfo().getStartDate(), greaterThanEquals, addConditions);
                        MinorThanEquals minorThanEquals = new MinorThanEquals();
                        addTimeFilters(queryData.getDateInfo().getEndDate(), minorThanEquals, addConditions);
                    }
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
        ExpressionList expressionList = new ExpressionList();
        List<Expression> expressions = new ArrayList<>();
        List<String> valueList = JsonUtil.toList(
                JsonUtil.toString(dslQueryFilter.getValue()), String.class);
        if (CollectionUtils.isEmpty(valueList)) {
            return;
        }
        valueList.stream().forEach(o -> {
            StringValue stringValue = new StringValue(o);
            expressions.add(stringValue);
        });
        expressionList.setExpressions(expressions);
        inExpression.setLeftExpression(column);
        inExpression.setRightItemsList(expressionList);
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

    private SemanticParseInfo getSemanticParseInfo(QueryDataReq queryData, ChatParseDO chatParseDO) {
        SemanticParseInfo parseInfo = JsonUtil.toObject(chatParseDO.getParseInfo(), SemanticParseInfo.class);
        if (LLMSqlQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            return parseInfo;
        }
        if (CollectionUtils.isNotEmpty(queryData.getDimensions())) {
            parseInfo.setDimensions(queryData.getDimensions());
        }
        //if (CollectionUtils.isNotEmpty(queryData.getMetrics())) {
        //    parseInfo.setMetrics(queryData.getMetrics());
        //}
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
        QueryResultWithSchemaResp queryResultWithSchemaResp = new QueryResultWithSchemaResp();
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        SemanticSchema semanticSchema = semanticService.getSemanticSchema();
        SchemaElement schemaElement = semanticSchema.getDimensions(dimensionValueReq.getElementID());
        Set<Long> detectModelIds = new HashSet<>();
        detectModelIds.add(schemaElement.getModel());
        dimensionValueReq.setModelId(schemaElement.getModel());
        List<String> dimensionValues = getDimensionValues(dimensionValueReq, detectModelIds);
        // if the search results is null,search dimensionValue from database
        if (CollectionUtils.isEmpty(dimensionValues)) {
            queryResultWithSchemaResp = queryDatabase(dimensionValueReq, user);
            return queryResultWithSchemaResp;
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
        queryResultWithSchemaResp.setColumns(columns);
        queryResultWithSchemaResp.setResultList(resultList);
        return queryResultWithSchemaResp;
    }

    private List<String> getDimensionValues(DimensionValueReq dimensionValueReq, Set<Long> detectModelIds) {
        //if value is null ,then search from NATURE_TO_VALUES
        if (StringUtils.isBlank(dimensionValueReq.getValue())) {
            String nature = DictWordType.NATURE_SPILT + dimensionValueReq.getModelId() + DictWordType.NATURE_SPILT
                    + dimensionValueReq.getElementID();
            PriorityQueue<Term> terms = MultiCustomDictionary.NATURE_TO_VALUES.get(nature);
            if (CollectionUtils.isEmpty(terms)) {
                return new ArrayList<>();
            }
            return terms.stream().map(term -> term.getWord()).collect(Collectors.toList());
        }
        //search from prefixSearch
        List<HanlpMapResult> hanlpMapResultList = SearchService.prefixSearch(dimensionValueReq.getValue(),
                2000, dimensionValueReq.getAgentId(), detectModelIds);
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

    private QueryResultWithSchemaResp queryDatabase(DimensionValueReq dimensionValueReq, User user) {
        QueryStructReq queryStructReq = new QueryStructReq();

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.RECENT);
        dateConf.setUnit(1);
        dateConf.setPeriod("DAY");
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setLimit(20L);
        queryStructReq.setModelId(dimensionValueReq.getModelId());
        queryStructReq.setQueryType(QueryType.OTHER);
        List<String> groups = new ArrayList<>();
        groups.add(dimensionValueReq.getBizName());
        queryStructReq.setGroups(groups);
        SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
        return semanticInterpreter.queryByStruct(queryStructReq, user);
    }

}

