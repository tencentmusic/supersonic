package com.tencent.supersonic.chat.parser.sql.llm;


import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.SqlGenerationMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sql generation factory
 */
public class SqlGenerationFactory {

    private static Map<SqlGenerationMode, SqlGeneration> sqlGenerationMap = new ConcurrentHashMap<>();

    public static SqlGeneration get(SqlGenerationMode strategyType) {
        return sqlGenerationMap.get(strategyType);
    }

    public static void addSqlGenerationForFactory(SqlGenerationMode strategy, SqlGeneration sqlGeneration) {
        sqlGenerationMap.put(strategy, sqlGeneration);
    }
}