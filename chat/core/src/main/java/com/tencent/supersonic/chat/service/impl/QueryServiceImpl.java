package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
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
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLParseResult;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.persistence.dataobject.CostType;
import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.QuerySelector;
import com.tencent.supersonic.chat.query.llm.dsl.DslQuery;
import com.tencent.supersonic.chat.query.llm.dsl.LLMResp;
import com.tencent.supersonic.chat.responder.execute.ExecuteResponder;
import com.tencent.supersonic.chat.responder.parse.ParseResponder;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.StatisticsService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.SolvedQueryManager;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FilterExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.service.SearchService;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.collections.CollectionUtils;
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
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());
        List<StatisticsDO> timeCostDOList = new ArrayList<>();
        for (SchemaMapper mapper : schemaMappers) {
            Long startTime = System.currentTimeMillis();
            mapper.map(queryCtx);
            Long endTime = System.currentTimeMillis();
            String className = mapper.getClass().getSimpleName();
            timeCostDOList.add(StatisticsDO.builder().cost((int) (endTime - startTime))
                    .interfaceName(className).type(CostType.MAPPER.getType()).build());
            log.info("{} result:{}", className, JsonUtil.toString(queryCtx));
        }
        for (SemanticParser parser : semanticParsers) {
            Long startTime = System.currentTimeMillis();
            parser.parse(queryCtx, chatCtx);
            Long endTime = System.currentTimeMillis();
            String className = parser.getClass().getSimpleName();
            timeCostDOList.add(StatisticsDO.builder().cost((int) (endTime - startTime))
                    .interfaceName(className).type(CostType.PARSER.getType()).build());
            log.info("{} result:{}", className, JsonUtil.toString(queryCtx));
        }
        ParseResp parseResult;
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.debug("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            List<SemanticQuery> selectedQueries = querySelector.select(queryCtx.getCandidateQueries(), queryReq);
            log.debug("pick after [{}]", selectedQueries.stream().collect(
                    Collectors.toList()));

            List<SemanticParseInfo> selectedParses = convertParseInfo(selectedQueries);
            List<SemanticParseInfo> candidateParses = convertParseInfo(queryCtx.getCandidateQueries());
            parseResult = ParseResp.builder()
                    .chatId(queryReq.getChatId())
                    .queryText(queryReq.getQueryText())
                    .state(selectedParses.size() > 1 ? ParseResp.ParseState.PENDING : ParseResp.ParseState.COMPLETED)
                    .selectedParses(selectedParses)
                    .candidateParses(candidateParses)
                    .build();
            chatService.batchAddParse(chatCtx, queryReq, parseResult, candidateParses, selectedParses);
            saveInfo(timeCostDOList, queryReq.getQueryText(), parseResult.getQueryId(),
                    queryReq.getUser().getName(), queryReq.getChatId().longValue());
        } else {
            parseResult = ParseResp.builder()
                    .chatId(queryReq.getChatId())
                    .queryText(queryReq.getQueryText())
                    .state(ParseResp.ParseState.FAILED)
                    .build();
        }
        for (ParseResponder parseResponder : parseResponders) {
            parseResponder.fillResponse(parseResult, queryCtx);
        }
        return parseResult;
    }

    private List<SemanticParseInfo> convertParseInfo(List<SemanticQuery> semanticQueries) {
        return semanticQueries.stream()
                .map(SemanticQuery::getParseInfo)
                .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception {
        ChatParseDO chatParseDO = chatService.getParseInfo(queryReq.getQueryId(),
                queryReq.getUser().getName(), queryReq.getParseId());
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
            saveInfo(timeCostDOList, queryReq.getQueryText(), queryReq.getQueryId(),
                    queryReq.getUser().getName(), queryReq.getChatId().longValue());
            queryResult.setChatContext(parseInfo);
            // update chat context after a successful semantic query
            if (queryReq.isSaveAnswer() && QueryState.SUCCESS.equals(queryResult.getQueryState())) {
                chatCtx.setParseInfo(parseInfo);
                chatService.updateContext(chatCtx);
                solvedQueryManager.saveSolvedQuery(SolvedQueryReq.builder().parseId(queryReq.getParseId())
                        .queryId(queryReq.getQueryId())
                        .agentId(chatQueryDO.getAgentId())
                        .modelId(parseInfo.getModelId())
                        .queryText(queryReq.getQueryText()).build());
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
                queryData.getUser().getName(), queryData.getParseId());
        SemanticParseInfo parseInfo = getSemanticParseInfo(queryData, chatParseDO);

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());

        if (DslQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
            String json = JsonUtil.toString(parseInfo.getProperties().get(Constants.CONTEXT));
            DSLParseResult dslParseResult = JsonUtil.toObject(json, DSLParseResult.class);
            LLMResp llmResp = dslParseResult.getLlmResp();
            String correctorSql = llmResp.getCorrectorSql();
            log.info("correctorSql before replacing:{}", correctorSql);

            List<FilterExpression> filterExpressionList = SqlParserSelectHelper.getFilterExpression(correctorSql);

            updateFilters(filedNameToValueMap, filterExpressionList, queryData.getDimensionFilters(),
                    parseInfo.getDimensionFilters());

            updateFilters(filedNameToValueMap, filterExpressionList, queryData.getMetricFilters(),
                    parseInfo.getMetricFilters());

            updateDateInfo(queryData, parseInfo, filedNameToValueMap, filterExpressionList);

            log.info("filedNameToValueMap:{}", filedNameToValueMap);
            correctorSql = SqlParserUpdateHelper.replaceValue(correctorSql, filedNameToValueMap);
            log.info("correctorSql after replacing:{}", correctorSql);
            llmResp.setCorrectorSql(correctorSql);

            parseInfo.getSqlInfo().setLogicSql(correctorSql);

            ExplainResp explain = semanticQuery.explain(user);
            if (!Objects.isNull(explain)) {
                parseInfo.getSqlInfo().setQuerySql(explain.getSql());
            }
        }
        semanticQuery.setParseInfo(parseInfo);
        QueryResult queryResult = semanticQuery.execute(user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        return queryResult;
    }

    private void updateDateInfo(QueryDataReq queryData, SemanticParseInfo parseInfo,
            Map<String, Map<String, String>> filedNameToValueMap, List<FilterExpression> filterExpressionList) {
        if (Objects.isNull(queryData.getDateInfo())) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        List<String> dateFields = new ArrayList<>(QueryStructUtils.internalTimeCols);
        String dateField = TimeDimensionEnum.DAY.getName();
        if (queryData.getDateInfo().getStartDate().equals(queryData.getDateInfo().getEndDate())) {
            for (FilterExpression filterExpression : filterExpressionList) {
                if (filterExpression.getFieldName() != null
                        && dateFields.contains(filterExpression.getFieldName())) {
                    dateField = filterExpression.getFieldName();
                    map.put(filterExpression.getFieldValue().toString(),
                            queryData.getDateInfo().getStartDate());
                    break;
                }
            }
        } else {
            for (FilterExpression filterExpression : filterExpressionList) {
                if (dateFields.contains(filterExpression.getFieldName())) {
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
                }
            }
        }
        filedNameToValueMap.put(dateField, map);
        parseInfo.setDateInfo(queryData.getDateInfo());
    }

    private void updateFilters(Map<String, Map<String, String>> filedNameToValueMap,
            List<FilterExpression> filterExpressionList, Set<QueryFilter> metricFilters,
            Set<QueryFilter> contextMetricFilters) {
        if (CollectionUtils.isEmpty(metricFilters)) {
            return;
        }
        for (QueryFilter dslQueryFilter : metricFilters) {
            Map<String, String> map = new HashMap<>();
            for (FilterExpression filterExpression : filterExpressionList) {
                if (filterExpression.getFieldName() != null
                        && filterExpression.getFieldName().equals(dslQueryFilter.getName())
                        && dslQueryFilter.getOperator().getValue().equals(filterExpression.getOperator())) {
                    map.put(filterExpression.getFieldValue().toString(), dslQueryFilter.getValue().toString());
                    contextMetricFilters.stream().forEach(o -> {
                        if (o.getName().equals(dslQueryFilter.getName())) {
                            o.setValue(dslQueryFilter.getValue());
                        }
                    });
                    break;
                }
            }
            filedNameToValueMap.put(dslQueryFilter.getName(), map);
        }
    }


    private SemanticParseInfo getSemanticParseInfo(QueryDataReq queryData, ChatParseDO chatParseDO) {
        SemanticParseInfo parseInfo = JsonUtil.toObject(chatParseDO.getParseInfo(), SemanticParseInfo.class);
        if (DslQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
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
        QueryStructReq queryStructReq = new QueryStructReq();

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.RECENT);
        dateConf.setUnit(1);
        dateConf.setPeriod("DAY");
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setLimit(20L);
        queryStructReq.setModelId(dimensionValueReq.getModelId());
        queryStructReq.setNativeQuery(true);
        List<String> groups = new ArrayList<>();
        groups.add(dimensionValueReq.getBizName());
        queryStructReq.setGroups(groups);
        if ((!Objects.isNull(dimensionValueReq.getValue()))
                && StringUtils.isNotBlank(dimensionValueReq.getValue().toString())) {
            return queryHanlpDimensionValue(dimensionValueReq, user);
        }
        SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
        QueryResultWithSchemaResp queryResultWithSchemaResp = semanticInterpreter.queryByStruct(queryStructReq, user);
        return queryResultWithSchemaResp;
    }

    public Object queryHanlpDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception {
        QueryResultWithSchemaResp queryResultWithSchemaResp = new QueryResultWithSchemaResp();
        Set<Long> detectModelIds = new HashSet<>();
        detectModelIds.add(dimensionValueReq.getModelId());
        List<MapResult> mapResultList = SearchService.prefixSearch(dimensionValueReq.getValue().toString(),
                2000, dimensionValueReq.getAgentId(), detectModelIds);
        log.info("mapResultList:{}", mapResultList);
        mapResultList = mapResultList.stream().filter(o -> {
            for (String nature : o.getNatures()) {
                String[] natureArray = nature.split("_");
                if (natureArray[2].equals(dimensionValueReq.getElementID().toString())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        log.info("mapResultList:{}", mapResultList);
        List<QueryColumn> columns = new ArrayList<>();
        QueryColumn queryColumn = new QueryColumn();
        queryColumn.setNameEn(dimensionValueReq.getBizName());
        queryColumn.setShowType("CATEGORY");
        queryColumn.setAuthorized(true);
        queryColumn.setType("CHAR");
        columns.add(queryColumn);
        List<Map<String, Object>> resultList = new ArrayList<>();
        mapResultList.stream().forEach(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put(dimensionValueReq.getBizName(), o.getName());
            resultList.add(map);
        });
        queryResultWithSchemaResp.setColumns(columns);
        queryResultWithSchemaResp.setResultList(resultList);
        return queryResultWithSchemaResp;
    }

}

