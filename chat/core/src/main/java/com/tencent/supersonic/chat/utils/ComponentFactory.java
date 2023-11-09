package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SemanticParser;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tencent.supersonic.chat.parser.llm.s2sql.ModelResolver;
import com.tencent.supersonic.chat.query.QuerySelector;
import com.tencent.supersonic.chat.responder.execute.ExecuteResponder;
import com.tencent.supersonic.chat.responder.parse.ParseResponder;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

public class ComponentFactory {

    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> s2SQLCorrections = new ArrayList<>();
    private static SemanticInterpreter semanticInterpreter;
    private static List<ParseResponder> parseResponders = new ArrayList<>();
    private static List<ExecuteResponder> executeResponders = new ArrayList<>();
    private static QuerySelector querySelector;
    private static ModelResolver modelResolver;
    public static List<SchemaMapper> getSchemaMappers() {
        return CollectionUtils.isEmpty(schemaMappers) ? init(SchemaMapper.class, schemaMappers) : schemaMappers;
    }

    public static List<SemanticParser> getSemanticParsers() {
        return CollectionUtils.isEmpty(semanticParsers) ? init(SemanticParser.class, semanticParsers) : semanticParsers;
    }

    public static List<SemanticCorrector> getSqlCorrections() {
        return CollectionUtils.isEmpty(s2SQLCorrections) ? init(SemanticCorrector.class,
                s2SQLCorrections) : s2SQLCorrections;
    }

    public static List<ParseResponder> getParseResponders() {
        return CollectionUtils.isEmpty(parseResponders) ? init(ParseResponder.class, parseResponders) : parseResponders;
    }

    public static List<ExecuteResponder> getExecuteResponders() {
        return CollectionUtils.isEmpty(executeResponders)
                ? init(ExecuteResponder.class, executeResponders) : executeResponders;
    }

    public static SemanticInterpreter getSemanticLayer() {
        if (Objects.isNull(semanticInterpreter)) {
            semanticInterpreter = init(SemanticInterpreter.class);
        }
        return semanticInterpreter;
    }

    public static void setSemanticLayer(SemanticInterpreter layer) {
        semanticInterpreter = layer;
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