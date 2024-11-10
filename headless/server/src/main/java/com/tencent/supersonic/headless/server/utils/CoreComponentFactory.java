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

    public static List<SemanticModeller> getSemanticModellers() {
        if (semanticModellers.isEmpty()) {
            initSemanticModellers();
        }
        return semanticModellers;
    }

    private static void initSemanticModellers() {
        init(SemanticModeller.class, semanticModellers);
    }

}
