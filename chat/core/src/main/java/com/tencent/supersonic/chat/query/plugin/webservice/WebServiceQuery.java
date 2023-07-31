package com.tencent.supersonic.chat.query.plugin.webservice;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.*;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebServiceQuery extends PluginSemanticQuery {

    public static String QUERY_MODE = "WEB_SERVICE";

    private S2ThreadContext s2ThreadContext;

    public WebServiceQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) throws SqlParseException {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);
        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = (PluginParseResult) properties.get(Constants.CONTEXT);
        WebServiceResponse webServiceResponse = buildResponse(pluginParseResult);
        Object object = webServiceResponse.getResult();
        Map<String,Object> data=JsonUtil.toMap(JsonUtil.toString(object),String.class,Object.class);
        queryResult.setQueryResults((List<Map<String, Object>>) data.get("resultList"));
        queryResult.setQueryColumns((List<QueryColumn>) data.get("columns"));
        //queryResult.setResponse(webServiceResponse);
        queryResult.setQueryState(QueryState.SUCCESS);
        parseInfo.setProperties(null);
        return queryResult;
    }

    protected WebServiceResponse buildResponse(PluginParseResult pluginParseResult) {
        WebServiceResponse webServiceResponse = new WebServiceResponse();
        Plugin plugin = pluginParseResult.getPlugin();
        WebBase webBase = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        webServiceResponse.setWebBase(webBase);
        //http todo
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        String authHeader = s2ThreadContext.get().getToken();
        log.info("authHeader:{}", authHeader);
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", authHeader);
            Map<String, String> params = new HashMap<>();
            params.put("queryText", pluginParseResult.getRequest().getQueryText());
            HttpClientResult httpClientResult = HttpClientUtils.doGet(webBase.getUrl(), headers, params);
            log.info(" response body:{}", httpClientResult.getContent());
            Map<String, Object> result = JsonUtil.toMap(JsonUtil.toString(httpClientResult.getContent()), String.class, Object.class);
            log.info(" result:{}", result);
            Map<String, Object> data = JsonUtil.toMap(JsonUtil.toString(result.get("data")), String.class, Object.class);
            log.info(" data:{}", data);
            webServiceResponse.setResult(data);
        } catch (Exception e) {
            log.info("Exception:{}", e.getMessage());
        }
        return webServiceResponse;
    }

}
