package dev.langchain4j.chroma.spring;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;

@Getter
@Setter
@ToString
public class EmbeddingStoreProperties {

    private String baseUrl;
    private Duration timeout;
}
