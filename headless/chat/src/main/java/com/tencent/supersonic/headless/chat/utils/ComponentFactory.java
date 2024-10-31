package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.chat.corrector.SemanticCorrector;
import com.tencent.supersonic.headless.chat.mapper.SchemaMapper;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.parser.llm.DataSetResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * QueryConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class ComponentFactory {
    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> semanticCorrectors = new ArrayList<>();
    private static DataSetResolver modelResolver;

    public static List<SchemaMapper> getSchemaMappers() {
        return CollectionUtils.isEmpty(schemaMappers) ? init(SchemaMapper.class, schemaMappers)
                : schemaMappers;
    }

    public static List<SemanticParser> getSemanticParsers() {
        return CollectionUtils.isEmpty(semanticParsers)
                ? init(SemanticParser.class, semanticParsers)
                : semanticParsers;
    }

    public static List<SemanticCorrector> getSemanticCorrectors() {
        return CollectionUtils.isEmpty(semanticCorrectors)
                ? init(SemanticCorrector.class, semanticCorrectors)
                : semanticCorrectors;
    }

    public static DataSetResolver getModelResolver() {
        if (Objects.isNull(modelResolver)) {
            modelResolver = init(DataSetResolver.class);
        }
        return modelResolver;
    }

    public static <T> T getBean(String name, Class<T> tClass) {
        return ContextUtils.getContext().getBean(name, tClass);
    }

    protected static <T> List<T> init(Class<T> factoryType, List list) {
        list.addAll(SpringFactoriesLoader.loadFactories(factoryType,
                Thread.currentThread().getContextClassLoader()));
        return list;
    }

    protected static <T> T init(Class<T> factoryType) {
        return SpringFactoriesLoader
                .loadFactories(factoryType, Thread.currentThread().getContextClassLoader()).get(0);
    }
}
