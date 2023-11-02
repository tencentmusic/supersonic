package com.tencent.supersonic.chat.plugin;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.PluginTool;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.chat.parser.plugin.embedding.EmbeddingResp;
import com.tencent.supersonic.chat.parser.plugin.embedding.RecallRetrieval;
import com.tencent.supersonic.chat.plugin.event.PluginAddEvent;
import com.tencent.supersonic.chat.plugin.event.PluginDelEvent;
import com.tencent.supersonic.chat.plugin.event.PluginUpdateEvent;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.net.URI;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.Optional;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class PluginManager {

    private EmbeddingConfig embeddingConfig;

    private RestTemplate restTemplate;

    public PluginManager(EmbeddingConfig embeddingConfig, RestTemplate restTemplate) {
        this.embeddingConfig = embeddingConfig;
        this.restTemplate = restTemplate;
    }

    public static List<Plugin> getPluginAgentCanSupport(Integer agentId) {
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        List<Plugin> plugins = pluginService.getPluginList();
        if (agentId == null) {
            return plugins;
        }
        Agent agent = ContextUtils.getBean(AgentService.class).getAgent(agentId);
        if (agent == null) {
            return plugins;
        }
        List<Long> pluginIds = getPluginTools(agentId).stream().map(PluginTool::getPlugins)
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

    private static List<PluginTool> getPluginTools(Integer agentId) {
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(agentId);
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

    public void requestEmbeddingPluginDelete(Set<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        doRequest(embeddingConfig.getDeletePath(), JSONObject.toJSONString(ids));
    }

    public void requestEmbeddingPluginAdd(List<Map<String, String>> maps) {
        if (CollectionUtils.isEmpty(maps)) {
            return;
        }
        doRequest(embeddingConfig.getAddPath(), JSONObject.toJSONString(maps));
    }

    public ResponseEntity<String> doRequest(String path, String jsonBody) {
        if (Strings.isEmpty(embeddingConfig.getUrl())) {
            return ResponseEntity.of(Optional.empty());
        }
        String url = embeddingConfig.getUrl() + path;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setLocation(URI.create(url));
            URI requestUrl = UriComponentsBuilder
                    .fromHttpUrl(url).build().encode().toUri();
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            log.info("[embedding] equest body :{}, url:{}", jsonBody, url);
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl,
                    HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {});
            log.info("[embedding] result body:{}", responseEntity);
            return responseEntity;
        } catch (Throwable e) {
            log.warn("connect to embedding service failed, url:{}", url);
        }
        return ResponseEntity.of(Optional.empty());
    }

    public void requestEmbeddingPluginAddALL(List<Plugin> plugins) {
        requestEmbeddingPluginAdd(convert(plugins));
    }

    public EmbeddingResp recognize(String embeddingText) {
        String url = embeddingConfig.getUrl() + embeddingConfig.getRecognizePath() + "?n_results="
                + embeddingConfig.getNResult();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setLocation(URI.create(url));
        URI requestUrl = UriComponentsBuilder
                .fromHttpUrl(url).build().encode().toUri();
        String jsonBody = JSONObject.toJSONString(Lists.newArrayList(embeddingText));
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        log.info("[embedding] request body:{}, url:{}", jsonBody, url);
        ResponseEntity<List<EmbeddingResp>> embeddingResponseEntity =
                restTemplate.exchange(requestUrl, HttpMethod.POST, entity,
                        new ParameterizedTypeReference<List<EmbeddingResp>>() {
                        });
        log.info("[embedding] recognize result body:{}", embeddingResponseEntity);
        List<EmbeddingResp> embeddingResps = embeddingResponseEntity.getBody();
        if (CollectionUtils.isNotEmpty(embeddingResps)) {
            for (EmbeddingResp embeddingResp : embeddingResps) {
                List<RecallRetrieval> embeddingRetrievals = embeddingResp.getRetrieval();
                for (RecallRetrieval embeddingRetrieval : embeddingRetrievals) {
                    embeddingRetrieval.setId(getPluginIdFromEmbeddingId(embeddingRetrieval.getId()));
                }
            }
            return embeddingResps.get(0);
        }
        throw new RuntimeException("get embedding result failed");
    }

    public List<Map<String, String>> convert(List<Plugin> plugins) {
        List<Map<String, String>> maps = Lists.newArrayList();
        for (Plugin plugin : plugins) {
            List<String> exampleQuestions = plugin.getExampleQuestionList();
            int num = 0;
            for (String pattern : exampleQuestions) {
                Map<String, String> map = new HashMap<>();
                map.put("preset_query_id", generateUniqueEmbeddingId(num, plugin.getId()));
                map.put("preset_query", pattern);
                maps.add(map);
                num++;
            }
        }
        return maps;
    }

    private Set<String> getEmbeddingId(List<Plugin> plugins) {
        Set<String> embeddingIdSet = new HashSet<>();
        for (Map<String, String> map : convert(plugins)) {
            embeddingIdSet.add(map.get("preset_query_id"));
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

    public static Pair<Boolean, Set<Long>> resolve(Plugin plugin, QueryContext queryContext) {
        SchemaMapInfo schemaMapInfo = queryContext.getMapInfo();
        Set<Long> pluginMatchedModel = getPluginMatchedModel(plugin, queryContext);
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

    private static Set<Long> getPluginMatchedModel(Plugin plugin, QueryContext queryContext) {
        Set<Long> matchedModel = queryContext.getMapInfo().getMatchedModels();
        if (plugin.isContainsAllModel()) {
            return Sets.newHashSet(plugin.getDefaultMode());
        }
        List<Long> modelIds = plugin.getModelList();
        Set<Long> pluginMatchedModel = Sets.newHashSet();
        for (Long modelId : modelIds) {
            if (matchedModel.contains(modelId)) {
                pluginMatchedModel.add(modelId);
            }
        }
        return pluginMatchedModel;
    }

}
