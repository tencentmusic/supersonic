package com.tencent.supersonic.chat.server.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.executor.ChatQueryExecutor;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatQueryService;
import com.tencent.supersonic.chat.server.util.ComponentFactory;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.jsqlparser.*;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatQueryServiceImpl implements ChatQueryService {

    @Autowired
    private ChatManageService chatManageService;
    @Autowired
    private ChatLayerService chatLayerService;
    @Autowired
    private SemanticLayerService semanticLayerService;
    @Autowired
    private AgentService agentService;

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
        }

        ParseContext parseContext = buildParseContext(chatParseReq, new ChatParseResp(queryId));
        chatQueryParsers.forEach(p -> p.parse(parseContext));

        for (ParseResultProcessor processor : parseResultProcessors) {
            if (processor.accept(parseContext)) {
                processor.process(parseContext);
            }
        }

        if (!parseContext.needFeedback()) {
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
            queryResult = chatQueryExecutor.execute(executeContext);
            if (queryResult != null) {
                break;
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
        executeReq.setParseId(parseResp.getSelectedParses().get(0).getId());
        executeReq.setQueryText(chatParseReq.getQueryText());
        executeReq.setChatId(chatParseReq.getChatId());
        executeReq.setUser(User.getDefaultUser());
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
        SemanticParseInfo parseInfo =
                chatManageService.getParseInfo(chatQueryDataReq.getQueryId(), parseId);
        mergeParseInfo(parseInfo, chatQueryDataReq);
        DataSetSchema dataSetSchema =
                semanticLayerService.getDataSetSchema(parseInfo.getDataSetId());

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        semanticQuery.setParseInfo(parseInfo);

        if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())) {
            handleLLMQueryMode(chatQueryDataReq, semanticQuery, dataSetSchema, user);
        } else {
            handleRuleQueryMode(semanticQuery, dataSetSchema, user);
        }

        return executeQuery(semanticQuery, user);
    }

    private List<String> getFieldsFromSql(SemanticParseInfo parseInfo) {
        SqlInfo sqlInfo = parseInfo.getSqlInfo();
        if (Objects.isNull(sqlInfo) || StringUtils.isNotBlank(sqlInfo.getCorrectedS2SQL())) {
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
        List<String> metricNames =
                metrics.stream().map(SchemaElement::getName).collect(Collectors.toList());
        return !oriFields.containsAll(metricNames);
    }

    private String replaceFilters(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
            DataSetSchema dataSetSchema) {
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("correctorSql before replacing:{}", correctorSql);

        if (shouldCallReplaceFiltersTest(correctorSql)) {
            log.info("校验sql结构存在子查询，使用replaceFiltersTest解析sql");
            correctorSql = replaceFiltersTest(queryData, parseInfo, dataSetSchema);
            return correctorSql;
        }
        log.info("校验sql结构不存在子查询，使用replaceFilters解析sql");
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

    private String replaceFiltersTest(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
            DataSetSchema dataSetSchema) {
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("correctorSql before replacing:{}", correctorSql);

        Select selectStatement = SqlSelectHelper.getSelect(correctorSql);
        PlainSelect plainSelect = (PlainSelect) selectStatement;

        Select fromItemSelect = plainSelect.getFromItem(ParenthesedSelect.class).getSelect();
        log.info("fromItemSelect is:{}", fromItemSelect);

        List<Join> joins = plainSelect.getJoins();
        FromItem fromItem = joins.get(0).getFromItem();
        ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
        Select JoinItemSelect = parenthesedSelect.getSelect();
        log.info("JoinItemSelect is:{}", JoinItemSelect);

        ArrayList<Select> selectArrayList = new ArrayList<>();
        selectArrayList.add(fromItemSelect);
        selectArrayList.add(JoinItemSelect);
        List<String> modifiedSubQueries = new ArrayList<>();
        selectArrayList.forEach(select -> {
            String selectSql = select.toString();
            log.info("selectSql is:{}", selectSql);

            Map<String, String> dateRange = extractDateRangeFromSql(selectSql);
            log.info("Extracted date range: {}", dateRange);

            // get where filter and having filter
            List<FieldExpression> whereExpressionList =
                    SqlSelectHelper.getWhereExpressions(selectSql);

            // replace where filter
            List<Expression> addWhereConditions = new ArrayList<>();
            Set<String> removeWhereFieldNames =
                    updateFilters(whereExpressionList, queryData.getDimensionFilters(),
                            parseInfo.getDimensionFilters(), addWhereConditions);

            Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
            Set<String> removeDataFieldNames =
                    updateDateInfoTest(queryData, parseInfo, dataSetSchema, filedNameToValueMap,
                            whereExpressionList, addWhereConditions, dateRange);
            removeWhereFieldNames.addAll(removeDataFieldNames);

            selectSql = SqlReplaceHelper.replaceValue(selectSql, filedNameToValueMap);
            selectSql = SqlRemoveHelper.removeWhereCondition(selectSql, removeWhereFieldNames);

            // replace having filter
            List<FieldExpression> havingExpressionList =
                    SqlSelectHelper.getHavingExpressions(selectSql);
            List<Expression> addHavingConditions = new ArrayList<>();
            Set<String> removeHavingFieldNames =
                    updateFilters(havingExpressionList, queryData.getDimensionFilters(),
                            parseInfo.getDimensionFilters(), addHavingConditions);
            selectSql = SqlReplaceHelper.replaceHavingValue(selectSql, new HashMap<>());
            selectSql = SqlRemoveHelper.removeHavingCondition(selectSql, removeHavingFieldNames);

            selectSql = SqlAddHelper.addWhere(selectSql, addWhereConditions);
            selectSql = SqlAddHelper.addHaving(selectSql, addHavingConditions);
            log.info("selectSql after replacing:{}", selectSql);
            modifiedSubQueries.add(selectSql);
        });

        correctorSql = rebuildCorrectorSql(correctorSql, modifiedSubQueries);
        log.info("correctorSql after replacing:{}", correctorSql);
        return correctorSql;
    }

    private boolean shouldCallReplaceFiltersTest(String correctorSql) {
        Select selectStatement = SqlSelectHelper.getSelect(correctorSql);
        if (!(selectStatement instanceof PlainSelect)) {
            throw new IllegalArgumentException("修正S2SQL的结构有误！");
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement;

        FromItem fromItem = plainSelect.getFromItem();
        List<Join> joins = plainSelect.getJoins();
        Expression where = plainSelect.getWhere();

        if (fromItem != null && joins != null && !joins.isEmpty() && where == null) {
            log.info("fromItem和joins不为null，where为null，返回true。");
            return true;
        }
        return false;
    }

    private String rebuildCorrectorSql(String correctorSql, List<String> modifiedSubQueries) {
        Select selectStatement = SqlSelectHelper.getSelect(correctorSql);
        if (!(selectStatement instanceof PlainSelect)) {
            throw new IllegalArgumentException("修正S2SQL的结构有误！");
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement;

        ParenthesedSelect fromItem = (ParenthesedSelect) plainSelect.getFromItem();
        Select fromItemSelect = fromItem.getSelect();

        String modifiedT1 = modifiedSubQueries.get(0);
        Select modifiedFromItemSelect = SqlSelectHelper.getSelect(modifiedT1);
        fromItem.setSelect(modifiedFromItemSelect);

        List<Join> joins = plainSelect.getJoins();
        if (joins == null || joins.isEmpty()) {
            throw new IllegalArgumentException("No JOIN found in correctorSql");
        }

        Join join = joins.get(0);
        ParenthesedSelect joinFromItem = (ParenthesedSelect) join.getRightItem();

        String modifiedT2 = modifiedSubQueries.get(1);
        Select modifiedJoinItemSelect = SqlSelectHelper.getSelect(modifiedT2);
        joinFromItem.setSelect(modifiedJoinItemSelect);

        String finalSql = selectStatement.toString();
        log.info("Rebuilt correctorSql: {}", finalSql);

        return finalSql;
    }

    private Map<String, String> extractDateRangeFromSql(String sql) {
        Map<String, String> dateRange = new HashMap<>();
        Select select = SqlSelectHelper.getSelect(sql);
        if (!(select instanceof PlainSelect plainSelect)) {
            return dateRange;
        }

        Expression where = plainSelect.getWhere();
        if (where == null) {
            return dateRange;
        }

        extractDateRangeFromExpression(where, dateRange);
        return dateRange;
    }

    private void extractDateRangeFromExpression(Expression expression,
            Map<String, String> dateRange) {
        if (expression instanceof EqualsTo equalsTo) {
            if (equalsTo.getLeftExpression() instanceof Column
                    && "数据日期".equals(((Column) equalsTo.getLeftExpression()).getColumnName())) {
                if (equalsTo.getRightExpression() instanceof StringValue) {
                    String date = ((StringValue) equalsTo.getRightExpression()).getValue();
                    dateRange.put("startDate", date);
                    dateRange.put("endDate", date);
                }
            }
        } else if (expression instanceof GreaterThanEquals greaterThanEquals) {
            if (greaterThanEquals.getLeftExpression() instanceof Column && "数据日期"
                    .equals(((Column) greaterThanEquals.getLeftExpression()).getColumnName())) {
                if (greaterThanEquals.getRightExpression() instanceof StringValue) {
                    dateRange.put("startDate",
                            ((StringValue) greaterThanEquals.getRightExpression()).getValue());
                }
            }
        } else if (expression instanceof GreaterThan greaterThan) {
            if (greaterThan.getLeftExpression() instanceof Column
                    && "数据日期".equals(((Column) greaterThan.getLeftExpression()).getColumnName())) {
                if (greaterThan.getRightExpression() instanceof StringValue) {
                    String originalDate =
                            ((StringValue) greaterThan.getRightExpression()).getValue();
                    String adjustedDate = DateUtils.getNextDate(originalDate);
                    dateRange.put("startDate", adjustedDate);
                }
            }
        } else if (expression instanceof MinorThanEquals minorThanEquals) {
            if (minorThanEquals.getLeftExpression() instanceof Column && "数据日期"
                    .equals(((Column) minorThanEquals.getLeftExpression()).getColumnName())) {
                if (minorThanEquals.getRightExpression() instanceof StringValue) {
                    dateRange.put("endDate",
                            ((StringValue) minorThanEquals.getRightExpression()).getValue());
                }
            }
        } else if (expression instanceof MinorThan minorThan) {
            if (minorThan.getLeftExpression() instanceof Column
                    && "数据日期".equals(((Column) minorThan.getLeftExpression()).getColumnName())) {
                if (minorThan.getRightExpression() instanceof StringValue) {
                    String originalDate = ((StringValue) minorThan.getRightExpression()).getValue();
                    String adjustedDate = DateUtils.getPreviousDate(originalDate);
                    dateRange.put("endDate", adjustedDate);
                }
            }
        } else if (expression instanceof AndExpression andExpression) {
            // 递归处理 AND 条件
            extractDateRangeFromExpression(andExpression.getLeftExpression(), dateRange);
            extractDateRangeFromExpression(andExpression.getRightExpression(), dateRange);
        }
    }


    private Set<String> updateDateInfoTest(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
            DataSetSchema dataSetSchema, Map<String, Map<String, String>> filedNameToValueMap,
            List<FieldExpression> fieldExpressionList, List<Expression> addConditions,
            Map<String, String> dateRange) {
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

        String startDate = dateRange.get("startDate");
        String endDate = dateRange.get("endDate");
        // startDate equals to endDate
        for (FieldExpression fieldExpression : fieldExpressionList) {
            if (partitionDimension.getName().equals(fieldExpression.getFieldName())) {
                // first remove,then add
                removeFieldNames.add(partitionDimension.getName());
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                String greaterThanEqualsDate;
                String minorThanEqualsDate;
                if (startDate != null) {
                    greaterThanEqualsDate = startDate;
                } else {
                    greaterThanEqualsDate = queryData.getDateInfo().getStartDate();
                }
                addTimeFilters(greaterThanEqualsDate, greaterThanEquals, addConditions,
                        partitionDimension);
                if (endDate != null) {
                    minorThanEqualsDate = endDate;
                } else {
                    minorThanEqualsDate = queryData.getDateInfo().getEndDate();
                }
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                addTimeFilters(minorThanEqualsDate, minorThanEquals, addConditions,
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

    private String replaceMetrics(SemanticParseInfo parseInfo, SchemaElement metric) {
        List<String> oriMetrics = parseInfo.getMetrics().stream().map(SchemaElement::getName)
                .collect(Collectors.toList());
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("before replaceMetrics:{}", correctorSql);
        log.info("filteredMetrics:{},metrics:{}", oriMetrics, metric);
        Map<String, Pair<String, String>> fieldMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(oriMetrics) && !oriMetrics.contains(metric.getName())) {
            fieldMap.put(oriMetrics.get(0), Pair.of(metric.getName(), metric.getDefaultAgg()));
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
        ParenthesedExpressionList parenthesedExpressionList = new ParenthesedExpressionList<>();
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
