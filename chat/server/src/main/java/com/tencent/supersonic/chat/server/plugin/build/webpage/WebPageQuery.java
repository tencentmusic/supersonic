package com.tencent.supersonic.chat.server.plugin.build.webpage;

import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.core.chat.query.QueryManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.stereotype.Component;

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
    public SemanticQueryReq buildSemanticQueryReq() throws SqlParseException {
        return null;
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
