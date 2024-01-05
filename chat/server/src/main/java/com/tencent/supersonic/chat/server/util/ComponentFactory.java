package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.core.corrector.SemanticCorrector;
import com.tencent.supersonic.chat.core.knowledge.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.mapper.SchemaMapper;
import com.tencent.supersonic.chat.core.parser.SemanticParser;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

@Slf4j
public class ComponentFactory {

    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> semanticCorrectors = new ArrayList<>();
    private static SemanticInterpreter semanticInterpreter;
    private static List<ParseResultProcessor> parseProcessors = new ArrayList<>();
    private static List<ExecuteResultProcessor> executeProcessors = new ArrayList<>();

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

    public static List<ParseResultProcessor> getParseProcessors() {
        return CollectionUtils.isEmpty(parseProcessors) ? init(ParseResultProcessor.class,
                parseProcessors) : parseProcessors;
    }

    public static List<ExecuteResultProcessor> getExecuteProcessors() {
        return CollectionUtils.isEmpty(executeProcessors)
                ? init(ExecuteResultProcessor.class, executeProcessors) : executeProcessors;
    }

    public static SemanticInterpreter getSemanticLayer() {
        if (Objects.isNull(semanticInterpreter)) {
            semanticInterpreter = init(SemanticInterpreter.class);
        }
        return semanticInterpreter;
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