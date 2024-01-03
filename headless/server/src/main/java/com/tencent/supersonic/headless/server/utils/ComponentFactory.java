package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.core.parser.SqlParser;
import com.tencent.supersonic.headless.core.parser.calcite.CalciteSqlParser;
import com.tencent.supersonic.headless.server.executor.JdbcExecutor;
import com.tencent.supersonic.headless.server.executor.QueryExecutor;

import java.util.ArrayList;
import java.util.List;

public class ComponentFactory {
    private static List<QueryExecutor> queryExecutors = new ArrayList<>();
    private static SqlParser sqlParser;

    static {
        initQueryExecutors();
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

    public static <T> T getBean(String name, Class<T> tClass) {
        return ContextUtils.getContext().getBean(name, tClass);
    }


    private static void initQueryExecutors() {
        queryExecutors.add(ContextUtils.getContext().getBean("JdbcExecutor", JdbcExecutor.class));
    }

}
