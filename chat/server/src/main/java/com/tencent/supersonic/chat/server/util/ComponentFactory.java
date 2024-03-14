package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.executor.ChatExecutor;
import com.tencent.supersonic.chat.server.parser.ChatParser;
import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.processor.execute.ExecuteResultProcessor;
import com.tencent.supersonic.chat.server.processor.parse.ParseResultProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ComponentFactory {
    private static List<ParseResultProcessor> parseProcessors = new ArrayList<>();
    private static List<ExecuteResultProcessor> executeProcessors = new ArrayList<>();
    private static List<ChatParser> chatParsers = new ArrayList<>();
    private static List<ChatExecutor> chatExecutors = new ArrayList<>();
    private static List<PluginRecognizer> pluginRecognizers = new ArrayList<>();

    public static List<ParseResultProcessor> getParseProcessors() {
        return CollectionUtils.isEmpty(parseProcessors) ? init(ParseResultProcessor.class,
                parseProcessors) : parseProcessors;
    }

    public static List<ExecuteResultProcessor> getExecuteProcessors() {
        return CollectionUtils.isEmpty(executeProcessors)
                ? init(ExecuteResultProcessor.class, executeProcessors) : executeProcessors;
    }

    public static List<ChatParser> getChatParsers() {
        return CollectionUtils.isEmpty(chatParsers)
                ? init(ChatParser.class, chatParsers) : chatParsers;
    }

    public static List<ChatExecutor> getChatExecutors() {
        return CollectionUtils.isEmpty(chatExecutors)
                ? init(ChatExecutor.class, chatExecutors) : chatExecutors;
    }

    public static List<PluginRecognizer> getPluginRecognizers() {
        return CollectionUtils.isEmpty(pluginRecognizers)
                ? init(PluginRecognizer.class, pluginRecognizers) : pluginRecognizers;
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