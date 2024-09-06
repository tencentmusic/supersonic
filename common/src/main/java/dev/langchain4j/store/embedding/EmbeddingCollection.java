package dev.langchain4j.store.embedding;

import lombok.Data;

import java.util.Map;

@Data
public class EmbeddingCollection {

    private String id;

    private String name;

    private Map<String, String> metaData;
}
