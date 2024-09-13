package dev.langchain4j.zhipu.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingModelProperties {

    String baseUrl;
    String apiKey;
    String model;
    Integer maxRetries;
    Boolean logRequests;
    Boolean logResponses;
}
