package com.tencent.supersonic.chat.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.ChatQueryExecutor;
import com.tencent.supersonic.chat.server.executor.DashboardExecutor;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatQueryDO;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.processor.execute.DataInterpretProcessor;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.api.service.SchemaService;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatQueryServiceImpl implements ChatQueryService {

    private static final String REPORT_SCHEDULE_QUERY_MODE = "REPORT_SCHEDULE";

    private final ChatManageService chatManageService;
    private final ChatLayerService chatLayerService;
    private final SemanticLayerService semanticLayerService;
    @Autowired
    @Lazy
    private AgentService agentService;
    private final UserService userService;

    private final List<ChatQueryParser> chatQueryParsers = ComponentFactory.getChatParsers();
    private final List<ChatQueryExecutor> chatQueryExecutors = ComponentFactory.getChatExecutors();
    private final List<ParseResultProcessor> parseResultProcessors =
            ComponentFactory.getParseProcessors();
    private final List<ExecuteResultProcessor> executeResultProcessors =
            ComponentFactory.getExecuteProcessors();

    @Override
    public List<SearchResult> search(ChatParseReq chatParseReq) {
        ParseContext parseContext = buildParseContext(chatParseReq, null);
        Agent agent = parseContext.getAgent();
        if (!agent.enableSearch()) {
            return Lists.newArrayList();
        }
        QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
        return chatLayerService.retrieve(queryNLReq);
    }

    @Override
    public ChatParseResp parse(ChatParseReq chatParseReq) {
        Long queryId = chatParseReq.getQueryId();
        if (Objects.isNull(queryId)) {
            queryId = chatManageService.createChatQuery(chatParseReq);
            chatParseReq.setQueryId(queryId);
        }

        ParseContext parseContext = buildParseContext(chatParseReq, new ChatParseResp(queryId));
        for (ChatQueryParser parser : chatQueryParsers) {
            if (parser.accept(parseContext)) {
                parser.parse(parseContext);
            }
        }

        for (ParseResultProcessor processor : parseResultProcessors) {
            if (processor.accept(parseContext)) {
                processor.process(parseContext);
            }
        }

        if (!parseContext.needFeedback()) {
            parseContext.getResponse().getParseTimeCost().setParseTime(System.currentTimeMillis()
                    - parseContext.getResponse().getParseTimeCost().getParseStartTime());
            chatManageService.batchAddParse(chatParseReq, parseContext.getResponse());
            chatManageService.updateParseCostTime(parseContext.getResponse());
        }

        return parseContext.getResponse();
    }

    @Override
    public QueryResult execute(ChatExecuteReq chatExecuteReq) {
        QueryResult queryResult = new QueryResult();
        ExecuteContext executeContext = buildExecuteContext(chatExecuteReq);
        for (ChatQueryExecutor chatQueryExecutor : chatQueryExecutors) {
            if (chatQueryExecutor.accept(executeContext)) {
                queryResult = chatQueryExecutor.execute(executeContext);
                if (queryResult != null) {
                    break;
                }
            }
        }

        executeContext.setResponse(queryResult);
        if (queryResult != null) {
            for (ExecuteResultProcessor processor : executeResultProcessors) {
                if (processor.accept(executeContext)) {
                    processor.process(executeContext);
                }
            }
            saveQueryResult(chatExecuteReq, queryResult);
        }

        return queryResult;
    }

    @Override
    public QueryResult getTextSummary(ChatExecuteReq chatExecuteReq) {
        String text = DataInterpretProcessor.getTextSummary(chatExecuteReq.getQueryId());
        if (StringUtils.isNotBlank(text)) {
            QueryResult res = new QueryResult();
            res.setTextSummary(text);
            res.setQueryId(chatExecuteReq.getQueryId());
            return res;
        } else {
            ChatQueryDO chatQueryDo = chatManageService.getChatQueryDO(chatExecuteReq.getQueryId());
            QueryResult res = JSON.parseObject(chatQueryDo.getQueryResult(), QueryResult.class);
            return res;
        }
    }

    @Override
    public QueryResult parseAndExecute(ChatParseReq chatParseReq) {
        ChatParseResp parseResp = parse(chatParseReq);
        if (CollectionUtils.isEmpty(parseResp.getSelectedParses())) {
            log.debug("chatId:{}, agentId:{}, queryText:{}, parseResp.getSelectedParses() is empty",
                    chatParseReq.getChatId(), chatParseReq.getAgentId(),
                    chatParseReq.getQueryText());
            return null;
        }
        ChatExecuteReq executeReq = new ChatExecuteReq();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getSelectedParses().getFirst().getId());
        executeReq.setQueryText(chatParseReq.getQueryText());
        executeReq.setChatId(chatParseReq.getChatId());
        executeReq.setUser(userService.getDefaultUser());
        executeReq.setAgentId(chatParseReq.getAgentId());
        executeReq.setSaveAnswer(true);
        return execute(executeReq);
    }

    private ParseContext buildParseContext(ChatParseReq chatParseReq, ChatParseResp chatParseResp) {
        ParseContext parseContext = new ParseContext(chatParseReq, chatParseResp);
        Agent agent = agentService.getAgent(chatParseReq.getAgentId());
        parseContext.setAgent(agent);
        return parseContext;
    }

    private ExecuteContext buildExecuteContext(ChatExecuteReq chatExecuteReq) {
        ExecuteContext executeContext = new ExecuteContext(chatExecuteReq);
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(chatExecuteReq.getQueryId(),
                chatExecuteReq.getParseId());
        Agent agent = agentService.getAgent(chatExecuteReq.getAgentId());
        executeContext.setAgent(agent);
        executeContext.setParseInfo(parseInfo);
        return executeContext;
    }

    @Override
    public Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception {
        Integer parseId = chatQueryDataReq.getParseId();
        if (parseId == null) {
            throw new IllegalArgumentException("parseId must not be null");
        }
        SemanticParseInfo parseInfo =
                chatManageService.getParseInfo(chatQueryDataReq.getQueryId(), parseId);
        if (parseInfo != null
                && REPORT_SCHEDULE_QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())) {
            throw new IllegalArgumentException("当前消息为报表执行结果，不支持重新查询");
        }
        mergeParseInfo(parseInfo, chatQueryDataReq);
        DataSetSchema dataSetSchema =
                semanticLayerService.getDataSetSchema(parseInfo.getDataSetId());

        String queryMode = parseInfo.getQueryMode();

        // For DASHBOARD re-queries, use struct query with all metrics (same approach
        // as DashboardExecutor) so the result includes metric columns for dashboard rendering.
        // Also check the saved QueryResult's queryMode, because the parse-phase parseInfo
        // may still have LLM_S2SQL while the execution used DASHBOARD.
        if (DashboardExecutor.QUERY_MODE.equals(queryMode)
                || isDashboardExecution(chatQueryDataReq.getQueryId())) {
            return handleDashboardReQuery(parseInfo, user);
        }

        // For non-DASHBOARD modes not registered in QueryManager, fall back to LLM_S2SQL.
        if (QueryManager.createQuery(queryMode) == null) {
            queryMode = LLMSqlQuery.QUERY_MODE;
        }

        SemanticQuery semanticQuery = QueryManager.createQuery(queryMode);
        semanticQuery.setParseInfo(parseInfo);

        if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(queryMode)) {
            handleLLMQueryMode(chatQueryDataReq, semanticQuery, dataSetSchema, user);
        } else {
            handleRuleQueryMode(semanticQuery, dataSetSchema, user);
        }

        return executeQuery(semanticQuery, user);
    }

    /**
     * Handle re-query for DASHBOARD mode by building a struct query with all metrics from the
     * dataset and the user's date filter. This mirrors DashboardExecutor.buildDefaultStructQuery to
     * ensure metric columns are present for dashboard rendering.
     */
    private QueryResult handleDashboardReQuery(SemanticParseInfo parseInfo, User user)
            throws Exception {
        Long dataSetId = parseInfo.getDataSetId();
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        SemanticSchema schema = schemaService.getSemanticSchema(Set.of(dataSetId));

        List<SchemaElement> dimensions = schema.getDimensions(dataSetId);
        SchemaElement dateColumn = dimensions.stream().filter(SchemaElement::isPartitionTime)
                .findFirst().orElseGet(() -> dimensions.stream()
                        .filter(SchemaElement::isTimeDimension).findFirst().orElse(null));

        List<SchemaElement> allMetrics = schema.getMetrics(dataSetId);

        if (dateColumn == null || allMetrics.isEmpty()) {
            log.warn("Dashboard re-query: no date column or metrics for dataSet {}", dataSetId);
            QueryResult empty = new QueryResult();
            empty.setQueryMode(DashboardExecutor.QUERY_MODE);
            empty.setQueryState(QueryState.EMPTY);
            empty.setQueryResults(new ArrayList<>());
            empty.setQueryColumns(new ArrayList<>());
            empty.setChatContext(parseInfo);
            return empty;
        }

        // Limit to 4 metrics (same as DashboardExecutor.MAX_KPI_METRICS)
        List<SchemaElement> selectedMetrics = allMetrics.stream().limit(4).toList();

        QueryStructReq structReq = new QueryStructReq();
        structReq.setDataSetId(dataSetId);
        structReq.setGroups(Collections.singletonList(dateColumn.getBizName()));

        List<Aggregator> aggregators = selectedMetrics.stream().map(metric -> {
            Aggregator agg = new Aggregator();
            agg.setColumn(metric.getBizName());
            agg.setFunc(AggOperatorEnum.SUM);
            return agg;
        }).collect(Collectors.toList());
        structReq.setAggregators(aggregators);

        // Use user's date filter if available
        // CRITICAL: dateField must be set for WHERE clause generation
        DateConf dateConf;
        if (parseInfo.getDateInfo() != null && parseInfo.getDateInfo().getStartDate() != null) {
            dateConf = parseInfo.getDateInfo();
        } else {
            dateConf = new DateConf();
            dateConf.setDateMode(DateConf.DateMode.RECENT);
            dateConf.setUnit(30);
            dateConf.setPeriod(DatePeriodEnum.DAY);
        }
        dateConf.setDateField(dateColumn.getBizName());
        structReq.setDateInfo(dateConf);
        structReq.setLimit(30L);

        SemanticQueryResp queryResp = semanticLayerService.queryByReq(structReq, user);

        QueryResult queryResult = new QueryResult();
        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
            queryResult.setQuerySql(queryResp.getSql());
            queryResult.setQueryResults(queryResp.getResultList());
            queryResult.setQueryColumns(queryResp.getColumns());
        } else {
            queryResult.setQueryResults(new ArrayList<>());
            queryResult.setQueryColumns(new ArrayList<>());
        }
        queryResult.setQueryMode(DashboardExecutor.QUERY_MODE);
        queryResult.setQueryState(QueryState.SUCCESS);
        queryResult.setChatContext(parseInfo);

        return queryResult;
    }

    /**
     * Check if a previous execution of this query used DASHBOARD mode. The parse-phase parseInfo
     * may have LLM_S2SQL, but the executor may have overridden it.
     */
    private boolean isDashboardExecution(Long queryId) {
        try {
            ChatQueryDO chatQueryDO = chatManageService.getChatQueryDO(queryId);
            if (chatQueryDO != null && chatQueryDO.getQueryResult() != null) {
                QueryResult savedResult =
                        JSON.parseObject(chatQueryDO.getQueryResult(), QueryResult.class);
                return DashboardExecutor.QUERY_MODE.equals(savedResult.getQueryMode());
            }
        } catch (Exception e) {
            log.debug("Failed to check saved query mode for queryId {}", queryId, e);
        }
        return false;
    }

    private List<String> getFieldsFromSql(SemanticParseInfo parseInfo) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isBlank(sqlInfo.getCorrectedS2SQL())) {
            return new ArrayList<>();
        }
        return SqlSelectHelper.getAllSelectFields(sqlInfo.getCorrectedS2SQL());
    }

    private void handleLLMQueryMode(ChatQueryDataReq chatQueryDataReq, SemanticQuery semanticQuery,
            DataSetSchema dataSetSchema, User user) throws Exception {
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        String rebuiltS2SQL;
        if (checkMetricReplace(chatQueryDataReq, parseInfo)) {
            log.info("rebuild S2SQL with adjusted metrics!");
            SchemaElement metricToReplace = chatQueryDataReq.getMetrics().iterator().next();
            rebuiltS2SQL = replaceMetrics(parseInfo, metricToReplace);
        } else {
            log.info("rebuild S2SQL with adjusted filters!");
            rebuiltS2SQL = replaceFilters(chatQueryDataReq, parseInfo, dataSetSchema);
        }
        // reset SqlInfo and request re-translation
        parseInfo.getSqlInfo().setCorrectedS2SQL(rebuiltS2SQL);
        parseInfo.getSqlInfo().setParsedS2SQL(rebuiltS2SQL);
        parseInfo.getSqlInfo().setQuerySQL(null);
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        SemanticTranslateResp explain = semanticLayerService.translate(semanticQueryReq, user);
        parseInfo.getSqlInfo().setQuerySQL(explain.getQuerySQL());
    }

    private void handleRuleQueryMode(SemanticQuery semanticQuery, DataSetSchema dataSetSchema,
            User user) {
        log.info("rule begin replace metrics and revise filters!");
        validFilter(semanticQuery.getParseInfo().getDimensionFilters());
        validFilter(semanticQuery.getParseInfo().getMetricFilters());
        semanticQuery.buildS2Sql(dataSetSchema);
    }

    private QueryResult executeQuery(SemanticQuery semanticQuery, User user) throws Exception {
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        QueryResult queryResult = doExecution(semanticQueryReq, parseInfo.getQueryMode(), user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        parseInfo.getSqlInfo().setQuerySQL(queryResult.getQuerySql());
        return queryResult;
    }

    private boolean checkMetricReplace(ChatQueryDataReq chatQueryDataReq,
            SemanticParseInfo parseInfo) {
        List<String> oriFields = getFieldsFromSql(parseInfo);
        Set<SchemaElement> metrics = chatQueryDataReq.getMetrics();
        if (CollectionUtils.isEmpty(oriFields) || CollectionUtils.isEmpty(metrics)) {
            return false;
        }
        List<String> metricNames = metrics.stream().map(SchemaElement::getName).toList();
        return !new HashSet<>(oriFields).containsAll(metricNames);
    }

    private String replaceFilters(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
            DataSetSchema dataSetSchema) {
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("correctorSql before replacing:{}", correctorSql);
        // get where filter and having filter
        List<FieldExpression> whereExpressionList =
                SqlSelectHelper.getWhereExpressions(correctorSql);

        // replace where filter
        List<Expression> addWhereConditions = new ArrayList<>();
        Set<String> removeWhereFieldNames =
                updateFilters(whereExpressionList, queryData.getDimensionFilters(),
                        parseInfo.getDimensionFilters(), addWhereConditions);

        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
        Set<String> removeDataFieldNames = updateDateInfo(queryData, parseInfo, dataSetSchema,
                filedNameToValueMap, whereExpressionList, addWhereConditions);
        removeWhereFieldNames.addAll(removeDataFieldNames);

        correctorSql = SqlReplaceHelper.replaceValue(correctorSql, filedNameToValueMap);
        correctorSql = SqlRemoveHelper.removeWhereCondition(correctorSql, removeWhereFieldNames);

        // replace having filter
        List<FieldExpression> havingExpressionList =
                SqlSelectHelper.getHavingExpressions(correctorSql);
        List<Expression> addHavingConditions = new ArrayList<>();
        Set<String> removeHavingFieldNames =
                updateFilters(havingExpressionList, queryData.getDimensionFilters(),
                        parseInfo.getDimensionFilters(), addHavingConditions);
        correctorSql = SqlReplaceHelper.replaceHavingValue(correctorSql, new HashMap<>());
        correctorSql = SqlRemoveHelper.removeHavingCondition(correctorSql, removeHavingFieldNames);

        correctorSql = SqlAddHelper.addWhere(correctorSql, addWhereConditions);
        correctorSql = SqlAddHelper.addHaving(correctorSql, addHavingConditions);
        log.info("correctorSql after replacing:{}", correctorSql);
        return correctorSql;
    }

    private String replaceMetrics(SemanticParseInfo parseInfo, SchemaElement metric) {
        List<String> oriMetrics = parseInfo.getMetrics().stream().map(SchemaElement::getName)
                .collect(Collectors.toList());
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("before replaceMetrics:{}", correctorSql);
        log.info("filteredMetrics:{},metrics:{}", oriMetrics, metric);
        Map<String, Pair<String, String>> fieldMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(oriMetrics) && !oriMetrics.contains(metric.getName())) {
            fieldMap.put(oriMetrics.getFirst(), Pair.of(metric.getName(), metric.getDefaultAgg()));
            correctorSql = SqlReplaceHelper.replaceAggFields(correctorSql, fieldMap);
        }
        log.info("after replaceMetrics:{}", correctorSql);
        return correctorSql;
    }

    private QueryResult doExecution(SemanticQueryReq semanticQueryReq, String queryMode, User user)
            throws Exception {
        SemanticQueryResp queryResp = semanticLayerService.queryByReq(semanticQueryReq, user);
        QueryResult queryResult = new QueryResult();

        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
            queryResult.setQuerySql(queryResp.getSql());
            queryResult.setQueryResults(queryResp.getResultList());
            queryResult.setQueryColumns(queryResp.getColumns());
        } else {
            queryResult.setQueryResults(new ArrayList<>());
            queryResult.setQueryColumns(new ArrayList<>());
        }

        queryResult.setQueryMode(queryMode);
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

    private Set<String> updateDateInfo(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
            DataSetSchema dataSetSchema, Map<String, Map<String, String>> filedNameToValueMap,
            List<FieldExpression> fieldExpressionList, List<Expression> addConditions) {
        Set<String> removeFieldNames = new HashSet<>();
        if (Objects.isNull(queryData.getDateInfo())) {
            return removeFieldNames;
        }
        if (queryData.getDateInfo().getUnit() > 1) {
            queryData.getDateInfo()
                    .setStartDate(DateUtils.getBeforeDate(queryData.getDateInfo().getUnit() + 1));
            queryData.getDateInfo().setEndDate(DateUtils.getBeforeDate(0));
        }
        SchemaElement partitionDimension = dataSetSchema.getPartitionDimension();
        // startDate equals to endDate
        for (FieldExpression fieldExpression : fieldExpressionList) {
            if (partitionDimension.getName().equals(fieldExpression.getFieldName())) {
                // first remove,then add
                removeFieldNames.add(partitionDimension.getName());
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                addTimeFilters(queryData.getDateInfo().getStartDate(), greaterThanEquals,
                        addConditions, partitionDimension);
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                addTimeFilters(queryData.getDateInfo().getEndDate(), minorThanEquals, addConditions,
                        partitionDimension);
                break;
            }
        }
        for (FieldExpression fieldExpression : fieldExpressionList) {
            for (QueryFilter queryFilter : queryData.getDimensionFilters()) {
                if (queryFilter.getOperator().equals(FilterOperatorEnum.LIKE)
                        && FilterOperatorEnum.LIKE.getValue()
                                .equalsIgnoreCase(fieldExpression.getOperator())) {
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
        return removeFieldNames;
    }

    private <T extends ComparisonOperator> void addTimeFilters(String date, T comparisonExpression,
            List<Expression> addConditions, SchemaElement partitionDimension) {
        Column column = new Column(partitionDimension.getName());
        StringValue stringValue = new StringValue(date);
        comparisonExpression.setLeftExpression(column);
        comparisonExpression.setRightExpression(stringValue);
        addConditions.add(comparisonExpression);
    }

    private Set<String> updateFilters(List<FieldExpression> fieldExpressionList,
            Set<QueryFilter> metricFilters, Set<QueryFilter> contextMetricFilters,
            List<Expression> addConditions) {
        Set<String> removeFieldNames = new HashSet<>();
        if (CollectionUtils.isEmpty(metricFilters)) {
            return removeFieldNames;
        }

        for (QueryFilter dslQueryFilter : metricFilters) {
            for (FieldExpression fieldExpression : fieldExpressionList) {
                if (fieldExpression.getFieldName() != null
                        && fieldExpression.getFieldName().contains(dslQueryFilter.getName())) {
                    removeFieldNames.add(dslQueryFilter.getName());
                    handleFilter(dslQueryFilter, contextMetricFilters, addConditions);
                    break;
                }
            }
        }
        return removeFieldNames;
    }

    private void handleFilter(QueryFilter dslQueryFilter, Set<QueryFilter> contextMetricFilters,
            List<Expression> addConditions) {
        FilterOperatorEnum operator = dslQueryFilter.getOperator();

        if (operator == FilterOperatorEnum.IN) {
            addWhereInFilters(dslQueryFilter, new InExpression(), contextMetricFilters,
                    addConditions);
        } else {
            ComparisonOperator expression = FilterOperatorEnum.createExpression(operator);
            if (Objects.nonNull(expression)) {
                addWhereFilters(dslQueryFilter, expression, contextMetricFilters, addConditions);
            }
        }
    }

    // add in condition to sql where condition
    private void addWhereInFilters(QueryFilter dslQueryFilter, InExpression inExpression,
            Set<QueryFilter> contextMetricFilters, List<Expression> addConditions) {
        Column column = new Column(dslQueryFilter.getName());
        ParenthesedExpressionList<Expression> parenthesedExpressionList =
                new ParenthesedExpressionList<>();
        List<String> valueList =
                JsonUtil.toList(JsonUtil.toString(dslQueryFilter.getValue()), String.class);
        if (CollectionUtils.isEmpty(valueList)) {
            return;
        }
        valueList.forEach(o -> {
            StringValue stringValue = new StringValue(o);
            parenthesedExpressionList.add(stringValue);
        });
        inExpression.setLeftExpression(column);
        inExpression.setRightExpression(parenthesedExpressionList);
        addConditions.add(inExpression);
        contextMetricFilters.forEach(o -> {
            if (o.getName().equals(dslQueryFilter.getName())) {
                o.setValue(dslQueryFilter.getValue());
                o.setOperator(dslQueryFilter.getOperator());
            }
        });
    }

    // add where filter
    private void addWhereFilters(QueryFilter dslQueryFilter,
            ComparisonOperator comparisonExpression, Set<QueryFilter> contextMetricFilters,
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
            LongValue longValue =
                    new LongValue(Long.parseLong(dslQueryFilter.getValue().toString()));
            comparisonExpression.setRightExpression(longValue);
        } else {
            StringValue stringValue = new StringValue(dslQueryFilter.getValue().toString());
            comparisonExpression.setRightExpression(stringValue);
        }
        addConditions.add(comparisonExpression);
        contextMetricFilters.forEach(o -> {
            if (o.getName().equals(dslQueryFilter.getName())) {
                o.setValue(dslQueryFilter.getValue());
                o.setOperator(dslQueryFilter.getOperator());
            }
        });
    }

    private void mergeParseInfo(SemanticParseInfo parseInfo, ChatQueryDataReq queryData) {
        if (Objects.nonNull(queryData.getDateInfo())) {
            parseInfo.setDateInfo(queryData.getDateInfo());
        }
        if (LLMSqlQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            return;
        }
        if (!CollectionUtils.isEmpty(queryData.getDimensions())) {
            parseInfo.setDimensions(queryData.getDimensions());
        }
        if (!CollectionUtils.isEmpty(queryData.getMetrics())) {
            parseInfo.setMetrics(queryData.getMetrics());
        }
        if (!CollectionUtils.isEmpty(queryData.getDimensionFilters())) {
            parseInfo.setDimensionFilters(queryData.getDimensionFilters());
        }
        if (!CollectionUtils.isEmpty(queryData.getMetricFilters())) {
            parseInfo.setMetricFilters(queryData.getMetricFilters());
        }

        parseInfo.setSqlInfo(new SqlInfo());
    }

    private void validFilter(Set<QueryFilter> filters) {
        Iterator<QueryFilter> iterator = filters.iterator();
        while (iterator.hasNext()) {
            QueryFilter queryFilter = iterator.next();
            Object queryFilterValue = queryFilter.getValue();
            if (Objects.isNull(queryFilterValue)) {
                iterator.remove();
                continue;
            }
            List<String> collection = new ArrayList<>();
            if (queryFilterValue instanceof List) {
                collection.addAll((List) queryFilterValue);
            } else if (queryFilterValue instanceof String) {
                collection.add((String) queryFilterValue);
            }
            if (FilterOperatorEnum.IN.equals(queryFilter.getOperator())
                    && CollectionUtils.isEmpty(collection)) {
                iterator.remove();
            }
        }
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) {
        Integer agentId = dimensionValueReq.getAgentId();
        Agent agent = agentService.getAgent(agentId);
        dimensionValueReq.setDataSetIds(agent.getDataSetIds());
        return semanticLayerService.queryDimensionValue(dimensionValueReq, user);
    }

    public void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        // The history record only retains the query result of the first parse
        if (chatExecuteReq.getParseId() > 1) {
            return;
        }
        chatManageService.saveQueryResult(chatExecuteReq, queryResult);
    }
}
