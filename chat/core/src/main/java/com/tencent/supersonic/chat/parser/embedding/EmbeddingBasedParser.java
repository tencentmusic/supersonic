package com.tencent.supersonic.chat.parser.embedding;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.config.ChatConfigRich;
import com.tencent.supersonic.chat.config.EntityRichInfo;
import com.tencent.supersonic.chat.parser.SatisfactionChecker;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EmbeddingBasedParser implements SemanticParser {

    private final static double THRESHOLD = 0.2d;

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        if (SatisfactionChecker.check(queryContext) || StringUtils.isBlank(embeddingConfig.getUrl())) {
            return;
        }
        log.info("EmbeddingBasedParser parser query ctx: {}, chat ctx: {}", queryContext, chatContext);
        for (Long domainId : getDomainMatched(queryContext)) {
            String text = replaceText(queryContext, domainId);
            List<RecallRetrieval> embeddingRetrievals = recallResult(text, hasCandidateQuery(queryContext));
            Optional<Plugin> pluginOptional = choosePlugin(embeddingRetrievals, domainId);
            if (pluginOptional.isPresent()) {
                Map<String, RecallRetrieval> embeddingRetrievalMap = embeddingRetrievals.stream()
                        .collect(Collectors.toMap(RecallRetrieval::getId, e -> e, (value1, value2) -> value1));
                Plugin plugin  = pluginOptional.get();
                log.info("EmbeddingBasedParser text: {} domain: {} choose plugin: [{} {}]",
                        text, domainId, plugin.getId(), plugin.getName());
                PluginSemanticQuery pluginQuery = QueryManager.createPluginQuery(plugin.getType());
                SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(queryContext, domainId,
                        plugin, embeddingRetrievalMap);
                semanticParseInfo.setQueryMode(pluginQuery.getQueryMode());
                pluginQuery.setParseInfo(semanticParseInfo);
                queryContext.getCandidateQueries().add(pluginQuery);
            }
        }
    }

    private Set<Long> getDomainMatched(QueryContext queryContext) {
        Long queryDomainId = queryContext.getRequest().getDomainId();
        if (queryDomainId != null && queryDomainId > 0) {
            return Sets.newHashSet(queryDomainId);
        }
        return queryContext.getMapInfo().getMatchedDomains();
    }

    private SemanticParseInfo buildSemanticParseInfo(QueryContext queryContext, Long domainId, Plugin plugin,
                                                     Map<String, RecallRetrieval> embeddingRetrievalMap) {
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDomain(domainId);
        schemaElement.setId(domainId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setDomain(schemaElement);
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        if (Double.parseDouble(embeddingRetrievalMap.get(plugin.getId().toString()).getDistance()) < THRESHOLD) {
            semanticParseInfo.setBonus(SatisfactionChecker.BONUS_THRESHOLD);
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, plugin);
        semanticParseInfo.setProperties(properties);
        semanticParseInfo.setElementMatches(schemaMapInfo.getMatchedElements(domainId));
        fillSemanticParseInfo(queryContext, semanticParseInfo);
        setEntityId(domainId, semanticParseInfo);
        return semanticParseInfo;
    }

    private Optional<Long> getEntityElementId(Long domainId) {
        ConfigService configService = ContextUtils.getBean(ConfigService.class);
        ChatConfigRich chatConfigRich = configService.getConfigRichInfo(domainId);
        EntityRichInfo entityRichInfo = chatConfigRich.getChatDetailRichConfig().getEntity();
        if (entityRichInfo != null) {
            SchemaElement schemaElement = entityRichInfo.getDimItem();
            if (schemaElement != null) {
                return Optional.of(schemaElement.getId());
            }
        }
        return Optional.empty();
    }

    private void setEntityId(Long domainId, SemanticParseInfo semanticParseInfo) {
        Optional<Long> entityElementIdOptional = getEntityElementId(domainId);
        if (entityElementIdOptional.isPresent()) {
            Long entityElementId = entityElementIdOptional.get();
            for (QueryFilter filter : semanticParseInfo.getDimensionFilters()) {
                if (entityElementId.equals(filter.getElementID())) {
                    String value = String.valueOf(filter.getValue());
                    if (StringUtils.isNumeric(value)) {
                        semanticParseInfo.setEntity(Long.parseLong(value));
                    }
                }
            }
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
            log.info("embedding result, text:{} embeddingResp:{}", embeddingText, embeddingResp);
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

    private void fillSemanticParseInfo(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        if (queryContext.getRequest().getQueryFilters() != null) {
            semanticParseInfo.getDimensionFilters()
                    .addAll(queryContext.getRequest().getQueryFilters().getFilters());
        }
    }

    protected String replaceText(QueryContext queryContext, Long domainId) {
        String text = queryContext.getRequest().getQueryText();
        List<SchemaElementMatch> schemaElementMatches = queryContext.getMapInfo().getMatchedElements(domainId);
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return text;
        }
        List<SchemaElementMatch> valueSchemaElementMatches = schemaElementMatches.stream()
                .filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType()))
                .collect(Collectors.toList());
        for (SchemaElementMatch schemaElementMatch : valueSchemaElementMatches) {
            String detectWord = schemaElementMatch.getDetectWord();
            text = text.replace(detectWord, "");
        }
        return text;
    }

}
