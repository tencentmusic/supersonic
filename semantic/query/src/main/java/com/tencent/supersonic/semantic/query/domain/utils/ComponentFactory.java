package com.tencent.supersonic.semantic.query.domain.utils;

import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.query.application.executor.JdbcExecutor;
import com.tencent.supersonic.semantic.query.application.executor.QueryExecutor;
import com.tencent.supersonic.semantic.query.application.optimizer.DetailQuery;
import com.tencent.supersonic.semantic.query.application.optimizer.QueryOptimizer;
import com.tencent.supersonic.semantic.query.application.parser.SemanticConverter;
import com.tencent.supersonic.semantic.query.application.parser.SqlParser;
import com.tencent.supersonic.semantic.query.application.parser.calcite.CalciteSqlParser;
import com.tencent.supersonic.semantic.query.application.parser.convert.CalculateConverterAgg;
import com.tencent.supersonic.semantic.query.application.parser.convert.DefaultDimValueConverter;
import com.tencent.supersonic.semantic.query.application.parser.convert.MultiSourceJoin;
import com.tencent.supersonic.semantic.query.application.parser.convert.ParserDefaultConverter;
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
        if(queryOptimizers.isEmpty()){
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
        semanticConverters.add(getBean("CalculateConverterAgg", CalculateConverterAgg.class));
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
