package com.tencent.supersonic.chat.server.plugin.support.webservice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.support.ParamOption;
import com.tencent.supersonic.chat.server.plugin.support.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.support.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebServiceQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "WEB_SERVICE";

    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int MAX_RETRY_COUNT = 2;

    public WebServiceQuery() {
        PluginQueryManager.register(QUERY_MODE, this);
    }

    @Override
    public QueryResult build() {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);

        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(
                JsonUtil.toString(properties.get(Constants.CONTEXT)), PluginParseResult.class);

        try {
            WebServiceResp webServiceResponse = buildResponse(pluginParseResult);
            processResponse(queryResult, webServiceResponse);
            queryResult.setQueryState(QueryState.SUCCESS);
        } catch (ResourceAccessException e) {
            log.error("Web service timeout or connection error: {}", e.getMessage());
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("服务请求超时，请稍后重试");
        } catch (RestClientException e) {
            log.error("Web service request failed: {}", e.getMessage());
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("服务请求失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in web service query", e);
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("服务调用异常: " + e.getMessage());
        }

        return queryResult;
    }

    private void processResponse(QueryResult queryResult, WebServiceResp webServiceResponse) {
        Object result = webServiceResponse.getResult();
        if (result == null) {
            return;
        }

        log.debug("Web service response: {}", JsonUtil.toString(result));

        try {
            Map<String, Object> data =
                    JsonUtil.toMap(JsonUtil.toString(result), String.class, Object.class);

            if (data.get("resultList") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> resultList =
                        (List<Map<String, Object>>) data.get("resultList");
                queryResult.setQueryResults(resultList);
            }

            if (data.get("columns") != null) {
                @SuppressWarnings("unchecked")
                List<QueryColumn> columns = (List<QueryColumn>) data.get("columns");
                queryResult.setQueryColumns(columns);
            }

            if (data.get("data") != null) {
                queryResult.setTextResult(String.valueOf(data.get("data")));
            }
        } catch (Exception e) {
            log.warn("Failed to parse structured response, using raw result: {}", e.getMessage());
            queryResult.setTextResult(String.valueOf(result));
        }
    }

    protected WebServiceResp buildResponse(PluginParseResult pluginParseResult) {
        WebServiceResp webServiceResponse = new WebServiceResp();
        ChatPlugin plugin = pluginParseResult.getPlugin();

        WebBase webBase = fillWebBaseResult(JsonUtil.toObject(plugin.getConfig(), WebBase.class),
                pluginParseResult);
        webServiceResponse.setWebBase(webBase);

        // Build request parameters
        Map<String, Object> params = buildParams(webBase);

        // Build headers
        HttpHeaders headers = buildHeaders(webBase);

        // Create RestTemplate with timeout
        RestTemplate restTemplate = createRestTemplate(webBase);

        // Build request URI
        URI requestUrl = buildRequestUri(webBase, pluginParseResult.getQueryText(), params);

        // Execute with retry
        Object responseBody =
                executeWithRetry(restTemplate, requestUrl, webBase.getMethod(), headers, params);

        // Extract response data using path if configured
        Object extractedResult = extractResponseData(responseBody, webBase.getResponsePath());
        webServiceResponse.setResult(extractedResult);

        return webServiceResponse;
    }

    private Map<String, Object> buildParams(WebBase webBase) {
        Map<String, Object> params = new HashMap<>();
        List<ParamOption> paramOptions = webBase.getParamOptions();
        if (paramOptions != null) {
            paramOptions.forEach(o -> {
                if (o.getValue() != null) {
                    params.put(o.getKey(), o.getValue());
                }
            });
        }
        return params;
    }

    private HttpHeaders buildHeaders(WebBase webBase) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add custom headers if configured
        if (webBase.getHeaders() != null) {
            webBase.getHeaders().forEach(headers::set);
        }

        return headers;
    }

    private RestTemplate createRestTemplate(WebBase webBase) {
        int timeoutMs =
                webBase.getTimeoutMs() != null ? webBase.getTimeoutMs() : DEFAULT_TIMEOUT_MS;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return new RestTemplate(factory);
    }

    private URI buildRequestUri(WebBase webBase, String queryText, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(webBase.getUrl());

        // Add queryText as a parameter
        if (StringUtils.isNotBlank(queryText)) {
            builder.queryParam("queryText", queryText);
        }

        // For GET requests, add params to URL
        String method = webBase.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            params.forEach((key, value) -> {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            });
        }

        return builder.build().encode().toUri();
    }

    private Object executeWithRetry(RestTemplate restTemplate, URI requestUrl, String method,
            HttpHeaders headers, Map<String, Object> params) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retrying web service request, attempt {}", attempt + 1);
                    Thread.sleep(1000L * attempt); // Exponential backoff
                }

                return executeRequest(restTemplate, requestUrl, method, headers, params);
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("Request timeout/connection error on attempt {}: {}", attempt + 1,
                        e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted", e);
            }
        }

        throw new ResourceAccessException("Failed after " + (MAX_RETRY_COUNT + 1) + " attempts",
                (java.io.IOException) lastException.getCause());
    }

    private Object executeRequest(RestTemplate restTemplate, URI requestUrl, String method,
            HttpHeaders headers, Map<String, Object> params) {
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

        HttpEntity<?> entity;
        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            entity = new HttpEntity<>(headers);
        } else {
            entity = new HttpEntity<>(JSON.toJSONString(params), headers);
        }

        ResponseEntity<String> responseEntity =
                restTemplate.exchange(requestUrl, httpMethod, entity, String.class);

        String body = responseEntity.getBody();
        if (StringUtils.isBlank(body)) {
            return null;
        }

        log.debug("Raw response: {}", body);

        // Try to parse as JSON
        try {
            return JSON.parseObject(body);
        } catch (Exception e) {
            // Return as plain text if not JSON
            return body;
        }
    }

    private Object extractResponseData(Object responseBody, String responsePath) {
        if (responseBody == null) {
            return null;
        }

        if (StringUtils.isBlank(responsePath)) {
            return responseBody;
        }

        try {
            return JSONPath.eval(responseBody, responsePath);
        } catch (Exception e) {
            log.warn("Failed to extract data using path '{}': {}", responsePath, e.getMessage());
            return responseBody;
        }
    }
}
