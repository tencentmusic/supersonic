package dev.langchain4j.inmemory.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
@ConfigurationProperties(prefix = Properties.PREFIX)
public class Properties {

    static final String PREFIX = "langchain4j.in-memory";

    @NestedConfigurationProperty
    EmbeddingStoreProperties embeddingStore;

    @NestedConfigurationProperty
    EmbeddingModelProperties embeddingModel;
}
