package com.tencent.supersonic.common.service.impl;

import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingQuery;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Autowired
    private EmbeddingStoreFactory embeddingStoreFactory;

    public synchronized void addCollection(String collectionName) {
        embeddingStoreFactory.create(collectionName);
    }

    @Override
    public void addQuery(String collectionName, List<EmbeddingQuery> queries) {
        EmbeddingStore embeddingStore = embeddingStoreFactory.create(collectionName);
        EmbeddingModel embeddingModel = getEmbeddingModel();
        for (EmbeddingQuery query : queries) {
            String question = query.getQuery();
            Embedding embedding = embeddingModel.embed(question).content();
            embeddingStore.add(embedding, query);
        }
    }

    private static EmbeddingModel getEmbeddingModel() {
        EmbeddingModel embeddingModel;
        try {
            embeddingModel = ContextUtils.getBean(EmbeddingModel.class);
        } catch (NoSuchBeanDefinitionException e) {
            embeddingModel = new BgeSmallZhEmbeddingModel();
        }
        return embeddingModel;
    }

    @Override
    public void deleteQuery(String collectionName, List<EmbeddingQuery> queries) {
    }

    @Override
    public List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery, int num) {
        EmbeddingStore embeddingStore = embeddingStoreFactory.create(collectionName);
        EmbeddingModel embeddingModel = getEmbeddingModel();
        List<RetrieveQueryResult> results = new ArrayList<>();

        List<String> queryTextsList = retrieveQuery.getQueryTextsList();
        Map<String, String> filterCondition = retrieveQuery.getFilterCondition();
        for (String queryText : queryTextsList) {
            Embedding embeddedText = embeddingModel.embed(queryText).content();
            Filter filter = createCombinedFilter(filterCondition);
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embeddedText).filter(filter).maxResults(num).build();

            EmbeddingSearchResult result = embeddingStore.search(request);
            List<EmbeddingMatch<EmbeddingQuery>> relevant = result.matches();

            RetrieveQueryResult retrieveQueryResult = new RetrieveQueryResult();
            retrieveQueryResult.setQuery(queryText);
            List<Retrieval> retrievals = new ArrayList<>();
            for (EmbeddingMatch<EmbeddingQuery> embeddingMatch : relevant) {
                Retrieval retrieval = new Retrieval();
                EmbeddingQuery embedded = embeddingMatch.embedded();
                retrieval.setDistance(1 - embeddingMatch.score());
                retrieval.setId(embedded.getQueryId());
                retrieval.setQuery(embedded.getQuery());
                Map<String, Object> metadata = new HashMap<>();
                if (Objects.nonNull(embedded)
                        && MapUtils.isNotEmpty(embedded.getMetadata())) {
                    metadata.putAll(embedded.getMetadata());
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
        if (Objects.isNull(map)) {
            return null;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            IsEqualTo isEqualTo = new IsEqualTo(entry.getKey(), entry.getValue());
            result = (result == null) ? isEqualTo : Filter.and(result, isEqualTo);
        }
        return result;
    }
}
