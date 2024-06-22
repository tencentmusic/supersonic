package dev.langchain4j.store.embedding;

import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.Objects;

public class ComponentFactory {

    private static S2EmbeddingStore s2EmbeddingStore;

    public static S2EmbeddingStore getS2EmbeddingStore() {
        if (Objects.isNull(s2EmbeddingStore)) {
            s2EmbeddingStore = init(S2EmbeddingStore.class);
        }
        return s2EmbeddingStore;
    }

    private static <T> T init(Class<T> factoryType) {
        return SpringFactoriesLoader.loadFactories(factoryType,
                Thread.currentThread().getContextClassLoader()).get(0);
    }

}
