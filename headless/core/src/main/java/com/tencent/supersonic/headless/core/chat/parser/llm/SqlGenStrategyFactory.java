package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.headless.core.chat.query.llm.s2sql.LLMReq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqlGenStrategyFactory {

    private static Map<LLMReq.SqlGenType, SqlGenStrategy> sqlGenStrategyMap = new ConcurrentHashMap<>();

    public static SqlGenStrategy get(LLMReq.SqlGenType strategyType) {
        return sqlGenStrategyMap.get(strategyType);
    }

    public static void addSqlGenerationForFactory(LLMReq.SqlGenType strategy, SqlGenStrategy sqlGenStrategy) {
        sqlGenStrategyMap.put(strategy, sqlGenStrategy);
    }
}