package com.tencent.supersonic.chat.server.plugin.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal connector executor that handles HTTP requests based on ConnectorConfig. Supports
 * multiple authentication methods, request/response transformation, retry with backoff, and
 * structured error handling.
 *
 * <p>
 * Inspired by:
 * <ul>
 * <li>n8n HTTP Request node</li>
 * <li>Appsmith API datasource</li>
 * <li>Zapier webhook configuration</li>
 * </ul>
 */
@Slf4j
@Component
public class ConnectorExecutor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final List<Integer> DEFAULT_SUCCESS_CODES = Arrays.asList(200, 201, 204);

    /**
     * Execute connector request with the given configuration and parameters.
     *
     * @param config Connector configuration
     * @param params Runtime parameters to substitute in URL/body
     * @return Execution result containing data or error
     */
    public ConnectorResult execute(ConnectorConfig config, Map<String, Object> params) {
        if (config == null) {
            return ConnectorResult.error("Connector configuration is null");
        }

        int maxRetries = config.getRetryCount() != null ? config.getRetryCount() : 2;
        int retryDelay = config.getRetryDelayMs() != null ? config.getRetryDelayMs() : 1000;

        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retrying connector request, attempt {}/{}", attempt + 1,
                            maxRetries + 1);
                    Thread.sleep((long) retryDelay * attempt);
                }

                return executeOnce(config, params);
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("Connection error on attempt {}: {}", attempt + 1, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ConnectorResult.error("Request interrupted");
            }
        }

        return ConnectorResult.error("Request failed after " + (maxRetries + 1) + " attempts: "
                + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private ConnectorResult executeOnce(ConnectorConfig config, Map<String, Object> params) {
        // Build RestTemplate with timeout
        RestTemplate restTemplate = createRestTemplate(config);

        // Build request URI
        URI requestUri = buildRequestUri(config, params);

        // Build headers with authentication
        HttpHeaders headers = buildHeaders(config);

        // Build request body
        Object body = buildRequestBody(config, params);

        // Create HTTP entity
        HttpEntity<?> entity =
                body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

        // Execute request
        HttpMethod httpMethod = HttpMethod.valueOf(config.getMethod().toUpperCase());

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(requestUri, httpMethod, entity, String.class);

            return processResponse(config, response);
        } catch (RestClientResponseException e) {
            return handleHttpError(config, e);
        }
    }

    private RestTemplate createRestTemplate(ConnectorConfig config) {
        int timeoutMs = config.getTimeoutMs() != null ? config.getTimeoutMs() : 30000;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return new RestTemplate(factory);
    }

    private URI buildRequestUri(ConnectorConfig config, Map<String, Object> params) {
        String url = substituteParams(config.getUrl(), params);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

        // Add query parameters
        if (config.getQueryParams() != null) {
            for (ParamOption param : config.getQueryParams()) {
                Object value = resolveParamValue(param, params);
                if (value != null) {
                    builder.queryParam(param.getKey(), value);
                }
            }
        }

        // Add API key to query if configured
        ConnectorConfig.AuthConfig auth = config.getAuth();
        if (auth != null && auth.getType() == ConnectorConfig.AuthType.API_KEY
                && auth.getApiKeyLocation() == ConnectorConfig.ApiKeyLocation.QUERY) {
            builder.queryParam(auth.getApiKeyHeader(), auth.getApiKey());
        }

        return builder.build().encode().toUri();
    }

    private HttpHeaders buildHeaders(ConnectorConfig config) {
        HttpHeaders headers = new HttpHeaders();

        // Set Content-Type
        String contentType = config.getContentType();
        if ("application/json".equals(contentType)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        } else if ("application/x-www-form-urlencoded".equals(contentType)) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }

        // Add custom headers
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::set);
        }

        // Add authentication headers
        addAuthHeaders(headers, config.getAuth());

        return headers;
    }

    private void addAuthHeaders(HttpHeaders headers, ConnectorConfig.AuthConfig auth) {
        if (auth == null || auth.getType() == ConnectorConfig.AuthType.NONE) {
            return;
        }

        switch (auth.getType()) {
            case API_KEY:
                if (auth.getApiKeyLocation() == ConnectorConfig.ApiKeyLocation.HEADER) {
                    headers.set(auth.getApiKeyHeader(), auth.getApiKey());
                }
                break;

            case BASIC:
                String credentials = auth.getUsername() + ":" + auth.getPassword();
                String encoded = Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encoded);
                break;

            case BEARER:
                headers.set("Authorization", "Bearer " + auth.getBearerToken());
                break;

            case OAUTH2:
                // OAuth2 token should be obtained separately and set as bearer token
                if (StringUtils.isNotBlank(auth.getBearerToken())) {
                    headers.set("Authorization", "Bearer " + auth.getBearerToken());
                }
                break;

            default:
                break;
        }
    }

    private Object buildRequestBody(ConnectorConfig config, Map<String, Object> params) {
        String method = config.getMethod().toUpperCase();
        if ("GET".equals(method) || "DELETE".equals(method)) {
            return null;
        }

        // Use body template if provided
        if (StringUtils.isNotBlank(config.getBodyTemplate())) {
            return substituteParams(config.getBodyTemplate(), params);
        }

        // Build from body params
        if (config.getBodyParams() != null && !config.getBodyParams().isEmpty()) {
            String contentType = config.getContentType();

            if ("application/x-www-form-urlencoded".equals(contentType)) {
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                for (ParamOption param : config.getBodyParams()) {
                    Object value = resolveParamValue(param, params);
                    if (value != null) {
                        formData.add(param.getKey(), String.valueOf(value));
                    }
                }
                return formData;
            } else {
                // JSON body
                Map<String, Object> bodyMap = new HashMap<>();
                for (ParamOption param : config.getBodyParams()) {
                    Object value = resolveParamValue(param, params);
                    if (value != null) {
                        bodyMap.put(param.getKey(), value);
                    }
                }
                return JSON.toJSONString(bodyMap);
            }
        }

        return null;
    }

    private Object resolveParamValue(ParamOption param, Map<String, Object> params) {
        // First check if there's a runtime value
        if (params != null && params.containsKey(param.getKey())) {
            return params.get(param.getKey());
        }
        // Fall back to configured value
        return param.getValue();
    }

    private String substituteParams(String template, Map<String, Object> params) {
        if (StringUtils.isBlank(template) || params == null) {
            return template;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String paramName = matcher.group(1).trim();
            Object value = params.get(paramName);
            String replacement =
                    value != null ? Matcher.quoteReplacement(String.valueOf(value)) : "";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private ConnectorResult processResponse(ConnectorConfig config,
            ResponseEntity<String> response) {
        int statusCode = response.getStatusCode().value();
        String body = response.getBody();

        // Check if status code indicates success
        List<Integer> successCodes =
                config.getResponse() != null && config.getResponse().getSuccessCodes() != null
                        ? config.getResponse().getSuccessCodes()
                        : DEFAULT_SUCCESS_CODES;

        if (!successCodes.contains(statusCode)) {
            return ConnectorResult.error("Unexpected status code: " + statusCode, statusCode, body);
        }

        if (StringUtils.isBlank(body)) {
            return ConnectorResult.success(null);
        }

        // Parse response
        Object parsedBody;
        try {
            parsedBody = JSON.parse(body);
        } catch (Exception e) {
            // Return as plain text if not JSON
            return ConnectorResult.success(body);
        }

        // Extract data using JSONPath if configured
        ConnectorConfig.ResponseConfig respConfig = config.getResponse();
        if (respConfig != null && StringUtils.isNotBlank(respConfig.getDataPath())) {
            try {
                Object extracted = JSONPath.eval(parsedBody, respConfig.getDataPath());
                return ConnectorResult.success(extracted);
            } catch (Exception e) {
                log.warn("Failed to extract data using path '{}': {}", respConfig.getDataPath(),
                        e.getMessage());
            }
        }

        return ConnectorResult.success(parsedBody);
    }

    private ConnectorResult handleHttpError(ConnectorConfig config, RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String body = e.getResponseBodyAsString();

        // Try to extract error message from response
        String errorMessage = "HTTP Error " + statusCode;

        if (StringUtils.isNotBlank(body)) {
            ConnectorConfig.ResponseConfig respConfig = config.getResponse();
            if (respConfig != null && StringUtils.isNotBlank(respConfig.getErrorPath())) {
                try {
                    Object parsed = JSON.parse(body);
                    Object errorObj = JSONPath.eval(parsed, respConfig.getErrorPath());
                    if (errorObj != null) {
                        errorMessage = String.valueOf(errorObj);
                    }
                } catch (Exception ex) {
                    // Use default error message
                }
            }
        }

        return ConnectorResult.error(errorMessage, statusCode, body);
    }

    /**
     * Test connector configuration without actually calling the service. Validates URL format,
     * required parameters, authentication config, etc.
     */
    public ConnectorResult testConfig(ConnectorConfig config) {
        if (config == null) {
            return ConnectorResult.error("Configuration is null");
        }

        if (StringUtils.isBlank(config.getUrl())) {
            return ConnectorResult.error("URL is required");
        }

        try {
            new java.net.URL(config.getUrl().replaceAll("\\{\\{[^}]+\\}\\}", "placeholder"));
        } catch (Exception e) {
            return ConnectorResult.error("Invalid URL format: " + e.getMessage());
        }

        ConnectorConfig.AuthConfig auth = config.getAuth();
        if (auth != null) {
            switch (auth.getType()) {
                case API_KEY:
                    if (StringUtils.isBlank(auth.getApiKey())) {
                        return ConnectorResult.error("API key is required for API_KEY auth");
                    }
                    break;
                case BASIC:
                    if (StringUtils.isBlank(auth.getUsername())
                            || StringUtils.isBlank(auth.getPassword())) {
                        return ConnectorResult
                                .error("Username and password are required for BASIC auth");
                    }
                    break;
                case BEARER:
                    if (StringUtils.isBlank(auth.getBearerToken())) {
                        return ConnectorResult.error("Token is required for BEARER auth");
                    }
                    break;
                default:
                    break;
            }
        }

        return ConnectorResult.success("Configuration is valid");
    }

    /**
     * Result of connector execution.
     */
    @lombok.Data
    @lombok.Builder
    public static class ConnectorResult {

        private boolean success;
        private Object data;
        private String errorMessage;
        private Integer statusCode;
        private String rawResponse;

        public static ConnectorResult success(Object data) {
            return ConnectorResult.builder().success(true).data(data).build();
        }

        public static ConnectorResult error(String message) {
            return ConnectorResult.builder().success(false).errorMessage(message).build();
        }

        public static ConnectorResult error(String message, int statusCode, String rawResponse) {
            return ConnectorResult.builder().success(false).errorMessage(message)
                    .statusCode(statusCode).rawResponse(rawResponse).build();
        }
    }
}
