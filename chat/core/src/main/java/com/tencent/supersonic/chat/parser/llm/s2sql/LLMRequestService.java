package com.tencent.supersonic.chat.parser.llm.s2sql;

import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.CommonAgentTool;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.config.LLMParserConfig;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMResp;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class LLMRequestService {

    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();
    @Autowired
    private LLMParserConfig llmParserConfig;
    @Autowired
    private AgentService agentService;
    @Autowired
    private SchemaService schemaService;

    @Autowired
    private RestTemplate restTemplate;

    public boolean check(QueryContext queryCtx) {
        QueryReq request = queryCtx.getRequest();
        if (StringUtils.isEmpty(llmParserConfig.getUrl())) {
            log.info("llm parser url is empty, skip {} , llmParserConfig:{}", LLMS2SQLParser.class, llmParserConfig);
            return true;
        }
        if (SatisfactionChecker.check(queryCtx)) {
            log.info("skip {}, queryText:{}", LLMS2SQLParser.class, request.getQueryText());
            return true;
        }
        return false;
    }

    public Long getModelId(QueryContext queryCtx, ChatContext chatCtx, Integer agentId) {
        Set<Long> distinctModelIds = agentService.getModelIds(agentId, AgentToolType.LLM_S2SQL);
        if (agentService.containsAllModel(distinctModelIds)) {
            distinctModelIds = new HashSet<>();
        }
        ModelResolver modelResolver = ComponentFactory.getModelResolver();
        Long modelId = modelResolver.resolve(queryCtx, chatCtx, distinctModelIds);
        log.info("resolve modelId:{},llmParser Models:{}", modelId, distinctModelIds);
        return modelId;
    }

    public CommonAgentTool getParserTool(QueryReq request, Long modelId) {
        List<CommonAgentTool> commonAgentTools = agentService.getParserTools(request.getAgentId(),
                AgentToolType.LLM_S2SQL);
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

    public LLMReq getLlmReq(QueryContext queryCtx, Long modelId) {
        SemanticSchema semanticSchema = schemaService.getSemanticSchema();
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();
        String queryText = queryCtx.getRequest().getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);

        LLMReq.FilterCondition filterCondition = new LLMReq.FilterCondition();
        filterCondition.setTableName(modelIdToName.get(modelId));
        llmReq.setFilterCondition(filterCondition);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setModelName(modelIdToName.get(modelId));
        llmSchema.setDomainName(modelIdToName.get(modelId));

        List<String> fieldNameList = getFieldNameList(queryCtx, modelId, semanticSchema, llmParserConfig);

        String priorExts = getPriorExts(modelId, fieldNameList);
        llmReq.setPriorExts(priorExts);

        fieldNameList.add(TimeDimensionEnum.DAY.getChName());
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);

        List<ElementValue> linking = new ArrayList<>();
        linking.addAll(getValueList(queryCtx, modelId, semanticSchema));
        llmReq.setLinking(linking);

        String currentDate = S2SQLDateHelper.getReferenceDate(modelId);
        if (StringUtils.isEmpty(currentDate)) {
            currentDate = DateUtils.getBeforeDate(0);
        }
        llmReq.setCurrentDate(currentDate);
        return llmReq;
    }

    public LLMResp requestLLM(LLMReq llmReq, Long modelId) {
        long startTime = System.currentTimeMillis();
        log.info("requestLLM request, modelId:{},llmReq:{}", modelId, llmReq);
        try {
            URL url = new URL(new URL(llmParserConfig.getUrl()), llmParserConfig.getQueryToSqlPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JsonUtil.toString(llmReq), headers);
            ResponseEntity<LLMResp> responseEntity = restTemplate.exchange(url.toString(), HttpMethod.POST, entity,
                    LLMResp.class);

            log.info("requestLLM response,cost:{}, questUrl:{} \n entity:{} \n body:{}",
                    System.currentTimeMillis() - startTime, url.toString(), entity, responseEntity.getBody());
            return responseEntity.getBody();
        } catch (Exception e) {
            log.error("requestLLM error", e);
        }
        return null;
    }

    protected List<String> getFieldNameList(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema,
            LLMParserConfig llmParserConfig) {

        Set<String> results = getTopNFieldNames(modelId, semanticSchema, llmParserConfig);

        Set<String> fieldNameList = getMatchedFieldNames(queryCtx, modelId, semanticSchema);

        results.addAll(fieldNameList);
        return new ArrayList<>(results);
    }

    private String getPriorExts(Long modelId, List<String> fieldNameList) {
        StringBuilder extraInfoSb = new StringBuilder();
        List<ModelSchemaResp> modelSchemaResps = semanticInterpreter.fetchModelSchema(
                Collections.singletonList(modelId), true);
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

    protected Map<Long, String> getItemIdToName(Long modelId, SemanticSchema semanticSchema) {
        return semanticSchema.getDimensions(modelId).stream()
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
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
}
