package com.tencent.supersonic.chat.core.utils;

import com.tencent.supersonic.chat.core.knowledge.semantic.SemanticInterpreter;
import com.tencent.supersonic.chat.core.parser.JavaLLMProxy;
import com.tencent.supersonic.chat.core.parser.LLMProxy;
import com.tencent.supersonic.chat.core.parser.sql.llm.ModelResolver;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

@Slf4j
public class ComponentFactory {

    private static SemanticInterpreter semanticInterpreter;
    private static LLMProxy llmProxy;
    private static ModelResolver modelResolver;

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

    private static <T> T init(Class<T> factoryType) {
        return SpringFactoriesLoader.loadFactories(factoryType,
                Thread.currentThread().getContextClassLoader()).get(0);
    }
}