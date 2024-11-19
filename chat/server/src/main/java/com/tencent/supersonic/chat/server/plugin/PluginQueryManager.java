package com.tencent.supersonic.chat.server.plugin;

import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;

import java.util.HashMap;
import java.util.Map;

public class PluginQueryManager {

    private static final Map<String, PluginSemanticQuery> pluginQueries = new HashMap<>();

    public static void register(String queryMode, PluginSemanticQuery pluginSemanticQuery) {
        pluginQueries.put(queryMode, pluginSemanticQuery);
    }

    public static boolean isPluginQuery(String queryMode) {
        return pluginQueries.containsKey(queryMode);
    }

    public static PluginSemanticQuery getPluginQuery(String queryMode) {
        return pluginQueries.get(queryMode);
    }
}
