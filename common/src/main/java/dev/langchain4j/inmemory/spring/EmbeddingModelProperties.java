package dev.langchain4j.inmemory.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingModelProperties {

    private String modelName;
    private String modelPath;
    private String vocabularyPath;
}
