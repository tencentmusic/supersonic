package com.tencent.supersonic.chat.plugin;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.parser.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.parser.embedding.EmbeddingResp;
import com.tencent.supersonic.chat.parser.embedding.RecallRetrieval;
import com.tencent.supersonic.chat.plugin.event.PluginAddEvent;
import com.tencent.supersonic.chat.plugin.event.PluginUpdateEvent;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.common.util.ContextUtils;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class PluginManager {

    private static Map<String, Plugin> internalPluginMap = new ConcurrentHashMap<>();

    private EmbeddingConfig embeddingConfig;

    private RestTemplate restTemplate;

    public PluginManager(EmbeddingConfig embeddingConfig, RestTemplate restTemplate) {
        this.embeddingConfig = embeddingConfig;
        this.restTemplate = restTemplate;
    }

    public static List<Plugin> getPlugins() {
        PluginService pluginService = ContextUtils.getBean(PluginService.class);
        List<Plugin> pluginList = pluginService.getPluginList();
        pluginList.addAll(internalPluginMap.values());
        return new ArrayList<>(pluginList);
    }

    @EventListener
    public void addPlugin(PluginAddEvent pluginAddEvent) {
        Plugin plugin = pluginAddEvent.getPlugin();
        if (ParseMode.EMBEDDING_RECALL.equals(plugin.getParseMode())) {
            requestEmbeddingPluginAdd(convert(Lists.newArrayList(plugin)));
        }
    }

    @EventListener
    public void updatePlugin(PluginUpdateEvent pluginUpdateEvent) {
        Plugin oldPlugin = pluginUpdateEvent.getOldPlugin();
        Plugin newPlugin = pluginUpdateEvent.getNewPlugin();
        if (ParseMode.EMBEDDING_RECALL.equals(oldPlugin.getParseMode())) {
            requestEmbeddingPluginDelete(getEmbeddingId(Lists.newArrayList(oldPlugin)));
        }
        if (ParseMode.EMBEDDING_RECALL.equals(newPlugin.getParseMode())) {
            requestEmbeddingPluginAdd(convert(Lists.newArrayList(newPlugin)));
        }
    }

    @EventListener
    public void delPlugin(PluginAddEvent pluginAddEvent) {
        Plugin plugin = pluginAddEvent.getPlugin();
        if (ParseMode.EMBEDDING_RECALL.equals(plugin.getParseMode())) {
            requestEmbeddingPluginDelete(getEmbeddingId(Lists.newArrayList(plugin)));
        }

    }

    public void requestEmbeddingPluginDelete(Set<String> ids) {
        if(CollectionUtils.isEmpty(ids)){
            return;
        }
        doRequest(embeddingConfig.getDeletePath(), JSONObject.toJSONString(ids));
    }


    public void requestEmbeddingPluginAdd(List<Map<String,String>> maps) {
        if(CollectionUtils.isEmpty(maps)){
            return;
        }
       doRequest(embeddingConfig.getAddPath(), JSONObject.toJSONString(maps));
    }

    public void doRequest(String path, String jsonBody) {
        String url = embeddingConfig.getUrl()+ path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setLocation(URI.create(url));
        URI requestUrl = UriComponentsBuilder
                .fromHttpUrl(url).build().encode().toUri();
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        log.info("[embedding] equest body :{}, url:{}", jsonBody, url);
        ResponseEntity<String> responseEntity =
                restTemplate.exchange(requestUrl, HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {});
        log.info("[embedding] result body:{}", responseEntity);
    }

    public void requestEmbeddingPluginAddALL(List<Plugin> plugins) {
        plugins = plugins.stream()
                .filter(plugin -> ParseMode.EMBEDDING_RECALL.equals(plugin.getParseMode()))
                .collect(Collectors.toList());
        requestEmbeddingPluginAdd(convert(plugins));
    }

    public EmbeddingResp recognize(String embeddingText) {
        String url = embeddingConfig.getUrl()+ embeddingConfig.getRecognizePath() + "?n_results=" + embeddingConfig.getNResult();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setLocation(URI.create(url));
        URI requestUrl = UriComponentsBuilder
                .fromHttpUrl(url).build().encode().toUri();
        String jsonBody = JSONObject.toJSONString(Lists.newArrayList(embeddingText));
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        log.info("[embedding] request body:{}, url:{}", jsonBody, url);
        ResponseEntity<List<EmbeddingResp>> embeddingResponseEntity =
                restTemplate.exchange(requestUrl, HttpMethod.POST, entity, new ParameterizedTypeReference<List<EmbeddingResp>>() {});
        log.info("[embedding] recognize result body:{}",embeddingResponseEntity);
        List<EmbeddingResp> embeddingResps = embeddingResponseEntity.getBody();
        if(CollectionUtils.isNotEmpty(embeddingResps)){
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

    public List<Map<String, String>> convert(List<Plugin> plugins){
        List<Map<String, String>> maps = Lists.newArrayList();
        for(Plugin plugin : plugins){
            List<String> patterns = plugin.getPatterns();
            int num = 0;
            for(String pattern : patterns){
                Map<String,String> map = new HashMap<>();
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
            embeddingIdSet.addAll(map.keySet());
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

}
