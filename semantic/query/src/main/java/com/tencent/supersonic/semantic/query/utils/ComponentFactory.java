package com.tencent.supersonic.semantic.query.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.query.executor.JdbcExecutor;
import com.tencent.supersonic.semantic.query.executor.QueryExecutor;
import com.tencent.supersonic.semantic.query.optimizer.DetailQuery;
import com.tencent.supersonic.semantic.query.optimizer.QueryOptimizer;
import com.tencent.supersonic.semantic.query.parser.SemanticConverter;
import com.tencent.supersonic.semantic.query.parser.SqlParser;
import com.tencent.supersonic.semantic.query.parser.calcite.CalciteSqlParser;
import com.tencent.supersonic.semantic.query.parser.convert.CalculateAggConverter;
import com.tencent.supersonic.semantic.query.parser.convert.DefaultDimValueConverter;
import com.tencent.supersonic.semantic.query.parser.convert.MultiSourceJoin;
import com.tencent.supersonic.semantic.query.parser.convert.ParserDefaultConverter;
import java.util.ArrayList;
import java.util.List;

public class ComponentFactory {

    private static List<SemanticConverter> semanticConverters = new ArrayList<>();
    private static List<QueryExecutor> queryExecutors = new ArrayList<>();
    private static List<QueryOptimizer> queryOptimizers = new ArrayList<>();
    private static SqlParser sqlParser;

    static {
        initSemanticConverter();
        initQueryExecutors();
        initQueryOptimizer();
    }

    public static List<SemanticConverter> getSemanticConverters() {
        if (semanticConverters.isEmpty()) {
            initSemanticConverter();
        }
        return semanticConverters;
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
        return queryOptimizers;
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
    private static void initQueryOptimizer() {
        queryOptimizers.add(getBean("DetailQuery", DetailQuery.class));
    }
    private static void initSemanticConverter() {
        semanticConverters.add(getBean("DefaultDimValueConverter", DefaultDimValueConverter.class));
        semanticConverters.add(getBean("CalculateAggConverter", CalculateAggConverter.class));
        semanticConverters.add(getBean("ParserDefaultConverter", ParserDefaultConverter.class));
        semanticConverters.add(getBean("MultiSourceJoin", MultiSourceJoin.class));
    }

    private static void initQueryExecutors() {
        queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
    }

    public static <T> T getBean(String name, Class<T> tClass) {
        return ContextUtils.getContext().getBean(name, tClass);
    }
}
