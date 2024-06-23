package com.tencent.supersonic.common.service;

import dev.langchain4j.store.embedding.EmbeddingQuery;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import java.util.List;

/**
 * Supersonic EmbeddingStore
 * Enhanced the functionality by enabling the addition and querying of collection names.
 */
public interface EmbeddingService {

    void addCollection(String collectionName);

    void addQuery(String collectionName, List<EmbeddingQuery> queries);

    void deleteQuery(String collectionName, List<EmbeddingQuery> queries);

    List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery, int num);

}
