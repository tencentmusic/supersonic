package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.parser.LLMProxy;
import com.tencent.supersonic.chat.parser.sql.llm.ModelResolver;
import com.tencent.supersonic.chat.processor.ParseResultProcessor;
import com.tencent.supersonic.chat.query.QueryResponder;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComponentFactory {

    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> semanticCorrectors = new ArrayList<>();
    private static SemanticInterpreter semanticInterpreter;

    private static LLMProxy llmProxy;
    private static List<ParseResultProcessor> responseProcessors = new ArrayList<>();
    private static List<QueryResponder> executeResponders = new ArrayList<>();
    private static ModelResolver modelResolver;

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

    public static List<ParseResultProcessor> getPostProcessors() {
        return CollectionUtils.isEmpty(responseProcessors) ? init(ParseResultProcessor.class,
                responseProcessors) : responseProcessors;
    }

    public static List<QueryResponder> getExecuteResponders() {
        return CollectionUtils.isEmpty(executeResponders)
                ? init(QueryResponder.class, executeResponders) : executeResponders;
    }

    public static SemanticInterpreter getSemanticLayer() {
        if (Objects.isNull(semanticInterpreter)) {
            semanticInterpreter = init(SemanticInterpreter.class);
        }
        return semanticInterpreter;
    }

    public static LLMProxy getLLMProxy() {
        if (Objects.isNull(llmProxy)) {
            llmProxy = init(LLMProxy.class);
        }
        return llmProxy;
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