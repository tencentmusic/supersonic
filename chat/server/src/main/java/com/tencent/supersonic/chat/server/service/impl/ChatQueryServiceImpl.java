package com.tencent.supersonic.chat.server.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatQueryDataReq;
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
import com.tencent.supersonic.common.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticTranslateResp;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMSqlQuery;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import com.tencent.supersonic.headless.server.facade.service.RetrieveService;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private RetrieveService retrieveService;
    @Autowired
    private AgentService agentService;

    private List<ChatQueryParser> chatQueryParsers = ComponentFactory.getChatParsers();
    private List<ChatQueryExecutor> chatQueryExecutors = ComponentFactory.getChatExecutors();
    private List<ParseResultProcessor> parseResultProcessors = ComponentFactory.getParseProcessors();
    private List<ExecuteResultProcessor> executeResultProcessors = ComponentFactory.getExecuteProcessors();

    @Override
    public List<SearchResult> search(ChatParseReq chatParseReq) {
        ParseContext parseContext = buildParseContext(chatParseReq);
        Agent agent = parseContext.getAgent();
        if (!agent.enableSearch()) {
            return Lists.newArrayList();
        }
        QueryNLReq queryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext);
        return retrieveService.retrieve(queryNLReq);
    }

    @Override
    public ParseResp performParsing(ChatParseReq chatParseReq) {
        ParseResp parseResp = new ParseResp(chatParseReq.getQueryText());
        chatManageService.createChatQuery(chatParseReq, parseResp);
        ParseContext parseContext = buildParseContext(chatParseReq);
        supplyMapInfo(parseContext);
        for (ChatQueryParser chatQueryParser : chatQueryParsers) {
            chatQueryParser.parse(parseContext, parseResp);
        }
        for (ParseResultProcessor processor : parseResultProcessors) {
            processor.process(parseContext, parseResp);
        }
        chatParseReq.setQueryText(parseContext.getQueryText());
        parseResp.setQueryText(parseContext.getQueryText());
        chatManageService.batchAddParse(chatParseReq, parseResp);
        chatManageService.updateParseCostTime(parseResp);
        return parseResp;
    }

    @Override
    public QueryResult performExecution(ChatExecuteReq chatExecuteReq) {
        QueryResult queryResult = new QueryResult();
        ExecuteContext executeContext = buildExecuteContext(chatExecuteReq);
        for (ChatQueryExecutor chatQueryExecutor : chatQueryExecutors) {
            queryResult = chatQueryExecutor.execute(executeContext);
            if (queryResult != null) {
                break;
            }
        }

        if (queryResult != null) {
            for (ExecuteResultProcessor processor : executeResultProcessors) {
                processor.process(executeContext, queryResult);
            }
            saveQueryResult(chatExecuteReq, queryResult);
        }

        return queryResult;
    }

    @Override
    public QueryResult parseAndExecute(int chatId, int agentId, String queryText) {
        ChatParseReq chatParseReq = new ChatParseReq();
        chatParseReq.setQueryText(queryText);
        chatParseReq.setChatId(chatId);
        chatParseReq.setAgentId(agentId);
        chatParseReq.setUser(User.getFakeUser());
        ParseResp parseResp = performParsing(chatParseReq);
        if (CollectionUtils.isEmpty(parseResp.getSelectedParses())) {
            log.debug("chatId:{}, agentId:{}, queryText:{}, parseResp.getSelectedParses() is empty",
                    chatId, agentId, queryText);
            return null;
        }
        ChatExecuteReq executeReq = new ChatExecuteReq();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getSelectedParses().get(0).getId());
        executeReq.setQueryText(queryText);
        executeReq.setChatId(chatId);
        executeReq.setUser(User.getFakeUser());
        executeReq.setAgentId(agentId);
        executeReq.setSaveAnswer(true);
        return performExecution(executeReq);
    }

    private ParseContext buildParseContext(ChatParseReq chatParseReq) {
        ParseContext parseContext = new ParseContext();
        BeanMapper.mapper(chatParseReq, parseContext);
        Agent agent = agentService.getAgent(chatParseReq.getAgentId());
        parseContext.setAgent(agent);
        return parseContext;
    }

    private void supplyMapInfo(ParseContext parseContext) {
        QueryNLReq queryNLReq = QueryReqConverter.buildText2SqlQueryReq(parseContext);
        MapResp mapResp = chatLayerService.performMapping(queryNLReq);
        parseContext.setMapInfo(mapResp.getMapInfo());
    }

    private ExecuteContext buildExecuteContext(ChatExecuteReq chatExecuteReq) {
        ExecuteContext executeContext = new ExecuteContext();
        BeanMapper.mapper(chatExecuteReq, executeContext);
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(
                chatExecuteReq.getQueryId(), chatExecuteReq.getParseId());
        Agent agent = agentService.getAgent(chatExecuteReq.getAgentId());
        executeContext.setAgent(agent);
        executeContext.setParseInfo(parseInfo);
        return executeContext;
    }

    //mainly used for executing after revising filters,for example:"fans_cnt>=100000"->"fans_cnt>500000",
    //"style='流行'"->"style in ['流行','爱国']"
    @Override
    public Object queryData(ChatQueryDataReq chatQueryDataReq, User user) throws Exception {
        Integer parseId = chatQueryDataReq.getParseId();
        SemanticParseInfo parseInfo = chatManageService.getParseInfo(
                chatQueryDataReq.getQueryId(), parseId);
        parseInfo = mergeSemanticParseInfo(parseInfo, chatQueryDataReq);
        DataSetSchema dataSetSchema = semanticLayerService.getDataSetSchema(parseInfo.getDataSetId());

        SemanticQuery semanticQuery = QueryManager.createQuery(parseInfo.getQueryMode());
        semanticQuery.setParseInfo(parseInfo);

        List<String> fields = new ArrayList<>();
        if (Objects.nonNull(parseInfo.getSqlInfo())
                && StringUtils.isNotBlank(parseInfo.getSqlInfo().getCorrectedS2SQL())) {
            String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
            fields = SqlSelectHelper.getAllFields(correctorSql);
        }
        if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())
                && checkMetricReplace(fields, chatQueryDataReq.getMetrics())) {
            //replace metrics
            log.info("llm begin replace metrics!");
            SchemaElement metricToReplace = chatQueryDataReq.getMetrics().iterator().next();
            replaceMetrics(parseInfo, metricToReplace);
        } else if (LLMSqlQuery.QUERY_MODE.equalsIgnoreCase(parseInfo.getQueryMode())) {
            log.info("llm begin revise filters!");
            String correctorSql = reviseCorrectS2SQL(chatQueryDataReq, parseInfo);
            parseInfo.getSqlInfo().setCorrectedS2SQL(correctorSql);
            semanticQuery.setParseInfo(parseInfo);
            SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
            SemanticTranslateResp explain = semanticLayerService.translate(semanticQueryReq, user);
            parseInfo.getSqlInfo().setQuerySQL(explain.getQuerySQL());
        } else {
            log.info("rule begin replace metrics and revise filters!");
            //remove unvalid filters
            validFilter(semanticQuery.getParseInfo().getDimensionFilters());
            validFilter(semanticQuery.getParseInfo().getMetricFilters());
            //init s2sql
            semanticQuery.initS2Sql(dataSetSchema, user);
        }
        SemanticQueryReq semanticQueryReq = semanticQuery.buildSemanticQueryReq();
        QueryResult queryResult = doExecution(semanticQueryReq, semanticQuery.getParseInfo(), user);
        queryResult.setChatContext(semanticQuery.getParseInfo());
        SemanticLayerService semanticService = ContextUtils.getBean(SemanticLayerService.class);
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

    private String reviseCorrectS2SQL(ChatQueryDataReq queryData, SemanticParseInfo parseInfo) {
        Map<String, Map<String, String>> filedNameToValueMap = new HashMap<>();
        Map<String, Map<String, String>> havingFiledNameToValueMap = new HashMap<>();

        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
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
        String correctorSql = parseInfo.getSqlInfo().getCorrectedS2SQL();
        log.info("before replaceMetrics:{}", correctorSql);
        log.info("filteredMetrics:{},metrics:{}", oriMetrics, metric);
        Map<String, Pair<String, String>> fieldMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(oriMetrics) && !oriMetrics.contains(metric.getName())) {
            fieldMap.put(oriMetrics.get(0), Pair.of(metric.getName(), metric.getDefaultAgg()));
            correctorSql = SqlReplaceHelper.replaceAggFields(correctorSql, fieldMap);
        }
        log.info("after replaceMetrics:{}", correctorSql);
        parseInfo.getSqlInfo().setCorrectedS2SQL(correctorSql);
    }

    private QueryResult doExecution(SemanticQueryReq semanticQueryReq,
                                    SemanticParseInfo parseInfo, User user) throws Exception {
        SemanticQueryResp queryResp = semanticLayerService.queryByReq(semanticQueryReq, user);
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

    private void updateDateInfo(ChatQueryDataReq queryData, SemanticParseInfo parseInfo,
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
        if (org.apache.commons.collections.CollectionUtils.isEmpty(metricFilters)) {
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
        if (org.apache.commons.collections.CollectionUtils.isEmpty(valueList)) {
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

    private SemanticParseInfo mergeSemanticParseInfo(SemanticParseInfo parseInfo,
                                                     ChatQueryDataReq queryData) {
        if (LLMSqlQuery.QUERY_MODE.equals(parseInfo.getQueryMode())) {
            return parseInfo;
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
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) {
        Integer agentId = dimensionValueReq.getAgentId();
        Agent agent = agentService.getAgent(agentId);
        dimensionValueReq.setDataSetIds(agent.getDataSetIds());
        return semanticLayerService.queryDimensionValue(dimensionValueReq, user);
    }

    public void saveQueryResult(ChatExecuteReq chatExecuteReq, QueryResult queryResult) {
        //The history record only retains the query result of the first parse
        if (chatExecuteReq.getParseId() > 1) {
            return;
        }
        chatManageService.saveQueryResult(chatExecuteReq, queryResult);
    }

}
