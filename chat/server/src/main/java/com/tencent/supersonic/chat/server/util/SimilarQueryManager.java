package com.tencent.supersonic.chat.server.util;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.request.SimilarQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SimilarQueryRecallResp;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import dev.langchain4j.store.embedding.ComponentFactory;
import dev.langchain4j.store.embedding.EmbeddingQuery;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.S2EmbeddingStore;
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
import java.util.stream.Collectors;

@Slf4j
@Component
public class SimilarQueryManager {

    private EmbeddingConfig embeddingConfig;

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();


    public SimilarQueryManager(EmbeddingConfig embeddingConfig) {
        this.embeddingConfig = embeddingConfig;
    }

    public void saveSimilarQuery(SimilarQueryReq similarQueryReq) {
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return;
        }
        String queryText = similarQueryReq.getQueryText();
        try {
            EmbeddingQuery embeddingQuery = new EmbeddingQuery();
            embeddingQuery.setQueryId(String.valueOf(similarQueryReq.getQueryId()));
            embeddingQuery.setQuery(queryText);

            Map<String, Object> metaData = new HashMap<>();
            metaData.put("agentId", similarQueryReq.getAgentId());
            embeddingQuery.setMetadata(metaData);
            String solvedQueryCollection = embeddingConfig.getSolvedQueryCollection();
            s2EmbeddingStore.addQuery(solvedQueryCollection, Lists.newArrayList(embeddingQuery));
        } catch (Exception e) {
            log.warn("save history question to embedding failed, queryText:{}", queryText, e);
        }
    }

    public List<SimilarQueryRecallResp> recallSimilarQuery(String queryText, Integer agentId) {
        if (StringUtils.isBlank(embeddingConfig.getUrl())) {
            return Lists.newArrayList();
        }
        List<SimilarQueryRecallResp> similarQueryRecallResps = Lists.newArrayList();
        try {
            String solvedQueryCollection = embeddingConfig.getSolvedQueryCollection();
            int solvedQueryResultNum = embeddingConfig.getSolvedQueryResultNum();

            Map<String, String> filterCondition = new HashMap<>();
            filterCondition.put("agentId", String.valueOf(agentId));
            RetrieveQuery retrieveQuery = RetrieveQuery.builder()
                    .queryTextsList(Lists.newArrayList(queryText))
                    .filterCondition(filterCondition)
                    .build();
            List<RetrieveQueryResult> resultList = s2EmbeddingStore.retrieveQuery(solvedQueryCollection, retrieveQuery,
                    solvedQueryResultNum * 20);

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
                        SimilarQueryRecallResp similarQueryRecallResp = SimilarQueryRecallResp.builder()
                                .queryText(retrieval.getQuery())
                                .queryId(Long.parseLong(id))
                                .build();
                        similarQueryRecallResps.add(similarQueryRecallResp);
                        querySet.add(retrieval.getQuery());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("recall similar solved query failed, queryText:{}", queryText, e);
        }
        return similarQueryRecallResps.stream()
                .limit(embeddingConfig.getSolvedQueryResultNum()).collect(Collectors.toList());
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
