package com.tencent.supersonic.chat.service.impl;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.persistence.dataobject.ChatParseDO;
import com.tencent.supersonic.chat.persistence.dataobject.CostType;
import com.tencent.supersonic.chat.persistence.dataobject.StatisticsDO;
import com.tencent.supersonic.chat.query.QuerySelector;
import com.tencent.supersonic.chat.api.pojo.request.QueryDataReq;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.service.StatisticsService;
import com.tencent.supersonic.chat.utils.ComponentFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

//import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
//import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.beans.BeanUtils;
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

    private final String entity = "ENTITY";

    @Value("${time.threshold: 100}")
    private Integer timeThreshold;

    private List<SchemaMapper> schemaMappers = ComponentFactory.getSchemaMappers();
    private List<SemanticParser> semanticParsers = ComponentFactory.getSemanticParsers();
    private QuerySelector querySelector = ComponentFactory.getQuerySelector();

    @Override
    public ParseResp performParsing(QueryReq queryReq) {
        QueryContext queryCtx = new QueryContext(queryReq);
        // in order to support multi-turn conversation, chat context is needed
        ChatContext chatCtx = chatService.getOrCreateContext(queryReq.getChatId());
        List<StatisticsDO> timeCostDOList = new ArrayList<>();
        schemaMappers.stream().forEach(mapper -> {
            Long startTime = System.currentTimeMillis();
            mapper.map(queryCtx);
            Long endTime = System.currentTimeMillis();
            String className = mapper.getClass().getSimpleName();
            timeCostDOList.add(StatisticsDO.builder().cost((int) (endTime - startTime))
                    .interfaceName(className).type(CostType.MAPPER.getType()).build());
            log.info("{} result:{}", className, JsonUtil.toString(queryCtx));
        });
        semanticParsers.stream().forEach(parser -> {
            Long startTime = System.currentTimeMillis();
            parser.parse(queryCtx, chatCtx);
            Long endTime = System.currentTimeMillis();
            String className = parser.getClass().getSimpleName();
            timeCostDOList.add(StatisticsDO.builder().cost((int) (endTime - startTime))
                    .interfaceName(className).type(CostType.PARSER.getType()).build());
            log.info("{} result:{}", className, JsonUtil.toString(queryCtx));
        });
        ParseResp parseResult;
        if (queryCtx.getCandidateQueries().size() > 0) {
            log.debug("pick before [{}]", queryCtx.getCandidateQueries().stream().collect(
                    Collectors.toList()));
            List<SemanticQuery> selectedQueries = querySelector.select(queryCtx.getCandidateQueries(), queryReq);
            log.debug("pick after [{}]", selectedQueries.stream().collect(
                    Collectors.toList()));

            List<SemanticParseInfo> selectedParses = selectedQueries.stream()
                    .map(SemanticQuery::getParseInfo)
                    .sorted(Comparator.comparingDouble(SemanticParseInfo::getScore).reversed())
                    .collect(Collectors.toList());
            selectedParses.forEach(parseInfo -> {
                if (parseInfo.getQueryMode().contains(entity)) {
                    EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class)
                            .getEntityInfo(parseInfo, queryReq.getUser());
                    parseInfo.setEntityInfo(entityInfo);
                }
            });
            List<SemanticParseInfo> candidateParses = queryCtx.getCandidateQueries().stream()
                    .map(SemanticQuery::getParseInfo).collect(Collectors.toList());
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
        return parseResult;
    }

    @Override
    public QueryResult performExecution(ExecuteQueryReq queryReq) throws Exception {
        ChatParseDO chatParseDO = chatService.getParseInfo(queryReq.getQueryId(),
                queryReq.getUser().getName(), queryReq.getParseId());
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
        Long endTime = System.currentTimeMillis();
        if (queryResult != null) {
            timeCostDOList.add(StatisticsDO.builder().cost((int) (endTime - startTime))
                    .interfaceName(semanticQuery.getClass().getSimpleName()).type(CostType.QUERY.getType()).build());
            saveInfo(timeCostDOList, queryReq.getQueryText(), queryReq.getQueryId(),
                    queryReq.getUser().getName(), queryReq.getChatId().longValue());
            queryResult.setChatContext(parseInfo);
            // update chat context after a successful semantic query
            if (queryReq.isSaveAnswer() && QueryState.SUCCESS.equals(queryResult.getQueryState())) {
                chatCtx.setParseInfo(parseInfo);
                chatService.updateContext(chatCtx);
            }
            chatCtx.setQueryText(queryReq.getQueryText());
            chatCtx.setUser(queryReq.getUser().getName());
            //chatService.addQuery(queryResult, chatCtx);
            chatService.updateQuery(queryReq.getQueryId(), queryResult, chatCtx);
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
        SemanticQuery semanticQuery = QueryManager.createRuleQuery(queryData.getQueryMode());
        BeanUtils.copyProperties(queryData, semanticQuery.getParseInfo());
        return semanticQuery.execute(user);
    }

    @Override
    public Object queryDimensionValue(DimensionValueReq dimensionValueReq, User user) throws Exception {
        com.tencent.supersonic.semantic.query.service.QueryService queryService =
                ContextUtils.getBean(com.tencent.supersonic.semantic.query.service.QueryService.class);
        QueryStructReq queryStructReq = new QueryStructReq();

        DateConf dateConf = new DateConf();
        dateConf.setDateMode(DateConf.DateMode.RECENT);
        dateConf.setUnit(1);
        dateConf.setPeriod("DAY");
        queryStructReq.setDateInfo(dateConf);
        queryStructReq.setLimit(20L);

        //        List<Aggregator> aggregators = new ArrayList<>();
        //        Aggregator aggregator = new Aggregator(dimensionValueReq.getQueryFilter().getBizName(),
        //                AggOperatorEnum.DISTINCT);
        //        aggregators.add(aggregator);
        //        queryStructReq.setAggregators(aggregators);

        queryStructReq.setModelId(dimensionValueReq.getModelId());
        queryStructReq.setNativeQuery(true);
        List<String> groups = new ArrayList<>();
        groups.add(dimensionValueReq.getBizName());
        queryStructReq.setGroups(groups);
        if (Objects.isNull(dimensionValueReq.getValue())) {
            List<Filter> dimensionFilters = new ArrayList<>();
            Filter dimensionFilter = new Filter();
            dimensionFilter.setOperator(FilterOperatorEnum.LIKE);
            dimensionFilter.setRelation(Filter.Relation.FILTER);
            dimensionFilter.setBizName(dimensionValueReq.getBizName());
            dimensionFilter.setValue(dimensionValueReq.getValue());
            dimensionFilters.add(dimensionFilter);
            queryStructReq.setDimensionFilters(dimensionFilters);
        }
        QueryResultWithSchemaResp queryResultWithSchemaResp = queryService.queryByStructWithAuth(queryStructReq, user);
        Set<String> dimensionValues = new HashSet<>();
        queryResultWithSchemaResp.getResultList().removeIf(o -> {
            if (dimensionValues.contains(o.get(dimensionValueReq.getBizName()))) {
                return true;
            } else {
                dimensionValues.add(o.get(dimensionValueReq.getBizName()).toString());
                return false;
            }
        });
        return queryResultWithSchemaResp;
    }

}

