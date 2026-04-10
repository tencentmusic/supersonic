package com.tencent.supersonic.headless.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.metrics.TemplateReportMetrics;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.ExportTaskService;
import com.tencent.supersonic.headless.server.service.RowCountEstimator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExportTaskServiceImpl extends ServiceImpl<ExportTaskMapper, ExportTaskDO>
        implements ExportTaskService {

    private final ThreadPoolExecutor exportExecutor;
    private final SemanticLayerService semanticLayerService;
    private final RowCountEstimator rowCountEstimator;
    private final UserService userService;
    private final DataSetService dataSetService;
    @Autowired(required = false)
    private TemplateReportMetrics reportMetrics;

    @Value("${supersonic.export.local-dir:${java.io.tmpdir}/supersonic-export}")
    private String exportDir;

    @Value("${supersonic.export.async-threshold:10000}")
    private long asyncThreshold;

    public ExportTaskServiceImpl(@Qualifier("exportExecutor") ThreadPoolExecutor exportExecutor,
            SemanticLayerService semanticLayerService, RowCountEstimator rowCountEstimator,
            UserService userService, DataSetService dataSetService) {
        this.exportExecutor = exportExecutor;
        this.semanticLayerService = semanticLayerService;
        this.rowCountEstimator = rowCountEstimator;
        this.userService = userService;
        this.dataSetService = dataSetService;
    }

    /**
     * Estimate the number of rows that an export task will produce. Used to decide whether to
     * execute synchronously or asynchronously.
     *
     * @param databaseId the database ID
     * @param sql the SQL query
     * @return estimated row count, or -1 if unknown
     */
    public long estimateRowCount(Long databaseId, String sql) {
        return rowCountEstimator.estimate(databaseId, sql);
    }

    /**
     * Check if the export task should be executed asynchronously based on estimated row count.
     *
     * @param databaseId the database ID
     * @param sql the SQL query
     * @return true if async execution is recommended
     */
    public boolean shouldExecuteAsync(Long databaseId, String sql) {
        long estimate = estimateRowCount(databaseId, sql);
        if (estimate < 0) {
            // Unknown estimate - default to async for safety
            log.debug("Row count estimation failed, defaulting to async execution");
            return true;
        }
        boolean isAsync = estimate > asyncThreshold;
        log.debug("Export async decision: estimate={}, threshold={}, async={}", estimate,
                asyncThreshold, isAsync);
        return isAsync;
    }

    /**
     * 默认任务名：优先带数据集中文名，无 id 时用「未知」；解析名称失败时回退为数据集 id。
     */
    private String buildDefaultTaskName(ExportTaskDO task) {
        String dsPart;
        if (task.getDatasetId() == null) {
            dsPart = "未知";
        } else {
            try {
                DataSetResp ds = dataSetService.getDataSet(task.getDatasetId());
                if (ds != null && StringUtils.isNotBlank(ds.getName())) {
                    dsPart = truncateForTaskName(ds.getName(), 48);
                } else {
                    dsPart = String.valueOf(task.getDatasetId());
                }
            } catch (Exception e) {
                log.debug("Could not resolve dataset name for export task, datasetId={}",
                        task.getDatasetId(), e);
                dsPart = String.valueOf(task.getDatasetId());
            }
        }
        return String.format("数据导出_%s_%s", dsPart, DateUtils.format(new Date(), "yyyyMMddHHmmss"));
    }

    private static String truncateForTaskName(String raw, int maxLen) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\r', ' ').replace('\n', ' ').trim();
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }

    @Override
    public ExportTaskDO submitExportTask(ExportTaskDO task) {
        if (task == null || StringUtils.isBlank(task.getQueryConfig())) {
            throw new IllegalArgumentException("queryConfig is required");
        }
        if (StringUtils.isBlank(task.getTaskName())) {
            task.setTaskName(buildDefaultTaskName(task));
        }
        task.setStatus(ExportTaskStatus.PENDING.name());
        task.setCreatedAt(new Date());
        task.setExpireTime(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        baseMapper.insert(task);

        exportExecutor.submit(() -> executeExport(task.getId()));
        return task;
    }

    @Override
    public Page<ExportTaskDO> getTaskList(Page<ExportTaskDO> page, Long userId) {
        QueryWrapper<ExportTaskDO> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.lambda().eq(ExportTaskDO::getUserId, userId);
        }
        wrapper.lambda().orderByDesc(ExportTaskDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public ExportTaskDO getTaskById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public void cancelTask(Long id) {
        ExportTaskDO task = baseMapper.selectById(id);
        if (task != null && ExportTaskStatus.PENDING.name().equals(task.getStatus())) {
            task.setStatus(ExportTaskStatus.EXPIRED.name());
            baseMapper.updateById(task);
        }
    }

    @Override
    public String getDownloadPath(Long id) {
        ExportTaskDO task = baseMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("Export task not found: " + id);
        }
        if (!ExportTaskStatus.SUCCESS.name().equals(task.getStatus())) {
            throw new IllegalStateException("Export task is not completed: " + task.getStatus());
        }
        return task.getFileLocation();
    }

    private void executeExport(Long taskId) {
        long startTimeMs = System.currentTimeMillis();
        ExportTaskDO task = baseMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        TenantContext.setTenantId(task.getTenantId());
        task.setStatus(ExportTaskStatus.RUNNING.name());
        baseMapper.updateById(task);

        try {
            // 1. Build user context
            User user = buildUserContext(task);

            // 2. Parse query config
            SemanticQueryReq queryReq = parseQueryConfig(task);

            // 3. Execute query
            log.info("Executing export task={}, dataset={}, format={}", taskId, task.getDatasetId(),
                    task.getOutputFormat());
            SemanticQueryResp queryResp = semanticLayerService.queryByReq(queryReq, user);

            // 4. Write to file
            File outputFile = writeOutputFile(task, queryResp);

            // 5. Update task with result
            task.setStatus(ExportTaskStatus.SUCCESS.name());
            task.setRowCount(
                    queryResp.getResultList() != null ? (long) queryResp.getResultList().size()
                            : 0L);
            task.setFileSize(outputFile.length());
            task.setFileLocation(outputFile.getAbsolutePath());
            baseMapper.updateById(task);
            if (reportMetrics != null) {
                reportMetrics.recordExport("success", normalizeFormat(task.getOutputFormat()),
                        System.currentTimeMillis() - startTimeMs);
            }

            log.info("Export task completed: taskId={}, rows={}, size={}", taskId,
                    task.getRowCount(), task.getFileSize());
        } catch (Exception e) {
            log.error("Export task failed: taskId={}", taskId, e);
            task.setStatus(ExportTaskStatus.FAILED.name());
            task.setErrorMessage(truncate(e.getMessage(), 2000));
            baseMapper.updateById(task);
            if (reportMetrics != null) {
                reportMetrics.recordExport("error", normalizeFormat(task.getOutputFormat()),
                        System.currentTimeMillis() - startTimeMs);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private String normalizeFormat(String format) {
        return StringUtils.isBlank(format) ? "unknown" : format.toLowerCase();
    }

    private User buildUserContext(ExportTaskDO task) {
        if (task.getUserId() != null && userService != null) {
            User user = userService.getUserById(task.getUserId());
            if (user != null) {
                if (user.getTenantId() == null) {
                    user.setTenantId(task.getTenantId());
                }
                return user;
            }
        }
        User user = new User();
        user.setId(task.getUserId() != null ? task.getUserId() : 0L);
        user.setName(task.getUserId() != null ? "user_" + task.getUserId() : "system");
        user.setTenantId(task.getTenantId());
        return user;
    }

    private SemanticQueryReq parseQueryConfig(ExportTaskDO task) {
        String queryConfig = task.getQueryConfig();
        if (StringUtils.isBlank(queryConfig)) {
            throw new IllegalArgumentException("queryConfig is required");
        }

        // Try QuerySqlReq first. Otherwise a SQL config carrying dataSetId can be deserialized
        // into an "empty" QueryStructReq and lose the original SQL.
        try {
            QuerySqlReq sqlReq = JsonUtil.toObject(queryConfig, QuerySqlReq.class);
            if (sqlReq != null && StringUtils.isNotBlank(sqlReq.getSql())) {
                if (sqlReq.getDataSetId() == null) {
                    sqlReq.setDataSetId(task.getDatasetId());
                }
                return sqlReq;
            }
        } catch (Exception e) {
            log.debug("Failed to parse as QuerySqlReq, trying QueryStructReq");
        }

        // Then try QueryStructReq
        try {
            QueryStructReq structReq = JsonUtil.toObject(queryConfig, QueryStructReq.class);
            if (structReq != null && structReq.getDataSetId() != null) {
                return structReq.convert(true);
            }
        } catch (Exception e) {
            log.debug("Failed to parse as QueryStructReq");
        }

        throw new IllegalArgumentException("Unable to parse queryConfig");
    }

    private File writeOutputFile(ExportTaskDO task, SemanticQueryResp queryResp) throws Exception {
        String timestamp = DateUtils.format(new Date(), "yyyyMMddHHmmss");
        boolean isCsv = "CSV".equalsIgnoreCase(task.getOutputFormat());
        String fileName =
                String.format("export_%d_%s.%s", task.getId(), timestamp, isCsv ? "csv" : "xlsx");

        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outputFile = new File(dir, fileName);

        if (isCsv) {
            writeCsv(outputFile, queryResp);
        } else {
            writeExcel(outputFile, queryResp);
        }

        return outputFile;
    }

    private void writeExcel(File file, SemanticQueryResp queryResp) {
        List<List<String>> headers = buildHeaders(queryResp.getColumns());
        List<List<String>> data = buildData(queryResp);
        EasyExcel.write(file).sheet("Sheet1").head(headers).doWrite(data);
    }

    private void writeCsv(File file, SemanticQueryResp queryResp) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            List<QueryColumn> columns = queryResp.getColumns();
            // Write header
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

    private String truncate(String s, int maxLen) {
        if (s == null)
            return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
