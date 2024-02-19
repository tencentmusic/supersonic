package com.tencent.supersonic.chat.core.parser.sql.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.core.agent.Agent;
import com.tencent.supersonic.chat.core.agent.AgentToolType;
import com.tencent.supersonic.chat.core.agent.NL2SQLTool;
import com.tencent.supersonic.chat.core.config.LLMParserConfig;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.query.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.core.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.response.ViewSchemaResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LLMRequestService {

    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
    @Autowired
    private LLMParserConfig llmParserConfig;
    @Autowired
    private OptimizationConfig optimizationConfig;

    public boolean isSkip(QueryContext queryCtx) {
        if (ComponentFactory.getLLMProxy().isSkip(queryCtx)) {
            return true;
        }
        if (SatisfactionChecker.isSkip(queryCtx)) {
            log.info("skip {}, queryText:{}", LLMSqlParser.class, queryCtx.getQueryText());
            return true;
        }
        return false;
    }

    public Long getViewId(QueryContext queryCtx) {
        Agent agent = queryCtx.getAgent();
        Set<Long> agentViewIds = new HashSet<>();
        if (Objects.nonNull(agent)) {
            agentViewIds = agent.getViewIds(AgentToolType.NL2SQL_LLM);
        }
        if (Agent.containsAllModel(agentViewIds)) {
            agentViewIds = new HashSet<>();
        }
        ViewResolver viewResolver = ComponentFactory.getModelResolver();
        return viewResolver.resolve(queryCtx, agentViewIds);
    }

    public NL2SQLTool getParserTool(QueryContext queryCtx, Long viewId) {
        Agent agent = queryCtx.getAgent();
        if (Objects.isNull(agent)) {
            return null;
        }
        List<NL2SQLTool> commonAgentTools = agent.getParserTools(AgentToolType.NL2SQL_LLM);
        Optional<NL2SQLTool> llmParserTool = commonAgentTools.stream()
                .filter(tool -> {
                    List<Long> viewIds = tool.getViewIds();
                    if (Agent.containsAllModel(new HashSet<>(viewIds))) {
                        return true;
                    }
                    return viewIds.contains(viewId);
                })
                .findFirst();
        return llmParserTool.orElse(null);
    }

    public LLMReq getLlmReq(QueryContext queryCtx, Long viewId,
                            SemanticSchema semanticSchema, List<ElementValue> linkingValues) {
        Map<Long, String> viewIdToName = semanticSchema.getViewIdToName();
        String queryText = queryCtx.getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        LLMReq.FilterCondition filterCondition = new LLMReq.FilterCondition();
        llmReq.setFilterCondition(filterCondition);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setViewName(viewIdToName.get(viewId));
        llmSchema.setDomainName(viewIdToName.get(viewId));

        List<String> fieldNameList = getFieldNameList(queryCtx, viewId, llmParserConfig);

        String priorExts = getPriorExts(viewId, fieldNameList);
        llmReq.setPriorExts(priorExts);

        fieldNameList.add(TimeDimensionEnum.DAY.getChName());
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);

        List<ElementValue> linking = new ArrayList<>();
        if (optimizationConfig.isUseLinkingValueSwitch()) {
            linking.addAll(linkingValues);
        }
        llmReq.setLinking(linking);

        String currentDate = S2SqlDateHelper.getReferenceDate(queryCtx, viewId);
        if (StringUtils.isEmpty(currentDate)) {
            currentDate = DateUtils.getBeforeDate(0);
        }
        llmReq.setCurrentDate(currentDate);
        llmReq.setSqlGenerationMode(optimizationConfig.getSqlGenerationMode().getName());
        return llmReq;
    }

    public LLMResp requestLLM(LLMReq llmReq, Long viewId) {
        return ComponentFactory.getLLMProxy().query2sql(llmReq, viewId);
    }

    protected List<String> getFieldNameList(QueryContext queryCtx, Long viewId,
            LLMParserConfig llmParserConfig) {

        Set<String> results = getTopNFieldNames(queryCtx, viewId, llmParserConfig);

        Set<String> fieldNameList = getMatchedFieldNames(queryCtx, viewId);

        results.addAll(fieldNameList);
        return new ArrayList<>(results);
    }

    private String getPriorExts(Long viewId, List<String> fieldNameList) {
        StringBuilder extraInfoSb = new StringBuilder();
        List<ViewSchemaResp> viewSchemaResps = semanticInterpreter.fetchViewSchema(
                Lists.newArrayList(viewId), true);
        if (!CollectionUtils.isEmpty(viewSchemaResps)) {
            ViewSchemaResp viewSchemaResp = viewSchemaResps.get(0);
            Map<String, String> fieldNameToDataFormatType = viewSchemaResp.getMetrics()
                    .stream().filter(metricSchemaResp -> Objects.nonNull(metricSchemaResp.getDataFormatType()))
                    .flatMap(metricSchemaResp -> {
                        Set<Pair<String, String>> result = new HashSet<>();
                        String dataFormatType = metricSchemaResp.getDataFormatType();
                        result.add(Pair.of(metricSchemaResp.getName(), dataFormatType));
                        List<String> aliasList = SchemaItem.getAliasList(metricSchemaResp.getAlias());
                        if (!CollectionUtils.isEmpty(aliasList)) {
                            for (String alias : aliasList) {
                                result.add(Pair.of(alias, dataFormatType));
                            }
                        }
                        return result.stream();
                    })
                    .collect(Collectors.toMap(a -> a.getLeft(), a -> a.getRight(), (k1, k2) -> k1));

            for (String fieldName : fieldNameList) {
                String dataFormatType = fieldNameToDataFormatType.get(fieldName);
                if (DataFormatTypeEnum.DECIMAL.getName().equalsIgnoreCase(dataFormatType)
                        || DataFormatTypeEnum.PERCENT.getName().equalsIgnoreCase(dataFormatType)) {
                    String format = String.format("%s的计量单位是%s", fieldName, "小数; ");
                    extraInfoSb.append(format);
                }
            }
        }
        return extraInfoSb.toString();
    }

    protected List<ElementValue> getValueList(QueryContext queryCtx, Long viewId) {
        Map<Long, String> itemIdToName = getItemIdToName(queryCtx, viewId);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(viewId);
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

    protected Map<Long, String> getItemIdToName(QueryContext queryCtx, Long viewId) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        return semanticSchema.getDimensions(viewId).stream()
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

    private Set<String> getTopNFieldNames(QueryContext queryCtx, Long viewId, LLMParserConfig llmParserConfig) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        Set<String> results = semanticSchema.getDimensions(viewId).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getDimensionTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());

        Set<String> metrics = semanticSchema.getMetrics(viewId).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getMetricTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());

        results.addAll(metrics);
        return results;
    }

    protected Set<String> getMatchedFieldNames(QueryContext queryCtx, Long viewId) {
        Map<Long, String> itemIdToName = getItemIdToName(queryCtx, viewId);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(viewId);
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
}
