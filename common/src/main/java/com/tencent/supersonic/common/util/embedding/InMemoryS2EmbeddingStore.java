package com.tencent.supersonic.common.util.embedding;

import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/***
 * Implementation of S2EmbeddingStore within the Java process's in-memory.
 */
@Slf4j
public class InMemoryS2EmbeddingStore implements S2EmbeddingStore {

    private static Map<String, InMemoryEmbeddingStore<EmbeddingQuery>> collectionNameToStore =
            new ConcurrentHashMap<>();

    @Override
    public void addCollection(String collectionName) {
        collectionNameToStore.computeIfAbsent(collectionName, k -> new InMemoryEmbeddingStore());
    }

    @Override
    public void addQuery(String collectionName, List<EmbeddingQuery> queries) {
        InMemoryEmbeddingStore<EmbeddingQuery> embeddingStore = getEmbeddingStore(collectionName);
        EmbeddingModel embeddingModel = ContextUtils.getBean(EmbeddingModel.class);
        for (EmbeddingQuery query : queries) {
            String question = query.getQuery();
            Embedding embedding = embeddingModel.embed(question).content();
            embeddingStore.add(query.getQueryId(), embedding, query);
        }
    }

    private InMemoryEmbeddingStore<EmbeddingQuery> getEmbeddingStore(String collectionName) {
        InMemoryEmbeddingStore<EmbeddingQuery> embeddingStore = collectionNameToStore.get(collectionName);
        if (Objects.isNull(embeddingStore)) {
            synchronized (InMemoryS2EmbeddingStore.class) {
                addCollection(collectionName);
                embeddingStore = collectionNameToStore.get(collectionName);
            }

        }
        return embeddingStore;
    }

    @Override
    public void deleteQuery(String collectionName, List<EmbeddingQuery> queries) {
        //not support in InMemoryEmbeddingStore
    }

    @Override
    public List<RetrieveQueryResult> retrieveQuery(String collectionName, RetrieveQuery retrieveQuery, int num) {
        InMemoryEmbeddingStore<EmbeddingQuery> embeddingStore = getEmbeddingStore(collectionName);
        EmbeddingModel embeddingModel = ContextUtils.getBean(EmbeddingModel.class);

        List<RetrieveQueryResult> results = new ArrayList<>();

        List<String> queryTextsList = retrieveQuery.getQueryTextsList();
        for (String queryText : queryTextsList) {
            Embedding embeddedText = embeddingModel.embed(queryText).content();
            List<EmbeddingMatch<EmbeddingQuery>> relevant = embeddingStore.findRelevant(embeddedText, num);

            RetrieveQueryResult retrieveQueryResult = new RetrieveQueryResult();
            retrieveQueryResult.setQuery(queryText);
            List<Retrieval> retrievals = new ArrayList<>();
            for (EmbeddingMatch<EmbeddingQuery> embeddingMatch : relevant) {
                Retrieval retrieval = new Retrieval();
                retrieval.setDistance(embeddingMatch.score());
                retrieval.setId(embeddingMatch.embeddingId());
                retrieval.setQuery(embeddingMatch.embedded().getQuery());
                retrieval.setMetadata(embeddingMatch.embedded().getMetadata());
                retrievals.add(retrieval);
            }
            retrieveQueryResult.setRetrieval(retrievals);
            results.add(retrieveQueryResult);
        }

        return results;
    }
}
