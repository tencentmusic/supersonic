package com.tencent.supersonic.chat.query.plugin.webservice;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebServiceQuery extends PluginSemanticQuery {

    public static String QUERY_MODE = "WEB_SERVICE";

    private RestTemplate restTemplate;

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
        PluginParseResult pluginParseResult =JsonUtil.toObject(JsonUtil.toString(properties.get(Constants.CONTEXT)),PluginParseResult.class);
        WebServiceResponse webServiceResponse = buildResponse(pluginParseResult);
        queryResult.setResponse(webServiceResponse);
        queryResult.setQueryState(QueryState.SUCCESS);
        parseInfo.setProperties(null);
        return queryResult;
    }

    protected WebServiceResponse buildResponse(PluginParseResult pluginParseResult) {
        WebServiceResponse webServiceResponse = new WebServiceResponse();
        Plugin plugin = pluginParseResult.getPlugin();
        WebBase webBase = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        webServiceResponse.setWebBase(webBase);
        List<ParamOption> paramOptions = webBase.getParamOptions();
        Map<String, Object> params = new HashMap<>();
        paramOptions.forEach(o -> params.put(o.getKey(), o.getValue()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(params), headers);
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(webBase.getUrl()).build().encode().toUri();
        ResponseEntity responseEntity = null;
        Object objectResponse = null;
        restTemplate = ContextUtils.getBean(RestTemplate.class);
        try {
            responseEntity = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, Object.class);
            objectResponse = responseEntity.getBody();
            log.info("objectResponse:{}", objectResponse);
            Map<String, Object> response = JsonUtil.objectToMap(objectResponse);
            webServiceResponse.setResult(response);
        } catch (Exception e) {
            log.info("Exception:{}", e.getMessage());
        }
        return webServiceResponse;
    }

}
