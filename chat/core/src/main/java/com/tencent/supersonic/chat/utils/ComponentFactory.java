package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.parser.LLMInterpreter;
import com.tencent.supersonic.chat.parser.llm.s2sql.ModelResolver;
import com.tencent.supersonic.chat.postprocessor.PostProcessor;
import com.tencent.supersonic.chat.responder.QueryResponder;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComponentFactory {

    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> s2SQLCorrections = new ArrayList<>();
    private static SemanticInterpreter semanticInterpreter;

    private static LLMInterpreter llmInterpreter;
    private static List<PostProcessor> postProcessors = new ArrayList<>();
    private static List<QueryResponder> executeResponders = new ArrayList<>();
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

    public static List<PostProcessor> getPostProcessors() {
        return CollectionUtils.isEmpty(postProcessors) ? init(PostProcessor.class, postProcessors) : postProcessors;
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

    public static void setSemanticLayer(SemanticInterpreter layer) {
        semanticInterpreter = layer;
    }


    public static LLMInterpreter getLLMInterpreter() {
        if (Objects.isNull(llmInterpreter)) {
            llmInterpreter = init(LLMInterpreter.class);
        }
        return llmInterpreter;
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