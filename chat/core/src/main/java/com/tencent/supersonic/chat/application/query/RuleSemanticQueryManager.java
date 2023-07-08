package com.tencent.supersonic.chat.application.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RuleSemanticQueryManager
 */
public class RuleSemanticQueryManager {

    private static Map<String, RuleSemanticQuery> semanticQueryMap = new ConcurrentHashMap<>();

    public static RuleSemanticQuery create(String queryMode) {
        RuleSemanticQuery semanticQuery = semanticQueryMap.get(queryMode);
        if (Objects.isNull(semanticQuery)) {
            throw new RuntimeException("no supported queryMode :" + queryMode);
        }
        try {
            return semanticQuery.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("no supported queryMode :" + queryMode);
        }
    }

    public static void register(RuleSemanticQuery query) {
        semanticQueryMap.put(query.getQueryMode(), query);
    }

    public static List<RuleSemanticQuery> getSemanticQueries() {
        return new ArrayList<>(semanticQueryMap.values());
    }

    public static List<String> getSemanticQueryModes() {
        return new ArrayList<>(semanticQueryMap.keySet());
    }
}