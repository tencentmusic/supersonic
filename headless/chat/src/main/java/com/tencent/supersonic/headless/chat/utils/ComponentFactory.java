package com.tencent.supersonic.headless.chat.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.chat.parser.llm.DataSetResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Objects;

/** HeadlessConverter QueryOptimizer QueryExecutor object factory */
@Slf4j
public class ComponentFactory {

    private static DataSetResolver modelResolver;

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
        return SpringFactoriesLoader
                .loadFactories(factoryType, Thread.currentThread().getContextClassLoader()).get(0);
    }
}
