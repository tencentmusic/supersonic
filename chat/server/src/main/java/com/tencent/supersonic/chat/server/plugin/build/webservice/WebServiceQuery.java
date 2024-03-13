package com.tencent.supersonic.chat.server.plugin.build.webservice;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.build.ParamOption;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.core.chat.query.QueryManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.parser.SqlParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
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
    public SemanticQueryReq buildSemanticQueryReq() throws SqlParseException {
        return null;
    }

    protected WebServiceResp buildResponse(PluginParseResult pluginParseResult) {
        WebServiceResp webServiceResponse = new WebServiceResp();
        Plugin plugin = pluginParseResult.getPlugin();
        WebBase webBase = fillWebBaseResult(JsonUtil.toObject(plugin.getConfig(), WebBase.class), pluginParseResult);
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
