package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

public class PluginExecutor implements ChatQueryExecutor {

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (!PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return null;
        }
        PluginSemanticQuery query = PluginQueryManager.getPluginQuery(parseInfo.getQueryMode());
        query.setParseInfo(parseInfo);
        return query.build();
    }
}
