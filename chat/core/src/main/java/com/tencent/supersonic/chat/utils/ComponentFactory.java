package com.tencent.supersonic.chat.utils;

import com.tencent.supersonic.chat.api.component.SemanticCorrector;
import com.tencent.supersonic.chat.api.component.SemanticInterpreter;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.parser.JavaLLMProxy;
import com.tencent.supersonic.chat.parser.LLMProxy;
import com.tencent.supersonic.chat.parser.sql.llm.ModelResolver;
import com.tencent.supersonic.chat.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.processor.parse.ParseResultProcessor;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ComponentFactory {

    private static List<SchemaMapper> schemaMappers = new ArrayList<>();
    private static List<SemanticParser> semanticParsers = new ArrayList<>();
    private static List<SemanticCorrector> semanticCorrectors = new ArrayList<>();
    private static SemanticInterpreter semanticInterpreter;

    private static LLMProxy llmProxy;
    private static List<ParseResultProcessor> parseProcessors = new ArrayList<>();
    private static List<ExecuteResultProcessor> executeProcessors = new ArrayList<>();
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

    public static LLMProxy getLLMProxy() {
        //1.Preferentially retrieve from environment variables
        String llmProxyEnv = System.getenv("llmProxy");
        if (StringUtils.isNotBlank(llmProxyEnv)) {
            Map<String, LLMProxy> implementations = ContextUtils.getBeansOfType(LLMProxy.class);
            llmProxy = implementations.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(llmProxyEnv))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        //2.default JavaLLMProxy
        if (Objects.isNull(llmProxy)) {
            llmProxy = ContextUtils.getBean(JavaLLMProxy.class);
        }
        log.info("llmProxy:{}", llmProxy);
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