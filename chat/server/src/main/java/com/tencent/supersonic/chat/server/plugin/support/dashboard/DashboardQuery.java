package com.tencent.supersonic.chat.server.plugin.support.dashboard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import com.tencent.supersonic.chat.api.plugin.ChatPlugin;
import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.api.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.support.ParamOption;
import com.tencent.supersonic.chat.server.plugin.support.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.support.WebBase;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Plugin Query - supports both external service calls (MCP, REST API) and internal data
 * processing.
 *
 * Plugin Config Structure: { "url": "https://api.example.com/dashboard", // External service URL
 * (optional) "method": "POST", // HTTP method for external calls "headers": {"Authorization":
 * "Bearer xxx"}, // Custom headers "timeoutMs": 30000, // Request timeout "responsePath": "$.data",
 * // JSONPath to extract data "dataSource": "EXTERNAL" | "INTERNAL", // Data source type
 * "dashboardConfig": { // Dashboard display config "title": "运营报表", "kpiMetrics": ["metric1",
 * "metric2"], // Metric bizNames for KPI cards "trendDays": 7, // Days for trend chart
 * "maxDetailRows": 10 // Max rows in detail table } }
 */
@Slf4j
@Component
public class DashboardQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "DASHBOARD";

    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int DEFAULT_MAX_DETAIL_ROWS = 10;
    private static final int MAX_KPI_METRICS = 4;

    public DashboardQuery() {
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
            DashboardPluginResp response = buildResponse(pluginParseResult);
            queryResult.setResponse(response);
            queryResult.setQueryState(QueryState.SUCCESS);

            // Generate text summary
            if (response.getDashboardData() != null) {
                queryResult.setTextResult(generateTextSummary(response.getDashboardData()));
            }

        } catch (ResourceAccessException e) {
            log.error("Dashboard service timeout: {}", e.getMessage());
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("Dashboard service timeout, please retry later");
        } catch (RestClientException e) {
            log.error("Dashboard service request failed: {}", e.getMessage());
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("Dashboard service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in dashboard query", e);
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("Dashboard query failed: " + e.getMessage());
        }

        return queryResult;
    }

    protected DashboardPluginResp buildResponse(PluginParseResult pluginParseResult) {
        ChatPlugin plugin = pluginParseResult.getPlugin();
        DashboardConfig config = JsonUtil.toObject(plugin.getConfig(), DashboardConfig.class);

        DashboardPluginResp response = new DashboardPluginResp();
        response.setName(plugin.getName());
        response.setPluginId(plugin.getId());
        response.setPluginType(plugin.getType());
        response.setDescription(plugin.getComment());

        // Check if using external service or internal data
        if (isExternalDataSource(config)) {
            // Call external service (MCP, REST API)
            DashboardData dashboardData = fetchFromExternalService(config, pluginParseResult);
            response.setDashboardData(dashboardData);
        } else {
            // Use data from plugin parameters or generate from config
            DashboardData dashboardData = buildFromConfig(config, pluginParseResult);
            response.setDashboardData(dashboardData);
        }

        return response;
    }

    private boolean isExternalDataSource(DashboardConfig config) {
        return StringUtils.isNotBlank(config.getUrl())
                && !"INTERNAL".equalsIgnoreCase(config.getDataSource());
    }

    private DashboardData fetchFromExternalService(DashboardConfig config,
            PluginParseResult pluginParseResult) {
        // Build request parameters
        Map<String, Object> params = buildParams(config, pluginParseResult);

        // Build headers
        HttpHeaders headers = buildHeaders(config);

        // Create RestTemplate with timeout
        RestTemplate restTemplate = createRestTemplate(config);

        // Build request URI
        URI requestUrl = buildRequestUri(config, pluginParseResult.getQueryText(), params);

        // Execute request
        Object responseBody =
                executeRequest(restTemplate, requestUrl, config.getMethod(), headers, params);

        // Extract dashboard data
        return extractDashboardData(responseBody, config);
    }

    private DashboardData buildFromConfig(DashboardConfig config,
            PluginParseResult pluginParseResult) {
        // Build dashboard from config without external service
        // This is useful for static dashboards or when data comes from plugin params
        DashboardDisplayConfig displayConfig = config.getDashboardConfig();

        return DashboardData.builder()
                .title(displayConfig != null && displayConfig.getTitle() != null
                        ? displayConfig.getTitle()
                        : "Dashboard")
                .kpiMetrics(new ArrayList<>()).trendData(new ArrayList<>())
                .trendMetrics(new ArrayList<>()).detailColumns(new ArrayList<>())
                .detailData(new ArrayList<>()).build();
    }

    private Map<String, Object> buildParams(DashboardConfig config,
            PluginParseResult pluginParseResult) {
        Map<String, Object> params = new HashMap<>();

        // Add params from parseInfo element matches
        Map<String, Object> elementMap = getElementMap(pluginParseResult);
        params.putAll(elementMap);

        // Add query text
        params.put("queryText", pluginParseResult.getQueryText());

        // Add datasetId if available
        if (parseInfo.getDataSetId() != null) {
            params.put("datasetId", parseInfo.getDataSetId());
        }

        return params;
    }

    private HttpHeaders buildHeaders(DashboardConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::set);
        }

        return headers;
    }

    private RestTemplate createRestTemplate(DashboardConfig config) {
        int timeoutMs = config.getTimeoutMs() != null ? config.getTimeoutMs() : DEFAULT_TIMEOUT_MS;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return new RestTemplate(factory);
    }

    private URI buildRequestUri(DashboardConfig config, String queryText,
            Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(config.getUrl());

        if (StringUtils.isNotBlank(queryText)) {
            builder.queryParam("queryText", queryText);
        }

        String method = config.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            params.forEach((key, value) -> {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            });
        }

        return builder.build().encode().toUri();
    }

    private Object executeRequest(RestTemplate restTemplate, URI requestUrl, String method,
            HttpHeaders headers, Map<String, Object> params) {
        HttpMethod httpMethod =
                HttpMethod.valueOf(StringUtils.isNotBlank(method) ? method.toUpperCase() : "POST");

        HttpEntity<?> entity;
        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            entity = new HttpEntity<>(headers);
        } else {
            entity = new HttpEntity<>(JSON.toJSONString(params), headers);
        }

        log.info("Calling dashboard service: {} {}", httpMethod, requestUrl);

        ResponseEntity<String> responseEntity =
                restTemplate.exchange(requestUrl, httpMethod, entity, String.class);

        String body = responseEntity.getBody();
        if (StringUtils.isBlank(body)) {
            return null;
        }

        log.debug("Dashboard service response: {}", body);

        try {
            return JSON.parseObject(body);
        } catch (Exception e) {
            return body;
        }
    }

    private DashboardData extractDashboardData(Object responseBody, DashboardConfig config) {
        if (responseBody == null) {
            return DashboardData.builder().title("Dashboard").kpiMetrics(new ArrayList<>())
                    .trendData(new ArrayList<>()).trendMetrics(new ArrayList<>())
                    .detailColumns(new ArrayList<>()).detailData(new ArrayList<>()).build();
        }

        // Try to extract using responsePath if configured
        Object data = responseBody;
        if (StringUtils.isNotBlank(config.getResponsePath())) {
            try {
                data = JSONPath.eval(responseBody, config.getResponsePath());
            } catch (Exception e) {
                log.warn("Failed to extract data using path '{}': {}", config.getResponsePath(),
                        e.getMessage());
            }
        }

        // Try to parse as DashboardData directly
        try {
            return JsonUtil.toObject(JsonUtil.toString(data), DashboardData.class);
        } catch (Exception e) {
            log.warn("Failed to parse response as DashboardData: {}", e.getMessage());
        }

        // Try to build DashboardData from raw data
        return buildDashboardDataFromRaw(data, config);
    }

    @SuppressWarnings("unchecked")
    private DashboardData buildDashboardDataFromRaw(Object rawData, DashboardConfig config) {
        DashboardDisplayConfig displayConfig = config.getDashboardConfig();
        int trendDays = displayConfig != null && displayConfig.getTrendDays() != null
                ? displayConfig.getTrendDays()
                : DEFAULT_TREND_DAYS;
        int maxDetailRows = displayConfig != null && displayConfig.getMaxDetailRows() != null
                ? displayConfig.getMaxDetailRows()
                : DEFAULT_MAX_DETAIL_ROWS;

        DashboardData.DashboardDataBuilder builder = DashboardData.builder()
                .title(displayConfig != null && displayConfig.getTitle() != null
                        ? displayConfig.getTitle()
                        : "Dashboard");

        try {
            Map<String, Object> dataMap =
                    JsonUtil.toMap(JsonUtil.toString(rawData), String.class, Object.class);

            // Extract KPI metrics
            if (dataMap.containsKey("kpiMetrics")) {
                List<KpiMetric> kpis = JsonUtil.toList(JsonUtil.toString(dataMap.get("kpiMetrics")),
                        KpiMetric.class);
                builder.kpiMetrics(kpis);
            } else {
                builder.kpiMetrics(new ArrayList<>());
            }

            // Extract trend data
            if (dataMap.containsKey("trendData")) {
                List<Map<String, Object>> trend =
                        (List<Map<String, Object>>) dataMap.get("trendData");
                builder.trendData(
                        trend != null ? trend.subList(0, Math.min(trend.size(), trendDays))
                                : new ArrayList<>());
            } else {
                builder.trendData(new ArrayList<>());
            }

            // Extract trend metrics
            if (dataMap.containsKey("trendMetrics")) {
                List<String> metrics = (List<String>) dataMap.get("trendMetrics");
                builder.trendMetrics(metrics != null ? metrics : new ArrayList<>());
            } else {
                builder.trendMetrics(new ArrayList<>());
            }

            // Extract detail columns
            if (dataMap.containsKey("detailColumns")) {
                List<DetailColumn> cols = JsonUtil.toList(
                        JsonUtil.toString(dataMap.get("detailColumns")), DetailColumn.class);
                builder.detailColumns(cols != null ? cols : new ArrayList<>());
            } else {
                builder.detailColumns(new ArrayList<>());
            }

            // Extract detail data
            if (dataMap.containsKey("detailData")) {
                List<Map<String, Object>> details =
                        (List<Map<String, Object>>) dataMap.get("detailData");
                builder.detailData(details != null
                        ? details.subList(0, Math.min(details.size(), maxDetailRows))
                        : new ArrayList<>());
            } else if (dataMap.containsKey("resultList")) {
                // Fallback to resultList format
                List<Map<String, Object>> results =
                        (List<Map<String, Object>>) dataMap.get("resultList");
                builder.detailData(results != null
                        ? results.subList(0, Math.min(results.size(), maxDetailRows))
                        : new ArrayList<>());
            } else {
                builder.detailData(new ArrayList<>());
            }

            // Extract report date
            if (dataMap.containsKey("reportDate")) {
                builder.reportDate(String.valueOf(dataMap.get("reportDate")));
            }

        } catch (Exception e) {
            log.warn("Failed to parse raw data as dashboard: {}", e.getMessage());
            builder.kpiMetrics(new ArrayList<>()).trendData(new ArrayList<>())
                    .trendMetrics(new ArrayList<>()).detailColumns(new ArrayList<>())
                    .detailData(new ArrayList<>());
        }

        return builder.build();
    }

    private String generateTextSummary(DashboardData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dashboard: ").append(data.getTitle());
        if (data.getReportDate() != null) {
            sb.append(" (").append(data.getReportDate()).append(")");
        }
        sb.append("\n\n");

        if (data.getKpiMetrics() != null) {
            for (KpiMetric kpi : data.getKpiMetrics()) {
                sb.append("- ").append(kpi.getName()).append(": ");
                sb.append(formatNumber(kpi.getValue()));
                if (kpi.getTrendPercent() != null && kpi.getTrendPercent() != 0) {
                    String arrow = "up".equals(kpi.getTrend()) ? "↑" : "↓";
                    sb.append(" (").append(arrow).append(kpi.getTrendPercent()).append("%)");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String formatNumber(double value) {
        if (value >= 100000000) {
            return String.format("%.2f亿", value / 100000000);
        } else if (value >= 10000) {
            return String.format("%.2f万", value / 10000);
        } else {
            return String.format("%.0f", value);
        }
    }

    // Configuration classes

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardConfig {
        private String url; // External service URL
        private String method; // HTTP method (GET/POST)
        private Map<String, String> headers; // Custom headers
        private Integer timeoutMs; // Request timeout
        private String responsePath; // JSONPath to extract data
        private String dataSource; // EXTERNAL or INTERNAL
        private DashboardDisplayConfig dashboardConfig; // Display configuration
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardDisplayConfig {
        private String title;
        private List<String> kpiMetrics; // Metric bizNames for KPI cards
        private Integer trendDays; // Days for trend chart
        private Integer maxDetailRows; // Max rows in detail table
    }

    // Response classes

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardPluginResp {
        private String name;
        private Long pluginId;
        private String pluginType;
        private String description;
        private DashboardData dashboardData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardData {
        private String title;
        private String reportDate;
        private List<KpiMetric> kpiMetrics;
        private List<Map<String, Object>> trendData;
        private List<String> trendMetrics;
        private List<DetailColumn> detailColumns;
        private List<Map<String, Object>> detailData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiMetric {
        private String name;
        private String bizName;
        private double value;
        private double previousValue;
        private String trend;
        private Double trendPercent;
        private String unit;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailColumn {
        private String name;
        private String bizName;
        private String type;
    }
}
