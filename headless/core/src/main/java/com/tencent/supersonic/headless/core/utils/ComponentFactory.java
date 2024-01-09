package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.executor.JdbcExecutor;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.optimizer.DetailQuery;
import com.tencent.supersonic.headless.core.optimizer.QueryOptimizer;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.calcite.CalciteSqlParser;
import com.tencent.supersonic.headless.core.parser.converter.CalculateAggConverter;
import com.tencent.supersonic.headless.core.parser.converter.DefaultDimValueConverter;
import com.tencent.supersonic.headless.core.parser.converter.ParserDefaultConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HeadlessConverter QueryOptimizer QueryExecutor object factory
 */
public class ComponentFactory {

    private static List<HeadlessConverter> headlessConverters = new ArrayList<>();
    private static Map<String, QueryOptimizer> queryOptimizers = new HashMap<>();
    private static List<QueryExecutor> queryExecutors = new ArrayList<>();
    private static SqlParser sqlParser;

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
        queryOptimizers.put("DetailQuery", getBean("DetailQuery", DetailQuery.class));
    }

    private static void initQueryExecutors() {
        queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
    }

    private static void initSemanticConverter() {
        headlessConverters.add(getBean("DefaultDimValueConverter", DefaultDimValueConverter.class));
        headlessConverters.add(getBean("CalculateAggConverter", CalculateAggConverter.class));
        headlessConverters.add(getBean("ParserDefaultConverter", ParserDefaultConverter.class));
    }

}
