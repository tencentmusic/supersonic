package com.tencent.supersonic.chat.server.plugin.build.react;

import java.util.Map;

public interface ReactServer {
    Map<String, Object> invoke(Map<String, Object> param);
}
