package dev.langchain4j.chroma.spring;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingStoreProperties {

    private String baseUrl;
    private String collectionName;
    private Duration timeout;
}