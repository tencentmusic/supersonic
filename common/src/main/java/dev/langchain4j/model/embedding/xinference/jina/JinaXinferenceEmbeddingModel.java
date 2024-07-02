package dev.langchain4j.model.embedding.xinference.jina;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;

import java.time.Duration;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static java.util.stream.Collectors.toList;
public class JinaXinferenceEmbeddingModel implements EmbeddingModel {
    private final JinaClient client;
    private final String model;
    private final Integer maxRetries;

    @Builder
    public JinaXinferenceEmbeddingModel(String baseUrl,
                              String model,
                              Duration timeout,
                              Integer maxRetries) {
        this.client = JinaClient.builder()
                .baseUrl(baseUrl)
                .timeout(getOrDefault(timeout, ofSeconds(60)))
                .build();
        this.model = model;
        this.maxRetries = getOrDefault(maxRetries, 3);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(model)
                .input(textSegments.stream().map(TextSegment::text).collect(toList()))
                .build();

        EmbeddingResponse response = withRetry(() -> client.embed(request), maxRetries);

        List<Embedding> embeddings = response.getData().stream()
                .map(JinaEmbedding::toEmbedding).collect(toList());

        TokenUsage tokenUsage = new TokenUsage(response.getUsage().getPromptTokens(),0 );
        return Response.from(embeddings,tokenUsage);
    }


}

