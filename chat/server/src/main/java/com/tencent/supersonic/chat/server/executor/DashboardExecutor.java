package com.tencent.supersonic.chat.server.executor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import com.tencent.supersonic.headless.server.utils.ModelConfigHelper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.provider.ModelProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Executor for dashboard-style query results. Returns a composite view with KPI cards, trend chart,
 * and detail table. <br/>
 * Activation conditions: 1. DASHBOARD_MODE ChatApp is enabled in the agent config 2. Query text
 * matches dashboard patterns (运营报表, 日报, 数据看板, etc.)
 */
@Slf4j
public class DashboardExecutor implements ChatQueryExecutor {

    public static final String APP_KEY = "DASHBOARD_MODE";
    public static final String LLM_INSIGHT_KEY = "DASHBOARD_LLM_INSIGHT";
    public static final String QUERY_MODE = "DASHBOARD";

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int DEFAULT_QUERY_DAYS = 30;
    private static final int MAX_KPI_METRICS = 4;
    private static final int MAX_DETAIL_ROWS = 10;

    // Analysis thresholds
    private static final double ANOMALY_THRESHOLD_SIGMA = 2.0; // Standard deviation multiplier
    private static final double SIGNIFICANT_CHANGE_PERCENT = 10.0; // % change to trigger insight
    private static final int MIN_DATA_POINTS_FOR_ANALYSIS = 3; // Minimum points for trend analysis

    // Attribution analysis settings
    public static final String ATTRIBUTION_KEY = "DASHBOARD_ATTRIBUTION";
    private static final int MAX_ATTRIBUTION_DIMENSIONS = 3; // Max dimensions to analyze
    private static final int MAX_ATTRIBUTION_VALUES = 5; // Max values per dimension
    private static final double MIN_CONTRIBUTION_PERCENT = 5.0; // Min contribution to report

    // Keywords that indicate dashboard/report intent
    private static final List<String> DASHBOARD_KEYWORDS = Arrays.asList("运营报表", "运营仪表盘", "运营看板",
            "日报", "周报", "月报", "数据看板", "数据仪表盘", "dashboard", "report", "今日数据", "昨日数据", "本周数据",
            "本月数据", "数据概览", "数据总览", "整体情况", "运营情况", "运营概况", "运营数据", "数据报表", "看板", "仪表盘");

    private static final String LLM_INSIGHT_PROMPT = """
            你是一位资深数据分析师，请根据以下运营数据生成简洁的分析报告。

            ## 数据概览
            报告日期: {{reportDate}}
            数据集: {{datasetName}}

            ## KPI 指标
            {{kpiSummary}}

            ## 已检测到的异常
            {{anomalies}}

            ## 已识别的趋势
            {{insights}}

            ## 要求
            1. 用 2-3 句话总结整体表现
            2. 指出最需要关注的 1-2 个问题
            3. 给出 1-2 条可行的建议
            4. 语言简洁专业，避免废话
            5. 使用中文回复

            请直接输出分析结论，不要有多余的格式标记。
            """;

    // Config keys for dashboard customization (stored in ChatApp.prompt as JSON)
    // Example: {"dataSetId": 1, "metrics": ["pv", "uv", "stay_time"], "defaultSql": "SELECT ..."}
    private static final String CONFIG_KEY_DATASET_ID = "dataSetId";
    private static final String CONFIG_KEY_METRICS = "metrics";
    private static final String CONFIG_KEY_DEFAULT_SQL = "defaultSql";
    private static final String CONFIG_KEY_TITLE = "title";

    public DashboardExecutor() {
        ChatAppManager.register(APP_KEY, ChatApp.builder().prompt("").name("运营报表模式")
                .appModule(AppModule.CHAT)
                .description("启用运营报表样式响应。开启后，包含「运营报表」「日报」「数据看板」等关键词的查询，"
                        + "将返回包含KPI卡片、趋势图、明细表的仪表盘视图。\n"
                        + "可在prompt字段配置JSON: {\"dataSetId\": 数据集ID, \"metrics\": [\"指标1\", \"指标2\"], "
                        + "\"defaultSql\": \"自定义SQL\", \"title\": \"报表标题\"}")
                .enable(false).build());

        ChatAppManager.register(LLM_INSIGHT_KEY, ChatApp.builder().prompt(LLM_INSIGHT_PROMPT)
                .name("AI智能分析").appModule(AppModule.CHAT)
                .description("使用大模型对运营数据进行智能分析和解读。开启后，仪表盘将包含AI生成的分析摘要和建议。").enable(false).build());

        ChatAppManager.register(ATTRIBUTION_KEY,
                ChatApp.builder().prompt("").name("变化归因分析").appModule(AppModule.CHAT)
                        .description("对显著变化的KPI进行归因分析。当指标变化超过阈值时，自动下钻各维度，" + "识别哪些维度值对变化贡献最大。")
                        .enable(false).build());
    }

    @Override
    public boolean accept(ExecuteContext executeContext) {
        // Skip if already handled by a registered plugin (e.g., DashboardQuery for external
        // services)
        String queryMode = executeContext.getParseInfo().getQueryMode();
        if (queryMode != null && PluginQueryManager.isPluginQuery(queryMode)) {
            return false;
        }

        // Check if dashboard mode is enabled for this agent
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(executeContext.getAgent().getId());
        ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);

        if (Objects.isNull(chatApp) || !chatApp.isEnable()) {
            return false;
        }

