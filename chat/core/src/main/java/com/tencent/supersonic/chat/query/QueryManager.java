package com.tencent.supersonic.chat.query;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.query.rule.entity.EntitySemanticQuery;
import com.tencent.supersonic.chat.query.rule.metric.MetricSemanticQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class QueryManager {

    private static Map<String, RuleSemanticQuery> ruleQueryMap = new ConcurrentHashMap<>();
    private static Map<String, PluginSemanticQuery> pluginQueryMap = new ConcurrentHashMap<>();

    public static void register(SemanticQuery query) {
        if (query instanceof RuleSemanticQuery) {
            ruleQueryMap.put(query.getQueryMode(), (RuleSemanticQuery) query);
        } else if (query instanceof PluginSemanticQuery) {
            pluginQueryMap.put(query.getQueryMode(), (PluginSemanticQuery) query);
        }
    }

    public static SemanticQuery createQuery(String queryMode) {
        if (containsRuleQuery(queryMode)) {
            return createRuleQuery(queryMode);
        } else {
            return createPluginQuery(queryMode);
        }
    }

    public static RuleSemanticQuery createRuleQuery(String queryMode) {
        RuleSemanticQuery semanticQuery = ruleQueryMap.get(queryMode);
        if (Objects.isNull(semanticQuery)) {
            throw new RuntimeException("no supported queryMode :" + queryMode);
        }
        try {
            return semanticQuery.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("no supported queryMode :" + queryMode);
        }
    }

    public static PluginSemanticQuery createPluginQuery(String queryMode) {
        PluginSemanticQuery semanticQuery = pluginQueryMap.get(queryMode);
        if (Objects.isNull(semanticQuery)) {
            throw new RuntimeException("no supported queryMode :" + queryMode);
        }
        try {
            return semanticQuery.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("no supported queryMode :" + queryMode);
        }
    }
    public static boolean containsRuleQuery(String queryMode) {
        if (queryMode == null) {
            return false;
        }
        return ruleQueryMap.containsKey(queryMode);
    }

    public static boolean isMetricQuery(String queryMode) {
        if (queryMode == null || !ruleQueryMap.containsKey(queryMode)) {
            return false;
        }
        return ruleQueryMap.get(queryMode) instanceof MetricSemanticQuery;
    }

    public static boolean isEntityQuery(String queryMode) {
        if (queryMode == null || !ruleQueryMap.containsKey(queryMode)) {
            return false;
        }
        return ruleQueryMap.get(queryMode) instanceof EntitySemanticQuery;
    }

    public static RuleSemanticQuery getRuleQuery(String queryMode) {
        if (queryMode == null) {
            return null;
        }
        return ruleQueryMap.get(queryMode);
    }

    public static List<RuleSemanticQuery> getRuleQueries() {
        return new ArrayList<>(ruleQueryMap.values());
    }

    public static List<String> getPluginQueryModes() {
        return new ArrayList<>(pluginQueryMap.keySet());
    }

}