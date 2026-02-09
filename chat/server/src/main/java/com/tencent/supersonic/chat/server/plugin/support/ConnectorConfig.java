package com.tencent.supersonic.chat.server.plugin.support;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Universal connector configuration model. Inspired by n8n HTTP Request node, Appsmith API
 * datasource, and Zapier webhook configuration.
 *
 * <p>
 * This provides a production-ready connector configuration that supports:
 * <ul>
 * <li>Multiple authentication methods</li>
 * <li>Flexible request/response handling</li>
 * <li>Data transformation via JSONPath</li>
 * <li>Error handling and retry policies</li>
 * <li>Testing capabilities</li>
 * </ul>
 */
@Data
public class ConnectorConfig {

    // ==================== Basic Settings ====================

    /** Connector name for display */
    private String name;

    /** Connector description */
    private String description;

    /** Connector type: HTTP_REQUEST, GRAPHQL, WEBHOOK, INTERNAL */
    private ConnectorType type = ConnectorType.HTTP_REQUEST;

    // ==================== Request Configuration ====================

    /** Request URL, supports parameter placeholders like {{param_name}} */
    private String url;

    /** HTTP method: GET, POST, PUT, PATCH, DELETE */
    private String method = "POST";

    /** Content-Type: application/json, application/x-www-form-urlencoded, multipart/form-data */
    private String contentType = "application/json";

    /** Request headers */
    private Map<String, String> headers;

    /** Query parameters (appended to URL) */
    private List<ParamOption> queryParams;

    /** Body parameters (for POST/PUT/PATCH) */
    private List<ParamOption> bodyParams;

    /** Raw body template (JSON string with {{placeholder}} support) */
    private String bodyTemplate;

    // ==================== Authentication ====================

    /** Authentication configuration */
    private AuthConfig auth;

    // ==================== Response Handling ====================

    /** Response configuration */
    private ResponseConfig response;

    // ==================== Advanced Settings ====================

    /** Timeout in milliseconds, default 30000 */
    private Integer timeoutMs = 30000;

    /** Maximum retry count on failure, default 2 */
    private Integer retryCount = 2;

    /** Retry delay in milliseconds, default 1000 */
    private Integer retryDelayMs = 1000;

    /** Whether to follow redirects, default true */
    private Boolean followRedirects = true;

    /** SSL verification, default true */
    private Boolean verifySsl = true;

    // ==================== Nested Configuration Classes ====================

    /**
     * Authentication configuration supporting multiple auth methods.
     */
    @Data
    public static class AuthConfig {

        /** Auth type: NONE, API_KEY, BASIC, BEARER, OAUTH2 */
        private AuthType type = AuthType.NONE;

        // API Key auth
        /** API key value */
        private String apiKey;
        /** API key header name, default "X-API-Key" */
        private String apiKeyHeader = "X-API-Key";
        /** Where to send API key: HEADER, QUERY */
        private ApiKeyLocation apiKeyLocation = ApiKeyLocation.HEADER;

        // Basic auth
        private String username;
        private String password;

        // Bearer token
        private String bearerToken;

        // OAuth2
        private String clientId;
        private String clientSecret;
        private String tokenUrl;
        private String scope;
    }

    /**
     * Response handling configuration.
     */
    @Data
    public static class ResponseConfig {

        /** JSONPath to extract data from response, e.g. "$.data.items" */
        private String dataPath;

        /** JSONPath for total count (pagination), e.g. "$.meta.total" */
        private String totalPath;

        /** JSONPath for error message, e.g. "$.error.message" */
        private String errorPath;

        /** Success status codes, default [200, 201, 204] */
        private List<Integer> successCodes;

        /** Field mappings: response field -> display name */
        private List<FieldMapping> fieldMappings;

        /** Response type: JSON, XML, TEXT, BINARY */
        private ResponseType responseType = ResponseType.JSON;
    }

    /**
     * Field mapping for response transformation.
     */
    @Data
    public static class FieldMapping {
        /** Source field path (JSONPath) */
        private String sourcePath;
        /** Target field name for display */
        private String targetName;
        /** Display label */
        private String label;
        /** Data type: STRING, NUMBER, DATE, BOOLEAN */
        private String dataType;
        /** Date format if dataType is DATE */
        private String dateFormat;
    }

    // ==================== Enums ====================

    public enum ConnectorType {
        /** Standard HTTP request */
        HTTP_REQUEST,
        /** GraphQL query */
        GRAPHQL,
        /** Webhook receiver */
        WEBHOOK,
        /** Internal service call */
        INTERNAL
    }

    public enum AuthType {
        NONE, API_KEY, BASIC, BEARER, OAUTH2
    }

    public enum ApiKeyLocation {
        HEADER, QUERY
    }

    public enum ResponseType {
        JSON, XML, TEXT, BINARY
    }
}
