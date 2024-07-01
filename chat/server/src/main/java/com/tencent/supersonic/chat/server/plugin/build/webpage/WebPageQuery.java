package com.tencent.supersonic.chat.server.plugin.build.webpage;

import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WebPageQuery extends PluginSemanticQuery {

    public static String QUERY_MODE = "WEB_PAGE";

    public WebPageQuery() {
        PluginQueryManager.register(QUERY_MODE, this);
    }

    protected WebPageResp buildResponse(PluginParseResult pluginParseResult) {
        ChatPlugin plugin = pluginParseResult.getPlugin();
        WebPageResp webPageResponse = new WebPageResp();
        webPageResponse.setName(plugin.getName());
        webPageResponse.setPluginId(plugin.getId());
        webPageResponse.setPluginType(plugin.getType());
        WebBase webPage = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        WebBase webBase = fillWebBaseResult(webPage, pluginParseResult);
        webPageResponse.setWebPage(webBase);
        return webPageResponse;
    }

    @Override
    public QueryResult build() {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);
        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(JsonUtil.toString(properties.get(Constants.CONTEXT)),
                PluginParseResult.class);
        WebPageResp webPageResponse = buildResponse(pluginParseResult);
        queryResult.setResponse(webPageResponse);
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

}