        // Check if query matches dashboard keywords
        String queryText = executeContext.getRequest().getQueryText();
        return isDashboardQuery(queryText);
    }

    private boolean isDashboardQuery(String queryText) {
        if (queryText == null || queryText.isEmpty()) {
            return false;
        }

        String lowerQuery = queryText.toLowerCase();
        for (String keyword : DASHBOARD_KEYWORDS) {
            if (lowerQuery.contains(keyword.toLowerCase())) {
                log.info("Query matched dashboard keyword: {}", keyword);
                return true;
            }
        }
        return false;
    }

    @Override
    public QueryResult execute(ExecuteContext executeContext) {
        SemanticParseInfo parseInfo = executeContext.getParseInfo();
        // Override queryMode so the saved chatContext carries DASHBOARD mode.
        // This enables re-query (queryData) to detect and route to handleDashboardReQuery.
        parseInfo.setQueryMode(QUERY_MODE);
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);
        queryResult.setQueryId(executeContext.getRequest().getQueryId());
        queryResult.setChatContext(parseInfo);

        // Get agent for default dataset lookup and LLM config
        AgentService agentService = ContextUtils.getBean(AgentService.class);
        Agent agent = agentService.getAgent(executeContext.getAgent().getId());

        try {
            // Execute query (prefers struct query for reliable metrics + date filtering)
            SemanticQueryResp queryResp = executeQuery(executeContext, parseInfo, agent);

            if (queryResp == null || queryResp.getResultList() == null
                    || queryResp.getResultList().isEmpty()) {
                queryResult.setQueryState(QueryState.EMPTY);
                queryResult.setTextResult("No data available for the dashboard");
                return queryResult;
            }

            // Build dashboard data from query results
            // Use configured title > dataSet name > default
            JSONObject dashboardConfig = getDashboardConfig(agent);
            String dashboardTitle;
            if (dashboardConfig != null && dashboardConfig.containsKey(CONFIG_KEY_TITLE)) {
                dashboardTitle = dashboardConfig.getString(CONFIG_KEY_TITLE);
            } else if (parseInfo.getDataSet() != null) {
                dashboardTitle = parseInfo.getDataSet().getName();
            } else {
                dashboardTitle = "运营报表";
            }

            Long dataSetIdForAnalysis = parseInfo.getDataSetId() != null ? parseInfo.getDataSetId()
                    : getDefaultDataSetId(agent);
            DashboardData dashboardData = buildDashboardData(queryResp, dashboardTitle, agent,
                    dataSetIdForAnalysis, executeContext.getRequest().getUser());

            // Set response with dashboard data
            DashboardResponse response = new DashboardResponse();
            response.setDashboardData(dashboardData);
            queryResult.setResponse(response);

            // Also set standard query results for fallback rendering
            queryResult.setQueryColumns(queryResp.getColumns());
            queryResult.setQueryResults(queryResp.getResultList());
            queryResult.setQueryState(QueryState.SUCCESS);

            // Generate text summary
            queryResult.setTextResult(generateTextSummary(dashboardData));

            log.info("Dashboard query executed successfully with {} rows",
                    queryResp.getResultList().size());

        } catch (Exception e) {
            log.error("Failed to execute dashboard query", e);
            queryResult.setQueryState(QueryState.SEARCH_EXCEPTION);
            queryResult.setTextResult("Dashboard query failed: " + e.getMessage());
        }

        return queryResult;
    }

    private SemanticQueryResp executeQuery(ExecuteContext executeContext,
            SemanticParseInfo parseInfo, Agent agent) throws Exception {
        SemanticLayerService semanticLayer = ContextUtils.getBean(SemanticLayerService.class);
        Long dataSetId = parseInfo.getDataSetId();

        // Get dashboard config from ChatApp prompt (if configured)
        JSONObject dashboardConfig = getDashboardConfig(agent);

        // Resolve dataSetId: parseInfo > dashboard config > agent default
        if (dataSetId == null && dashboardConfig != null) {
            Long configDataSetId = dashboardConfig.getLong(CONFIG_KEY_DATASET_ID);
            if (configDataSetId != null) {
                dataSetId = configDataSetId;
                log.info("Using dataSetId {} from dashboard config", dataSetId);
            }
        }
        if (dataSetId == null) {
            dataSetId = getDefaultDataSetId(agent);
            if (dataSetId != null) {
                log.info("Using default dataSetId {} from Agent config", dataSetId);
            }
        }

        // Always use struct query for dashboard mode. This ensures:
        // 1. All metrics are included (LLM SQL may omit them)
        // 2. Date filter from parseInfo.dateInfo is always applied (LLM SQL may lack WHERE clause)
        List<String> configuredMetrics =
                dashboardConfig != null && dashboardConfig.getJSONArray(CONFIG_KEY_METRICS) != null
                        ? dashboardConfig.getJSONArray(CONFIG_KEY_METRICS).toJavaList(String.class)
                        : null;
        QueryStructReq structReq =
                buildDefaultStructQuery(dataSetId, configuredMetrics, parseInfo.getDateInfo());
        if (structReq != null) {
            log.info("Using struct query for dashboard: dataSetId={}, dateInfo={}", dataSetId,
                    parseInfo.getDateInfo());
            return semanticLayer.queryByReq(structReq, executeContext.getRequest().getUser());
        }

        // Fallback: use parser SQL or configured defaultSql if struct query cannot be built
        String finalSql = null;
        if (parseInfo.getSqlInfo() != null && parseInfo.getSqlInfo().getCorrectedS2SQL() != null) {
            finalSql = parseInfo.getSqlInfo().getQuerySQL() != null
                    ? parseInfo.getSqlInfo().getQuerySQL()
                    : parseInfo.getSqlInfo().getCorrectedS2SQL();
            log.info("Struct query unavailable, falling back to parser SQL");
        } else if (dashboardConfig != null && dashboardConfig.containsKey(CONFIG_KEY_DEFAULT_SQL)) {
            finalSql = dashboardConfig.getString(CONFIG_KEY_DEFAULT_SQL);
            log.info("Struct query unavailable, falling back to defaultSql from config");
        }

        if (finalSql == null) {
            log.warn("No query method available for dashboard");
            return null;
        }

        QuerySqlReq sqlReq = QuerySqlReq.builder().sql(finalSql).build();
        sqlReq.setDataSetId(dataSetId);
        if (parseInfo.getSqlInfo() != null && parseInfo.getSqlInfo().getCorrectedS2SQL() != null) {
            sqlReq.setSqlInfo(parseInfo.getSqlInfo());
        }
        return semanticLayer.queryByReq(sqlReq, executeContext.getRequest().getUser());
    }

    /**
     * Parse dashboard config from ChatApp prompt field. Expected format: {"dataSetId": 1,
     * "metrics": ["pv", "uv"], "defaultSql": "...", "title": "..."}
     */
    private JSONObject getDashboardConfig(Agent agent) {
        try {
            ChatApp chatApp = agent.getChatAppConfig().get(APP_KEY);
            if (chatApp == null || chatApp.getPrompt() == null || chatApp.getPrompt().isBlank()) {
                return null;
            }
            String prompt = chatApp.getPrompt().trim();
            if (prompt.startsWith("{")) {
                return JSON.parseObject(prompt);
            }
        } catch (Exception e) {
            log.debug("Failed to parse dashboard config from prompt", e);
        }
        return null;
    }

    /**
     * Get default DataSet ID from Agent's configured datasets.
     */
    private Long getDefaultDataSetId(Agent agent) {
        if (agent == null) {
            return null;
        }
        Set<Long> dataSetIds = agent.getDataSetIds();
        if (dataSetIds == null || dataSetIds.isEmpty()) {
            return null;
        }
        // Return the first (or smallest) dataSetId as default
        return dataSetIds.stream().sorted().findFirst().orElse(null);
    }

    /**
     * Build default dashboard query using QueryStructReq (more robust than S2SQL for handling
     * Chinese bizNames). Selects the partition time dimension and top N metrics from the DataSet.
     *
     * @param dataSetId the dataset ID
     * @param configuredMetrics optional list of metric bizNames to use (from dashboard config)
     */
    private QueryStructReq buildDefaultStructQuery(Long dataSetId, List<String> configuredMetrics,
            DateConf parseDateInfo) {
        if (dataSetId == null) {
            return null;
        }

        try {
            SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
            SemanticSchema schema = schemaService.getSemanticSchema(Set.of(dataSetId));

            // Get dimensions and find date column (prefer partition_time, fall back to time)
            List<SchemaElement> dimensions = schema.getDimensions(dataSetId);
            SchemaElement dateColumn = dimensions.stream().filter(SchemaElement::isPartitionTime)
                    .findFirst().orElseGet(() -> dimensions.stream()
                            .filter(SchemaElement::isTimeDimension).findFirst().orElse(null));

            if (dateColumn == null) {
                log.warn("No time dimension found for dataSet {}", dataSetId);
                return null;
            }

            // Get metrics
            List<SchemaElement> allMetrics = schema.getMetrics(dataSetId);
            if (allMetrics.isEmpty()) {
                log.warn("No metrics found for dataSet {}", dataSetId);
                return null;
            }

            // Select metrics: use configured metrics if available, otherwise use first N
            List<SchemaElement> selectedMetrics;
            if (configuredMetrics != null && !configuredMetrics.isEmpty()) {
                // Filter metrics by configured bizNames, preserving order
                selectedMetrics = configuredMetrics.stream()
                        .map(bizName -> allMetrics.stream()
                                .filter(m -> m.getBizName().equals(bizName)).findFirst()
                                .orElse(null))
                        .filter(Objects::nonNull).limit(MAX_KPI_METRICS).toList();

                if (selectedMetrics.isEmpty()) {
                    log.warn("Configured metrics {} not found in dataSet, falling back to default",
                            configuredMetrics);
                    selectedMetrics = allMetrics.stream().limit(MAX_KPI_METRICS).toList();
                } else {
                    log.info("Using {} configured metrics for dashboard", selectedMetrics.size());
                }
            } else {
                selectedMetrics = allMetrics.stream().limit(MAX_KPI_METRICS).toList();
            }

            // Build QueryStructReq - this approach avoids S2SQL translation issues with Chinese
            // bizNames
            QueryStructReq structReq = new QueryStructReq();
            structReq.setDataSetId(dataSetId);

            // Set groups (dimensions) - use bizName
            structReq.setGroups(Collections.singletonList(dateColumn.getBizName()));

            // Set aggregators (metrics) - use bizName with SUM aggregation
            List<Aggregator> aggregators = selectedMetrics.stream().map(metric -> {
                Aggregator agg = new Aggregator();
                agg.setColumn(metric.getBizName());
                // Use SUM as default - system will use metric's defaultAgg if defined
                agg.setFunc(AggOperatorEnum.SUM);
                return agg;
            }).collect(Collectors.toList());
            structReq.setAggregators(aggregators);

            // Set date filter: use parser's date range if available, otherwise last 30 days
            // CRITICAL: dateField must be set for WHERE clause generation
            DateConf dateConf;
            if (parseDateInfo != null && parseDateInfo.getStartDate() != null) {
                dateConf = parseDateInfo;
            } else {
                dateConf = new DateConf();
                dateConf.setDateMode(DateConf.DateMode.RECENT);
                dateConf.setUnit(DEFAULT_QUERY_DAYS);
                dateConf.setPeriod(DatePeriodEnum.DAY);
            }
            dateConf.setDateField(dateColumn.getBizName());
            structReq.setDateInfo(dateConf);

            // Set limit
            structReq.setLimit(DEFAULT_QUERY_DAYS);

            log.info("Built QueryStructReq: groups={}, metrics={}", structReq.getGroups(),
                    selectedMetrics.stream().map(SchemaElement::getBizName).toList());

            return structReq;

        } catch (Exception e) {
            log.error("Failed to build default dashboard query for dataSet {}", dataSetId, e);
            return null;
        }
    }

    private DashboardData buildDashboardData(SemanticQueryResp queryResp, String datasetName,
            Agent agent, Long dataSetId, User user) {
        List<Map<String, Object>> results = queryResp.getResultList();
        var columns = queryResp.getColumns();

        // Find date column and metric columns
        var dateColumn = columns.stream()
                .filter(c -> "DATE".equals(c.getType()) || "DATE".equals(c.getShowType()))
                .findFirst().orElse(null);

        var metricColumns = columns.stream().filter(c -> "NUMBER".equals(c.getShowType()))
                .limit(MAX_KPI_METRICS).toList();

        if (dateColumn == null || metricColumns.isEmpty()) {
            // Fallback: return minimal dashboard without KPIs
            return DashboardData.builder().title(datasetName).kpiMetrics(new ArrayList<>())
                    .trendData(new ArrayList<>()).trendMetrics(new ArrayList<>())
                    .detailColumns(columns.stream()
                            .map(c -> new DetailColumn(c.getName(), c.getBizName(),
                                    c.getShowType() != null ? c.getShowType() : c.getType()))
                            .toList())
                    .detailData(results.stream().limit(MAX_DETAIL_ROWS).toList()).build();
        }

        String dateBizName = dateColumn.getBizName();

        // Sort results by date descending
        List<Map<String, Object>> sortedResults = results.stream().sorted((a, b) -> {
            String dateA = String.valueOf(a.get(dateBizName));
            String dateB = String.valueOf(b.get(dateBizName));
            return dateB.compareTo(dateA);
        }).toList();

        // Get latest and previous data for KPI calculation
        Map<String, Object> latestData = sortedResults.isEmpty() ? null : sortedResults.get(0);
        Map<String, Object> previousData = sortedResults.size() > 1 ? sortedResults.get(1) : null;

        // Build KPI metrics
        List<KpiMetric> kpiMetrics = new ArrayList<>();
        for (var col : metricColumns) {
            double currentValue = parseNumber(latestData, col.getBizName());
            double prevValue = parseNumber(previousData, col.getBizName());
            double trendPercent =
                    prevValue != 0 ? ((currentValue - prevValue) / prevValue) * 100 : 0;

            kpiMetrics.add(KpiMetric.builder().name(col.getName()).bizName(col.getBizName())
                    .value(currentValue).previousValue(prevValue)
                    .trend(trendPercent > 0 ? "up" : trendPercent < 0 ? "down" : "flat")
                    .trendPercent(Math.abs(round(trendPercent, 2))).description(col.getName())
                    .build());
        }

        // Build trend data (last N days)
        List<Map<String, Object>> trendData =
                sortedResults.stream().limit(DEFAULT_TREND_DAYS).sorted((a, b) -> {
                    String dateA = String.valueOf(a.get(dateBizName));
                    String dateB = String.valueOf(b.get(dateBizName));
                    return dateA.compareTo(dateB);
                }).map(row -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", String.valueOf(row.get(dateBizName)));
                    for (var col : metricColumns) {
                        point.put(col.getBizName(), parseNumber(row, col.getBizName()));
                    }
                    return point;
                }).toList();

        // Build detail columns
        List<DetailColumn> detailColumns = columns.stream().map(c -> new DetailColumn(c.getName(),
                c.getBizName(), c.getShowType() != null ? c.getShowType() : c.getType())).toList();

        // Get report date
        String reportDate = latestData != null ? String.valueOf(latestData.get(dateBizName)) : null;

        // Perform analysis on the data
        AnalysisResult analysis = null;
        if (!kpiMetrics.isEmpty() && trendData.size() >= MIN_DATA_POINTS_FOR_ANALYSIS) {
            try {
                // Get previous date for attribution analysis
                String previousDate = null;
                if (trendData.size() >= 2) {
                    // Get second-to-last date (trendData is sorted by date ascending)
                    previousDate = String.valueOf(trendData.get(trendData.size() - 2).get("date"));
                }

                analysis = analyzeData(kpiMetrics, trendData, agent, datasetName, reportDate);

                // Perform attribution analysis for significant changes
                if (dataSetId != null && reportDate != null && previousDate != null) {
                    List<Attribution> attributions = performAttributionAnalysis(agent, dataSetId,
                            kpiMetrics, reportDate, previousDate, user);
                    if (analysis != null && !attributions.isEmpty()) {
                        analysis.setAttributions(attributions);
                    }
                }

                if (analysis != null) {
                    log.info(
                            "Dashboard analysis completed: status={}, anomalies={}, insights={}, attributions={}",
                            analysis.getOverallStatus(), analysis.getAnomalyCount(),
                            analysis.getInsightCount(),
                            analysis.getAttributions() != null ? analysis.getAttributions().size()
                                    : 0);
                }
            } catch (Exception e) {
                log.warn("Failed to perform dashboard analysis", e);
            }
        }

        return DashboardData.builder().title(datasetName).reportDate(reportDate)
                .kpiMetrics(kpiMetrics).trendData(trendData)
                .trendMetrics(metricColumns.stream().map(QueryColumn::getBizName).toList())
                .detailColumns(detailColumns)
                .detailData(sortedResults.stream().limit(MAX_DETAIL_ROWS).toList())
                .analysis(analysis).build();
    }

    private double parseNumber(Map<String, Object> row, String key) {
        if (row == null || !row.containsKey(key)) {
            return 0;
        }
        Object value = row.get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private String generateTextSummary(DashboardData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dashboard Summary");
        if (data.getReportDate() != null) {
            sb.append(" (").append(data.getReportDate()).append(")");
        }
        sb.append("\n\n");

        // Add analysis summary if available
        if (data.getAnalysis() != null && data.getAnalysis().getSummary() != null) {
            sb.append(data.getAnalysis().getSummary()).append("\n\n");
        }

        // KPI details
        for (KpiMetric kpi : data.getKpiMetrics()) {
            sb.append("- ").append(kpi.getName()).append(": ");
            sb.append(formatNumber(kpi.getValue()));
            if (kpi.getTrendPercent() != null && kpi.getTrendPercent() != 0) {
                String arrow = "up".equals(kpi.getTrend()) ? "↑" : "↓";
                sb.append(" (").append(arrow).append(kpi.getTrendPercent()).append("%)");
            }
            sb.append("\n");
        }

        // Add key insights
        if (data.getAnalysis() != null && data.getAnalysis().getInsights() != null) {
            List<Insight> keyInsights = data.getAnalysis().getInsights().stream()
                    .filter(i -> i.getConfidence() >= 0.8).limit(3).toList();
            if (!keyInsights.isEmpty()) {
                sb.append("\n💡 关键洞察:\n");
                for (Insight insight : keyInsights) {
                    sb.append("  • ").append(insight.getTitle()).append("\n");
                }
            }
        }

        // Add anomaly warnings
        if (data.getAnalysis() != null && data.getAnalysis().getAnomalies() != null
                && !data.getAnalysis().getAnomalies().isEmpty()) {
            sb.append("\n⚠️ 异常提醒: 检测到 ").append(data.getAnalysis().getAnomalyCount())
                    .append(" 个异常数据点\n");
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

    // ==================== Analysis Methods ====================

    /**
     * Perform analysis on dashboard data: anomaly detection + trend analysis
     */
    private AnalysisResult analyzeData(List<KpiMetric> kpiMetrics,
            List<Map<String, Object>> trendData, Agent agent, String datasetName,
            String reportDate) {
        List<Anomaly> anomalies = new ArrayList<>();
        List<Insight> insights = new ArrayList<>();

        for (KpiMetric metric : kpiMetrics) {
            String bizName = metric.getBizName();

            // Extract time series for this metric
            List<Double> values = trendData.stream().map(row -> parseNumber(row, bizName))
                    .collect(Collectors.toList());

            List<String> dates = trendData.stream().map(row -> String.valueOf(row.get("date")))
                    .collect(Collectors.toList());

            if (values.size() >= MIN_DATA_POINTS_FOR_ANALYSIS) {
                // Detect anomalies
                List<Anomaly> metricAnomalies = detectAnomalies(metric, values, dates);
                anomalies.addAll(metricAnomalies);

                // Analyze trend
                Insight trendInsight = analyzeTrend(metric, values);
                if (trendInsight != null) {
                    insights.add(trendInsight);
                }

                // Check for consecutive changes
                Insight consecutiveInsight = detectConsecutiveChange(metric, values);
                if (consecutiveInsight != null) {
                    insights.add(consecutiveInsight);
                }

                // Check significant change
                if (Math.abs(metric.getTrendPercent()) >= SIGNIFICANT_CHANGE_PERCENT) {
                    insights.add(Insight.builder().type("THRESHOLD").metricName(metric.getName())
                            .metricBizName(metric.getBizName())
                            .title(metric.getName() + "显著"
                                    + ("up".equals(metric.getTrend()) ? "上升" : "下降"))
                            .description(String.format("%s 较前日%s %.1f%%，超过 %.0f%% 阈值",
                                    metric.getName(), "up".equals(metric.getTrend()) ? "上升" : "下降",
                                    metric.getTrendPercent(), SIGNIFICANT_CHANGE_PERCENT))
                            .confidence(0.9).build());
                }
            }
        }

        // Determine overall status
        String overallStatus = determineOverallStatus(anomalies, insights, kpiMetrics);

        // Generate rule-based summary
        String summary = generateAnalysisSummary(kpiMetrics, anomalies, insights, overallStatus);

        // Generate LLM insight if enabled
        String llmInsight =
                generateLLMInsight(agent, datasetName, reportDate, kpiMetrics, anomalies, insights);

        return AnalysisResult.builder().summary(summary).llmInsight(llmInsight).anomalies(anomalies)
                .insights(insights).overallStatus(overallStatus).anomalyCount(anomalies.size())
                .insightCount(insights.size()).build();
    }

    /**
     * Detect anomalies using IQR (Interquartile Range) method
     */
    private List<Anomaly> detectAnomalies(KpiMetric metric, List<Double> values,
            List<String> dates) {
        List<Anomaly> anomalies = new ArrayList<>();

        if (values.size() < MIN_DATA_POINTS_FOR_ANALYSIS) {
            return anomalies;
        }

        // Calculate statistics
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = calculateStdDev(values, mean);

        // Skip if no variation
        if (stdDev == 0) {
            return anomalies;
        }

        // Check each point for anomaly (using z-score)
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            double zScore = Math.abs((value - mean) / stdDev);

            if (zScore >= ANOMALY_THRESHOLD_SIGMA) {
                double deviationPercent = ((value - mean) / mean) * 100;
                String severity = zScore >= 3.0 ? "HIGH" : (zScore >= 2.5 ? "MEDIUM" : "LOW");

                anomalies.add(Anomaly.builder().metricName(metric.getName())
                        .metricBizName(metric.getBizName()).date(dates.get(i)).value(value)
                        .expectedValue(round(mean, 2)).deviationPercent(round(deviationPercent, 1))
                        .severity(severity)
                        .description(String.format("%s 在 %s 的值 %s 偏离均值 %.1f%%", metric.getName(),
                                dates.get(i), formatNumber(value), Math.abs(deviationPercent)))
                        .build());
            }
        }

        return anomalies;
    }

    /**
     * Analyze trend using linear regression
     */
    private Insight analyzeTrend(KpiMetric metric, List<Double> values) {
        if (values.size() < MIN_DATA_POINTS_FOR_ANALYSIS) {
            return null;
        }

        // Simple linear regression: y = ax + b
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double meanY = sumY / n;

        // Calculate R-squared for confidence
        double ssRes = 0, ssTot = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * i + (sumY - slope * sumX) / n;
            ssRes += Math.pow(values.get(i) - predicted, 2);
            ssTot += Math.pow(values.get(i) - meanY, 2);
        }
        double rSquared = ssTot > 0 ? 1 - (ssRes / ssTot) : 0;

        // Determine trend direction and strength
        double changePercent = meanY != 0 ? (slope * (n - 1) / meanY) * 100 : 0;

        // Only report if trend is significant (R² > 0.5 and change > 5%)
        if (rSquared > 0.5 && Math.abs(changePercent) > 5) {
            String trendDir = slope > 0 ? "上升" : "下降";
            String trendStrength = rSquared > 0.8 ? "明显" : "轻微";

            return Insight.builder().type("TREND").metricName(metric.getName())
                    .metricBizName(metric.getBizName())
                    .title(metric.getName() + trendStrength + trendDir + "趋势")
                    .description(String.format("%s 近 %d 天呈%s%s趋势，累计变化约 %.1f%%", metric.getName(), n,
                            trendStrength, trendDir, Math.abs(changePercent)))
                    .confidence(round(rSquared, 2)).build();
        }

        return null;
    }

    /**
     * Detect consecutive increases or decreases
     */
    private Insight detectConsecutiveChange(KpiMetric metric, List<Double> values) {
        if (values.size() < 3) {
            return null;
        }

        int consecutiveUp = 0;
        int consecutiveDown = 0;

        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) > values.get(i - 1)) {
                consecutiveUp++;
                consecutiveDown = 0;
            } else if (values.get(i) < values.get(i - 1)) {
                consecutiveDown++;
                consecutiveUp = 0;
            } else {
                consecutiveUp = 0;
                consecutiveDown = 0;
            }
        }

        // Report if 3+ consecutive days of same direction
        if (consecutiveUp >= 3) {
            return Insight.builder().type("CONSECUTIVE").metricName(metric.getName())
                    .metricBizName(metric.getBizName()).title(metric.getName() + "连续上涨")
                    .description(String.format("%s 已连续 %d 天上涨", metric.getName(), consecutiveUp))
                    .confidence(0.85).build();
        } else if (consecutiveDown >= 3) {
            return Insight.builder().type("CONSECUTIVE").metricName(metric.getName())
                    .metricBizName(metric.getBizName()).title(metric.getName() + "连续下跌")
                    .description(
                            String.format("%s 已连续 %d 天下跌，需关注", metric.getName(), consecutiveDown))
                    .confidence(0.85).build();
        }

        return null;
    }

    private double calculateStdDev(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0;
        }
        double sumSquaredDiff = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum();
        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
    }

    private String determineOverallStatus(List<Anomaly> anomalies, List<Insight> insights,
            List<KpiMetric> kpiMetrics) {
        // Check for high severity anomalies
        boolean hasHighAnomaly = anomalies.stream().anyMatch(a -> "HIGH".equals(a.getSeverity()));

        // Check for significant negative trends
        long negativeInsights = insights.stream()
                .filter(i -> i.getDescription().contains("下降") || i.getDescription().contains("下跌"))
                .count();

        // Check KPI trends
        long decliningKpis = kpiMetrics.stream().filter(k -> "down".equals(k.getTrend())
                && k.getTrendPercent() > SIGNIFICANT_CHANGE_PERCENT).count();

        if (hasHighAnomaly || decliningKpis >= 2) {
            return "CRITICAL";
        } else if (!anomalies.isEmpty() || negativeInsights > 0 || decliningKpis >= 1) {
            return "WARNING";
        }
        return "GOOD";
    }

    private String generateAnalysisSummary(List<KpiMetric> kpiMetrics, List<Anomaly> anomalies,
            List<Insight> insights, String overallStatus) {
        StringBuilder sb = new StringBuilder();

        // Overall status
        switch (overallStatus) {
            case "GOOD":
                sb.append("📊 整体表现良好。");
                break;
            case "WARNING":
                sb.append("⚠️ 部分指标需要关注。");
                break;
            case "CRITICAL":
                sb.append("🚨 存在异常情况，需要重点关注。");
                break;
        }

        // KPI summary
        long upCount = kpiMetrics.stream().filter(k -> "up".equals(k.getTrend())).count();
        long downCount = kpiMetrics.stream().filter(k -> "down".equals(k.getTrend())).count();
        if (upCount > 0 || downCount > 0) {
            sb.append(String.format(" %d 项指标上升，%d 项下降。", upCount, downCount));
        }

        // Highlight anomalies
        if (!anomalies.isEmpty()) {
            sb.append(String.format(" 检测到 %d 个异常点。", anomalies.size()));
        }

        // Key insights
        List<Insight> keyInsights = insights.stream()
                .filter(i -> "CONSECUTIVE".equals(i.getType()) || "THRESHOLD".equals(i.getType()))
                .limit(2).toList();

        for (Insight insight : keyInsights) {
            sb.append(" ").append(insight.getDescription());
        }

        return sb.toString();
    }

    /**
     * Perform attribution analysis for metrics with significant changes. This drills down by
     * dimensions to identify which values contributed most to the change.
     */
    private List<Attribution> performAttributionAnalysis(Agent agent, Long dataSetId,
            List<KpiMetric> kpiMetrics, String currentDate, String previousDate, User user) {
        List<Attribution> attributions = new ArrayList<>();

        // Check if attribution is enabled
        ChatApp chatApp = agent.getChatAppConfig().get(ATTRIBUTION_KEY);
        if (chatApp == null || !chatApp.isEnable()) {
            return attributions;
        }

        // Find metrics with significant change
        List<KpiMetric> significantMetrics = kpiMetrics.stream()
                .filter(m -> Math.abs(m.getTrendPercent()) >= SIGNIFICANT_CHANGE_PERCENT).toList();

        if (significantMetrics.isEmpty() || dataSetId == null) {
            return attributions;
        }

        try {
            SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
            SemanticLayerService semanticLayer = ContextUtils.getBean(SemanticLayerService.class);
            SemanticSchema schema = schemaService.getSemanticSchema(Set.of(dataSetId));

            // Get dimensions for drill-down (exclude partition time)
            List<SchemaElement> dimensions = schema.getDimensions(dataSetId).stream()
                    .filter(d -> !d.isPartitionTime()).limit(MAX_ATTRIBUTION_DIMENSIONS).toList();

            // Get date column
            SchemaElement dateColumn = schema.getDimensions(dataSetId).stream()
                    .filter(SchemaElement::isPartitionTime).findFirst().orElse(null);

            if (dateColumn == null || dimensions.isEmpty()) {
                log.warn("Cannot perform attribution: missing date column or dimensions");
                return attributions;
            }

            // Get dataset name for S2SQL table reference
            Map<Long, String> dataSetNames = schema.getDataSetIdToName();
            String dataSetName = dataSetNames.getOrDefault(dataSetId, "");
            if (dataSetName.isEmpty()) {
                log.warn("Cannot perform attribution: dataset name not found for id {}", dataSetId);
                return attributions;
            }

            // For each significant metric, drill down by each dimension
            for (KpiMetric metric : significantMetrics) {
                double totalChange = metric.getValue() - metric.getPreviousValue();
                if (totalChange == 0) {
                    continue;
                }

                for (SchemaElement dim : dimensions) {
                    List<Attribution> dimAttributions =
                            queryDimensionAttribution(semanticLayer, dataSetId, dataSetName, metric,
                                    dim, dateColumn, currentDate, previousDate, totalChange, user);
                    attributions.addAll(dimAttributions);
                }
            }

            // Sort by contribution and limit
            attributions = attributions.stream()
                    .filter(a -> Math.abs(a.getContribution()) >= MIN_CONTRIBUTION_PERCENT)
                    .sorted((a, b) -> Double.compare(Math.abs(b.getContribution()),
                            Math.abs(a.getContribution())))
                    .limit(MAX_ATTRIBUTION_DIMENSIONS * MAX_ATTRIBUTION_VALUES).toList();

            log.info("Attribution analysis found {} contributors", attributions.size());

        } catch (Exception e) {
            log.warn("Attribution analysis failed", e);
        }

        return attributions;
    }

    /**
     * Query dimension values and calculate their contribution to metric change.
     */
    private List<Attribution> queryDimensionAttribution(SemanticLayerService semanticLayer,
            Long dataSetId, String dataSetName, KpiMetric metric, SchemaElement dimension,
            SchemaElement dateColumn, String currentDate, String previousDate, double totalChange,
            User user) {
        List<Attribution> result = new ArrayList<>();

        try {
            // Build S2SQL for current period using dataset name as table
            String sql = String.format(
                    "SELECT %s, %s FROM %s WHERE %s = '%s' GROUP BY %s ORDER BY %s DESC LIMIT %d",
                    dimension.getBizName(), metric.getBizName(), dataSetName,
                    dateColumn.getBizName(), currentDate, dimension.getBizName(),
                    metric.getBizName(), MAX_ATTRIBUTION_VALUES);

            QuerySqlReq currentReq = QuerySqlReq.builder().sql(sql).build();
            currentReq.setDataSetId(dataSetId);
            SemanticQueryResp currentResp = semanticLayer.queryByReq(currentReq, user);

            // Build S2SQL for previous period
            String prevSql = String.format(
                    "SELECT %s, %s FROM %s WHERE %s = '%s' GROUP BY %s ORDER BY %s DESC LIMIT %d",
                    dimension.getBizName(), metric.getBizName(), dataSetName,
                    dateColumn.getBizName(), previousDate, dimension.getBizName(),
                    metric.getBizName(), MAX_ATTRIBUTION_VALUES);

            QuerySqlReq prevReq = QuerySqlReq.builder().sql(prevSql).build();
            prevReq.setDataSetId(dataSetId);
            SemanticQueryResp prevResp = semanticLayer.queryByReq(prevReq, user);

            // Build map of previous values
            Map<String, Double> prevValues = new LinkedHashMap<>();
            if (prevResp != null && prevResp.getResultList() != null) {
                for (Map<String, Object> row : prevResp.getResultList()) {
                    String dimValue = String.valueOf(row.get(dimension.getBizName()));
                    double value = parseNumber(row, metric.getBizName());
                    prevValues.put(dimValue, value);
                }
            }

            // Calculate attribution for each dimension value
            if (currentResp != null && currentResp.getResultList() != null) {
                for (Map<String, Object> row : currentResp.getResultList()) {
                    String dimValue = String.valueOf(row.get(dimension.getBizName()));
                    double currentValue = parseNumber(row, metric.getBizName());
                    double prevValue = prevValues.getOrDefault(dimValue, 0.0);
                    double change = currentValue - prevValue;
                    double changePercent = prevValue != 0 ? (change / prevValue) * 100 : 0;
                    double contribution = totalChange != 0 ? (change / totalChange) * 100 : 0;

                    if (Math.abs(contribution) >= MIN_CONTRIBUTION_PERCENT) {
                        result.add(Attribution.builder().metricName(metric.getName())
                                .metricBizName(metric.getBizName())
                                .dimensionName(dimension.getName())
                                .dimensionBizName(dimension.getBizName()).dimensionValue(dimValue)
                                .currentValue(round(currentValue, 2))
                                .previousValue(round(prevValue, 2)).change(round(change, 2))
                                .changePercent(round(changePercent, 1))
                                .contribution(round(contribution, 1))
                                .direction(change >= 0 ? "INCREASE" : "DECREASE").build());
                    }
                }
            }

            // Also check for values that disappeared (present in prev but not in current)
            if (prevResp != null && prevResp.getResultList() != null) {
                Set<String> currentDimValues = result.stream().map(Attribution::getDimensionValue)
                        .collect(Collectors.toSet());

                for (Map.Entry<String, Double> entry : prevValues.entrySet()) {
                    if (!currentDimValues.contains(entry.getKey())) {
                        double prevValue = entry.getValue();
                        double change = -prevValue;
                        double contribution = totalChange != 0 ? (change / totalChange) * 100 : 0;

                        if (Math.abs(contribution) >= MIN_CONTRIBUTION_PERCENT) {
                            result.add(Attribution.builder().metricName(metric.getName())
                                    .metricBizName(metric.getBizName())
                                    .dimensionName(dimension.getName())
                                    .dimensionBizName(dimension.getBizName())
                                    .dimensionValue(entry.getKey()).currentValue(0)
                                    .previousValue(round(prevValue, 2)).change(round(change, 2))
                                    .changePercent(-100.0).contribution(round(contribution, 1))
                                    .direction("DECREASE").build());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.debug("Failed to query dimension attribution for {} by {}", metric.getBizName(),
                    dimension.getBizName(), e);
        }

        return result;
    }

    /**
     * Generate LLM-powered insight for the dashboard data.
     */
    private String generateLLMInsight(Agent agent, String datasetName, String reportDate,
            List<KpiMetric> kpiMetrics, List<Anomaly> anomalies, List<Insight> insights) {
        try {
            ChatApp chatApp = agent.getChatAppConfig().get(LLM_INSIGHT_KEY);
            if (chatApp == null || !chatApp.isEnable()) {
                return null;
            }

            // Build KPI summary
            StringBuilder kpiSummary = new StringBuilder();
            for (KpiMetric kpi : kpiMetrics) {
                kpiSummary.append(
                        String.format("- %s: %s", kpi.getName(), formatNumber(kpi.getValue())));
                if (kpi.getTrendPercent() != null && kpi.getTrendPercent() != 0) {
                    String direction = "up".equals(kpi.getTrend()) ? "上升" : "下降";
                    kpiSummary.append(
                            String.format(" (%s %.1f%%)", direction, kpi.getTrendPercent()));
                }
                kpiSummary.append("\n");
            }

            // Build anomaly summary
            StringBuilder anomalySummary = new StringBuilder();
            if (anomalies.isEmpty()) {
                anomalySummary.append("无异常检测到");
            } else {
                for (Anomaly a : anomalies) {
                    anomalySummary.append(
                            String.format("- [%s] %s\n", a.getSeverity(), a.getDescription()));
                }
            }

            // Build insight summary
            StringBuilder insightSummary = new StringBuilder();
            if (insights.isEmpty()) {
                insightSummary.append("暂无明显趋势");
            } else {
                for (Insight i : insights) {
                    insightSummary.append(String.format("- %s\n", i.getDescription()));
                }
            }

            // Build prompt with variables
            String prompt = chatApp.getPrompt()
                    .replace("{{reportDate}}", reportDate != null ? reportDate : "未知")
                    .replace("{{datasetName}}", datasetName != null ? datasetName : "运营数据")
                    .replace("{{kpiSummary}}", kpiSummary.toString())
                    .replace("{{anomalies}}", anomalySummary.toString())
                    .replace("{{insights}}", insightSummary.toString());

            // Get ChatModel and generate
            ChatLanguageModel chatLanguageModel =
                    ModelProvider.getChatModel(ModelConfigHelper.getChatModelConfig(chatApp));
            if (chatLanguageModel == null) {
                log.warn("No ChatModel available for LLM insight generation");
                return null;
            }

            Response<AiMessage> response = chatLanguageModel
                    .generate(dev.langchain4j.data.message.UserMessage.from(prompt));
            String llmInsight = response.content().text();

            log.info("LLM insight generated successfully, length={}", llmInsight.length());
            return llmInsight;

        } catch (Exception e) {
            log.warn("Failed to generate LLM insight", e);
            return null;
        }
    }

    // Inner classes for dashboard data structure

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
        // Analysis results
        private AnalysisResult analysis;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        private String summary; // Overall analysis summary (rule-based)
        private String llmInsight; // LLM-generated analysis insight
        private List<Anomaly> anomalies; // Detected anomalies
        private List<Insight> insights; // Auto-discovered insights
        private List<Attribution> attributions; // Attribution analysis results
        private String overallStatus; // GOOD / WARNING / CRITICAL
        private int anomalyCount;
        private int insightCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Anomaly {
        private String metricName;
        private String metricBizName;
        private String date;
        private double value;
        private double expectedValue; // Mean or median
        private double deviationPercent;
        private String severity; // HIGH / MEDIUM / LOW
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Insight {
        private String type; // TREND / VOLATILITY / THRESHOLD / CONSECUTIVE
        private String metricName;
        private String metricBizName;
        private String title;
        private String description;
        private double confidence; // 0.0 - 1.0
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attribution {
        private String metricName;
        private String metricBizName;
        private String dimensionName; // e.g., "渠道"
        private String dimensionBizName; // e.g., "channel"
        private String dimensionValue; // e.g., "iOS"
        private double currentValue; // Value in current period
        private double previousValue; // Value in previous period
        private double change; // Absolute change
        private double changePercent; // Percentage change
        private double contribution; // Contribution to total change (%)
        private String direction; // INCREASE / DECREASE
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
        private String trend; // up, down, flat
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private DashboardData dashboardData;
    }
}
