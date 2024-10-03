package com.tencent.supersonic.chat.server.util;

import com.tencent.supersonic.chat.server.executor.ChatQueryExecutor;
import com.tencent.supersonic.chat.server.parser.ChatQueryParser;
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
    private static List<ChatQueryParser> chatQueryParsers = new ArrayList<>();
    private static List<ChatQueryExecutor> chatQueryExecutors = new ArrayList<>();
    private static List<PluginRecognizer> pluginRecognizers = new ArrayList<>();

    public static List<ParseResultProcessor> getParseProcessors() {
        return CollectionUtils.isEmpty(parseProcessors)
                ? init(ParseResultProcessor.class, parseProcessors)
                : parseProcessors;
    }

    public static List<ExecuteResultProcessor> getExecuteProcessors() {
        return CollectionUtils.isEmpty(executeProcessors)
                ? init(ExecuteResultProcessor.class, executeProcessors)
                : executeProcessors;
    }

    public static List<ChatQueryParser> getChatParsers() {
        return CollectionUtils.isEmpty(chatQueryParsers)
                ? init(ChatQueryParser.class, chatQueryParsers)
                : chatQueryParsers;
    }

    public static List<ChatQueryExecutor> getChatExecutors() {
        return CollectionUtils.isEmpty(chatQueryExecutors)
                ? init(ChatQueryExecutor.class, chatQueryExecutors)
                : chatQueryExecutors;
    }

    public static List<PluginRecognizer> getPluginRecognizers() {
        return CollectionUtils.isEmpty(pluginRecognizers)
                ? init(PluginRecognizer.class, pluginRecognizers)
                : pluginRecognizers;
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
