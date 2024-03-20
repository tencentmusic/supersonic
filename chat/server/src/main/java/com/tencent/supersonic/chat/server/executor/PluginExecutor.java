package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;

public class PluginExecutor implements ChatExecutor {

    @Override
    public QueryResult execute(ChatExecuteContext chatExecuteContext) {
        SemanticParseInfo parseInfo = chatExecuteContext.getParseInfo();
        if (!PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return null;
        }
        PluginSemanticQuery query = PluginQueryManager.getPluginQuery(parseInfo.getQueryMode());
        query.setParseInfo(parseInfo);
        return query.build();
    }

}
