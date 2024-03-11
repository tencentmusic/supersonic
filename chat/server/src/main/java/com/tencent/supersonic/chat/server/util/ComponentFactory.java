package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.headless.server.processor.ResultProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ComponentFactory {
    private static List<ResultProcessor> parseProcessors = new ArrayList<>();
    private static List<ExecuteResultProcessor> executeProcessors = new ArrayList<>();

    public static List<ResultProcessor> getParseProcessors() {
        return CollectionUtils.isEmpty(parseProcessors) ? init(ResultProcessor.class,
                parseProcessors) : parseProcessors;
    }

    public static List<ExecuteResultProcessor> getExecuteProcessors() {
        return CollectionUtils.isEmpty(executeProcessors)
                ? init(ExecuteResultProcessor.class, executeProcessors) : executeProcessors;
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