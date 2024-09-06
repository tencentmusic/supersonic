package dev.langchain4j.dashscope.spring;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
class ChatModelProperties {

    String baseUrl;
    String apiKey;
    String modelName;
    Double topP;
    Integer topK;
    Boolean enableSearch;
    Integer seed;
    Float repetitionPenalty;
    Float temperature;
    List<String> stops;
    Integer maxTokens;
}
