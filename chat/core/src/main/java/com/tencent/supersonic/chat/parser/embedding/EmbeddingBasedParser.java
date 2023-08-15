package com.tencent.supersonic.chat.parser.embedding;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EmbeddingBasedParser implements SemanticParser {

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return;
        }
        log.info("EmbeddingBasedParser parser query ctx: {}, chat ctx: {}", queryContext, chatContext);
        String text = queryContext.getRequest().getQueryText();
        List<RecallRetrieval> embeddingRetrievals = recallResult(text);
        choosePlugin(embeddingRetrievals, queryContext);
    }

    private void choosePlugin(List<RecallRetrieval> embeddingRetrievals,
            QueryContext queryContext) {
        if (CollectionUtils.isEmpty(embeddingRetrievals)) {
            return;
        }
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        List<Plugin> plugins = pluginService.getPluginList();
        Map<Long, Plugin> pluginMap = plugins.stream().collect(Collectors.toMap(Plugin::getId, p -> p));
        for (RecallRetrieval embeddingRetrieval : embeddingRetrievals) {
            Plugin plugin = pluginMap.get(Long.parseLong(embeddingRetrieval.getId()));
            if (plugin == null) {
                continue;
            }
            Pair<Boolean, List<Long>> pair = PluginManager.resolve(plugin, queryContext);
            log.info("embedding plugin resolve: {}", pair);
            if (pair.getLeft()) {
                List<Long> modelList = pair.getRight();
                if (CollectionUtils.isEmpty(modelList)) {
                    return;
                }
                modelList = distinctModelList(plugin, queryContext.getMapInfo(), modelList);
                for (Long modelId : modelList) {
                    buildQuery(plugin, Double.parseDouble(embeddingRetrieval.getDistance()), modelId, queryContext,
                            queryContext.getMapInfo().getMatchedElements(modelId));
                }
                return;
            }
        }
    }

    public List<Long> distinctModelList(Plugin plugin, SchemaMapInfo schemaMapInfo, List<Long> modelList) {
        if (!plugin.isContainsAllModel()) {
            return modelList;
        }
        boolean noElementMatch = true;
        for (Long model : modelList) {
            List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(model);
            if (!CollectionUtils.isEmpty(schemaElementMatches)) {
                noElementMatch = false;
            }
        }
        if (noElementMatch) {
            return modelList.subList(0, 1);
        }
        return modelList;
    }

    private void buildQuery(Plugin plugin, double distance, Long modelId,
            QueryContext queryContext, List<SchemaElementMatch> schemaElementMatches) {
        log.info("EmbeddingBasedParser Model: {} choose plugin: [{} {}]", modelId, plugin.getId(), plugin.getName());
        PluginSemanticQuery pluginQuery = QueryManager.createPluginQuery(plugin.getType());
        plugin.setParseMode(ParseMode.EMBEDDING_RECALL);
        SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(modelId, plugin, queryContext.getRequest(),
                schemaElementMatches, distance);
        double score = queryContext.getRequest().getQueryText().length() * (1 - distance);
        semanticParseInfo.setQueryMode(pluginQuery.getQueryMode());
        semanticParseInfo.setScore(score);
        pluginQuery.setParseInfo(semanticParseInfo);
        queryContext.getCandidateQueries().add(pluginQuery);
    }

    private SemanticParseInfo buildSemanticParseInfo(Long modelId, Plugin plugin, QueryReq queryReq,
            List<SchemaElementMatch> schemaElementMatches, double distance) {
        if (modelId == null && !CollectionUtils.isEmpty(plugin.getModelList())) {
            modelId = plugin.getModelList().get(0);
        }
        SchemaElement Model = new SchemaElement();
        Model.setModel(modelId);
        Model.setId(modelId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setElementMatches(schemaElementMatches);
        semanticParseInfo.setModel(Model);
        Map<String, Object> properties = new HashMap<>();
        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setRequest(queryReq);
        pluginParseResult.setDistance(distance);
        properties.put(Constants.CONTEXT, pluginParseResult);
        semanticParseInfo.setProperties(properties);
        semanticParseInfo.setScore(distance);
        fillSemanticParseInfo(semanticParseInfo);
        setEntity(modelId, semanticParseInfo);
        return semanticParseInfo;
    }

    private void setEntity(Long modelId, SemanticParseInfo semanticParseInfo) {
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        ModelSchema ModelSchema = semanticService.getModelSchema(modelId);
        if (ModelSchema != null && ModelSchema.getEntity() != null) {
            semanticParseInfo.setEntity(ModelSchema.getEntity());
        }
    }

    public List<RecallRetrieval> recallResult(String embeddingText) {
        try {
            PluginManager pluginManager = ContextUtils.getBean(PluginManager.class);
            EmbeddingResp embeddingResp = pluginManager.recognize(embeddingText);
            List<RecallRetrieval> embeddingRetrievals = embeddingResp.getRetrieval();
            if (!CollectionUtils.isEmpty(embeddingRetrievals)) {
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

}
