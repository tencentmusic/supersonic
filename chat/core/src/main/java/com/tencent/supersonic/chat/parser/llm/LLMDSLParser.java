package com.tencent.supersonic.chat.parser.llm;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.config.LLMConfig;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.parser.function.ModelResolver;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.dsl.DSLBuilder;
import com.tencent.supersonic.chat.query.dsl.DSLQuery;
import com.tencent.supersonic.chat.query.dsl.LLMReq;
import com.tencent.supersonic.chat.query.dsl.LLMReq.ElementValue;
import com.tencent.supersonic.chat.query.dsl.LLMResp;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.knowledge.service.SchemaService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
public class LLMDSLParser implements SemanticParser {

    public static final double FUNCTION_BONUS_THRESHOLD = 201;

    @Override
    public void parse(QueryContext queryCtx, ChatContext chatCtx) {
        final LLMConfig llmConfig = ContextUtils.getBean(LLMConfig.class);
        if (StringUtils.isEmpty(llmConfig.getUrl()) || SatisfactionChecker.check(queryCtx)) {
            log.info("llmConfig:{}, skip function parser, queryText:{}", llmConfig,
                    queryCtx.getRequest().getQueryText());
            return;
        }
        List<Plugin> dslPlugins = PluginManager.getPlugins().stream()
                .filter(plugin -> DSLQuery.QUERY_MODE.equalsIgnoreCase(plugin.getType()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(dslPlugins)) {
            return;
        }
        Plugin plugin = dslPlugins.get(0);
        List<Long> dslModels = plugin.getModelList();

        try {
            ModelResolver modelResolver = ComponentFactory.getModelResolver();
            Long modelId = modelResolver.resolve(queryCtx, chatCtx, dslModels);
            log.info("resolve modelId:{},dslModels:{}", modelId, dslModels);

            if (Objects.isNull(modelId)) {
                return;
            }

            LLMResp llmResp = requestLLM(queryCtx, modelId);
            if (Objects.isNull(llmResp)) {
                return;
            }

            PluginSemanticQuery semanticQuery = QueryManager.createPluginQuery(DSLQuery.QUERY_MODE);

            SemanticParseInfo parseInfo = semanticQuery.getParseInfo();
            if (Objects.nonNull(modelId) && modelId > 0) {
                parseInfo.getElementMatches().addAll(queryCtx.getMapInfo().getMatchedElements(modelId));
            }
            DSLParseResult dslParseResult = new DSLParseResult();
            dslParseResult.setRequest(queryCtx.getRequest());
            dslParseResult.setLlmResp(llmResp);
            dslParseResult.setPlugin(plugin);

            Map<String, Object> properties = new HashMap<>();
            properties.put(Constants.CONTEXT, dslParseResult);
            parseInfo.setProperties(properties);
            parseInfo.setScore(FUNCTION_BONUS_THRESHOLD);
            parseInfo.setQueryMode(semanticQuery.getQueryMode());
            SchemaElement Model = new SchemaElement();
            Model.setModel(modelId);
            Model.setId(modelId);
            parseInfo.setModel(Model);
            queryCtx.getCandidateQueries().add(semanticQuery);
        } catch (Exception e) {
            log.error("LLMDSLParser error", e);
        }
    }


    private LLMResp requestLLM(QueryContext queryCtx, Long modelId) {
        long startTime = System.currentTimeMillis();
        String queryText = queryCtx.getRequest().getQueryText();
        final LLMConfig llmConfig = ContextUtils.getBean(LLMConfig.class);

        if (StringUtils.isEmpty(llmConfig.getUrl())) {
            log.warn("llmConfig url is null, skip llm parser");
            return null;
        }

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Map<Long, String> modelIdToName = semanticSchema.getModelIdToName();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setModelName(modelIdToName.get(modelId));
        llmSchema.setDomainName(modelIdToName.get(modelId));
        List<String> fieldNameList = getFieldNameList(queryCtx, modelId, semanticSchema);
        fieldNameList.add(DSLBuilder.DATA_Field);
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);
        List<ElementValue> linking = new ArrayList<>();
        linking.addAll(getValueList(queryCtx, modelId, semanticSchema));
        llmReq.setLinking(linking);
        String currentDate = getCurrentDate(modelId);
        llmReq.setCurrentDate(currentDate);

        log.info("requestLLM request, modelId:{},llmReq:{}", modelId, llmReq);
        String questUrl = llmConfig.getUrl() + llmConfig.getQueryToSqlPath();

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


    private String getCurrentDate(Long modelId) {
        return DateUtils.getBeforeDate(4);
//        ChatConfigFilter filter = new ChatConfigFilter();
//        filter.setModelId(modelId);
//
//        List<ChatConfigResp> configResps = ContextUtils.getBean(ConfigService.class).search(filter, null);
//        if (CollectionUtils.isEmpty(configResps)) {
//            return
//        }
//        ChatConfigResp chatConfigResp = configResps.get(0);
//        chatConfigResp.getChatDetailConfig().getChatDefaultConfig().get

    }

    private List<ElementValue> getValueList(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = getItemIdToName(modelId, semanticSchema);

        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(modelId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<ElementValue> valueMatches = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType type = schemaElementMatch.getElement().getType();
                    return SchemaElementType.VALUE.equals(type) || SchemaElementType.ID.equals(type);
                })
                .map(elementMatch ->
                        {
                            ElementValue elementValue = new ElementValue();
                            elementValue.setFieldName(itemIdToName.get(elementMatch.getElement().getId()));
                            elementValue.setFieldValue(elementMatch.getWord());
                            return elementValue;
                        }
                )
                .collect(Collectors.toSet());
        return new ArrayList<>(valueMatches);
    }

    private List<String> getFieldNameList(QueryContext queryCtx, Long modelId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = getItemIdToName(modelId, semanticSchema);

        List<SchemaElementMatch> matchedElements = queryCtx.getMapInfo().getMatchedElements(modelId);
        if (CollectionUtils.isEmpty(matchedElements)) {
            return new ArrayList<>();
        }
        Set<String> fieldNameList = matchedElements.stream()
                .filter(schemaElementMatch -> {
                    SchemaElementType elementType = schemaElementMatch.getElement().getType();
                    return SchemaElementType.METRIC.equals(elementType) ||
                            SchemaElementType.DIMENSION.equals(elementType) ||
                            SchemaElementType.VALUE.equals(elementType);
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

    private Map<Long, String> getItemIdToName(Long modelId, SemanticSchema semanticSchema) {
        return semanticSchema.getDimensions().stream()
                .filter(entry -> modelId.equals(entry.getModel()))
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));
    }

}
