package dev.langchain4j.store.embedding;

import java.util.List;

/**
 * Supersonic EmbeddingStore
 * Enhanced the functionality by enabling the addition and querying of collection names.
 */
public interface S2EmbeddingStore {

    void addCollection(String collectionName);

    void addQuery(String collectionName, List<EmbeddingQuery> queries);

    void deleteQuery(String collectionName, List<EmbeddingQuery> queries);

    List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery, int num);

}
