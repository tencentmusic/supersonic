package dev.langchain4j.dashscope.spring;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

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