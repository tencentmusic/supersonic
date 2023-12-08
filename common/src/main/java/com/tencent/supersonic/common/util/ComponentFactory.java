package com.tencent.supersonic.common.util;

import com.tencent.supersonic.common.util.embedding.S2EmbeddingStore;
import java.util.Objects;
import org.springframework.core.io.support.SpringFactoriesLoader;

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
