package dev.langchain4j.store.embedding.qdrant;

import com.google.common.util.concurrent.ListenableFuture;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import dev.langchain4j.store.embedding.filter.Filter;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorOutputHelper;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.DeletePoints;
import io.qdrant.client.grpc.Points.DenseVector;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PointsSelector;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.internal.Utils.randomUUID;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class QdrantEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final String TEXT_FIELD = "text_segment";
    private static final int DEFAULT_PORT = 6334;

    private final QdrantClient client;
    private final String collectionName;

    public QdrantEmbeddingStore(String collectionName, String host, Integer port, boolean useTls,
            String apiKey, Integer dimension) {
        QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(
                host == null ? "localhost" : host, port == null ? DEFAULT_PORT : port, useTls);
        if (apiKey != null) {
            grpcBuilder.withApiKey(apiKey);
        }
        this.client = new QdrantClient(grpcBuilder.build());
        this.collectionName = collectionName;
        ensureCollectionExists(dimension);
    }

    @Override
    public String add(Embedding embedding) {
        String id = randomUUID();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = randomUUID();
        addInternal(id, embedding, segment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = embeddings.stream().map(e -> randomUUID()).collect(toList());
        addAllInternal(ids, embeddings, null);
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = embeddings.stream().map(e -> randomUUID()).collect(toList());
        addAllInternal(ids, embeddings, segments);
        return ids;
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        SearchPoints.Builder search = SearchPoints.newBuilder().setCollectionName(collectionName)
                .addAllVector(request.queryEmbedding().vectorAsList())
                .setWithVectors(WithVectorsSelectorFactory.enable(true))
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .setLimit(request.maxResults());
        if (request.filter() != null) {
            search.setFilter(QdrantFilterConverter.convert(request.filter()));
        }
        List<EmbeddingMatch<TextSegment>> matches = await(client.searchAsync(search.build()))
                .stream().map(point -> toEmbeddingMatch(point, request.queryEmbedding()))
                .filter(match -> match.score() >= request.minScore())
                .sorted(comparingDouble(EmbeddingMatch::score)).collect(toList());
        Collections.reverse(matches);
        return new EmbeddingSearchResult<>(matches);
    }

    @Override
    public void removeAll(Filter filter) {
        await(client.deleteAsync(collectionName, QdrantFilterConverter.convert(filter)));
    }

    @Override
    public void removeAll() {
        io.qdrant.client.grpc.Common.Filter emptyFilter =
                io.qdrant.client.grpc.Common.Filter.newBuilder().build();
        await(client.deleteAsync(DeletePoints.newBuilder().setCollectionName(collectionName)
                .setPoints(PointsSelector.newBuilder().setFilter(emptyFilter).build()).build()));
    }

    private void addInternal(String id, Embedding embedding, TextSegment segment) {
        addAllInternal(Collections.singletonList(id), Collections.singletonList(embedding),
                segment == null ? null : Collections.singletonList(segment));
    }

    private void addAllInternal(List<String> ids, List<Embedding> embeddings,
            List<TextSegment> segments) {
        List<PointStruct> points = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            PointStruct.Builder point =
                    PointStruct.newBuilder().setId(PointIdFactory.id(UUID.fromString(ids.get(i))))
                            .setVectors(VectorsFactory.vectors(embeddings.get(i).vector()));
            if (segments != null) {
                point.putAllPayload(toPayload(segments.get(i)));
            }
            points.add(point.build());
        }
        await(client.upsertAsync(collectionName, points));
    }

    private Map<String, Value> toPayload(TextSegment segment) {
        Map<String, Value> payload = new HashMap<>();
        segment.metadata().toMap().forEach((key, value) -> payload.put(key, toValue(value)));
        payload.put(TEXT_FIELD, ValueFactory.value(segment.text()));
        return payload;
    }

    private static Value toValue(Object value) {
        if (value == null) {
            return ValueFactory.nullValue();
        }
        if (value instanceof Float || value instanceof Double) {
            return ValueFactory.value(((Number) value).doubleValue());
        }
        if (value instanceof Number) {
            return ValueFactory.value(((Number) value).longValue());
        }
        return ValueFactory.value(value.toString());
    }

    private EmbeddingMatch<TextSegment> toEmbeddingMatch(ScoredPoint point,
            Embedding referenceEmbedding) {
        Map<String, Value> payload = point.getPayloadMap();
        Value text = payload.get(TEXT_FIELD);
        Map<String, Object> metadata =
                payload.entrySet().stream().filter(entry -> !entry.getKey().equals(TEXT_FIELD))
                        .collect(toMap(Map.Entry::getKey, entry -> toObject(entry.getValue())));
        Embedding embedding = Embedding.from(extractVectorData(point));
        double score = RelevanceScore
                .fromCosineSimilarity(CosineSimilarity.between(embedding, referenceEmbedding));
        return new EmbeddingMatch<>(score, point.getId().getUuid(), embedding, text == null ? null
                : TextSegment.from(text.getStringValue(), new Metadata(metadata)));
    }

    private static Object toObject(Value value) {
        switch (value.getKindCase()) {
            case INTEGER_VALUE:
                return value.getIntegerValue();
            case DOUBLE_VALUE:
                return value.getDoubleValue();
            case NULL_VALUE:
                return null;
            default:
                return value.getStringValue();
        }
    }

    private List<Float> extractVectorData(ScoredPoint point) {
        DenseVector dense = VectorOutputHelper.getDenseVector(point.getVectors().getVector());
        return dense == null ? Collections.emptyList() : dense.getDataList();
    }

    private void ensureCollectionExists(Integer dimension) {
        if (dimension == null) {
            return;
        }
        try {
            if (!client.collectionExistsAsync(collectionName).get()) {
                client.createCollectionAsync(collectionName, VectorParams.newBuilder()
                        .setSize(dimension).setDistance(Distance.Cosine).build()).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T await(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
