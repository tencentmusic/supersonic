package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.chat.parser.llm.DataSetResolver;
import com.tencent.supersonic.headless.core.chat.parser.llm.JavaLLMProxy;
import com.tencent.supersonic.headless.core.chat.parser.llm.LLMProxy;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.executor.accelerator.QueryAccelerator;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.converter.HeadlessConverter;
import com.tencent.supersonic.headless.core.planner.QueryOptimizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * HeadlessConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class ComponentFactory {

    private static List<HeadlessConverter> headlessConverters = new ArrayList<>();
    private static Map<String, QueryOptimizer> queryOptimizers = new HashMap<>();
    private static List<QueryExecutor> queryExecutors = new ArrayList<>();
    private static List<QueryAccelerator> queryAccelerators = new ArrayList<>();
    private static SqlParser sqlParser;
    private static QueryCache queryCache;

    private static LLMProxy llmProxy;
    private static DataSetResolver modelResolver;

    static {
        initSemanticConverter();
        initQueryOptimizer();
        initQueryExecutors();
    }

    public static List<HeadlessConverter> getSemanticConverters() {
        if (headlessConverters.isEmpty()) {
            initSemanticConverter();
        }
        return headlessConverters;
    }

    public static List<QueryOptimizer> getQueryOptimizers() {
        if (queryOptimizers.isEmpty()) {
            initQueryOptimizer();
        }
        return queryOptimizers.values().stream().collect(Collectors.toList());
    }

    public static List<QueryExecutor> getQueryExecutors() {
        if (queryExecutors.isEmpty()) {
            initQueryExecutors();
        }
        return queryExecutors;
    }

    public static List<QueryAccelerator> getQueryAccelerators() {
        if (queryAccelerators.isEmpty()) {
            initQueryAccelerators();
        }
        return queryAccelerators;
    }

    public static SqlParser getSqlParser() {
        if (sqlParser == null) {
            initQueryParser();
        }
        return sqlParser;
    }

    public static QueryCache getQueryCache() {
        if (queryCache == null) {
            initQueryCache();
        }
        return queryCache;
    }

    public static void setSqlParser(SqlParser parser) {
        sqlParser = parser;
    }

    public static void addQueryOptimizer(String name, QueryOptimizer queryOptimizer) {
        queryOptimizers.put(name, queryOptimizer);
    }

    private static void initQueryOptimizer() {
        List<QueryOptimizer> queryOptimizerList = new ArrayList<>();
        init(QueryOptimizer.class, queryOptimizerList);
        if (!queryOptimizerList.isEmpty()) {
            queryOptimizerList.stream().forEach(q -> addQueryOptimizer(q.getClass().getSimpleName(), q));
        }
    }

    private static void initQueryExecutors() {
        //queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
        init(QueryExecutor.class, queryExecutors);
    }

    private static void initQueryAccelerators() {
        //queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
        init(QueryAccelerator.class, queryAccelerators);
    }

    private static void initSemanticConverter() {
        init(HeadlessConverter.class, headlessConverters);
    }

    private static void initQueryParser() {
        sqlParser = init(SqlParser.class);
    }

    private static void initQueryCache() {
        queryCache = init(QueryCache.class);
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
