package com.tencent.supersonic.common.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.common.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.provider.ModelProvider;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactoryProvider;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
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

    private Cache<String, Boolean> cache = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.HOURS).build();

    @Override
    public void addQuery(String collectionName, List<TextSegment> queries) {
        EmbeddingStore embeddingStore =
                EmbeddingStoreFactoryProvider.getFactory().create(collectionName);
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
                cache.put(TextSegmentConvert.getQueryId(query), true);
            } catch (Exception e) {
                log.error("embeddingModel embed error question: {}, embeddingStore: {}", question,
                        embeddingStore.getClass().getSimpleName(), e);
            }
        }
    }

    private boolean existSegment(EmbeddingStore embeddingStore, TextSegment query,
            Embedding embedding) {
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
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder().queryEmbedding(embedding)
                .filter(filter).minScore(1.0d).maxResults(1).build();

        EmbeddingSearchResult result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> relevant = result.matches();
        boolean exists = CollectionUtils.isNotEmpty(relevant);
        cache.put(queryId, exists);
        return exists;
    }

    @Override
    public void deleteQuery(String collectionName, List<TextSegment> queries) {
        EmbeddingStore embeddingStore =
                EmbeddingStoreFactoryProvider.getFactory().create(collectionName);
        try {

            List<String> queryIds =
                    queries.stream().map(textSegment -> TextSegmentConvert.getQueryId(textSegment))
                            .filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(queryIds)) {
                MetadataFilterBuilder filterBuilder =
                        new MetadataFilterBuilder(TextSegmentConvert.QUERY_ID);
                Filter filter = filterBuilder.isIn(queryIds);
                embeddingStore.removeAll(filter);
                queryIds.stream().forEach(queryId -> cache.put(queryId, false));
            }

        } catch (Exception e) {
            log.error("deleteQuery error,collectionName:{},queries:{}", collectionName, queries);
        }
    }

    @Override
    public List<RetrieveQueryResult> retrieveQuery(String collectionName,
            RetrieveQuery retrieveQuery, int num) {
        EmbeddingStore embeddingStore =
                EmbeddingStoreFactoryProvider.getFactory().create(collectionName);
        EmbeddingModel embeddingModel = ModelProvider.getEmbeddingModel();
        Map<String, Object> filterCondition = retrieveQuery.getFilterCondition();
        return retrieveQuery
                .getQueryTextsList().stream().map(queryText -> retrieveSingleQuery(queryText,
                        embeddingModel, embeddingStore, filterCondition, num))
                .collect(Collectors.toList());
    }

    @Override
    public void removeAll() {
        BaseEmbeddingStoreFactory factory =
                (BaseEmbeddingStoreFactory) EmbeddingStoreFactoryProvider.getFactory();
        Map<String, EmbeddingStore<TextSegment>> collectionNameToStore =
                factory.getCollectionNameToStore();
        for (EmbeddingStore<TextSegment> embeddingStore : collectionNameToStore.values()) {
            embeddingStore.removeAll();
        }
        cache.invalidateAll();
    }

    private RetrieveQueryResult retrieveSingleQuery(String queryText, EmbeddingModel embeddingModel,
            EmbeddingStore embeddingStore, Map<String, Object> filterCondition, int num) {
        Embedding embeddedText = embeddingModel.embed(queryText).content();
        Filter filter = createCombinedFilter(filterCondition);
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddedText).filter(filter).maxResults(num).build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        List<Retrieval> retrievals = result.matches().stream().map(this::convertToRetrieval)
                .sorted(Comparator.comparingDouble(Retrieval::getSimilarity)).limit(num)
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

    public static Filter createCombinedFilter(Map<String, Object> criteriaMap) {
        if (MapUtils.isEmpty(criteriaMap)) {
            return null;
        }
        Filter combinedFilter = null;
        for (Map.Entry<String, Object> entry : criteriaMap.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            Filter fieldFilter = null;
            if (fieldValue instanceof List) {
                // Create an OR filter for each value in the list
                for (String value : (List<String>) fieldValue) {
                    IsEqualTo equalToFilter = new IsEqualTo(fieldName, value);
                    fieldFilter = (fieldFilter == null) ? equalToFilter
                            : Filter.or(fieldFilter, equalToFilter);
                }
            } else if (fieldValue instanceof String) {
                // Create a simple equality filter
                fieldFilter = new IsEqualTo(fieldName, fieldValue);
            }
            // Combine the current field filter with the overall filter using AND logic
            if (fieldFilter != null) {
                combinedFilter = (combinedFilter == null) ? fieldFilter
                        : Filter.and(combinedFilter, fieldFilter);
            }
        }
        return combinedFilter;
    }
}
