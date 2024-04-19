package com.tencent.supersonic.common.util.embedding;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.randomUUID;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Comparator.comparingDouble;

/***
 * Implementation of S2EmbeddingStore within the Java process's in-memory.
 */
@Slf4j
public class InMemoryS2EmbeddingStore implements S2EmbeddingStore {

    public static final String PERSISTENT_FILE_PRE = "InMemory.";
    private static Map<String, InMemoryEmbeddingStore<EmbeddingQuery>> collectionNameToStore =
            new ConcurrentHashMap<>();

    @Override
    public synchronized void addCollection(String collectionName) {
        InMemoryEmbeddingStore<EmbeddingQuery> embeddingStore = null;
        Path filePath = getPersistentPath(collectionName);
        try {
            EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
            if (Files.exists(filePath) && !collectionName.equals(embeddingConfig.getMetaCollectionName())
                    && !collectionName.equals(embeddingConfig.getText2sqlCollectionName())) {
                embeddingStore = InMemoryEmbeddingStore.fromFile(filePath);
                embeddingStore.entries = new CopyOnWriteArraySet<>(embeddingStore.entries);
                log.info("embeddingStore reload from file:{}", filePath);
            }
        } catch (Exception e) {
            log.error("load persistentFile error, persistentFile:" + filePath, e);
        }
        if (Objects.isNull(embeddingStore)) {
            embeddingStore = new InMemoryEmbeddingStore();
        }
        collectionNameToStore.putIfAbsent(collectionName, embeddingStore);
    }

    private Path getPersistentPath(String collectionName) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        String persistentFile = PERSISTENT_FILE_PRE + collectionName;
        return Paths.get(embeddingConfig.getEmbeddingStorePersistentPath(), persistentFile);
    }

    public void persistentToFile() {
        for (Entry<String, InMemoryEmbeddingStore<EmbeddingQuery>> entry : collectionNameToStore.entrySet()) {
            Path filePath = getPersistentPath(entry.getKey());
            try {
                Path directoryPath = filePath.getParent();
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                }
                entry.getValue().serializeToFile(filePath);
            } catch (Exception e) {
                log.error("persistentToFile error, persistentFile:" + filePath, e);
            }
        }
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
        Map<String, String> filterCondition = retrieveQuery.getFilterCondition();
        for (String queryText : queryTextsList) {
            Embedding embeddedText = embeddingModel.embed(queryText).content();
            int maxResults = getMaxResults(num, filterCondition);
            List<EmbeddingMatch<EmbeddingQuery>> relevant = embeddingStore.findRelevant(embeddedText, maxResults);

            RetrieveQueryResult retrieveQueryResult = new RetrieveQueryResult();
            retrieveQueryResult.setQuery(queryText);
            List<Retrieval> retrievals = new ArrayList<>();
            for (EmbeddingMatch<EmbeddingQuery> embeddingMatch : relevant) {
                Retrieval retrieval = new Retrieval();
                retrieval.setDistance(1 - embeddingMatch.score());
                retrieval.setId(embeddingMatch.embeddingId());
                retrieval.setQuery(embeddingMatch.embedded().getQuery());
                Map<String, Object> metadata = new HashMap<>();
                if (Objects.nonNull(embeddingMatch.embedded())
                        && MapUtils.isNotEmpty(embeddingMatch.embedded().getMetadata())) {
                    metadata.putAll(embeddingMatch.embedded().getMetadata());
                }
                if (filterRetrieval(filterCondition, metadata)) {
                    continue;
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

    private int getMaxResults(int num, Map<String, String> filterCondition) {
        int maxResults = num;
        if (MapUtils.isNotEmpty(filterCondition)) {
            maxResults = num * 5;
        }
        return maxResults;
    }

    private boolean filterRetrieval(Map<String, String> filterCondition, Map<String, Object> metadata) {
        if (MapUtils.isNotEmpty(metadata) && MapUtils.isNotEmpty(filterCondition)) {
            for (Entry<String, Object> entry : metadata.entrySet()) {
                String filterValue = filterCondition.get(entry.getKey());
                if (StringUtils.isNotBlank(filterValue) && !filterValue.equalsIgnoreCase(
                        entry.getValue().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * An {@link EmbeddingStore} that stores embeddings in memory.
     * <p>
     * Uses a brute force approach by iterating over all embeddings to find the best matches.
     *
     * @param <Embedded> The class of the object that has been embedded.
     *                   Typically, it is {@link dev.langchain4j.data.segment.TextSegment}.
     *                   copy from dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
     *                   and fix concurrentModificationException in a multi-threaded environment
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
                InMemoryEmbeddingStore.Entry<?> that = (InMemoryEmbeddingStore.Entry<?>) o;
                return Objects.equals(this.id, that.id)
                        && Objects.equals(this.embedding, that.embedding)
                        && Objects.equals(this.embedded, that.embedded);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, embedding, embedded);
            }
        }

        private static final InMemoryEmbeddingStoreJsonCodec CODEC = loadCodec();
        private Set<Entry<Embedded>> entries = new CopyOnWriteArraySet<>();

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
            entries.add(new InMemoryEmbeddingStore.Entry<>(id, embedding, embedded));
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

            for (InMemoryEmbeddingStore.Entry<Embedded> entry : entries) {
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

        public String serializeToJson() {
            return CODEC.toJson(this);
        }

        public void serializeToFile(Path filePath) {
            try {
                String json = serializeToJson();
                Files.write(filePath, json.getBytes(), CREATE, TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void serializeToFile(String filePath) {
            serializeToFile(Paths.get(filePath));
        }

        private static InMemoryEmbeddingStoreJsonCodec loadCodec() {
            // fallback to default
            return new GsonInMemoryEmbeddingStoreJsonCodec();
        }

        public static InMemoryEmbeddingStore<EmbeddingQuery> fromJson(String json) {
            return CODEC.fromJson(json);
        }

        public static InMemoryEmbeddingStore<EmbeddingQuery> fromFile(Path filePath) {
            try {
                String json = new String(Files.readAllBytes(filePath));
                return fromJson(json);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static InMemoryEmbeddingStore<EmbeddingQuery> fromFile(String filePath) {
            return fromFile(Paths.get(filePath));
        }
    }

}
