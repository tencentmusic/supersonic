package com.tencent.supersonic.chat.api.plugin;

import java.util.HashMap;
import java.util.Map;

public class PluginQueryManager {

    private static final Map<String, PluginQuery> pluginQueries = new HashMap<>();

    public static void register(String queryMode, PluginQuery pluginQuery) {
        pluginQueries.put(queryMode, pluginQuery);
    }

    public static boolean isPluginQuery(String queryMode) {
        return pluginQueries.containsKey(queryMode);
    }

    public static PluginQuery getPluginQuery(String queryMode) {
        return pluginQueries.get(queryMode);
    }
}
