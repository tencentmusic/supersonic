package com.tencent.supersonic.chat.server.plugin.support.webpage;

import com.tencent.supersonic.chat.api.plugin.ChatPlugin;
import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.api.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.support.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.support.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@Slf4j
@Component
public class WebPageQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "WEB_PAGE";

    public WebPageQuery() {
        PluginQueryManager.register(QUERY_MODE, this);
    }

    @Override
    public QueryResult build() {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);

        try {
            Map<String, Object> properties = parseInfo.getProperties();
            PluginParseResult pluginParseResult = JsonUtil.toObject(
                    JsonUtil.toString(properties.get(Constants.CONTEXT)), PluginParseResult.class);

            WebPageResp webPageResponse = buildResponse(pluginParseResult);

            // Validate URL
            String url =
                    webPageResponse.getWebPage() != null ? webPageResponse.getWebPage().getUrl()
                            : null;
            if (!isValidUrl(url)) {
                log.error("Invalid URL configured for web page plugin: {}", url);
                queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
                queryResult.setTextResult("页面配置错误：无效的 URL");
                return queryResult;
            }

            queryResult.setResponse(webPageResponse);
            queryResult.setQueryState(QueryState.SUCCESS);
        } catch (Exception e) {
            log.error("Error building web page response", e);
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("页面加载失败: " + e.getMessage());
        }

        return queryResult;
    }

    protected WebPageResp buildResponse(PluginParseResult pluginParseResult) {
        ChatPlugin plugin = pluginParseResult.getPlugin();
        WebPageResp webPageResponse = new WebPageResp();
        webPageResponse.setName(plugin.getName());
        webPageResponse.setPluginId(plugin.getId());
        webPageResponse.setPluginType(plugin.getType());
        webPageResponse.setDescription(plugin.getComment());

        WebBase webPage = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        WebBase webBase = fillWebBaseResult(webPage, pluginParseResult);
        webPageResponse.setWebPage(webBase);

        return webPageResponse;
    }

    /**
     * Validate that the URL is well-formed and uses http/https protocol.
     */
    private boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
