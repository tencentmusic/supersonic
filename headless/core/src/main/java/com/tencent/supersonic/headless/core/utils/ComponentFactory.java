package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.calcite.CalciteSqlParser;
import com.tencent.supersonic.headless.core.parser.convert.DefaultDimValueConverter;
import com.tencent.supersonic.headless.core.parser.convert.ZipperModelConverter;
import com.tencent.supersonic.headless.core.executor.JdbcExecutor;
import com.tencent.supersonic.headless.core.executor.QueryExecutor;
import com.tencent.supersonic.headless.core.optimizer.DetailQuery;
import com.tencent.supersonic.headless.core.optimizer.QueryOptimizer;
import com.tencent.supersonic.headless.core.parser.convert.CalculateAggConverter;
import com.tencent.supersonic.headless.core.parser.convert.MetricCheckConverter;
import com.tencent.supersonic.headless.core.parser.convert.ParserDefaultConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComponentFactory {

    private static List<HeadlessConverter> headlessConverters = new ArrayList<>();
    private static List<QueryExecutor> queryExecutors = new ArrayList<>();
    private static Map<String, QueryOptimizer> queryOptimizers = new HashMap<>();
    private static SqlParser sqlParser;

    static {
        initSemanticConverter();
        initQueryExecutors();
        initQueryOptimizer();
    }

    public static List<HeadlessConverter> getSemanticConverters() {
        if (headlessConverters.isEmpty()) {
            initSemanticConverter();
        }
        return headlessConverters;
    }

    public static List<QueryExecutor> getQueryExecutors() {
        if (queryExecutors.isEmpty()) {
            initQueryExecutors();
        }
        return queryExecutors;
    }

    public static List<QueryOptimizer> getQueryOptimizers() {
        if (queryOptimizers.isEmpty()) {
            initQueryOptimizer();
        }
        return queryOptimizers.values().stream().collect(Collectors.toList());
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

    private static void initSemanticConverter() {
        headlessConverters.add(getBean("MetricCheckConverter", MetricCheckConverter.class));
        headlessConverters.add(getBean("DefaultDimValueConverter", DefaultDimValueConverter.class));
        headlessConverters.add(getBean("CalculateAggConverter", CalculateAggConverter.class));
        headlessConverters.add(getBean("ParserDefaultConverter", ParserDefaultConverter.class));
        headlessConverters.add(getBean("ZipperModelConverter", ZipperModelConverter.class));
    }

    private static void initQueryExecutors() {
        queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
    }

}
