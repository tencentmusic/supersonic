package com.tencent.supersonic.knowledge.utils;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Objects;

public class ComponentFactory {

    private static SemanticLayer semanticLayer;

    public static SemanticLayer getSemanticLayer() {
        if (Objects.isNull(semanticLayer)) {
            semanticLayer = init(SemanticLayer.class);
        }
        return semanticLayer;
    }

    public static void setSemanticLayer(SemanticLayer layer) {
        semanticLayer = layer;
    }

    private static <T> List<T> init(Class<T> factoryType, List list) {
        list.addAll(SpringFactoriesLoader.loadFactories(factoryType,
                Thread.currentThread().getContextClassLoader()));
        return list;
    }

    private static <T> T init(Class<T> factoryType) {
        return SpringFactoriesLoader.loadFactories(factoryType,
                Thread.currentThread().getContextClassLoader()).get(0);
    }
}