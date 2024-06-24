package com.tencent.supersonic.common.service.impl;

import com.tencent.supersonic.common.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Autowired
    private EmbeddingStoreFactory embeddingStoreFactory;

    @Autowired
    private EmbeddingModel embeddingModel;

    public synchronized void addCollection(String collectionName) {
        embeddingStoreFactory.create(collectionName);
    }

    @Override
    public void addQuery(String collectionName, List<TextSegment> queries) {
        EmbeddingStore embeddingStore = embeddingStoreFactory.create(collectionName);
        for (TextSegment query : queries) {
            String question = query.text();
            Embedding embedding = embeddingModel.embed(question).content();
            embeddingStore.add(embedding, query);
        }
    }

    @Override
    public void deleteQuery(String collectionName, List<TextSegment> queries) {
    }

    @Override
    public List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery, int num) {
        List<RetrieveQueryResult> results = new ArrayList<>();

        EmbeddingStore embeddingStore = embeddingStoreFactory.create(collectionName);
        List<String> queryTextsList = retrieveQuery.getQueryTextsList();
        Map<String, String> filterCondition = retrieveQuery.getFilterCondition();
        for (String queryText : queryTextsList) {
            Embedding embeddedText = embeddingModel.embed(queryText).content();
            Filter filter = createCombinedFilter(filterCondition);
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embeddedText).filter(filter).maxResults(num).build();

            EmbeddingSearchResult result = embeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> relevant = result.matches();

            RetrieveQueryResult retrieveQueryResult = new RetrieveQueryResult();
            retrieveQueryResult.setQuery(queryText);
            List<Retrieval> retrievals = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> embeddingMatch : relevant) {
                Retrieval retrieval = new Retrieval();
                TextSegment embedded = embeddingMatch.embedded();
                retrieval.setDistance(1 - embeddingMatch.score());
                retrieval.setId(TextSegmentConvert.getQueryId(embedded));
                retrieval.setQuery(embedded.text());
                Map<String, Object> metadata = new HashMap<>();
                if (Objects.nonNull(embedded)
                        && MapUtils.isNotEmpty(embedded.metadata().toMap())) {
                    metadata.putAll(embedded.metadata().toMap());
                }
                retrieval.setMetadata(metadata);
                retrievals.add(retrieval);
            }
            retrievals = retrievals.stream()
                    .sorted(Comparator.comparingDouble(Retrieval::getDistance).reversed())
                    .limit(num)
                    .collect(Collectors.toList());
            retrieveQueryResult.setRetrieval(retrievals);
            results.add(retrieveQueryResult);
        }

        return results;
    }

    private static Filter createCombinedFilter(Map<String, String> map) {
        Filter result = null;
        if (MapUtils.isEmpty(map)) {
            return null;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            IsEqualTo isEqualTo = new IsEqualTo(entry.getKey(), entry.getValue());
            result = (result == null) ? isEqualTo : Filter.and(result, isEqualTo);
        }
        return result;
    }
}
