package com.tencent.supersonic.chat.query.plugin.dsl;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.config.LLMConfig;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.dsl.LLMReq.ElementValue;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class DSLQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "DSL";
    private DSLBuilder dslBuilder = new DSLBuilder();

    protected SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public DSLQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        PluginParseResult functionCallParseResult =JsonUtil.toObject(JsonUtil.toString(parseInfo.getProperties().get(Constants.CONTEXT)),PluginParseResult.class);
        Long domainId = parseInfo.getDomainId();
        LLMResp llmResp = requestLLM(functionCallParseResult, domainId);
        if (Objects.isNull(llmResp)) {
            return null;
        }
        String querySql = convertToSql(functionCallParseResult.getRequest().getQueryFilters(), llmResp, parseInfo,
                domainId);
        QueryResult queryResult = new QueryResult();

        long startTime = System.currentTimeMillis();
        QueryResultWithSchemaResp queryResp = semanticLayer.queryByDsl(
                QueryReqBuilder.buildDslReq(querySql, domainId), user);
        log.info("queryByDsl cost:{},querySql:{}", System.currentTimeMillis() - startTime, querySql);

        if (queryResp != null) {
            queryResult.setQueryAuthorization(queryResp.getQueryAuthorization());
        }
        String resultQql = queryResp == null ? null : queryResp.getSql();
        List<Map<String, Object>> resultList = queryResp == null ? new ArrayList<>()
                : queryResp.getResultList();
        List<QueryColumn> columns = queryResp == null ? new ArrayList<>() : queryResp.getColumns();
        queryResult.setQuerySql(resultQql);
        queryResult.setQueryResults(resultList);
        queryResult.setQueryColumns(columns);
        queryResult.setQueryMode(QUERY_MODE);
        queryResult.setQueryState(QueryState.SUCCESS);

        // add domain info
        EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class)
                .getEntityInfo(parseInfo, user);
        queryResult.setEntityInfo(entityInfo);
        parseInfo.setProperties(null);
        return queryResult;
    }


    protected String convertToSql(QueryFilters queryFilters, LLMResp llmResp, SemanticParseInfo parseInfo,
            Long domainId) {
        try {
            return dslBuilder.build(queryFilters, parseInfo, llmResp, domainId);
        } catch (SqlParseException e) {
            log.error("convertToSql error", e);
        }
        return null;
    }

    protected LLMResp requestLLM(PluginParseResult parseResult, Long domainId) {
        long startTime = System.currentTimeMillis();
        String queryText = parseResult.getRequest().getQueryText();
        final LLMConfig llmConfig = ContextUtils.getBean(LLMConfig.class);

        if (StringUtils.isEmpty(llmConfig.getUrl())) {
            log.warn("llmConfig url is null, skip llm parser");
            return null;
        }

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        Map<Long, String> domainIdToName = semanticSchema.getDomainIdToName();

        LLMReq llmReq = new LLMReq();
        llmReq.setQueryText(queryText);
        LLMReq.LLMSchema llmSchema = new LLMReq.LLMSchema();
        llmSchema.setDomainName(domainIdToName.get(domainId));
        List<String> fieldNameList = getFieldNameList(domainId, semanticSchema);
        llmSchema.setFieldNameList(fieldNameList);
        llmReq.setSchema(llmSchema);
        List<ElementValue> linking = new ArrayList<>();
        linking.addAll(getValueList(domainId, semanticSchema));
        llmReq.setLinking(linking);

        log.info("requestLLM request, domainId:{},llmReq:{}", domainId, llmReq);
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

    private List<ElementValue> getValueList(Long domainId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = semanticSchema.getDimensions().stream()
                .filter(entry -> domainId.equals(entry.getDomain()))
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));

        List<SchemaElementMatch> matchedElements = parseInfo.getElementMatches();
        Set<ElementValue> valueMatches = matchedElements.stream()
                .filter(schemaElementMatch -> SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType()))
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

    private List<String> getFieldNameList(Long domainId, SemanticSchema semanticSchema) {
        Map<Long, String> itemIdToName = semanticSchema.getDimensions().stream()
                .filter(entry -> domainId.equals(entry.getDomain()))
                .collect(Collectors.toMap(SchemaElement::getId, SchemaElement::getName, (value1, value2) -> value2));

        List<SchemaElementMatch> matchedElements = parseInfo.getElementMatches();
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


}
