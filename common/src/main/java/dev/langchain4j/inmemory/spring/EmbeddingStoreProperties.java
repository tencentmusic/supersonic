package dev.langchain4j.inmemory.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbeddingStoreProperties {

    private String persistPath;
}