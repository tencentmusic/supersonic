package com.tencent.supersonic.chat.server.util;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.response.SimilarQueryRecallResp;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.service.EmbeddingService;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SimilarQueryManager {

    private EmbeddingConfig embeddingConfig;

    @Autowired
    private EmbeddingService embeddingService;


    public SimilarQueryManager(EmbeddingConfig embeddingConfig) {
        this.embeddingConfig = embeddingConfig;
    }

    public List<SimilarQueryRecallResp> recallSimilarQuery(String queryText, Integer agentId) {
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
            List<RetrieveQueryResult> resultList = embeddingService.retrieveQuery(solvedQueryCollection, retrieveQuery,
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
}
