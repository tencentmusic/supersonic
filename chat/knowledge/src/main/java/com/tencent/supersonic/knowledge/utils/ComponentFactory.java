package com.tencent.supersonic.knowledge.utils;

import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Objects;

public class ComponentFactory {

    private static SemanticInterpreter semanticInterpreter;

    public static SemanticInterpreter getSemanticLayer() {
        if (Objects.isNull(semanticInterpreter)) {
            semanticInterpreter = init(SemanticInterpreter.class);
        }
        return semanticInterpreter;
    }

    public static void setSemanticLayer(SemanticInterpreter layer) {
        semanticInterpreter = layer;
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