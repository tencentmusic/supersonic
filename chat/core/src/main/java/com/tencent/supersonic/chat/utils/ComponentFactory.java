package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticParser;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tencent.supersonic.chat.parser.plugin.function.ModelResolver;
import com.tencent.supersonic.chat.query.QuerySelector;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

public class ComponentFactory {

    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();

    private static List<SemanticCorrector> dslCorrections = new ArrayList<>();
    private static SemanticLayer semanticLayer;
    private static QuerySelector querySelector;
    private static ModelResolver modelResolver;
    public static List<SchemaMapper> getSchemaMappers() {
        return CollectionUtils.isEmpty(schemaMappers) ? init(SchemaMapper.class, schemaMappers) : schemaMappers;
    }

    public static List<SemanticParser> getSemanticParsers() {
        return CollectionUtils.isEmpty(semanticParsers) ? init(SemanticParser.class, semanticParsers) : semanticParsers;
    }

    public static List<SemanticCorrector> getSqlCorrections() {
        return CollectionUtils.isEmpty(dslCorrections) ? init(SemanticCorrector.class, dslCorrections) : dslCorrections;
    }


    public static SemanticLayer getSemanticLayer() {
        if (Objects.isNull(semanticLayer)) {
            semanticLayer = init(SemanticLayer.class);
        }
        return semanticLayer;
    }

    public static void setSemanticLayer(SemanticLayer layer) {
        semanticLayer = layer;
    }

    public static QuerySelector getQuerySelector() {
        if (Objects.isNull(querySelector)) {
            querySelector = init(QuerySelector.class);
        }
        return querySelector;
    }

    public static ModelResolver getModelResolver() {
        if (Objects.isNull(modelResolver)) {
            modelResolver = init(ModelResolver.class);
        }
        return modelResolver;
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