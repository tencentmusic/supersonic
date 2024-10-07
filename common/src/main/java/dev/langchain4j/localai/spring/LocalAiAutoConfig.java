package dev.langchain4j.localai.spring;

import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiLanguageModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingLanguageModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.localai.spring.Properties.PREFIX;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class LocalAiAutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".chat-model.base-url")
    LocalAiChatModel localAiChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getChatModel();
        return LocalAiChatModel.builder().baseUrl(chatModelProperties.getBaseUrl())
                .modelName(chatModelProperties.getModelName())
                .temperature(chatModelProperties.getTemperature())
                .topP(chatModelProperties.getTopP()).maxRetries(chatModelProperties.getMaxRetries())
                .logRequests(chatModelProperties.getLogRequests())
                .logResponses(chatModelProperties.getLogResponses()).build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".streaming-chat-model.base-url")
    LocalAiStreamingChatModel localAiStreamingChatModel(Properties properties) {
        ChatModelProperties chatModelProperties = properties.getStreamingChatModel();
        return LocalAiStreamingChatModel.builder().temperature(chatModelProperties.getTemperature())
                .topP(chatModelProperties.getTopP()).baseUrl(chatModelProperties.getBaseUrl())
                .modelName(chatModelProperties.getModelName())
                .logRequests(chatModelProperties.getLogRequests())
                .logResponses(chatModelProperties.getLogResponses()).build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".language-model.base-url")
    LocalAiLanguageModel localAiLanguageModel(Properties properties) {
        LanguageModelProperties languageModelProperties = properties.getLanguageModel();
        return LocalAiLanguageModel.builder().topP(languageModelProperties.getTopP())
                .baseUrl(languageModelProperties.getBaseUrl())
                .modelName(languageModelProperties.getModelName())
                .temperature(languageModelProperties.getTemperature())
                .maxRetries(languageModelProperties.getMaxRetries())
                .logRequests(languageModelProperties.getLogRequests())
                .logResponses(languageModelProperties.getLogResponses()).build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".streaming-language-model.base-url")
    LocalAiStreamingLanguageModel localAiStreamingLanguageModel(Properties properties) {
        LanguageModelProperties languageModelProperties = properties.getStreamingLanguageModel();
        return LocalAiStreamingLanguageModel.builder().topP(languageModelProperties.getTopP())
                .baseUrl(languageModelProperties.getBaseUrl())
                .modelName(languageModelProperties.getModelName())
                .temperature(languageModelProperties.getTemperature())
                .logRequests(languageModelProperties.getLogRequests())
                .logResponses(languageModelProperties.getLogResponses()).build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-model.base-url")
    LocalAiEmbeddingModel localAiEmbeddingModel(Properties properties) {
        EmbeddingModelProperties embeddingModelProperties = properties.getEmbeddingModel();
        return LocalAiEmbeddingModel.builder().baseUrl(embeddingModelProperties.getBaseUrl())
                .modelName(embeddingModelProperties.getModelName())
                .maxRetries(embeddingModelProperties.getMaxRetries())
                .logRequests(embeddingModelProperties.getLogRequests())
                .logResponses(embeddingModelProperties.getLogResponses()).build();
    }
}
