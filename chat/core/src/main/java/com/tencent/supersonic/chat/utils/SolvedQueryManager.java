package com.tencent.supersonic.chat.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.request.SolvedQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import com.tencent.supersonic.chat.parser.plugin.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.parser.plugin.embedding.EmbeddingResp;
import com.tencent.supersonic.chat.parser.plugin.embedding.RecallRetrieval;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class SolvedQueryManager {

    private EmbeddingConfig embeddingConfig;

    public SolvedQueryManager(EmbeddingConfig embeddingConfig) {
        this.embeddingConfig = embeddingConfig;
    }

    public void saveSolvedQuery(SolvedQueryReq solvedQueryReq) {
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return;
        }
        String queryText = solvedQueryReq.getQueryText();
        try {
            String uniqueId = generateUniqueId(solvedQueryReq.getQueryId(), solvedQueryReq.getParseId());
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("query", queryText);
            requestMap.put("query_id", uniqueId);
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("modelId", String.valueOf(solvedQueryReq.getModelId()));
            metaData.put("agentId", String.valueOf(solvedQueryReq.getAgentId()));
            requestMap.put("metadata", metaData);
            doRequest(embeddingConfig.getSolvedQueryAddPath(),
                    JSONObject.toJSONString(Lists.newArrayList(requestMap)));
        } catch (Exception e) {
            log.warn("save history question to embedding failed, queryText:{}", queryText, e);
        }
    }

    public List<SolvedQueryRecallResp> recallSolvedQuery(String queryText, Integer agentId) {
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return Lists.newArrayList();
        }
        List<SolvedQueryRecallResp> solvedQueryRecallResps = Lists.newArrayList();
        try {
            String url = embeddingConfig.getUrl() + embeddingConfig.getSolvedQueryRecallPath() + "?n_results="
                    + embeddingConfig.getSolvedQueryResultNum();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setLocation(URI.create(url));
            URI requestUrl = UriComponentsBuilder
                    .fromHttpUrl(url).build().encode().toUri();
            Map<String, Object> map = new HashMap<>();
            map.put("queryTextsList", Lists.newArrayList(queryText));
            Map<String, Object> filterCondition = new HashMap<>();
            filterCondition.put("agentId", String.valueOf(agentId));
            map.put("filterCondition", filterCondition);
            String jsonBody = JSONObject.toJSONString(map, SerializerFeature.WriteMapNullValue);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            log.info("[embedding] request body:{}, url:{}", jsonBody, url);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<List<EmbeddingResp>> embeddingResponseEntity =
                    restTemplate.exchange(requestUrl, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<List<EmbeddingResp>>() {
                            });
            log.info("[embedding] recognize result body:{}", embeddingResponseEntity);
            List<EmbeddingResp> embeddingResps = embeddingResponseEntity.getBody();
            Set<String> querySet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(embeddingResps)) {
                for (EmbeddingResp embeddingResp : embeddingResps) {
                    List<RecallRetrieval> embeddingRetrievals = embeddingResp.getRetrieval();
                    for (RecallRetrieval embeddingRetrieval : embeddingRetrievals) {
                        if (queryText.equalsIgnoreCase(embeddingRetrieval.getQuery())) {
                            continue;
                        }
                        if (querySet.contains(embeddingRetrieval.getQuery())) {
                            continue;
                        }
                        String id = embeddingRetrieval.getId();
                        SolvedQueryRecallResp solvedQueryRecallResp = SolvedQueryRecallResp.builder()
                                .queryText(embeddingRetrieval.getQuery())
                                .queryId(getQueryId(id)).parseId(getParseId(id))
                                .build();
                        solvedQueryRecallResps.add(solvedQueryRecallResp);
                        querySet.add(embeddingRetrieval.getQuery());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("recall similar solved query failed, queryText:{}", queryText);
        }
        return solvedQueryRecallResps;
    }

    private String generateUniqueId(Long queryId, Integer parseId) {
        String uniqueId = queryId + String.valueOf(parseId);
        if (parseId < 10) {
            uniqueId = queryId + String.format("0%s", parseId);
        }
        return uniqueId;
    }

    private Long getQueryId(String uniqueId) {
        return Long.parseLong(uniqueId) / 100;
    }

    private Integer getParseId(String uniqueId) {
        return Integer.parseInt(uniqueId) % 100;
    }

    private ResponseEntity<String> doRequest(String path, String jsonBody) {
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
            log.info("[embedding] request body :{}, url:{}", jsonBody, url);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl,
                    HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {});
            log.info("[embedding] result body:{}", responseEntity);
            return responseEntity;
        } catch (Exception e) {
            log.warn("connect to embedding service failed, url:{}", url);
        }
        return ResponseEntity.of(Optional.empty());
    }

}
