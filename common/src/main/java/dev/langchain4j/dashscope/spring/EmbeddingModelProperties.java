package dev.langchain4j.dashscope.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingModelProperties {

    private String apiKey;
    private String modelName;
}