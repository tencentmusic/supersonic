package dev.langchain4j.zhipu.spring;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class ChatModelProperties {

    String baseUrl;
    String apiKey;
    Double temperature;
    Double topP;
    String modelName;
    Integer maxRetries;
    Integer maxToken;
    Boolean logRequests;
    Boolean logResponses;
}