package dev.langchain4j.model.embedding.xinference.jina;

import lombok.Data;

import java.util.List;
@Data
public class EmbeddingResponse {
    Usage usage;
    List<JinaEmbedding> data;
}