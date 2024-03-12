package com.tencent.supersonic.chat.core.parser.sql.llm;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.core.utils.S2SqlDateHelper;
import com.tencent.supersonic.common.pojo.enums.QueryType;
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
import com.tencent.supersonic.headless.api.pojo.response.DataSetSchemaResp;
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

    public Long getDataSetId(QueryContext queryCtx) {
        Agent agent = queryCtx.getAgent();
        Set<Long> agentDataSetIds = new HashSet<>();
        if (Objects.nonNull(agent)) {
            agentDataSetIds = agent.getDataSetIds(AgentToolType.NL2SQL_LLM);
        }
        if (Agent.containsAllModel(agentDataSetIds)) {
            agentDataSetIds = new HashSet<>();
        }
        DataSetResolver dataSetResolver = ComponentFactory.getModelResolver();
        return dataSetResolver.resolve(queryCtx, agentDataSetIds);
    }

    public NL2SQLTool getParserTool(QueryContext queryCtx, Long dataSetId) {
        Agent agent = queryCtx.getAgent();
        if (Objects.isNull(agent)) {
            return null;
        }
        List<NL2SQLTool> commonAgentTools = agent.getParserTools(AgentToolType.NL2SQL_LLM);
        Optional<NL2SQLTool> llmParserTool = commonAgentTools.stream()
                .filter(tool -> {
                    List<Long> dataSetIds = tool.getDataSetIds();
                    if (Agent.containsAllModel(new HashSet<>(dataSetIds))) {
                        return true;
                    }
                    return dataSetIds.contains(dataSetId);
                })
                .findFirst();
        return llmParserTool.orElse(null);
    }

    public LLMReq getLlmReq(QueryContext queryCtx, Long dataSetId, List<ElementValue> linkingValues) {
        Map<Long, String> dataSetIdToName = queryCtx.getSemanticSchema().getDataSetIdToName();
        String queryText = queryCtx.getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        LLMReq.FilterCondition filterCondition = new LLMReq.FilterCondition();
        llmReq.setFilterCondition(filterCondition);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setDataSetName(dataSetIdToName.get(dataSetId));
        llmSchema.setDomainName(dataSetIdToName.get(dataSetId));

        List<String> fieldNameList = getFieldNameList(queryCtx, dataSetId, llmParserConfig);

        String priorExts = getPriorExts(dataSetId, fieldNameList);
        llmReq.setPriorExts(priorExts);

        fieldNameList.add(TimeDimensionEnum.DAY.getChName());
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);

        List<ElementValue> linking = new ArrayList<>();
        if (optimizationConfig.isUseLinkingValueSwitch()) {
            linking.addAll(linkingValues);
        }
        llmReq.setLinking(linking);

        String currentDate = S2SqlDateHelper.getReferenceDate(queryCtx, dataSetId);
        if (StringUtils.isEmpty(currentDate)) {
            currentDate = DateUtils.getBeforeDate(0);
        }
        llmReq.setCurrentDate(currentDate);
        llmReq.setSqlGenerationMode(optimizationConfig.getSqlGenerationMode().getName());
        return llmReq;
    }

    public LLMResp requestLLM(LLMReq llmReq, Long dataSetId) {
        return ComponentFactory.getLLMProxy().query2sql(llmReq, dataSetId);
    }

    protected List<String> getFieldNameList(QueryContext queryCtx, Long dataSetId,
            LLMParserConfig llmParserConfig) {

        Set<String> results = getTopNFieldNames(queryCtx, dataSetId, llmParserConfig);

        Set<String> fieldNameList = getMatchedFieldNames(queryCtx, dataSetId);

        results.addAll(fieldNameList);
        return new ArrayList<>(results);
    }

    private String getPriorExts(Long dataSetId, List<String> fieldNameList) {
        StringBuilder extraInfoSb = new StringBuilder();
        List<DataSetSchemaResp> dataSetSchemaResps = semanticInterpreter.fetchDataSetSchema(
                Lists.newArrayList(dataSetId), true);
        if (!CollectionUtils.isEmpty(dataSetSchemaResps)) {
            DataSetSchemaResp dataSetSchemaResp = dataSetSchemaResps.get(0);
            Map<String, String> fieldNameToDataFormatType = dataSetSchemaResp.getMetrics()
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

    protected List<ElementValue> getValueList(QueryContext queryCtx, Long dataSetId) {
        Map<Long, String> itemIdToName = getItemIdToName(queryCtx, dataSetId);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<ElementValue> valueMatches = matchedElements
                .stream()
                .filter(elementMatch -> !elementMatch.isInherited())
                .filter(schemaElementMatch -> {
                    SchemaElementType type = schemaElementMatch.getElement().getType();
                    return SchemaElementType.VALUE.equals(type) || SchemaElementType.TAG_VALUE.equals(type)
                            || SchemaElementType.ID.equals(type);
                })
                .map(elementMatch -> {
                    ElementValue elementValue = new ElementValue();
                    elementValue.setFieldName(itemIdToName.get(elementMatch.getElement().getId()));
                    elementValue.setFieldValue(elementMatch.getWord());
                    return elementValue;
                }).collect(Collectors.toSet());
        return new ArrayList<>(valueMatches);
    }

    protected Map<Long, String> getItemIdToName(QueryContext queryCtx, Long dataSetId) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        List<SchemaElement> elements = semanticSchema.getDimensions(dataSetId);
        if (QueryType.TAG.equals(queryCtx.getQueryType(dataSetId))) {
            elements = semanticSchema.getTags(dataSetId);
        }
        return elements.stream()
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

    private Set<String> getTopNFieldNames(QueryContext queryCtx, Long dataSetId, LLMParserConfig llmParserConfig) {
        SemanticSchema semanticSchema = queryCtx.getSemanticSchema();
        Set<String> results = new HashSet<>();
        if (QueryType.TAG.equals(queryCtx.getQueryType(dataSetId))) {
            Set<String> tags = semanticSchema.getTags(dataSetId).stream()
                    .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(llmParserConfig.getDimensionTopN())
                    .map(entry -> entry.getName())
                    .collect(Collectors.toSet());
            results.addAll(tags);
        } else {
            Set<String> dimensions = semanticSchema.getDimensions(dataSetId).stream()
                    .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(llmParserConfig.getDimensionTopN())
                    .map(entry -> entry.getName())
                    .collect(Collectors.toSet());
            results.addAll(dimensions);
            Set<String> metrics = semanticSchema.getMetrics(dataSetId).stream()
                    .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(llmParserConfig.getMetricTopN())
                    .map(entry -> entry.getName())
                    .collect(Collectors.toSet());
            results.addAll(metrics);
        }
        return results;
    }

    protected Set<String> getMatchedFieldNames(QueryContext queryCtx, Long dataSetId) {
        Map<Long, String> itemIdToName = getItemIdToName(queryCtx, dataSetId);
        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(dataSetId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new HashSet<>();
        }
        Set<String> fieldNameList = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.METRIC.equals(elementType)
                            || SchemaElementType.DIMENSION.equals(elementType)
                            || SchemaElementType.VALUE.equals(elementType)
                            || SchemaElementType.TAG.equals(elementType)
                            || SchemaElementType.TAG_VALUE.equals(elementType);
                })
                .map(schemaElementMatch -> {
                    SchemaElement element = schemaElementMatch.getElement();
                    SchemaElementType elementType = element.getType();
                    if (SchemaElementType.VALUE.equals(elementType) || SchemaElementType.TAG_VALUE.equals(
                            elementType)) {
                        return itemIdToName.get(element.getId());
                    }
                    return schemaElementMatch.getWord();
                })
                .collect(Collectors.toSet());
        return fieldNameList;
    }
}
