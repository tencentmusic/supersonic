package com.tencent.supersonic.chat.parser.embedding;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.*;
import java.util.stream.Collectors;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EmbeddingBasedParser implements SemanticParser {

    private final static double THRESHOLD = 0.2d;

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return;
        }
        log.info("EmbeddingBasedParser parser query ctx: {}, chat ctx: {}", queryContext, chatContext);
        Set<Long> domainIds = getDomainMatched(queryContext);
        String text = queryContext.getRequest().getQueryText();
        if (!CollectionUtils.isEmpty(domainIds)) {
            for (Long domainId : domainIds) {
                List<SchemaElementMatch> schemaElementMatches = getMatchedElements(queryContext, domainId);
                String textReplaced = replaceText(text, schemaElementMatches);
                List<RecallRetrieval> embeddingRetrievals = recallResult(textReplaced, hasCandidateQuery(queryContext));
                Optional<Plugin> pluginOptional = choosePlugin(embeddingRetrievals, domainId);
                log.info("domain id :{} embedding result, text:{} embeddingResp:{} ",domainId, textReplaced, embeddingRetrievals);
                pluginOptional.ifPresent(plugin -> buildQuery(plugin, embeddingRetrievals, domainId, textReplaced, queryContext, schemaElementMatches));
            }
        } else {
            List<RecallRetrieval> embeddingRetrievals = recallResult(text, hasCandidateQuery(queryContext));
            Optional<Plugin> pluginOptional = choosePlugin(embeddingRetrievals, null);
            pluginOptional.ifPresent(plugin -> buildQuery(plugin, embeddingRetrievals, null, text, queryContext, Lists.newArrayList()));
        }
    }

    private void buildQuery(Plugin plugin, List<RecallRetrieval> embeddingRetrievals,
                            Long domainId, String text,
                            QueryContext queryContext, List<SchemaElementMatch> schemaElementMatches) {
        Map<String, RecallRetrieval> embeddingRetrievalMap = embeddingRetrievals.stream()
                .collect(Collectors.toMap(RecallRetrieval::getId, e -> e, (value1, value2) -> value1));
        log.info("EmbeddingBasedParser text: {} domain: {} choose plugin: [{} {}]",
                text, domainId, plugin.getId(), plugin.getName());
        PluginSemanticQuery pluginQuery = QueryManager.createPluginQuery(plugin.getType());
        SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(domainId, plugin, text,
                queryContext.getRequest(), embeddingRetrievalMap, schemaElementMatches);
        semanticParseInfo.setQueryMode(pluginQuery.getQueryMode());
        pluginQuery.setParseInfo(semanticParseInfo);
        queryContext.getCandidateQueries().add(pluginQuery);
    }

    private Set<Long> getDomainMatched(QueryContext queryContext) {
        Long queryDomainId = queryContext.getRequest().getDomainId();
        if (queryDomainId != null && queryDomainId > 0) {
            return Sets.newHashSet(queryDomainId);
        }
        return queryContext.getMapInfo().getMatchedDomains();
    }

    private SemanticParseInfo buildSemanticParseInfo(Long domainId, Plugin plugin, String text, QueryReq queryReq,
                                                     Map<String, RecallRetrieval> embeddingRetrievalMap,
                                                     List<SchemaElementMatch> schemaElementMatches) {
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDomain(domainId);
        schemaElement.setId(domainId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setElementMatches(schemaElementMatches);
        semanticParseInfo.setDomain(schemaElement);
        double distance = Double.parseDouble(embeddingRetrievalMap.get(plugin.getId().toString()).getDistance());
        double score = text.length() * (1 - distance);
        Map<String, Object> properties = new HashMap<>();
        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setRequest(queryReq);
        pluginParseResult.setDistance(distance);
        properties.put(Constants.CONTEXT, pluginParseResult);
        semanticParseInfo.setProperties(properties);
        semanticParseInfo.setScore(score);
        fillSemanticParseInfo(semanticParseInfo);
        setEntity(domainId, semanticParseInfo);
        return semanticParseInfo;
    }

    private List<SchemaElementMatch> getMatchedElements(QueryContext queryContext, Long domainId) {
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(domainId);
        if (schemaElementMatches == null) {
            return Lists.newArrayList();
        }
        QueryReq queryReq = queryContext.getRequest();
        QueryFilters queryFilters = queryReq.getQueryFilters();
        if (queryFilters == null || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return schemaElementMatches;
        }
        Map<Long, Object> element = queryFilters.getFilters().stream()
                .collect(Collectors.toMap(QueryFilter::getElementID, QueryFilter::getValue, (v1, v2) -> v1));
        return schemaElementMatches.stream().filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType())
                || SchemaElementType.ENTITY.equals(schemaElementMatch.getElement().getType()))
                .filter(schemaElementMatch ->
                !element.containsKey(schemaElementMatch.getElement().getId()) || (
                        element.containsKey(schemaElementMatch.getElement().getId()) &&
                                element.get(schemaElementMatch.getElement().getId()).toString()
                                        .equalsIgnoreCase(schemaElementMatch.getWord())
                        ))
                .collect(Collectors.toList());
    }

    private void setEntity(Long domainId, SemanticParseInfo semanticParseInfo) {
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        DomainSchema domainSchema = semanticService.getDomainSchema(domainId);
        if (domainSchema != null && domainSchema.getEntity() != null) {
            semanticParseInfo.setEntity(domainSchema.getEntity());
        }
    }

    private Optional<Plugin> choosePlugin(List<RecallRetrieval> embeddingRetrievals,
                                          Long domainId) {
        if (CollectionUtils.isEmpty(embeddingRetrievals)) {
            return Optional.empty();
        }
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        List<Plugin> plugins = pluginService.getPluginList();
        Map<Long, Plugin> pluginMap = plugins.stream().collect(Collectors.toMap(Plugin::getId, p -> p));
        for (RecallRetrieval embeddingRetrieval : embeddingRetrievals) {
            Plugin plugin = pluginMap.get(Long.parseLong(embeddingRetrieval.getId()));
            if (plugin == null) {
                continue;
            }
            if (domainId == null) {
                return Optional.of(plugin);
            }
            if (!CollectionUtils.isEmpty(plugin.getDomainList()) && plugin.getDomainList().contains(domainId)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public List<RecallRetrieval> recallResult(String embeddingText, boolean hasCandidateQuery) {
        try {
            PluginManager pluginManager = ContextUtils.getBean(PluginManager.class);
            EmbeddingResp embeddingResp = pluginManager.recognize(embeddingText);
            List<RecallRetrieval> embeddingRetrievals = embeddingResp.getRetrieval();
            if(!CollectionUtils.isEmpty(embeddingRetrievals)){
                if (hasCandidateQuery) {
                    embeddingRetrievals = embeddingRetrievals.stream()
                            .filter(llmRetrieval -> Double.parseDouble(llmRetrieval.getDistance())<THRESHOLD)
                            .collect(Collectors.toList());
                }
                embeddingRetrievals = embeddingRetrievals.stream().sorted(Comparator.comparingDouble(o ->
                        Math.abs(Double.parseDouble(o.getDistance())))).collect(Collectors.toList());
                embeddingResp.setRetrieval(embeddingRetrievals);
            }
            return embeddingRetrievals;
        } catch (Exception e) {
            log.warn("get embedding result error ", e);
        }
        return Lists.newArrayList();
    }

    private boolean hasCandidateQuery(QueryContext queryContext) {
        return !CollectionUtils.isEmpty(queryContext.getCandidateQueries());
    }

    private void fillSemanticParseInfo(SemanticParseInfo semanticParseInfo) {
        List<SchemaElementMatch> schemaElementMatches = semanticParseInfo.getElementMatches();
        if (!CollectionUtils.isEmpty(schemaElementMatches)) {
            schemaElementMatches.stream().filter(schemaElementMatch ->
                    SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                            || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                    .forEach(schemaElementMatch -> {
                       QueryFilter queryFilter = new QueryFilter();
                       queryFilter.setValue(schemaElementMatch.getWord());
                       queryFilter.setElementID(schemaElementMatch.getElement().getId());
                       queryFilter.setName(schemaElementMatch.getElement().getName());
                       queryFilter.setOperator(FilterOperatorEnum.EQUALS);
                       queryFilter.setBizName(schemaElementMatch.getElement().getBizName());
                       semanticParseInfo.getDimensionFilters().add(queryFilter);
                    });
        }
    }

    protected String replaceText(String text, List<SchemaElementMatch> schemaElementMatches) {
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return text;
        }
        List<SchemaElementMatch> valueSchemaElementMatches = schemaElementMatches.stream()
                .filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : valueSchemaElementMatches) {
            String detectWord = schemaElementMatch.getDetectWord();
            if (StringUtils.isBlank(detectWord)) {
                continue;
            }
            text = text.replace(detectWord, "");
        }
        return text;
    }

}
