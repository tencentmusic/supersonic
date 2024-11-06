package com.tencent.supersonic.common.util;

import com.google.common.collect.Maps;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChatAppManager {
    private static final Map<String, ChatApp> chatApps = Maps.newConcurrentMap();

    public static void register(String key, ChatApp app) {
        chatApps.put(key, app);
    }

    public static Map<String, ChatApp> getAllApps(AppModule appType) {
        return chatApps.entrySet().stream().filter(e -> e.getValue().getAppModule().equals(appType))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    public static Optional<ChatApp> getApp(String appKey) {
        return chatApps.entrySet().stream().filter(e -> e.getKey().equals(appKey))
                .map(Map.Entry::getValue).findFirst();
    }
}
