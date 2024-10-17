package dev.langchain4j.pgvector.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
@ConfigurationProperties(prefix = Properties.PREFIX)
public class Properties {

    static final String PREFIX = "langchain4j.pgvector";

    @NestedConfigurationProperty
    EmbeddingStoreProperties embeddingStore;
}
