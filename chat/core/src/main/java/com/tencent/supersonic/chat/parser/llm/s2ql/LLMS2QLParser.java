package com.tencent.supersonic.chat.parser.llm.s2ql;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.CommonAgentTool;
import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.LLMParserConfig;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMResp;
import com.tencent.supersonic.chat.query.llm.s2ql.S2QLQuery;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FilterExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class LLMS2QLParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        QueryReq request = queryCtx.getRequest();
        LLMParserConfig llmParserConfig = ContextUtils.getBean(LLMParserConfig.class);
        if (StringUtils.isEmpty(llmParserConfig.getUrl())) {
            log.info("llm parser url is empty, skip {} , llmParserConfig:{}", LLMS2QLParser.class, llmParserConfig);
            return;
        }
        if (SatisfactionChecker.check(queryCtx)) {
            log.info("skip {}, queryText:{}", LLMS2QLParser.class, request.getQueryText());
            return;
        }
        try {
            Long modelId = getModelId(queryCtx, chatCtx, request.getAgentId());
            if (Objects.isNull(modelId) || modelId <= 0) {
                return;
            }

            CommonAgentTool commonAgentTool = getParserTool(request, modelId);
            if (Objects.isNull(commonAgentTool)) {
                log.info("no tool in this agent, skip {}", LLMS2QLParser.class);
                return;
            }

            LLMReq llmReq = getLlmReq(queryCtx, modelId, llmParserConfig);
            LLMResp llmResp = requestLLM(llmReq, modelId, llmParserConfig);

            if (Objects.isNull(llmResp)) {
                return;
            }
            ParseResult parseResult = ParseResult.builder().request(request)
                    .commonAgentTool(commonAgentTool).llmReq(llmReq).llmResp(llmResp).build();

            SemanticParseInfo parseInfo = getParseInfo(queryCtx, modelId, commonAgentTool, parseResult);

            SemanticCorrectInfo semanticCorrectInfo = getCorrectorSql(queryCtx, parseInfo, llmResp.getSqlOutput());

            llmResp.setCorrectorSql(semanticCorrectInfo.getSql());

            updateParseInfo(semanticCorrectInfo, modelId, parseInfo);

        } catch (Exception e) {
            log.error("LLMS2QLParser error", e);
        }
    }

    private Set<SchemaElement> getElements(Long modelId, List<String> allFields, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> modelId.equals(schemaElement.getModel())
                        && allFields.contains(schemaElement.getName())
                ).collect(Collectors.toSet());
    }

    private List<String> getFieldsExceptDate(List<String> allFields) {
        if (CollectionUtils.isEmpty(allFields)) {
            return new ArrayList<>();
        }
        return allFields.stream()
                .filter(entry -> !DateUtils.DATE_FIELD.equalsIgnoreCase(entry))
                .collect(Collectors.toList());
    }

    public void updateParseInfo(SemanticCorrectInfo semanticCorrectInfo, Long modelId, SemanticParseInfo parseInfo) {

        String correctorSql = semanticCorrectInfo.getSql();
        parseInfo.getSqlInfo().setLogicSql(correctorSql);

        List<FilterExpression> expressions = SqlParserSelectHelper.getFilterExpression(correctorSql);
        //set dataInfo
        try {
            if (!CollectionUtils.isEmpty(expressions)) {
                DateConf dateInfo = getDateInfo(expressions);
                parseInfo.setDateInfo(dateInfo);
            }
        } catch (Exception e) {
            log.error("set dateInfo error :", e);
        }

        //set filter
        try {
            Map<String, SchemaElement> fieldNameToElement = getNameToElement(modelId);
            List<QueryFilter> result = getDimensionFilter(fieldNameToElement, expressions);
            parseInfo.getDimensionFilters().addAll(result);
        } catch (Exception e) {
            log.error("set dimensionFilter error :", e);
        }

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        if (Objects.isNull(semanticSchema)) {
            return;
        }
        List<String> allFields = getFieldsExceptDate(SqlParserSelectHelper.getAllFields(semanticCorrectInfo.getSql()));

        Set<SchemaElement> metrics = getElements(modelId, allFields, semanticSchema.getMetrics());
        parseInfo.setMetrics(metrics);

        if (SqlParserSelectFunctionHelper.hasAggregateFunction(semanticCorrectInfo.getSql())) {
            parseInfo.setNativeQuery(false);
            List<String> groupByFields = SqlParserSelectHelper.getGroupByFields(semanticCorrectInfo.getSql());
            List<String> groupByDimensions = getFieldsExceptDate(groupByFields);
            parseInfo.setDimensions(getElements(modelId, groupByDimensions, semanticSchema.getDimensions()));
        } else {
            parseInfo.setNativeQuery(true);
            List<String> selectFields = SqlParserSelectHelper.getSelectFields(semanticCorrectInfo.getSql());
            List<String> selectDimensions = getFieldsExceptDate(selectFields);
            parseInfo.setDimensions(getElements(modelId, selectDimensions, semanticSchema.getDimensions()));
        }
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
                .filter(expression -> DateUtils.DATE_FIELD.equalsIgnoreCase(expression.getFieldName()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dateExpressions)) {
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

    private SemanticCorrectInfo getCorrectorSql(QueryContext queryCtx, SemanticParseInfo parseInfo, String sql) {

        SemanticCorrectInfo correctInfo = SemanticCorrectInfo.builder()
                .queryFilters(queryCtx.getRequest().getQueryFilters()).sql(sql)
                .parseInfo(parseInfo).build();

        List<SemanticCorrector> corrections = ComponentFactory.getSqlCorrections();

        corrections.forEach(correction -> {
            try {
                correction.correct(correctInfo);
                log.info("sqlCorrection:{} sql:{}", correction.getClass().getSimpleName(), correctInfo.getSql());
            } catch (Exception e) {
                log.error(String.format("correct error,correctInfo:%s", correctInfo), e);
            }
        });
        return correctInfo;
    }

    private SemanticParseInfo getParseInfo(QueryContext queryCtx, Long modelId, CommonAgentTool commonAgentTool,
                                           ParseResult parseResult) {
        PluginSemanticQuery semanticQuery = QueryManager.createPluginQuery(S2QLQuery.QUERY_MODE);
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(modelId));

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, parseResult);
        properties.put("type", "internal");
        properties.put("name", commonAgentTool.getName());

        parseInfo.setProperties(properties);
        parseInfo.setScore(queryCtx.getRequest().getQueryText().length());
        parseInfo.setQueryMode(semanticQuery.getQueryMode());
        parseInfo.getSqlInfo().setS2QL(parseResult.getLlmResp().getSqlOutput());

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();

        SchemaElement model = new SchemaElement();
        model.setModel(modelId);
        model.setId(modelId);
        model.setName(modelIdToName.get(modelId));
        parseInfo.setModel(model);
        queryCtx.getCandidateQueries().add(semanticQuery);
        return parseInfo;
    }

    private CommonAgentTool getParserTool(QueryReq request, Long modelId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        List<CommonAgentTool> commonAgentTools = agentService.getParserTools(request.getAgentId(),
                AgentToolType.LLM_S2QL);
        Optional<CommonAgentTool> llmParserTool = commonAgentTools.stream()
                .filter(tool -> {
                    List<Long> modelIds = tool.getModelIds();
                    if (agentService.containsAllModel(new HashSet<>(modelIds))) {
                        return true;
                    }
                    return modelIds.contains(modelId);
                })
                .findFirst();
        return llmParserTool.orElse(null);
    }

    private Long getModelId(QueryContext queryCtx, ChatContext chatCtx, Integer agentId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Set<Long> distinctModelIds = agentService.getModelIds(agentId, AgentToolType.LLM_S2QL);
        if (agentService.containsAllModel(distinctModelIds)) {
            distinctModelIds = new HashSet<>();
        }
        ModelResolver modelResolver = ComponentFactory.getModelResolver();
        Long modelId = modelResolver.resolve(queryCtx, chatCtx, distinctModelIds);
        log.info("resolve modelId:{},llmParser Models:{}", modelId, distinctModelIds);
        return modelId;
    }

    private LLMResp requestLLM(LLMReq llmReq, Long modelId, LLMParserConfig llmParserConfig) {
        String questUrl = llmParserConfig.getUrl() + llmParserConfig.getQueryToSqlPath();
        long startTime = System.currentTimeMillis();
        log.info("requestLLM request, modelId:{},llmReq:{}", modelId, llmReq);
        RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(llmReq), headers);
            ResponseEntity<LLMResp> responseEntity = restTemplate.exchange(questUrl, HttpMethod.POST, entity,
                    LLMResp.class);

            log.info("requestLLM response,cost:{}, questUrl:{} \n entity:{} \n body:{}",
                    System.currentTimeMillis() - startTime, questUrl, entity, responseEntity.getBody());
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("requestLLM error", e);
        }
        return null;
    }

    private LLMReq getLlmReq(QueryContext queryCtx, Long modelId, LLMParserConfig llmParserConfig) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();
        String queryText = queryCtx.getRequest().getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setModelName(modelIdToName.get(modelId));
        llmSchema.setDomainName(modelIdToName.get(modelId));

        List<String> fieldNameList = getFieldNameList(queryCtx, modelId, semanticSchema, llmParserConfig);

        fieldNameList.add(DateUtils.DATE_FIELD);
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);

        List<ElementValue> linking = new ArrayList<>();
        linking.addAll(getValueList(queryCtx, modelId, semanticSchema));
        llmReq.setLinking(linking);

        String currentDate = S2QLDateHelper.getReferenceDate(modelId);
        if (StringUtils.isEmpty(currentDate)) {
            currentDate = DateUtils.getBeforeDate(0);
        }
        llmReq.setCurrentDate(currentDate);
        return llmReq;
    }

    protected List<ElementValue> getValueList(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = getItemIdToName(modelId, semanticSchema);

        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(modelId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<ElementValue> valueMatches = matchedElements
                .stream()
                .filter(elementMatch -> !elementMatch.isInherited())
                .filter(schemaElementMatch -> {
                    SchemaElementType type = schemaElementMatch.getElement().getType();
                    return SchemaElementType.VALUE.equals(type) || SchemaElementType.ID.equals(type);
                })
                .map(elementMatch -> {
                    ElementValue elementValue = new ElementValue();
                    elementValue.setFieldName(itemIdToName.get(elementMatch.getElement().getId()));
                    elementValue.setFieldValue(elementMatch.getWord());
                    return elementValue;
                }).collect(Collectors.toSet());
        return new ArrayList<>(valueMatches);
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
                    if (!CollectionUtils.isEmpty(aliasList)) {
                        for (String alias : aliasList) {
                            result.add(Pair.of(alias, schemaElement));
                        }
                    }
                    return result.stream();
                })
                .collect(Collectors.toMap(pair -> pair.getLeft(), pair -> pair.getRight(), (value1, value2) -> value2));
    }


    protected List<String> getFieldNameList(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema,
                                            LLMParserConfig llmParserConfig) {

        Set<String> results = getTopNFieldNames(modelId, semanticSchema, llmParserConfig);

        Set<String> fieldNameList = getMatchedFieldNames(queryCtx, modelId, semanticSchema);

        results.addAll(fieldNameList);
        return new ArrayList<>(results);
    }

    protected Set<String> getMatchedFieldNames(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = getItemIdToName(modelId, semanticSchema);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(modelId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new HashSet<>();
        }
        Set<String> fieldNameList = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.METRIC.equals(elementType)
                            || SchemaElementType.DIMENSION.equals(elementType)
                            || SchemaElementType.VALUE.equals(elementType);
                })
                .map(schemaElementMatch -> {
                    SchemaElement element = schemaElementMatch.getElement();
                    SchemaElementType elementType = element.getType();
                    if (SchemaElementType.VALUE.equals(elementType)) {
                        return itemIdToName.get(element.getId());
                    }
                    return schemaElementMatch.getWord();
                })
                .collect(Collectors.toSet());
        return fieldNameList;
    }

    private Set<String> getTopNFieldNames(Long modelId, SemanticSchema semanticSchema,
                                          LLMParserConfig llmParserConfig) {
        Set<String> results = semanticSchema.getDimensions(modelId).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getDimensionTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());

        Set<String> metrics = semanticSchema.getMetrics(modelId).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getMetricTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());

        results.addAll(metrics);
        return results;
    }

    protected Map<Long, String> getItemIdToName(Long modelId, SemanticSchema semanticSchema) {
        return semanticSchema.getDimensions(modelId).stream()
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

}
