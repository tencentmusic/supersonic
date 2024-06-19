package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.chat.parser.llm.DataSetResolver;
import com.tencent.supersonic.headless.chat.parser.llm.JavaLLMProxy;
import com.tencent.supersonic.headless.chat.parser.llm.LLMProxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HeadlessConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class ComponentFactory {

    private static LLMProxy llmProxy;
    private static DataSetResolver modelResolver;

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
        return llmProxy;
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
