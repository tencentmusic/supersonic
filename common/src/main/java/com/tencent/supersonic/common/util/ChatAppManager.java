package com.tencent.supersonic.common.util;

import com.google.common.collect.Maps;
import com.tencent.supersonic.common.pojo.ChatApp;

import java.util.Map;

public class ChatAppManager {
    private static final Map<String, ChatApp> chatApps = Maps.newConcurrentMap();

    public static void register(String key, ChatApp app) {
        chatApps.put(key, app);
    }

    public static Map<String, ChatApp> getAllApps() {
        return chatApps;
    }
}
