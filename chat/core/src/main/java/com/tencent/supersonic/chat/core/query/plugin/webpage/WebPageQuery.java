package com.tencent.supersonic.chat.core.query.plugin.webpage;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.core.plugin.Plugin;
import com.tencent.supersonic.chat.core.plugin.PluginParseResult;
import com.tencent.supersonic.chat.core.query.QueryManager;
import com.tencent.supersonic.chat.core.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.core.query.plugin.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WebPageQuery extends PluginSemanticQuery {

    public static String QUERY_MODE = "WEB_PAGE";

    public WebPageQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
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

    protected WebPageResp buildResponse(PluginParseResult pluginParseResult) {
        Plugin plugin = pluginParseResult.getPlugin();
        WebPageResp webPageResponse = new WebPageResp();
        webPageResponse.setName(plugin.getName());
        webPageResponse.setPluginId(plugin.getId());
        webPageResponse.setPluginType(plugin.getType());
        WebBase webPage = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        WebBase webBase = fillWebBaseResult(webPage, pluginParseResult);
        webPageResponse.setWebPage(webBase);
        return webPageResponse;
    }
}
