package com.tencent.supersonic.chat.server.executor;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

public class PluginExecutor implements ChatQueryExecutor {

    @Override
    public boolean accept(ExecuteContext executeContext) {
        return PluginQueryManager.isPluginQuery(executeContext.getParseInfo().getQueryMode());
    }

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        if (!PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            return null;
        }
        PluginSemanticQuery query = PluginQueryManager.getPluginQuery(parseInfo.getQueryMode());
        query.setParseInfo(parseInfo);//  针对 react 插件 存储记忆， 为后期使用的时候能召回
        QueryResult res = query.build();
        query.setChatMemory(executeContext, res);
        return res;
    }
}
