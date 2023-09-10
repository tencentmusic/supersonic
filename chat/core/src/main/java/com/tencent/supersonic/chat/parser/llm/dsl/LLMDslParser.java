package com.tencent.supersonic.chat.parser.llm.dsl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.DslTool;
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
import com.tencent.supersonic.chat.config.LLMConfig;
import com.tencent.supersonic.chat.corrector.BaseSemanticCorrector;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.parser.plugin.function.ModelResolver;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.llm.dsl.DslQuery;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.dsl.LLMResp;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.FilterExpression;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class LLMDslParser implements SemanticParser {

    public static final double function_bonus_threshold = 201;

    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        QueryReq request = queryCtx.getRequest();
        LLMConfig llmConfig = ContextUtils.getBean(LLMConfig.class);
        if (StringUtils.isEmpty(llmConfig.getUrl()) || SatisfactionChecker.check(queryCtx)) {
            log.info("llmConfig:{}, skip dsl parser, queryText:{}", llmConfig, request.getQueryText());
            return;
        }
        try {
            Long modelId = getModelId(queryCtx, chatCtx, request.getAgentId());
            if (Objects.isNull(modelId) || modelId <= 0) {
                return;
            }

            DslTool dslTool = getDslTool(request, modelId);
            if (Objects.isNull(dslTool)) {
                log.info("no dsl tool in this agent, skip dsl parser");
                return;
            }

            LLMReq llmReq = getLlmReq(queryCtx, modelId);
            LLMResp llmResp = requestLLM(llmReq, modelId, llmConfig);

            if (Objects.isNull(llmResp)) {
                return;
            }
            DSLParseResult dslParseResult = DSLParseResult.builder().request(request).dslTool(dslTool).llmReq(llmReq)
                    .llmResp(llmResp).build();

            SemanticParseInfo parseInfo = getParseInfo(queryCtx, modelId, dslTool, dslParseResult);

            SemanticCorrectInfo semanticCorrectInfo = getCorrectorSql(queryCtx, parseInfo, llmResp.getSqlOutput());

            llmResp.setCorrectorSql(semanticCorrectInfo.getSql());

            setFilter(semanticCorrectInfo, modelId, parseInfo);

            setDimensionsAndMetrics(modelId, parseInfo, semanticCorrectInfo.getSql());

        } catch (Exception e) {
            log.error("LLMDSLParser error", e);
        }
    }

    private void setDimensionsAndMetrics(Long modelId, SemanticParseInfo parseInfo, String sql) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        if (Objects.isNull(semanticSchema)) {
            return;
        }
        List<String> allFields = getFieldsExceptDate(sql);

        Set<SchemaElement> metrics = getElements(modelId, allFields, semanticSchema.getMetrics());
        parseInfo.setMetrics(metrics);

        Set<SchemaElement> dimensions = getElements(modelId, allFields, semanticSchema.getDimensions());
        parseInfo.setDimensions(dimensions);
    }

    private Set<SchemaElement> getElements(Long modelId, List<String> allFields, List<SchemaElement> elements) {
        return elements.stream()
                .filter(schemaElement -> modelId.equals(schemaElement.getModel())
                        && allFields.contains(schemaElement.getBizName())
                ).collect(Collectors.toSet());
    }

    private List<String> getFieldsExceptDate(String sql) {
        List<String> allFields = SqlParserSelectHelper.getAllFields(sql);
        if (CollectionUtils.isEmpty(allFields)) {
            return new ArrayList<>();
        }
        return allFields.stream()
                .filter(entry -> !TimeDimensionEnum.getNameList().contains(entry))
                .collect(Collectors.toList());
    }

    public void setFilter(SemanticCorrectInfo semanticCorrectInfo, Long modelId, SemanticParseInfo parseInfo) {

        String correctorSql = semanticCorrectInfo.getPreSql();
        if (StringUtils.isEmpty(correctorSql)) {
            correctorSql = semanticCorrectInfo.getSql();
        }
        List<FilterExpression> expressions = SqlParserSelectHelper.getFilterExpression(correctorSql);
        if (CollectionUtils.isEmpty(expressions)) {
            return;
        }
        //set dataInfo
        try {
            DateConf dateInfo = getDateInfo(expressions);
            parseInfo.setDateInfo(dateInfo);
        } catch (Exception e) {
            log.error("set dateInfo error :", e);
        }

        //set filter
        try {
            Map<String, SchemaElement> bizNameToElement = getBizNameToElement(modelId);
            List<QueryFilter> result = getDimensionFilter(bizNameToElement, expressions);
            parseInfo.getDimensionFilters().addAll(result);
        } catch (Exception e) {
            log.error("set dimensionFilter error :", e);
        }
    }

    private List<QueryFilter> getDimensionFilter(Map<String, SchemaElement> bizNameToElement,
            List<FilterExpression> filterExpressions) {
        List<QueryFilter> result = Lists.newArrayList();
        for (FilterExpression expression : filterExpressions) {
            QueryFilter dimensionFilter = new QueryFilter();
            dimensionFilter.setValue(expression.getFieldValue());
            String bizName = expression.getFieldName();
            SchemaElement schemaElement = bizNameToElement.get(bizName);
            if (Objects.isNull(schemaElement)) {
                continue;
            }
            String fieldName = schemaElement.getName();
            dimensionFilter.setName(fieldName);
            dimensionFilter.setBizName(bizName);
            dimensionFilter.setElementID(schemaElement.getId());

            FilterOperatorEnum operatorEnum = FilterOperatorEnum.getSqlOperator(expression.getOperator());
            dimensionFilter.setOperator(operatorEnum);
            result.add(dimensionFilter);
        }
        return result;
    }

    private DateConf getDateInfo(List<FilterExpression> filterExpressions) {
        List<FilterExpression> dateExpressions = filterExpressions.stream()
                .filter(expression -> {
                    List<String> nameList = TimeDimensionEnum.getNameList();
                    if (StringUtils.isEmpty(expression.getFieldName())) {
                        return false;
                    }
                    return nameList.contains(expression.getFieldName().toLowerCase());
                }).collect(Collectors.toList());
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

        List<SemanticCorrector> dslCorrections = ComponentFactory.getSqlCorrections();

        dslCorrections.forEach(dslCorrection -> {
            try {
                dslCorrection.correct(correctInfo);
                log.info("sqlCorrection:{} sql:{}", dslCorrection.getClass().getSimpleName(), correctInfo.getSql());
            } catch (Exception e) {
                log.error("sqlCorrection:{} correct error,correctInfo:{}", dslCorrection, correctInfo, e);
            }
        });
        return correctInfo;
    }

    private SemanticParseInfo getParseInfo(QueryContext queryCtx, Long modelId, DslTool dslTool,
            DSLParseResult dslParseResult) {
        PluginSemanticQuery semanticQuery = QueryManager.createPluginQuery(DslQuery.QUERY_MODE);
        SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
        parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(modelId));

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, dslParseResult);
        properties.put("type", "internal");
        properties.put("name", dslTool.getName());

        parseInfo.setProperties(properties);
        parseInfo.setScore(function_bonus_threshold);
        parseInfo.setQueryMode(semanticQuery.getQueryMode());

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

    private DslTool getDslTool(QueryReq request, Long modelId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        List<DslTool> dslTools = agentService.getDslTools(request.getAgentId(), AgentToolType.DSL);
        Optional<DslTool> dslToolOptional = dslTools.stream().filter(tool -> tool.getModelIds().contains(modelId))
                .findFirst();
        return dslToolOptional.orElse(null);
    }

    private Long getModelId(QueryContext queryCtx, ChatContext chatCtx, Integer agentId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Set<Long> distinctModelIds = agentService.getDslToolsModelIds(agentId, AgentToolType.DSL);
        ModelResolver modelResolver = ComponentFactory.getModelResolver();
        Long modelId = modelResolver.resolve(queryCtx, chatCtx, distinctModelIds);
        log.info("resolve modelId:{},dslModels:{}", modelId, distinctModelIds);
        return modelId;
    }

    private LLMResp requestLLM(LLMReq llmReq, Long modelId, LLMConfig llmConfig) {
        String questUrl = llmConfig.getUrl() + llmConfig.getQueryToSqlPath();
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

    private LLMReq getLlmReq(QueryContext queryCtx, Long modelId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();
        String queryText = queryCtx.getRequest().getQueryText();
        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setModelName(modelIdToName.get(modelId));
        llmSchema.setDomainName(modelIdToName.get(modelId));
        List<String> fieldNameList = getFieldNameList(queryCtx, modelId, semanticSchema);
        fieldNameList.add(BaseSemanticCorrector.DATE_FIELD);
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);
        List<ElementValue> linking = new ArrayList<>();
        linking.addAll(getValueList(queryCtx, modelId, semanticSchema));
        llmReq.setLinking(linking);
        String currentDate = DSLDateHelper.getReferenceDate(modelId);
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


    protected Map<String, SchemaElement> getBizNameToElement(Long modelId) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        List<SchemaElement> dimensions = semanticSchema.getDimensions();
        List<SchemaElement> metrics = semanticSchema.getMetrics();

        List<SchemaElement> allElements = Lists.newArrayList();
        allElements.addAll(dimensions);
        allElements.addAll(metrics);
        return allElements.stream()
                .filter(schemaElement -> schemaElement.getModel().equals(modelId))
                .collect(Collectors.toMap(SchemaElement::getBizName, Function.identity(), (value1, value2) -> value2));
    }


    protected List<String> getFieldNameList(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = getItemIdToName(modelId, semanticSchema);

        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(modelId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<String> fieldNameList = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.METRIC.equals(elementType)
                            || SchemaElementType.DIMENSION.equals(elementType)
                            || SchemaElementType.VALUE.equals(elementType);
                })
                .map(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();

                    if (!SchemaElementType.VALUE.equals(elementType)) {
                        return schemaElementMatch.getWord();
                    }
                    return itemIdToName.get(schemaElementMatch.getElement().getId());
                })
                .filter(name -> StringUtils.isNotEmpty(name) && !name.contains("%"))
                .collect(Collectors.toSet());
        return new ArrayList<>(fieldNameList);
    }

    protected Map<Long, String> getItemIdToName(Long modelId, SemanticSchema semanticSchema) {
        return semanticSchema.getDimensions().stream()
                .filter(entry -> modelId.equals(entry.getModel()))
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

}
