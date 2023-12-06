package com.tencent.supersonic.chat.parser.sql.llm;

import com.tencent.supersonic.chat.agent.AgentToolType;
import com.tencent.supersonic.chat.agent.NL2SQLTool;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.LLMParserConfig;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.parser.LLMProxy;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.ModelCluster;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
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

    protected LLMProxy llmProxy = ComponentFactory.getLLMProxy();

    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
    @Autowired
    private LLMParserConfig llmParserConfig;
    @Autowired
    private AgentService agentService;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private OptimizationConfig optimizationConfig;

    public boolean isSkip(QueryContext queryCtx) {
        QueryReq request = queryCtx.getRequest();
        if (StringUtils.isEmpty(llmParserConfig.getUrl())) {
            log.info("llm parser url is empty, skip {} , llmParserConfig:{}", LLMSqlParser.class, llmParserConfig);
            return true;
        }
        if (SatisfactionChecker.isSkip(queryCtx)) {
            log.info("skip {}, queryText:{}", LLMSqlParser.class, request.getQueryText());
            return true;
        }
        return false;
    }

    public ModelCluster getModelCluster(QueryContext queryCtx, ChatContext chatCtx, Integer agentId) {
        Set<Long> distinctModelIds = agentService.getModelIds(agentId, AgentToolType.NL2SQL_LLM);
        if (agentService.containsAllModel(distinctModelIds)) {
            distinctModelIds = new HashSet<>();
        }
        ModelResolver modelResolver = ComponentFactory.getModelResolver();
        String modelCluster = modelResolver.resolve(queryCtx, chatCtx, distinctModelIds);
        log.info("resolve modelId:{},llmParser Models:{}", modelCluster, distinctModelIds);
        return ModelCluster.build(modelCluster);
    }

    public NL2SQLTool getParserTool(QueryReq request, Set<Long> modelIdSet) {
        List<NL2SQLTool> commonAgentTools = agentService.getParserTools(request.getAgentId(),
                AgentToolType.NL2SQL_LLM);
        Optional<NL2SQLTool> llmParserTool = commonAgentTools.stream()
                .filter(tool -> {
                    List<Long> modelIds = tool.getModelIds();
                    if (agentService.containsAllModel(new HashSet<>(modelIds))) {
                        return true;
                    }
                    for (Long modelId : modelIdSet) {
                        if (modelIds.contains(modelId)) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst();
        return llmParserTool.orElse(null);
    }

    public LLMReq getLlmReq(QueryContext queryCtx, SemanticSchema semanticSchema,
                            ModelCluster modelCluster, List<ElementValue> linkingValues) {
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();
        String queryText = queryCtx.getRequest().getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        Long firstModelId = modelCluster.getFirstModel();
        LLMReq.FilterCondition filterCondition = new LLMReq.FilterCondition();
        llmReq.setFilterCondition(filterCondition);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setModelName(modelIdToName.get(firstModelId));
        llmSchema.setDomainName(modelIdToName.get(firstModelId));

        List<String> fieldNameList = getFieldNameList(queryCtx, modelCluster, llmParserConfig);

        String priorExts = getPriorExts(modelCluster.getModelIds(), fieldNameList);
        llmReq.setPriorExts(priorExts);

        fieldNameList.add(TimeDimensionEnum.DAY.getChName());
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);

        List<ElementValue> linking = new ArrayList<>();
        if (optimizationConfig.isUseLinkingValueSwitch()) {
            linking.addAll(linkingValues);
        }
        llmReq.setLinking(linking);

        String currentDate = S2SqlDateHelper.getReferenceDate(firstModelId);
        if (StringUtils.isEmpty(currentDate)) {
            currentDate = DateUtils.getBeforeDate(0);
        }
        llmReq.setCurrentDate(currentDate);
        return llmReq;
    }

    public LLMResp requestLLM(LLMReq llmReq, String modelClusterKey) {
        return llmProxy.query2sql(llmReq, modelClusterKey);
    }

    protected List<String> getFieldNameList(QueryContext queryCtx, ModelCluster modelCluster,
                                            LLMParserConfig llmParserConfig) {

        Set<String> results = getTopNFieldNames(modelCluster, llmParserConfig);

        Set<String> fieldNameList = getMatchedFieldNames(queryCtx, modelCluster);

        results.addAll(fieldNameList);
        return new ArrayList<>(results);
    }

    private String getPriorExts(Set<Long> modelIds, List<String> fieldNameList) {
        StringBuilder extraInfoSb = new StringBuilder();
        List<ModelSchemaResp> modelSchemaResps = semanticInterpreter.fetchModelSchema(
                new ArrayList<>(modelIds), true);
        if (!CollectionUtils.isEmpty(modelSchemaResps)) {

            ModelSchemaResp modelSchemaResp = modelSchemaResps.get(0);
            Map<String, String> fieldNameToDataFormatType = modelSchemaResp.getMetrics()
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

    protected List<ElementValue> getValueList(QueryContext queryCtx, ModelCluster modelCluster) {
        Map<Long, String> itemIdToName = getItemIdToName(modelCluster);

        List<SchemaElementMatch> matchedElements = queryCtx.getModelClusterMapInfo()
                .getMatchedElements(modelCluster.getKey());
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

    protected Map<Long, String> getItemIdToName(ModelCluster modelCluster) {
        SemanticSchema semanticSchema = schemaService.getSemanticSchema();
        return semanticSchema.getDimensions(modelCluster.getModelIds()).stream()
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

    private Set<String> getTopNFieldNames(ModelCluster modelCluster, LLMParserConfig llmParserConfig) {
        SemanticSchema semanticSchema = schemaService.getSemanticSchema();
        Set<String> results = semanticSchema.getDimensions(modelCluster.getModelIds()).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getDimensionTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());

        Set<String> metrics = semanticSchema.getMetrics(modelCluster.getModelIds()).stream()
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(llmParserConfig.getMetricTopN())
                .map(entry -> entry.getName())
                .collect(Collectors.toSet());

        results.addAll(metrics);
        return results;
    }

    protected Set<String> getMatchedFieldNames(QueryContext queryCtx, ModelCluster modelCluster) {
        Map<Long, String> itemIdToName = getItemIdToName(modelCluster);
        List<SchemaElementMatch> matchedElements = queryCtx.getModelClusterMapInfo()
                .getMatchedElements(modelCluster.getKey());
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
