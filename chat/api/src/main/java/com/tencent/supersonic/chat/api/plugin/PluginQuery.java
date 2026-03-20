package com.tencent.supersonic.chat.api.plugin;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

public interface PluginQuery {
    QueryResult build();

    void setParseInfo(SemanticParseInfo parseInfo);
}
