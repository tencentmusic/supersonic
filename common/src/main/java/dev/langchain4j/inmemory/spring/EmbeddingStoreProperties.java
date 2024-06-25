package dev.langchain4j.inmemory.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingStoreProperties {

    private String filePath;
}