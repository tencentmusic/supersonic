package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.chat.corrector.SemanticCorrector;
import com.tencent.supersonic.headless.core.chat.mapper.SchemaMapper;
import com.tencent.supersonic.headless.core.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.server.processor.ResultProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * HeadlessConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class ComponentFactory {
    private static List<ResultProcessor> resultProcessors = new ArrayList<>();
    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> semanticCorrectors = new ArrayList<>();

    public static List<ResultProcessor> getResultProcessors() {
        return CollectionUtils.isEmpty(resultProcessors) ? init(ResultProcessor.class,
                resultProcessors) : resultProcessors;
    }

    public static List<SchemaMapper> getSchemaMappers() {
        return CollectionUtils.isEmpty(schemaMappers) ? init(SchemaMapper.class, schemaMappers) : schemaMappers;
    }

    public static List<SemanticParser> getSemanticParsers() {
        return CollectionUtils.isEmpty(semanticParsers) ? init(SemanticParser.class, semanticParsers) : semanticParsers;
    }

    public static List<SemanticCorrector> getSemanticCorrectors() {
        return CollectionUtils.isEmpty(semanticCorrectors) ? init(SemanticCorrector.class,
                semanticCorrectors) : semanticCorrectors;
    }

    public static <T> T getBean(String name, Class<T> tClass) {
        return ContextUtils.getContext().getBean(name, tClass);
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
