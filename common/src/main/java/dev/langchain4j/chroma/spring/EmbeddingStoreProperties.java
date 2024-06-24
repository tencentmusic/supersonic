package dev.langchain4j.chroma.spring;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
class EmbeddingStoreProperties {

    private String baseUrl;
    private Duration timeout;
}