package dev.langchain4j.dashscope.spring;

import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenLanguageModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingLanguageModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.dashscope.spring.Properties.PREFIX;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class DashscopeAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".chat-model.api-key")
    QwenChatModel qwenChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getChatModel();
        return QwenChatModel.builder().baseUrl(chatModelProperties.getBaseUrl())
                .apiKey(chatModelProperties.getApiKey())
                .modelName(chatModelProperties.getModelName()).topP(chatModelProperties.getTopP())
                .topK(chatModelProperties.getTopK())
                .enableSearch(chatModelProperties.getEnableSearch())
                .seed(chatModelProperties.getSeed())
                .repetitionPenalty(chatModelProperties.getRepetitionPenalty())
                .temperature(chatModelProperties.getTemperature())
                .stops(chatModelProperties.getStops()).maxTokens(chatModelProperties.getMaxTokens())
                .build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".streaming-chat-model.api-key")
    QwenStreamingChatModel qwenStreamingChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getStreamingChatModel();
        return QwenStreamingChatModel.builder().baseUrl(chatModelProperties.getBaseUrl())
                .apiKey(chatModelProperties.getApiKey())
                .modelName(chatModelProperties.getModelName()).topP(chatModelProperties.getTopP())
                .topK(chatModelProperties.getTopK())
                .enableSearch(chatModelProperties.getEnableSearch())
                .seed(chatModelProperties.getSeed())
                .repetitionPenalty(chatModelProperties.getRepetitionPenalty())
                .temperature(chatModelProperties.getTemperature())
                .stops(chatModelProperties.getStops()).maxTokens(chatModelProperties.getMaxTokens())
                .build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".language-model.api-key")
    QwenLanguageModel qwenLanguageModel(Properties properties) {
        ChatModelProperties languageModel = properties.getLanguageModel();
        return QwenLanguageModel.builder().baseUrl(languageModel.getBaseUrl())
                .apiKey(languageModel.getApiKey()).modelName(languageModel.getModelName())
                .topP(languageModel.getTopP()).topK(languageModel.getTopK())
                .enableSearch(languageModel.getEnableSearch()).seed(languageModel.getSeed())
                .repetitionPenalty(languageModel.getRepetitionPenalty())
                .temperature(languageModel.getTemperature()).stops(languageModel.getStops())
                .maxTokens(languageModel.getMaxTokens()).build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".streaming-language-model.api-key")
    QwenStreamingLanguageModel qwenStreamingLanguageModel(Properties properties) {
        ChatModelProperties languageModel = properties.getStreamingLanguageModel();
        return QwenStreamingLanguageModel.builder().baseUrl(languageModel.getBaseUrl())
                .apiKey(languageModel.getApiKey()).modelName(languageModel.getModelName())
                .topP(languageModel.getTopP()).topK(languageModel.getTopK())
                .enableSearch(languageModel.getEnableSearch()).seed(languageModel.getSeed())
                .repetitionPenalty(languageModel.getRepetitionPenalty())
                .temperature(languageModel.getTemperature()).stops(languageModel.getStops())
                .maxTokens(languageModel.getMaxTokens()).build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-model.api-key")
    QwenEmbeddingModel qwenEmbeddingModel(Properties properties) {
        EmbeddingModelProperties embeddingModelProperties = properties.getEmbeddingModel();
        return QwenEmbeddingModel.builder().apiKey(embeddingModelProperties.getApiKey())
                .modelName(embeddingModelProperties.getModelName()).build();
    }
}
