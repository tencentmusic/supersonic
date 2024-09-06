package dev.langchain4j.localai.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class ChatModelProperties {
    private String baseUrl;
    private String apiKey;
    private String secretKey;
    private Double temperature;
    private Integer maxRetries;
    private Double topP;
    private String modelName;
    private String endpoint;
    private String responseFormat;
    private Double penaltyScore;
    private Boolean logRequests;
    private Boolean logResponses;
}
