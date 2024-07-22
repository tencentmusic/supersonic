package dev.langchain4j.model.embedding.xinference.jina;

import lombok.Builder;

import java.util.List;
@Builder
public class EmbeddingRequest {
    String model;
    List<String> input;
}