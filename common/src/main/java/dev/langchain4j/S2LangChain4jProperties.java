package dev.langchain4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "s2.langchain4j")
public class S2LangChain4jProperties {

    @NestedConfigurationProperty
    private ChatModel chatModel;
    @NestedConfigurationProperty
    private LanguageModel languageModel;
    @NestedConfigurationProperty
    private S2EmbeddingModel embeddingModel;
    @NestedConfigurationProperty
    private ModerationModel moderationModel;

    public ChatModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public LanguageModel getLanguageModel() {
        return languageModel;
    }

    public void setLanguageModel(LanguageModel languageModel) {
        this.languageModel = languageModel;
    }

    public S2EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(S2EmbeddingModel s2EmbeddingModel) {
        this.embeddingModel = s2EmbeddingModel;
    }

    public ModerationModel getModerationModel() {
        return moderationModel;
    }

    public void setModerationModel(ModerationModel moderationModel) {
        this.moderationModel = moderationModel;
    }
}
