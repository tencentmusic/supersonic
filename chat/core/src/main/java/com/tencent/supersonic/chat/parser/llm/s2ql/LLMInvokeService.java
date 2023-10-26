package com.tencent.supersonic.chat.parser.llm.s2ql;

import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.config.LLMParserConfig;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMResp;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class LLMInvokeService {

    protected SemanticInterpreter semanticInterpreter = ComponentFactory.getSemanticLayer();


    public LLMResp requestLLM(LLMReq llmReq, Long modelId, LLMParserConfig llmParserConfig) {
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

    public LLMReq getLlmReq(QueryContext queryCtx, Long modelId, LLMParserConfig llmParserConfig) {
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();
        String queryText = queryCtx.getRequest().getQueryText();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);

        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setModelName(modelIdToName.get(modelId));
        llmSchema.setDomainName(modelIdToName.get(modelId));

        List<String> fieldNameList = getFieldNameList(queryCtx, modelId, semanticSchema, llmParserConfig);

        String priorExts = getPriorExts(modelId, fieldNameList);
        llmReq.setPriorExts(priorExts);

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

    private String getPriorExts(Long modelId, List<String> fieldNameList) {
        StringBuilder extraInfoSb = new StringBuilder();
        List<ModelSchemaResp> modelSchemaResps = semanticInterpreter.fetchModelSchema(
                Collections.singletonList(modelId), true);
        if (!CollectionUtils.isEmpty(modelSchemaResps)) {
            ModelSchemaResp modelSchemaResp = modelSchemaResps.get(0);
            Map<String, String> fieldNameToDataFormatType = modelSchemaResp.getMetrics()
                    .stream().collect(Collectors.toMap(a -> a.getName(), a -> a.getDataFormatType(), (k1, k2) -> k1));

            for (String fieldName : fieldNameList) {
                String dataFormatType = fieldNameToDataFormatType.get(fieldName);
                if (DataFormatTypeEnum.DECIMAL.getName().equalsIgnoreCase(dataFormatType)
                        || DataFormatTypeEnum.PERCENT.getName().equalsIgnoreCase(dataFormatType)) {
                    String format = String.format("%s 的字段类型是 %s", fieldName, "小数; ");
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
