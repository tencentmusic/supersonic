package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.exception.ParamValidationException;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.service.ReportDeliveryService;
import com.tencent.supersonic.headless.api.service.SchemaService;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.ExecutionSnapshotData;
import com.tencent.supersonic.headless.server.pojo.OutputFormat;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.service.DataSetAuthService;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportExecutionOrchestrator {

    /** Aligned with {@code SemanticTemplateServiceImpl} STATUS_OFFLINE */
    private static final int TEMPLATE_STATUS_OFFLINE = 2;

    private final ReportExecutionMapper executionMapper;
    private final SemanticLayerService semanticLayerService;
    private final SemanticTemplateService templateService;
    private final QueryConfigParser queryConfigParser;
    private final UserService userService;
    private final SchemaService schemaService;
    private final DataSetAuthService dataSetAuthService;
    private final SensitiveLevelConfig sensitiveLevelConfig;

    @Autowired(required = false)
    private ReportDeliveryService deliveryService;

    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    @Value("${supersonic.export.local-dir:${java.io.tmpdir}/supersonic-export}")
    private String exportDir;

    public ReportExecutionOrchestrator(ReportExecutionMapper executionMapper,
            SemanticLayerService semanticLayerService, SemanticTemplateService templateService,
            QueryConfigParser queryConfigParser, UserService userService,
            SchemaService schemaService, DataSetAuthService dataSetAuthService,
            SensitiveLevelConfig sensitiveLevelConfig) {
        this.executionMapper = executionMapper;
        this.semanticLayerService = semanticLayerService;
        this.templateService = templateService;
        this.queryConfigParser = queryConfigParser;
        this.userService = userService;
        this.schemaService = schemaService;
        this.dataSetAuthService = dataSetAuthService;
        this.sensitiveLevelConfig = sensitiveLevelConfig;
    }

    public void execute(ReportExecutionContext ctx) {
        Timer.Sample executionSample = reportMetrics != null ? reportMetrics.startTimer() : null;
        String source = ctx.getSource() != null ? ctx.getSource().name().toLowerCase() : "unknown";
        Date startTime = new Date();
        ReportExecutionDO execution = new ReportExecutionDO();
        execution.setScheduleId(ctx.getScheduleId());
        execution.setStatus("RUNNING");
        execution.setStartTime(startTime);
        execution.setTenantId(ctx.getTenantId());
        execution.setTemplateVersion(ctx.getTemplateVersion());
        execution.setExecutionSnapshot(JsonUtil.toString(new ExecutionSnapshotData(ctx, null)));
        executionMapper.insert(execution);

        try {
            // Step 1: Validate template status
            validateTemplate(ctx);

            // Step 2: Validate params
            validateParams(ctx);

            // Step 3: Permission injection handled by S2DataPermissionAspect (AOP)

            // Step 4: Build user context for query execution
            User user = buildUserContext(ctx);

            // Step 5: Parse query config and execute
            long queryStart = System.currentTimeMillis();
            SemanticQueryReq queryReq = parseQueryConfig(ctx);
            expandDetailFields(queryReq, user);
            SemanticQueryResp queryResp = semanticLayerService.queryByReq(queryReq, user);
            long executionTimeMs = System.currentTimeMillis() - queryStart;

            log.info("Report executed for dataset={}, source={}, rows={}, timeMs={}",
                    ctx.getDatasetId(), ctx.getSource(),
                    queryResp.getResultList() != null ? queryResp.getResultList().size() : 0,
                    executionTimeMs);

            // Step 6: Generate output file if configured
            String resultLocation = null;
            if (ctx.getOutputConfig() != null && ctx.getOutputConfig().getFormat() != null) {
                resultLocation = generateOutputFile(ctx, queryResp);
            }

            // Step 7: Persist execution result
            long rowCount =
                    queryResp.getResultList() != null ? (long) queryResp.getResultList().size()
                            : 0L;
            execution.setStatus("SUCCESS");
            execution.setEndTime(new Date());
            execution.setExecutionTimeMs(executionTimeMs);
            execution.setSqlHash(computeSqlHash(queryResp.getSql()));
            execution.setRowCount(rowCount);
            execution.setResultLocation(resultLocation);

            // Update snapshot: store rendered SQL + result preview for audit replay (P2)
            List<Map<String, Object>> previewRows = buildResultPreview(queryResp.getResultList());
            ExecutionSnapshotData finalSnapshot = new ExecutionSnapshotData(ctx, previewRows);
            finalSnapshot.setRenderedSql(queryResp.getSql());
            execution.setExecutionSnapshot(JsonUtil.toString(finalSnapshot));

            executionMapper.updateById(execution);

            // Step 8: Deliver output to configured channels
            deliverOutput(ctx, execution.getId(), resultLocation, rowCount);
            if (reportMetrics != null && executionSample != null) {
                reportMetrics.recordExecution("success", source, executionSample);
            }

        } catch (Exception e) {
            log.error("Report execution failed for schedule={}", ctx.getScheduleId(), e);
            execution.setStatus("FAILED");
            execution.setEndTime(new Date());
            execution.setErrorMessage(truncate(e.getMessage(), 2000));
            executionMapper.updateById(execution);
            if (reportMetrics != null && executionSample != null) {
                reportMetrics.recordExecution("error", source, executionSample);
            }
            throw new RuntimeException("Report execution failed: " + e.getMessage(), e);
        }
    }

    private User buildUserContext(ReportExecutionContext ctx) {
        if (ctx.getOperatorUserId() == null) {
            throw new IllegalStateException("Schedule has no ownerId, cannot execute report");
        }
        User user = userService.getUserById(ctx.getOperatorUserId());
        if (user == null) {
            throw new IllegalStateException("Owner user not found for id=" + ctx.getOperatorUserId()
                    + ", cannot execute report");
        }
        if (user.getTenantId() != null && !user.getTenantId().equals(ctx.getTenantId())) {
            throw new IllegalStateException(
                    "Owner tenantId=" + user.getTenantId() + " does not match schedule tenantId="
                            + ctx.getTenantId() + " for ownerId=" + ctx.getOperatorUserId());
        }
        user.setTenantId(ctx.getTenantId());
        return user;
    }

    private SemanticQueryReq parseQueryConfig(ReportExecutionContext ctx) {
        return queryConfigParser.parse(ctx.getQueryConfig(), ctx.getDatasetId(),
                ctx.getResolvedParams());
    }

    /**
     * 明细模式下若用户未显式选择投影列，按 owner 的字段权限展开成"全部可见维度+指标"。
     *
     * <p>
     * 必要性：{@code SqlGenerateUtils.getSelect()} 在 groups/aggregators 都为空时输出字面量 {@code "*"}，但
     * {@code S2DataPermissionAspect.checkColPermission()} 通过
     * {@code QueryStructUtils.getBizNameFromStruct()} 判断查询里的字段，该方法只看
     * groups/aggregators/orders/filters 抽出的字段名 —— 它感知不到 {@code SELECT *}，因此高敏感字段 会被绕过列级权限检查。
     *
     * <p>
     * 这里在请求送入 semanticLayerService 前，把 owner 在该 dataset 上可见的维度+指标 bizName 列表显式填进 groups。
     * {@code SqlGenerateUtils.getGroupBy()} 会因 queryType=DETAIL 跳过 GROUP BY，所以等价于带权限过滤的"全字段明细"。
     */
    private void expandDetailFields(SemanticQueryReq queryReq, User user) {
        if (!(queryReq instanceof QueryStructReq req)) {
            return;
        }
        if (!QueryType.DETAIL.equals(req.getQueryType())) {
            return;
        }
        if (req.getGroups() != null && !req.getGroups().isEmpty()) {
            return;
        }
        // 失败必须 fail-closed：dataSetId/schema 缺失时若静默放行，下游会发出
        // 字面量 SELECT *，而 S2DataPermissionAspect 仅扫描 groups/aggregators/orders/filters，
        // 看不到 SELECT *，HIGH 字段会被绕过授权检查。
        if (req.getDataSetId() == null) {
            throw new IllegalStateException("明细模式缺少 dataSetId，拒绝执行明细导出");
        }

        SchemaFilterReq filter = new SchemaFilterReq();
        filter.setDataSetId(req.getDataSetId());
        SemanticSchemaResp schema = schemaService.fetchSemanticSchema(filter);
        if (schema == null) {
            throw new IllegalStateException("无法获取数据集 schema, 拒绝执行明细导出: " + req.getDataSetId());
        }

        boolean isAdmin = user.isSuperAdmin()
                || dataSetAuthService.checkDataSetAdminPermission(req.getDataSetId(), user);
        Set<String> authedNames = isAdmin ? null : fetchAuthedColumns(req.getDataSetId(), user);
        boolean includeMid =
                sensitiveLevelConfig != null && sensitiveLevelConfig.isMidLevelRequireAuth();

        Set<String> visible = new LinkedHashSet<>();
        if (schema.getDimensions() != null) {
            for (DimSchemaResp dim : schema.getDimensions()) {
                if (StringUtils.isBlank(dim.getBizName())) {
                    continue;
                }
                if (isFieldVisible(dim.getSensitiveLevel(), dim.getBizName(), authedNames,
                        includeMid)) {
                    visible.add(dim.getBizName());
                }
            }
        }
        if (schema.getMetrics() != null) {
            for (MetricSchemaResp metric : schema.getMetrics()) {
                if (StringUtils.isBlank(metric.getBizName())) {
                    continue;
                }
                if (isFieldVisible(metric.getSensitiveLevel(), metric.getBizName(), authedNames,
                        includeMid)) {
                    visible.add(metric.getBizName());
                }
            }
        }

        if (visible.isEmpty()) {
            throw new IllegalStateException("当前用户在该数据集下无可见字段，无法导出明细：" + req.getDataSetId());
        }

        log.info("Expanded DETAIL query for dataset={} owner={} into {} fields", req.getDataSetId(),
                user.getName(), visible.size());
        req.setGroups(new ArrayList<>(visible));
    }

    private Set<String> fetchAuthedColumns(Long dataSetId, User user) {
        try {
            AuthorizedResourceResp authResp =
                    dataSetAuthService.queryAuthorizedResources(dataSetId, user);
            if (authResp == null || authResp.getAuthResList() == null) {
                return Set.of();
            }
            return authResp.getAuthResList().stream().map(AuthRes::getName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to query column permissions for dataset={}, user={}: {}", dataSetId,
                    user.getName(), e.getMessage());
            return Set.of();
        }
    }

    /**
     * 与 {@code S2DataPermissionAspect.isRestrictedSensitiveLevel} 保持一致：HIGH 始终需要授权， MID 根据
     * {@code sensitiveLevelConfig.midLevelRequireAuth} 决定是否需要授权。
     */
    private boolean isFieldVisible(Integer sensitiveLevel, String bizName, Set<String> authedNames,
            boolean includeMid) {
        if (authedNames == null) {
            return true;
        }
        if (sensitiveLevel == null) {
            return true;
        }
        boolean restricted = SensitiveLevelEnum.HIGH.getCode().equals(sensitiveLevel)
                || (includeMid && SensitiveLevelEnum.MID.getCode().equals(sensitiveLevel));
        if (!restricted) {
            return true;
        }
        return authedNames.contains(bizName);
    }

    private String generateOutputFile(ReportExecutionContext ctx, SemanticQueryResp queryResp)
            throws Exception {
        OutputFormat format = ctx.getOutputConfig().getFormat();
        String timestamp = DateUtils.format(new Date(), "yyyyMMddHHmmss");
        String fileName = String.format("report_%d_%s.%s", ctx.getScheduleId(), timestamp,
                format == OutputFormat.CSV ? "csv" : "xlsx");

        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outputFile = new File(dir, fileName);

        if (format == OutputFormat.CSV) {
            writeCsv(outputFile, queryResp);
        } else {
            writeExcel(outputFile, queryResp);
        }

        return outputFile.getAbsolutePath();
    }

    private void writeExcel(File file, SemanticQueryResp queryResp) {
        List<List<String>> headers = buildHeaders(queryResp.getColumns());
        List<List<String>> data = buildData(queryResp);
        EasyExcel.write(file).sheet("Sheet1").head(headers).doWrite(data);
    }

    private void writeCsv(File file, SemanticQueryResp queryResp) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write header
            List<QueryColumn> columns = queryResp.getColumns();
            String header =
                    columns.stream().map(QueryColumn::getName).collect(Collectors.joining(","));
            writer.write(header);
            writer.newLine();

            // Write data
            if (queryResp.getResultList() != null) {
                for (Map<String, Object> row : queryResp.getResultList()) {
                    String line = columns.stream().map(col -> escapeCsv(row.get(col.getBizName())))
                            .collect(Collectors.joining(","));
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    private List<List<String>> buildHeaders(List<QueryColumn> columns) {
        List<List<String>> headers = new ArrayList<>();
        for (QueryColumn col : columns) {
            List<String> header = new ArrayList<>();
            header.add(col.getName());
            headers.add(header);
        }
        return headers;
    }

    private List<List<String>> buildData(SemanticQueryResp queryResp) {
        List<List<String>> data = new ArrayList<>();
        if (queryResp.getResultList() == null) {
            return data;
        }
        List<QueryColumn> columns = queryResp.getColumns();
        for (Map<String, Object> row : queryResp.getResultList()) {
            List<String> rowData = new ArrayList<>();
            for (QueryColumn col : columns) {
                Object value = row.get(col.getBizName());
                rowData.add(value != null ? String.valueOf(value) : "");
            }
            data.add(rowData);
        }
        return data;
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String str = String.valueOf(value);
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private void validateTemplate(ReportExecutionContext ctx) {
        if (ctx.getDatasetId() == null) {
            throw new IllegalArgumentException("datasetId is required");
        }
        if (ctx.getTemplateId() == null) {
            return;
        }
        User systemUser = new User();
        systemUser.setId(0L);
        systemUser.setName("system");
        systemUser.setTenantId(ctx.getTenantId());
        SemanticTemplate template =
                templateService.getTemplateById(ctx.getTemplateId(), systemUser);
        if (template == null) {
            throw new IllegalStateException(
                    "Template not found for id=" + ctx.getTemplateId() + ", cannot execute report");
        }
        if (Integer.valueOf(TEMPLATE_STATUS_OFFLINE).equals(template.getStatus())) {
            throw new IllegalStateException(
                    "Template is offline (id=" + ctx.getTemplateId() + "), execution skipped");
        }
    }

    private void deliverOutput(ReportExecutionContext ctx, Long executionId, String fileLocation,
            long rowCount) {
        if (deliveryService == null) {
            log.debug("Delivery service not available, skipping delivery");
            return;
        }

        if (ctx.getDeliveryConfigIds() == null || ctx.getDeliveryConfigIds().isEmpty()) {
            log.debug("No delivery configs specified, skipping delivery");
            return;
        }

        try {
            String executionTime = DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
            String outputFormat =
                    ctx.getOutputConfig() != null && ctx.getOutputConfig().getFormat() != null
                            ? ctx.getOutputConfig().getFormat().name()
                            : "XLSX";

            String scheduleName = ctx.getScheduleName() != null ? ctx.getScheduleName()
                    : "Schedule " + ctx.getScheduleId();
            DeliveryContext deliveryContext = DeliveryContext.builder()
                    .scheduleId(ctx.getScheduleId()).executionId(executionId)
                    .scheduleName(scheduleName).reportName(scheduleName).fileLocation(fileLocation)
                    .outputFormat(outputFormat).rowCount(rowCount).tenantId(ctx.getTenantId())
                    .executionTime(executionTime).build();

            deliveryService.deliver(ctx.getDeliveryConfigIds(), deliveryContext);

            log.info("Delivery completed for schedule={}, configIds={}", ctx.getScheduleId(),
                    ctx.getDeliveryConfigIds());

        } catch (Exception e) {
            // Log but don't fail the execution for delivery errors
            log.error("Delivery failed for schedule={}: {}", ctx.getScheduleId(), e.getMessage(),
                    e);
        }
    }

    private void validateParams(ReportExecutionContext ctx) {
        if (ctx.getTemplateId() == null) {
            log.debug("No template ID in context, skipping param validation");
            return;
        }

        // Build a system user for template lookup
        User systemUser = new User();
        systemUser.setId(0L);
        systemUser.setName("system");
        systemUser.setTenantId(ctx.getTenantId());

        SemanticTemplate template =
                templateService.getTemplateById(ctx.getTemplateId(), systemUser);
        if (template == null || template.getTemplateConfig() == null) {
            log.debug("Template not found or has no config, skipping param validation");
            return;
        }

        List<SemanticTemplateConfig.ConfigParam> configParams =
                template.getTemplateConfig().getConfigParams();
        if (configParams == null || configParams.isEmpty()) {
            log.debug("Template has no configParams, skipping validation");
            return;
        }

        Map<String, Object> resolvedParams = ctx.getResolvedParams();
        List<String> errors = new ArrayList<>();

        for (SemanticTemplateConfig.ConfigParam param : configParams) {
            String key = param.getKey();
            Object value = resolvedParams != null ? resolvedParams.get(key) : null;

            // Check required params
            if (param.isRequired() && isEmptyValue(value)) {
                if (StringUtils.isNotBlank(param.getDefaultValue())) {
                    // Has default value, not an error
                    continue;
                }
                errors.add(String.format("Required parameter '%s' (%s) is missing", param.getName(),
                        key));
                continue;
            }

            // Skip validation if value is empty and not required
            if (isEmptyValue(value)) {
                continue;
            }

            // Type-specific validation
            String type = param.getType();
            if (type == null) {
                continue;
            }

            switch (type.toUpperCase()) {
                case "DATABASE":
                    if (!isValidDatabaseRef(value)) {
                        errors.add(String.format(
                                "Parameter '%s' must be a valid database ID (positive number)",
                                key));
                    }
                    break;
                case "TABLE":
                    if (!isValidTableRef(value)) {
                        errors.add(String.format(
                                "Parameter '%s' must be a valid table name (non-empty string)",
                                key));
                    }
                    break;
                case "FIELD":
                    if (!isValidFieldRef(value)) {
                        errors.add(String.format(
                                "Parameter '%s' must be a valid field name (non-empty string)",
                                key));
                    }
                    break;
                case "TEXT":
                    // TEXT type accepts any non-null string
                    if (!(value instanceof String)) {
                        errors.add(String.format("Parameter '%s' must be a text string", key));
                    }
                    break;
                default:
                    log.warn("Unknown parameter type '{}' for key '{}', skipping validation", type,
                            key);
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Parameter validation failed: {}", errors);
            throw new ParamValidationException(errors);
        }

        log.debug("Parameter validation passed for template={}, params={}", ctx.getTemplateId(),
                resolvedParams != null ? resolvedParams.keySet() : "none");
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return StringUtils.isBlank((String) value);
        }
        return false;
    }

    private boolean isValidDatabaseRef(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue() > 0;
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isValidTableRef(Object value) {
        if (value instanceof String tableName) {
            // Basic validation: non-empty, no spaces, follows identifier pattern
            return StringUtils.isNotBlank(tableName)
                    && tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
        }
        return false;
    }

    private boolean isValidFieldRef(Object value) {
        if (value instanceof String fieldName) {
            // Basic validation: non-empty, no spaces, follows identifier pattern
            return StringUtils.isNotBlank(fieldName)
                    && fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
        }
        return false;
    }

    private String computeSqlHash(String queryConfig) {
        if (queryConfig == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(queryConfig.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null)
            return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * Capture up to 20 rows from the full result list for storage in the execution snapshot.
     * Truncating avoids bloating the snapshot column for large result sets.
     */
    private List<Map<String, Object>> buildResultPreview(List<Map<String, Object>> resultList) {
        if (resultList == null || resultList.isEmpty()) {
            return List.of();
        }
        int limit = Math.min(resultList.size(), 20);
        return new ArrayList<>(resultList.subList(0, limit));
    }
}
