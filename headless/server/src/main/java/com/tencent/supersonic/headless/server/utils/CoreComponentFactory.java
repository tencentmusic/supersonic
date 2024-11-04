package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.modeller.SemanticModeller;
import lombok.extern.slf4j.Slf4j;

/**
 * QueryConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class CoreComponentFactory extends ComponentFactory {

    private static SemanticModeller semanticModeller;

    public static SemanticModeller getSemanticModeller() {
        return semanticModeller == null ? init(SemanticModeller.class) : semanticModeller;
    }
}
