package com.tencent.supersonic.chat.utils;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.request.SolvedQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.embedding.EmbeddingQuery;
import com.tencent.supersonic.common.util.embedding.EmbeddingUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

@Slf4j
@Component
public class SolvedQueryManager {

    private EmbeddingConfig embeddingConfig;

    private EmbeddingUtils embeddingUtils;

    public SolvedQueryManager(EmbeddingConfig embeddingConfig, EmbeddingUtils embeddingUtils) {
        this.embeddingConfig = embeddingConfig;
        this.embeddingUtils = embeddingUtils;
    }

    public void saveSolvedQuery(SolvedQueryReq solvedQueryReq) {
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return;
        }
        String queryText = solvedQueryReq.getQueryText();
        try {
            String uniqueId = generateUniqueId(solvedQueryReq.getQueryId(), solvedQueryReq.getParseId());
            EmbeddingQuery embeddingQuery = new EmbeddingQuery();
            embeddingQuery.setQueryId(uniqueId);
            embeddingQuery.setQuery(queryText);

            Map<String, String> metaData = new HashMap<>();
            metaData.put("modelId", String.valueOf(solvedQueryReq.getModelId()));
            metaData.put("agentId", String.valueOf(solvedQueryReq.getAgentId()));
            embeddingQuery.setMetadata(metaData);
            String solvedQueryCollection = embeddingConfig.getSolvedQueryCollection();
            embeddingUtils.addQuery(solvedQueryCollection, Lists.newArrayList(embeddingQuery));
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
            String solvedQueryCollection = embeddingConfig.getSolvedQueryCollection();
            int solvedQueryResultNum = embeddingConfig.getSolvedQueryResultNum();

            Map<String, String> filterCondition = new HashMap<>();
            filterCondition.put("agentId", String.valueOf(agentId));
            RetrieveQuery retrieveQuery = RetrieveQuery.builder()
                    .queryTextsList(Lists.newArrayList(queryText))
                    .filterCondition(filterCondition)
                    .build();
            List<RetrieveQueryResult> resultList = embeddingUtils.retrieveQuery(solvedQueryCollection, retrieveQuery,
                    solvedQueryResultNum);

            log.info("[embedding] recognize result body:{}", resultList);
            Set<String> querySet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(resultList)) {
                for (RetrieveQueryResult retrieveQueryResult : resultList) {
                    List<Retrieval> retrievals = retrieveQueryResult.getRetrieval();
                    for (Retrieval retrieval : retrievals) {
                        if (queryText.equalsIgnoreCase(retrieval.getQuery())) {
                            continue;
                        }
                        if (querySet.contains(retrieval.getQuery())) {
                            continue;
                        }
                        String id = retrieval.getId();
                        SolvedQueryRecallResp solvedQueryRecallResp = SolvedQueryRecallResp.builder()
                                .queryText(retrieval.getQuery())
                                .queryId(getQueryId(id)).parseId(getParseId(id))
                                .build();
                        solvedQueryRecallResps.add(solvedQueryRecallResp);
                        querySet.add(retrieval.getQuery());
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
                    HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                    });
            log.info("[embedding] result body:{}", responseEntity);
            return responseEntity;
        } catch (Exception e) {
            log.warn("connect to embedding service failed, url:{}", url);
        }
        return ResponseEntity.of(Optional.empty());
    }

}
