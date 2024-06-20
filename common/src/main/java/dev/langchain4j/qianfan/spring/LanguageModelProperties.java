package dev.langchain4j.qianfan.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class LanguageModelProperties {
    private String baseUrl;
    private String apiKey;
    private String secretKey;
    private Double temperature;
    private Integer maxRetries;
    private Integer topK;
    private Double topP;
    private String modelName;
    private String endpoint;
    private Double penaltyScore;
    private Boolean logRequests;
    private Boolean logResponses;

}