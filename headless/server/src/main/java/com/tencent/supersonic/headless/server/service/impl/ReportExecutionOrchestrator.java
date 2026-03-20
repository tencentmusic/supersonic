package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.ParamValidationException;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.ExecutionSnapshotData;
import com.tencent.supersonic.headless.server.pojo.OutputFormat;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import com.tencent.supersonic.headless.server.service.delivery.DeliveryContext;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportExecutionOrchestrator {

    private final ReportExecutionMapper executionMapper;
    private final SemanticLayerService semanticLayerService;
    private final SemanticTemplateService templateService;
    private final QueryConfigParser queryConfigParser;

    @Autowired(required = false)
    private ReportDeliveryService deliveryService;

    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    @Value("${supersonic.export.local-dir:${java.io.tmpdir}/supersonic-export}")
    private String exportDir;

    public ReportExecutionOrchestrator(ReportExecutionMapper executionMapper,
            SemanticLayerService semanticLayerService, SemanticTemplateService templateService,
            QueryConfigParser queryConfigParser) {
        this.executionMapper = executionMapper;
        this.semanticLayerService = semanticLayerService;
        this.templateService = templateService;
        this.queryConfigParser = queryConfigParser;
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
        // Build user from context for permission injection
        User user = new User();
        user.setId(ctx.getOperatorUserId() != null ? ctx.getOperatorUserId() : 0L);
        user.setName(
                ctx.getOperatorUserId() != null ? "user_" + ctx.getOperatorUserId() : "system");
        user.setTenantId(ctx.getTenantId());
        return user;
    }

    private SemanticQueryReq parseQueryConfig(ReportExecutionContext ctx) {
        return queryConfigParser.parse(ctx.getQueryConfig(), ctx.getDatasetId(),
                ctx.getResolvedParams());
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

            DeliveryContext deliveryContext = DeliveryContext.builder()
                    .scheduleId(ctx.getScheduleId()).executionId(executionId)
                    .scheduleName(ctx.getScheduleName() != null ? ctx.getScheduleName()
                            : "Schedule " + ctx.getScheduleId())
                    .reportName("Report " + ctx.getDatasetId()).fileLocation(fileLocation)
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
