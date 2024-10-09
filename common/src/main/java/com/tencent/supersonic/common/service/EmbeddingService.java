package com.tencent.supersonic.common.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;

import java.util.List;

/**
 * Supersonic EmbeddingStore Enhanced the functionality by enabling the addition and querying of
 * collection names.
 */
public interface EmbeddingService {

    void addQuery(String collectionName, List<TextSegment> queries);

    void deleteQuery(String collectionName, List<TextSegment> queries);

    List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery,
            int num);

    void removeAll();
}
