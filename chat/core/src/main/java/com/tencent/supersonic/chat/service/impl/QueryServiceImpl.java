package com.tencent.supersonic.chat.service.impl;


import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.request.SolvedQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.parser.llm.s2ql.ParseResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.CostType;
import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.QuerySelector;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMResp;
import com.tencent.supersonic.chat.query.llm.s2ql.S2QLQuery;
import com.tencent.supersonic.chat.responder.execute.ExecuteResponder;
import com.tencent.supersonic.chat.responder.parse.ParseResponder;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.service.StatisticsService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.SolvedQueryManager;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FilterExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserRemoveHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.dictionary.MultiCustomDictionary;
import com.tencent.supersonic.knowledge.service.SearchService;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import com.tencent.supersonic.knowledge.utils.NatureHelper;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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
    private QuerySelector querySelector = ComponentFactory.getQuerySelector();
    private List<ParseResponder> parseResponders = ComponentFactory.getParseResponders();
    private List<ExecuteResponder> executeResponders = ComponentFactory.getExecuteResponders();

    @Override
    public ParseResp performParsing(QueryReq queryReq) {
        Long parseTime = System.currentTimeMillis();
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());
        List<StatisticsDO> timeCostDOList = new ArrayList<>();
        schemaMappers.stream().forEach(mapper -> {
            Long startTime = System.currentTimeMillis();
            mapper.map(queryCtx);
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(mapper.getClass().getSimpleName()).type(CostType.MAPPER.getType()).build());
            log.info("{} result:{}", mapper.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });
        semanticParsers.stream().forEach(parser -> {
            Long startTime = System.currentTimeMillis();
            parser.parse(queryCtx, chatCtx);
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(parser.getClass().getSimpleName()).type(CostType.PARSER.getType()).build());
            log.info("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });
        ParseResp parseResult;
        List<ChatParseDO> chatParseDOS = Lists.newArrayList();
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.debug("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            List<SemanticQuery> selectedQueries = querySelector.select(queryCtx.getCandidateQueries(), queryReq);
            log.debug("pick after [{}]", selectedQueries.stream().collect(
                    Collectors.toList()));

            List<SemanticParseInfo> selectedParses = convertParseInfo(selectedQueries);
            List<SemanticParseInfo> candidateParses = convertParseInfo(queryCtx.getCandidateQueries());
            candidateParses = getTop5CandidateParseInfo(selectedParses, candidateParses);
            parseResult = ParseResp.builder()
                    .chatId(queryReq.getChatId())
                    .queryText(queryReq.getQueryText())
                    .state(selectedParses.size() > 1 ? ParseResp.ParseState.PENDING : ParseResp.ParseState.COMPLETED)
                    .selectedParses(selectedParses)
                    .candidateParses(candidateParses)
                    .build();
            chatParseDOS = chatService.batchAddParse(chatCtx, queryReq, parseResult, candidateParses, selectedParses);
        } else {
            parseResult = ParseResp.builder()
                    .chatId(queryReq.getChatId())
                    .queryText(queryReq.getQueryText())
                    .state(ParseResp.ParseState.FAILED)
                    .build();
        }
        for (ParseResponder parseResponder : parseResponders) {
            Long startTime = System.currentTimeMillis();
            parseResponder.fillResponse(parseResult, queryCtx, chatParseDOS);
            timeCostDOList.add(StatisticsDO.builder().cost((int) (System.currentTimeMillis() - startTime))
                    .interfaceName(parseResponder.getClass().getSimpleName())
                    .type(CostType.PARSERRESPONDER.getType()).build());
        }
        if (Objects.nonNull(parseResult.getQueryId()) && timeCostDOList.size() > 0) {
            saveInfo(timeCostDOList, queryReq.getQueryText(), parseResult.getQueryId(),
                    queryReq.getUser().getName(), queryReq.getChatId().longValue());
        }
        chatService.updateChatParse(chatParseDOS);
        parseResult.getParseTimeCost().setParseTime(
                System.currentTimeMillis() - parseTime - parseResult.getParseTimeCost().getSqlTime());
        return parseResult;
    }

    private List<SemanticParseInfo> convertParseInfo(List<SemanticQuery> semanticQueries) {
        return semanticQueries.stream()
                .map(SemanticQuery::getParseInfo)
                .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                .collect(Collectors.toList());
    }

    private List<SemanticParseInfo> getTop5CandidateParseInfo(List<SemanticParseInfo> selectedParses,
            List<SemanticParseInfo> candidateParses) {
        if (CollectionUtils.isEmpty(selectedParses) || CollectionUtils.isEmpty(candidateParses)) {
            return candidateParses;
        }
        int selectParseSize = selectedParses.size();
        Set<Double> selectParseScoreSet = selectedParses.stream()
                .map(SemanticParseInfo::getScore).collect(Collectors.toSet());
        int candidateParseSize = 5 - selectParseSize;
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

    @Override
    public QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception {
        Long executeTime = System.currentTimeMillis();
        ChatParseDO chatParseDO = chatService.getParseInfo(queryReq.getQueryId(),
                queryReq.getParseId());
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
            saveInfo(timeCostDOList, queryReq.getQueryText(), queryReq.getQueryId(),
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
            chatService.updateQuery(queryReq.getQueryId(), queryResult, chatCtx);
            for (ExecuteResponder executeResponder : executeResponders) {
                executeResponder.fillResponse(queryResult, parseInfo, queryReq);
            }
        } else {
            chatService.deleteChatQuery(queryReq.getQueryId());
        }
        queryResult.setQueryTimeCost(System.currentTimeMillis() - executeTime);
        return queryResult;
    }

    public void saveInfo(List<StatisticsDO> timeCostDOList,
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
                .modelId(parseInfo.getModelId())
                .queryText(queryReq.getQueryText()).build());
    }

    @Override
    public QueryResult executeQuery(QueryReq queryReq) throws Exception {
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());

        schemaMappers.stream().forEach(mapper -> {
            mapper.map(queryCtx);
            log.info("{} result:{}", mapper.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        semanticParsers.stream().forEach(parser -> {
            parser.parse(queryCtx, chatCtx);
            log.info("{} result:{}", parser.getClass().getSimpleName(), JsonUtil.toString(queryCtx));
        });

        QueryResult queryResult = null;
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.info("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            List<SemanticQuery> selectedQueries = querySelector.select(queryCtx.getCandidateQueries(), queryReq);
            log.info("pick after [{}]", selectedQueries.stream().collect(
                    Collectors.toList()));

            SemanticQuery semanticQuery = selectedQueries.get(0);
            queryResult = semanticQuery.execute(queryReq.getUser());
            if (queryResult != null) {
                chatCtx.setQueryText(queryReq.getQueryText());
                // update chat context after a successful semantic query
                if (queryReq.isSaveAnswer() && QueryState.SUCCESS.equals(queryResult.getQueryState())) {
                    chatCtx.setParseInfo(semanticQuery.getParseInfo());
                    chatService.updateContext(chatCtx);
                }
                queryResult.setChatContext(chatCtx.getParseInfo());
                chatService.addQuery(queryResult, chatCtx);
            }
        }

        return queryResult;
    }

    @Override
    public SemanticParseInfo queryContext(QueryReq queryCtx) {
        ChatContext context = chatService.getOrCreateContext(queryCtx.getChatId());
        return context.getParseInfo();
    }

    @Override
    public QueryResult executeDirectQuery(QueryDataReq queryData, User user) throws SqlParseException {
        ChatParseDO chatParseDO = chatService.getParseInfo(queryData.getQueryId(),
                queryData.getParseId());
        SemanticParseInfo parseInfo = getSemanticParseInfo(queryData, chatParseDO);

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());

        if (S2QLQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
            Map<String, Map<String, String>> havingFiledNameToValueMap = new HashMap<>();
            String json = JsonUtil.toString(parseInfo.getProperties().get(Constants.CONTEXT));
            ParseResult parseResult = JsonUtil.toObject(json, ParseResult.class);
            LLMResp llmResp = parseResult.getLlmResp();
            String correctorSql = llmResp.getCorrectorSql();
            log.info("correctorSql before replacing:{}", correctorSql);
            List<FilterExpression> whereExpressionList = SqlParserSelectHelper.getWhereExpressions(correctorSql);
            List<FilterExpression> havingExpressionList = SqlParserSelectHelper.getHavingExpressions(correctorSql);
            List<Expression> addWhereConditions = new ArrayList<>();
            List<Expression> addHavingConditions = new ArrayList<>();
            Set<String> removeWhereFieldNames = new HashSet<>();
            Set<String> removeHavingFieldNames = new HashSet<>();
            updateFilters(filedNameToValueMap, whereExpressionList, queryData.getDimensionFilters(),
                    parseInfo.getDimensionFilters(), addWhereConditions, removeWhereFieldNames);
            updateDateInfo(queryData, parseInfo, filedNameToValueMap,
                    whereExpressionList, addWhereConditions, removeWhereFieldNames);
            correctorSql = SqlParserReplaceHelper.replaceValue(correctorSql, filedNameToValueMap);
            correctorSql = SqlParserRemoveHelper.removeWhereCondition(correctorSql, removeWhereFieldNames);

            updateFilters(havingFiledNameToValueMap, havingExpressionList, queryData.getDimensionFilters(),
                    parseInfo.getDimensionFilters(), addHavingConditions, removeHavingFieldNames);
            correctorSql = SqlParserReplaceHelper.replaceHavingValue(correctorSql, havingFiledNameToValueMap);
            correctorSql = SqlParserRemoveHelper.removeHavingCondition(correctorSql, removeHavingFieldNames);

            correctorSql = SqlParserAddHelper.addWhere(correctorSql, addWhereConditions);
            correctorSql = SqlParserAddHelper.addHaving(correctorSql, addHavingConditions);

            log.info("correctorSql after replacing:{}", correctorSql);
            llmResp.setCorrectorSql(correctorSql);
            parseResult.setLlmResp(llmResp);
            Map<String, Object> properties = new HashMap<>();
            properties.put(Constants.CONTEXT, parseResult);
            parseInfo.setProperties(properties);
            parseInfo.getSqlInfo().setLogicSql(correctorSql);
            semanticQuery.setParseInfo(parseInfo);
            ExplainResp explain = semanticQuery.explain(user);
            if (!Objects.isNull(explain)) {
                parseInfo.getSqlInfo().setQuerySql(explain.getSql());
            }
        }
        semanticQuery.setParseInfo(parseInfo);
        QueryResult queryResult = semanticQuery.execute(user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        EntityInfo entityInfo = semanticService.getEntityInfo(parseInfo, user);
        queryResult.setEntityInfo(entityInfo);
        return queryResult;
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
            List<FilterExpression> filterExpressionList,
            List<Expression> addConditions,
            Set<String> removeFieldNames) {
        if (Objects.isNull(queryData.getDateInfo())) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        String dateField = DateUtils.DATE_FIELD;
        if (queryData.getDateInfo().getUnit() > 1) {
            queryData.getDateInfo().setStartDate(DateUtils.getBeforeDate(queryData.getDateInfo().getUnit() + 1));
            queryData.getDateInfo().setEndDate(DateUtils.getBeforeDate(1));
        }
        if (queryData.getDateInfo().getStartDate().equals(queryData.getDateInfo().getEndDate())) {
            for (FilterExpression filterExpression : filterExpressionList) {
                if (DateUtils.DATE_FIELD.equals(filterExpression.getFieldName())) {
                    if (filterExpression.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                        dateField = filterExpression.getFieldName();
                        map.put(filterExpression.getFieldValue().toString(),
                                queryData.getDateInfo().getStartDate());
                        filedNameToValueMap.put(dateField, map);
                    } else {
                        removeFieldNames.add(DateUtils.DATE_FIELD);
                        EqualsTo equalsTo = new EqualsTo();
                        Column column = new Column(DateUtils.DATE_FIELD);
                        StringValue stringValue = new StringValue(queryData.getDateInfo().getStartDate());
                        equalsTo.setLeftExpression(column);
                        equalsTo.setRightExpression(stringValue);
                        addConditions.add(equalsTo);
                    }
                    break;
                }
            }
        } else {
            for (FilterExpression filterExpression : filterExpressionList) {
                if (DateUtils.DATE_FIELD.equals(filterExpression.getFieldName())) {
                    dateField = filterExpression.getFieldName();
                    if (FilterOperatorEnum.GREATER_THAN_EQUALS.getValue().equals(filterExpression.getOperator())
                            || FilterOperatorEnum.GREATER_THAN.getValue().equals(filterExpression.getOperator())) {
                        map.put(filterExpression.getFieldValue().toString(),
                                queryData.getDateInfo().getStartDate());
                    }
                    if (FilterOperatorEnum.MINOR_THAN_EQUALS.getValue().equals(filterExpression.getOperator())
                            || FilterOperatorEnum.MINOR_THAN.getValue().equals(filterExpression.getOperator())) {
                        map.put(filterExpression.getFieldValue().toString(),
                                queryData.getDateInfo().getEndDate());
                    }
                    filedNameToValueMap.put(dateField, map);
                    if (FilterOperatorEnum.EQUALS.getValue().equals(filterExpression.getOperator())) {
                        removeFieldNames.add(DateUtils.DATE_FIELD);
                        GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                        addTimeCondition(queryData.getDateInfo().getStartDate(), greaterThanEquals, addConditions);
                        MinorThanEquals minorThanEquals = new MinorThanEquals();
                        addTimeCondition(queryData.getDateInfo().getEndDate(), minorThanEquals, addConditions);
                    }
                }
            }
        }
        parseInfo.setDateInfo(queryData.getDateInfo());
    }

    public <T extends ComparisonOperator> void addTimeCondition(String date,
            T comparisonExpression,
            List<Expression> addConditions) {
        Column column = new Column(DateUtils.DATE_FIELD);
        StringValue stringValue = new StringValue(date);
        comparisonExpression.setLeftExpression(column);
        comparisonExpression.setRightExpression(stringValue);
        addConditions.add(comparisonExpression);
    }

    private void updateFilters(Map<String, Map<String, String>> filedNameToValueMap,
            List<FilterExpression> filterExpressionList,
            Set<QueryFilter> metricFilters,
            Set<QueryFilter> contextMetricFilters,
            List<Expression> addConditions,
            Set<String> removeFieldNames) {
        if (CollectionUtils.isEmpty(metricFilters)) {
            return;
        }
        for (QueryFilter dslQueryFilter : metricFilters) {
            Map<String, String> map = new HashMap<>();
            for (FilterExpression filterExpression : filterExpressionList) {
                if (filterExpression.getFieldName() != null
                        && filterExpression.getFieldName().contains(dslQueryFilter.getName())) {
                    if (dslQueryFilter.getOperator().getValue().equals(filterExpression.getOperator())
                            && Objects.nonNull(dslQueryFilter.getValue())) {
                        map.put(filterExpression.getFieldValue().toString(), dslQueryFilter.getValue().toString());
                        filedNameToValueMap.put(dslQueryFilter.getName(), map);
                        contextMetricFilters.stream().forEach(o -> {
                            if (o.getName().equals(dslQueryFilter.getName())) {
                                o.setValue(dslQueryFilter.getValue());
                            }
                        });
                    } else {
                        removeFieldNames.add(dslQueryFilter.getName());
                        if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.EQUALS)) {
                            EqualsTo equalsTo = new EqualsTo();
                            addWhereCondition(dslQueryFilter, equalsTo, contextMetricFilters, addConditions);
                        } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.GREATER_THAN_EQUALS)) {
                            GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                            addWhereCondition(dslQueryFilter, greaterThanEquals, contextMetricFilters, addConditions);
                        } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.GREATER_THAN)) {
                            GreaterThan greaterThan = new GreaterThan();
                            addWhereCondition(dslQueryFilter, greaterThan, contextMetricFilters, addConditions);
                        } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.MINOR_THAN_EQUALS)) {
                            MinorThanEquals minorThanEquals = new MinorThanEquals();
                            addWhereCondition(dslQueryFilter, minorThanEquals, contextMetricFilters, addConditions);
                        } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.MINOR_THAN)) {
                            MinorThan minorThan = new MinorThan();
                            addWhereCondition(dslQueryFilter, minorThan, contextMetricFilters, addConditions);
                        } else if (dslQueryFilter.getOperator().equals(FilterOperatorEnum.IN)) {
                            InExpression inExpression = new InExpression();
                            addWhereInCondition(dslQueryFilter, inExpression, contextMetricFilters, addConditions);
                        }
                    }
                    break;
                }
            }
        }
    }

    public void addWhereInCondition(QueryFilter dslQueryFilter,
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

    public <T extends ComparisonOperator> void addWhereCondition(QueryFilter dslQueryFilter,
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
        LongValue longValue = new LongValue(Long.parseLong(dslQueryFilter.getValue().toString()));
        comparisonExpression.setLeftExpression(column);
        comparisonExpression.setRightExpression(longValue);
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
        if (S2QLQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
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
        if (Objects.nonNull(queryData.getDateInfo())) {
            parseInfo.setDateInfo(queryData.getDateInfo());
        }
        return parseInfo;
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception {
        QueryResultWithSchemaResp queryResultWithSchemaResp = new QueryResultWithSchemaResp();
        Set<Long> detectModelIds = new HashSet<>();
        detectModelIds.add(dimensionValueReq.getModelId());
        List<String> dimensionValues = getDimensionValues(dimensionValueReq, detectModelIds);

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
        List<MapResult> mapResultList = SearchService.prefixSearch(dimensionValueReq.getValue(),
                2000, dimensionValueReq.getAgentId(), detectModelIds);
        HanlpHelper.transLetterOriginal(mapResultList);
        return mapResultList.stream()
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

}

