package com.tencent.supersonic.chat.server.plugin;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.PluginTool;
import com.tencent.supersonic.chat.server.plugin.build.ParamOption;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.chat.server.plugin.event.PluginAddEvent;
import com.tencent.supersonic.chat.server.plugin.event.PluginDelEvent;
import com.tencent.supersonic.chat.server.plugin.event.PluginUpdateEvent;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PluginManager {

    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Autowired
    private EmbeddingService embeddingService;

    public static List<Plugin> getPluginAgentCanSupport(ChatParseContext chatParseContext) {
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        Agent agent = chatParseContext.getAgent();
        List<Plugin> plugins = pluginService.getPluginList();
        if (Objects.isNull(agent)) {
            return plugins;
        }
        List<Long> pluginIds = getPluginTools(agent).stream().map(PluginTool::getPlugins)
                .flatMap(Collection::stream).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pluginIds)) {
            return Lists.newArrayList();
        }
        plugins = plugins.stream().filter(plugin -> pluginIds.contains(plugin.getId()))
                .collect(Collectors.toList());
        log.info("plugins witch can be supported by cur agent :{} {}", agent.getName(),
                plugins.stream().map(Plugin::getName).collect(Collectors.toList()));
        return plugins;
    }

    private static List<PluginTool> getPluginTools(Agent agent) {
        if (agent == null) {
            return Lists.newArrayList();
        }
        List<String> tools = agent.getTools(AgentToolType.PLUGIN);
        if (CollectionUtils.isEmpty(tools)) {
            return Lists.newArrayList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, PluginTool.class))
                .collect(Collectors.toList());
    }

    @EventListener
    public void addPlugin(PluginAddEvent pluginAddEvent) {
        Plugin plugin = pluginAddEvent.getPlugin();
        if (CollectionUtils.isNotEmpty(plugin.getExampleQuestionList())) {
            requestEmbeddingPluginAdd(convert(Lists.newArrayList(plugin)));
        }
    }

    @EventListener
    public void updatePlugin(PluginUpdateEvent pluginUpdateEvent) {
        Plugin oldPlugin = pluginUpdateEvent.getOldPlugin();
        Plugin newPlugin = pluginUpdateEvent.getNewPlugin();
        if (CollectionUtils.isNotEmpty(oldPlugin.getExampleQuestionList())) {
            requestEmbeddingPluginDelete(getEmbeddingId(Lists.newArrayList(oldPlugin)));
        }
        if (CollectionUtils.isNotEmpty(newPlugin.getExampleQuestionList())) {
            requestEmbeddingPluginAdd(convert(Lists.newArrayList(newPlugin)));
        }
    }

    @EventListener
    public void delPlugin(PluginDelEvent pluginDelEvent) {
        Plugin plugin = pluginDelEvent.getPlugin();
        if (CollectionUtils.isNotEmpty(plugin.getExampleQuestionList())) {
            requestEmbeddingPluginDelete(getEmbeddingId(Lists.newArrayList(plugin)));
        }
    }

    public void requestEmbeddingPluginDelete(Set<String> queryIds) {
        if (CollectionUtils.isEmpty(queryIds)) {
            return;
        }
        String presetCollection = embeddingConfig.getPresetCollection();

        List<TextSegment> queries = new ArrayList<>();
        for (String id : queryIds) {
            TextSegment query = TextSegment.from("");
            TextSegmentConvert.addQueryId(query, id);
            queries.add(query);
        }
        embeddingService.deleteQuery(presetCollection, queries);
    }

    public void requestEmbeddingPluginAdd(List<TextSegment> queries) {
        if (CollectionUtils.isEmpty(queries)) {
            return;
        }
        String presetCollection = embeddingConfig.getPresetCollection();
        embeddingService.addQuery(presetCollection, queries);
    }

    public RetrieveQueryResult recognize(String embeddingText) {

        RetrieveQuery retrieveQuery = RetrieveQuery.builder()
                .queryTextsList(Collections.singletonList(embeddingText))
                .build();

        List<RetrieveQueryResult> resultList = embeddingService.retrieveQuery(embeddingConfig.getPresetCollection(),
                retrieveQuery, embeddingConfig.getNResult());

        if (CollectionUtils.isNotEmpty(resultList)) {
            for (RetrieveQueryResult embeddingResp : resultList) {
                List<Retrieval> embeddingRetrievals = embeddingResp.getRetrieval();
                for (Retrieval embeddingRetrieval : embeddingRetrievals) {
                    embeddingRetrieval.setId(getPluginIdFromEmbeddingId(embeddingRetrieval.getId()));
                }
            }
            return resultList.get(0);
        }
        throw new RuntimeException("get embedding result failed");
    }

    public List<TextSegment> convert(List<Plugin> plugins) {
        List<TextSegment> queries = Lists.newArrayList();
        for (Plugin plugin : plugins) {
            List<String> exampleQuestions = plugin.getExampleQuestionList();
            int num = 0;
            for (String pattern : exampleQuestions) {
                TextSegment query = TextSegment.from(pattern);
                TextSegmentConvert.addQueryId(query, generateUniqueEmbeddingId(num, plugin.getId()));
                queries.add(query);
                num++;
            }
        }
        return queries;
    }

    private Set<String> getEmbeddingId(List<Plugin> plugins) {
        Set<String> embeddingIdSet = new HashSet<>();
        for (TextSegment query : convert(plugins)) {
            TextSegmentConvert.addQueryId(query, TextSegmentConvert.getQueryId(query));
        }
        return embeddingIdSet;
    }

    //num can not bigger than 100
    private String generateUniqueEmbeddingId(int num, Long pluginId) {
        if (num < 10) {
            return String.format("%s00%s", pluginId, num);
        } else {
            return String.format("%s0%s", pluginId, num);
        }
    }

    private String getPluginIdFromEmbeddingId(String id) {
        return String.valueOf(Integer.parseInt(id) / 1000);
    }

    public static Pair<Boolean, Set<Long>> resolve(Plugin plugin, ChatParseContext chatParseContext) {
        SchemaMapInfo schemaMapInfo = chatParseContext.getMapInfo();
        Set<Long> pluginMatchedModel = getPluginMatchedModel(plugin, chatParseContext);
        if (CollectionUtils.isEmpty(pluginMatchedModel) && !plugin.isContainsAllModel()) {
            return Pair.of(false, Sets.newHashSet());
        }
        List<ParamOption> paramOptions = getSemanticOption(plugin);
        if (CollectionUtils.isEmpty(paramOptions)) {
            return Pair.of(true, pluginMatchedModel);
        }
        Set<Long> matchedModel = Sets.newHashSet();
        Map<Long, List<ParamOption>> paramOptionMap = paramOptions.stream()
                .collect(Collectors.groupingBy(ParamOption::getModelId));
        for (Long modelId : paramOptionMap.keySet()) {
            List<ParamOption> params = paramOptionMap.get(modelId);
            if (CollectionUtils.isEmpty(params)) {
                matchedModel.add(modelId);
                continue;
            }
            boolean matched = true;
            for (ParamOption paramOption : params) {
                Set<Long> elementIdSet = getSchemaElementMatch(modelId, schemaMapInfo);
                if (CollectionUtils.isEmpty(elementIdSet)) {
                    matched = false;
                    break;
                }
                if (!elementIdSet.contains(paramOption.getElementId())) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                matchedModel.add(modelId);
            }
        }
        if (CollectionUtils.isEmpty(matchedModel)) {
            return Pair.of(false, Sets.newHashSet());
        }
        return Pair.of(true, matchedModel);
    }

    private static Set<Long> getSchemaElementMatch(Long modelId, SchemaMapInfo schemaMapInfo) {
        List<SchemaElementMatch> schemaElementMatches = schemaMapInfo.getMatchedElements(modelId);
        if (org.springframework.util.CollectionUtils.isEmpty(schemaElementMatches)) {
            return Sets.newHashSet();
        }
        return schemaElementMatches.stream().filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                                || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                .map(SchemaElementMatch::getElement)
                .map(SchemaElement::getId)
                .collect(Collectors.toSet());
    }

    private static List<ParamOption> getSemanticOption(Plugin plugin) {
        WebBase webBase = JSONObject.parseObject(plugin.getConfig(), WebBase.class);
        if (Objects.isNull(webBase)) {
            return null;
        }
        List<ParamOption> paramOptions = webBase.getParamOptions();
        if (org.springframework.util.CollectionUtils.isEmpty(paramOptions)) {
            return Lists.newArrayList();
        }
        return paramOptions.stream()
                .filter(paramOption -> ParamOption.ParamType.SEMANTIC.equals(paramOption.getParamType()))
                .collect(Collectors.toList());
    }

    private static Set<Long> getPluginMatchedModel(Plugin plugin, ChatParseContext chatParseContext) {
        Set<Long> matchedDataSets = chatParseContext.getMapInfo().getMatchedDataSetInfos();
        if (plugin.isContainsAllModel()) {
            return Sets.newHashSet(plugin.getDefaultMode());
        }
        List<Long> modelIds = plugin.getDataSetList();
        Set<Long> pluginMatchedModel = Sets.newHashSet();
        for (Long modelId : modelIds) {
            if (matchedDataSets.contains(modelId)) {
                pluginMatchedModel.add(modelId);
            }
        }
        return pluginMatchedModel;
    }

}
