package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.modeller.SemanticModeller;
import com.tencent.supersonic.headless.server.processor.ResultProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class CoreComponentFactory extends ComponentFactory {

    private static List<ResultProcessor> resultProcessors = new ArrayList<>();

    private static SemanticModeller semanticModeller;

    public static List<ResultProcessor> getResultProcessors() {
        return CollectionUtils.isEmpty(resultProcessors)
                ? init(ResultProcessor.class, resultProcessors)
                : resultProcessors;
    }

    public static SemanticModeller getSemanticModeller() {
        return semanticModeller == null ? init(SemanticModeller.class) : semanticModeller;
    }
}
