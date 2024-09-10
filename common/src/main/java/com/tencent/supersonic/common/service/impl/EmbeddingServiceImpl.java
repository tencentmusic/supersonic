package com.tencent.supersonic.common.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.common.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStoreFactoryProvider;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private Cache<String, Boolean> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(10, TimeUnit.HOURS)
                    .build();

    @Override
    public void addQuery(String collectionName, List<TextSegment> queries) {
        EmbeddingStoreFactory embeddingStoreFactory = EmbeddingStoreFactoryProvider.getFactory();
        EmbeddingStore embeddingStore = embeddingStoreFactory.create(collectionName);

        for (TextSegment query : queries) {
            String question = query.text();
            try {
                EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel();
                Embedding embedding = embeddingModel.embed(question).content();
                boolean existSegment = existSegment(embeddingStore, query, embedding);
                if (existSegment) {
                    continue;
                }
                embeddingStore.add(embedding, query);
            } catch (Exception e) {
                log.error(
                        "embeddingModel embed error question: {}, embeddingStore: {}",
                        question,
                        embeddingStore.getClass().getSimpleName(),
                        e);
            }
        }
    }

    private boolean existSegment(
            EmbeddingStore embeddingStore, TextSegment query, Embedding embedding) {
        String queryId = TextSegmentConvert.getQueryId(query);
        if (queryId == null) {
            return false;
        }
        // Check cache first
        Boolean cachedResult = cache.getIfPresent(queryId);
        if (cachedResult != null) {
            return cachedResult;
        }
        Map<String, Object> filterCondition = new HashMap<>();
        filterCondition.put(TextSegmentConvert.QUERY_ID, queryId);
        Filter filter = createCombinedFilter(filterCondition);
        EmbeddingSearchRequest request =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(embedding)
                        .filter(filter)
                        .minScore(1.0d)
                        .maxResults(1)
                        .build();

        EmbeddingSearchResult result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> relevant = result.matches();
        boolean exists = CollectionUtils.isNotEmpty(relevant);
        cache.put(queryId, exists);
        return exists;
    }

    @Override
    public void deleteQuery(String collectionName, List<TextSegment> queries) {
        // Not supported yet in Milvus and Chroma
        EmbeddingStoreFactory embeddingStoreFactory = EmbeddingStoreFactoryProvider.getFactory();
        EmbeddingStore embeddingStore = embeddingStoreFactory.create(collectionName);
        try {
            if (embeddingStore instanceof InMemoryEmbeddingStore) {
                InMemoryEmbeddingStore inMemoryEmbeddingStore =
                        (InMemoryEmbeddingStore) embeddingStore;
                List<String> queryIds =
                        queries.stream()
                                .map(textSegment -> TextSegmentConvert.getQueryId(textSegment))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(queryIds)) {
                    MetadataFilterBuilder filterBuilder =
                            new MetadataFilterBuilder(TextSegmentConvert.QUERY_ID);
                    Filter filter = filterBuilder.isIn(queryIds);
                    inMemoryEmbeddingStore.removeAll(filter);
                }
            } else {
                throw new RuntimeException("Not supported yet.");
            }
        } catch (Exception e) {
            log.error("deleteQuery error,collectionName:{},queries:{}", collectionName, queries);
        }
    }

    @Override
    public List<RetrieveQueryResult> retrieveQuery(
            String collectionName, RetrieveQuery retrieveQuery, int num) {
        EmbeddingStore embeddingStore =
                EmbeddingStoreFactoryProvider.getFactory().create(collectionName);
        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel();
        Map<String, Object> filterCondition = retrieveQuery.getFilterCondition();
        return retrieveQuery.getQueryTextsList().stream()
                .map(
                        queryText ->
                                retrieveSingleQuery(
                                        queryText,
                                        embeddingModel,
                                        embeddingStore,
                                        filterCondition,
                                        num))
                .collect(Collectors.toList());
    }

    private RetrieveQueryResult retrieveSingleQuery(
            String queryText,
            EmbeddingModel embeddingModel,
            EmbeddingStore embeddingStore,
            Map<String, Object> filterCondition,
            int num) {
        Embedding embeddedText = embeddingModel.embed(queryText).content();
        Filter filter = createCombinedFilter(filterCondition);
        EmbeddingSearchRequest request =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(embeddedText)
                        .filter(filter)
                        .maxResults(num)
                        .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        List<Retrieval> retrievals =
                result.matches().stream()
                        .map(this::convertToRetrieval)
                        .sorted(Comparator.comparingDouble(Retrieval::getSimilarity))
                        .limit(num)
                        .collect(Collectors.toList());

        RetrieveQueryResult retrieveQueryResult = new RetrieveQueryResult();
        retrieveQueryResult.setQuery(queryText);
        retrieveQueryResult.setRetrieval(retrievals);
        return retrieveQueryResult;
    }

    private Retrieval convertToRetrieval(EmbeddingMatch<TextSegment> embeddingMatch) {
        Retrieval retrieval = new Retrieval();
        TextSegment embedded = embeddingMatch.embedded();
        retrieval.setSimilarity(embeddingMatch.score());
        retrieval.setId(TextSegmentConvert.getQueryId(embedded));
        retrieval.setQuery(embedded.text());

        Map<String, Object> metadata = new HashMap<>();
        if (Objects.nonNull(embedded) && MapUtils.isNotEmpty(embedded.metadata().toMap())) {
            metadata.putAll(embedded.metadata().toMap());
        }
        retrieval.setMetadata(metadata);
        return retrieval;
    }

    public static Filter createCombinedFilter(Map<String, Object> map) {
        if (MapUtils.isEmpty(map)) {
            return null;
        }
        Filter result = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Filter orFilter = null;

            if (value instanceof List) {
                for (String val : (List<String>) value) {
                    IsEqualTo isEqualTo = new IsEqualTo(key, val);
                    orFilter = (orFilter == null) ? isEqualTo : Filter.or(orFilter, isEqualTo);
                }
            } else if (value instanceof String) {
                orFilter = new IsEqualTo(key, value);
            }
            if (orFilter != null) {
                result = (result == null) ? orFilter : Filter.and(result, orFilter);
            }
        }
        return result;
    }
}
