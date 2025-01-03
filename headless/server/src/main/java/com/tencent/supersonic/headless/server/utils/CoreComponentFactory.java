package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.modeller.SemanticModeller;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class CoreComponentFactory extends ComponentFactory {

    private static List<SemanticModeller> semanticModellers = new ArrayList<>();

    static {
        init(SemanticModeller.class, semanticModellers);
    }

    public static List<SemanticModeller> getSemanticModellers() {
        return semanticModellers;
    }

}
