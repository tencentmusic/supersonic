package com.tencent.supersonic.common.util.embedding;

import static dev.langchain4j.internal.Utils.randomUUID;
import static java.util.Comparator.comparingDouble;

import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.spi.ServiceHelper;
import dev.langchain4j.spi.store.embedding.inmemory.InMemoryEmbeddingStoreJsonCodecFactory;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import dev.langchain4j.store.embedding.inmemory.GsonInMemoryEmbeddingStoreJsonCodec;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStoreJsonCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    /**
     * An {@link EmbeddingStore} that stores embeddings in memory.
     * <p>
     * Uses a brute force approach by iterating over all embeddings to find the best matches.
     * @param <Embedded> The class of the object that has been embedded.
     *         Typically, it is {@link dev.langchain4j.data.segment.TextSegment}.
     * copy from dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
     * and fix concurrentModificationException in a multi-threaded environment
     */
    public static class InMemoryEmbeddingStore<Embedded> implements EmbeddingStore<Embedded> {

        private static class Entry<Embedded> {

            String id;
            Embedding embedding;
            Embedded embedded;

            Entry(String id, Embedding embedding, Embedded embedded) {
                this.id = id;
                this.embedding = embedding;
                this.embedded = embedded;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Entry<?> that = (Entry<?>) o;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.embedding, that.embedding)
                        && Objects.equals(this.embedded, that.embedded);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, embedding, embedded);
            }
        }

        private final List<Entry<Embedded>> entries = new CopyOnWriteArrayList<>();

        @Override
        public String add(Embedding embedding) {
            String id = randomUUID();
            add(id, embedding);
            return id;
        }

        @Override
        public void add(String id, Embedding embedding) {
            add(id, embedding, null);
        }

        @Override
        public String add(Embedding embedding, Embedded embedded) {
            String id = randomUUID();
            add(id, embedding, embedded);
            return id;
        }

        public void add(String id, Embedding embedding, Embedded embedded) {
            entries.add(new Entry<>(id, embedding, embedded));
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            List<String> ids = new ArrayList<>();
            for (Embedding embedding : embeddings) {
                ids.add(add(embedding));
            }
            return ids;
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings, List<Embedded> embedded) {
            if (embeddings.size() != embedded.size()) {
                throw new IllegalArgumentException("The list of embeddings and embedded must have the same size");
            }

            List<String> ids = new ArrayList<>();
            for (int i = 0; i < embeddings.size(); i++) {
                ids.add(add(embeddings.get(i), embedded.get(i)));
            }
            return ids;
        }

        @Override
        public List<EmbeddingMatch<Embedded>> findRelevant(Embedding referenceEmbedding, int maxResults,
                double minScore) {

            Comparator<EmbeddingMatch<Embedded>> comparator = comparingDouble(EmbeddingMatch::score);
            PriorityQueue<EmbeddingMatch<Embedded>> matches = new PriorityQueue<>(comparator);

            for (Entry<Embedded> entry : entries) {
                double cosineSimilarity = CosineSimilarity.between(entry.embedding, referenceEmbedding);
                double score = RelevanceScore.fromCosineSimilarity(cosineSimilarity);
                if (score >= minScore) {
                    matches.add(new EmbeddingMatch<>(score, entry.id, entry.embedding, entry.embedded));
                    if (matches.size() > maxResults) {
                        matches.poll();
                    }
                }
            }

            List<EmbeddingMatch<Embedded>> result = new ArrayList<>(matches);
            result.sort(comparator);
            Collections.reverse(result);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InMemoryEmbeddingStore<?> that = (InMemoryEmbeddingStore<?>) o;
            return Objects.equals(this.entries, that.entries);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entries);
        }

        private static InMemoryEmbeddingStoreJsonCodec loadCodec() {
            Collection<InMemoryEmbeddingStoreJsonCodecFactory> factories = ServiceHelper.loadFactories(
                    InMemoryEmbeddingStoreJsonCodecFactory.class);
            for (InMemoryEmbeddingStoreJsonCodecFactory factory : factories) {
                return factory.create();
            }
            // fallback to default
            return new GsonInMemoryEmbeddingStoreJsonCodec();
        }

    }

}
