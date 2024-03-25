package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.chat.parser.JavaLLMProxy;
import com.tencent.supersonic.headless.core.chat.parser.LLMProxy;
import com.tencent.supersonic.headless.core.chat.parser.llm.DataSetResolver;
import com.tencent.supersonic.headless.core.executor.JdbcExecutor;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.calcite.CalciteSqlParser;
import com.tencent.supersonic.headless.core.parser.converter.CalculateAggConverter;
import com.tencent.supersonic.headless.core.parser.converter.DefaultDimValueConverter;
import com.tencent.supersonic.headless.core.parser.converter.HeadlessConverter;
import com.tencent.supersonic.headless.core.parser.converter.ParserDefaultConverter;
import com.tencent.supersonic.headless.core.parser.converter.SqlVariableParseConverter;
import com.tencent.supersonic.headless.core.planner.DetailQueryOptimizer;
import com.tencent.supersonic.headless.core.planner.QueryOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HeadlessConverter QueryOptimizer QueryExecutor object factory
 */
@Slf4j
public class ComponentFactory {

    private static List<HeadlessConverter> headlessConverters = new ArrayList<>();
    private static Map<String, QueryOptimizer> queryOptimizers = new HashMap<>();
    private static List<QueryExecutor> queryExecutors = new ArrayList<>();
    private static SqlParser sqlParser;

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

    public static SqlParser getSqlParser() {
        if (sqlParser == null) {
            sqlParser = ContextUtils.getContext().getBean("CalciteSqlParser", CalciteSqlParser.class);
        }
        return sqlParser;
    }

    public static void setSqlParser(SqlParser parser) {
        sqlParser = parser;
    }

    public static void addQueryOptimizer(String name, QueryOptimizer queryOptimizer) {
        queryOptimizers.put(name, queryOptimizer);
    }

    public static <T> T getBean(String name, Class<T> tClass) {
        return ContextUtils.getContext().getBean(name, tClass);
    }

    private static void initQueryOptimizer() {
        queryOptimizers.put("DetailQueryOptimizer", getBean("DetailQueryOptimizer", DetailQueryOptimizer.class));
    }

    private static void initQueryExecutors() {
        queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
    }

    private static void initSemanticConverter() {
        headlessConverters.add(getBean("DefaultDimValueConverter", DefaultDimValueConverter.class));
        headlessConverters.add(getBean("SqlVariableParseConverter", SqlVariableParseConverter.class));
        headlessConverters.add(getBean("CalculateAggConverter", CalculateAggConverter.class));
        headlessConverters.add(getBean("ParserDefaultConverter", ParserDefaultConverter.class));
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

    public static DataSetResolver getModelResolver() {
        if (Objects.isNull(modelResolver)) {
            modelResolver = init(DataSetResolver.class);
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
