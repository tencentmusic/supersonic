package dev.langchain4j.localai.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class EmbeddingModelProperties {
    private String baseUrl;
    private String apiKey;
    private String secretKey;
    private Integer maxRetries;
    private String modelName;
    private String endpoint;
    private String user;
    private Boolean logRequests;
    private Boolean logResponses;

}