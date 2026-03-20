package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryRecordMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.pojo.ExecutionSnapshotData;
import com.tencent.supersonic.headless.server.pojo.ExecutionSnapshotResp;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import com.tencent.supersonic.headless.server.service.DataSetAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * REST controller for report execution audit replay (snapshot view).
 *
 * <p>
 * Endpoint: {@code GET /api/v1/report-executions/{executionId}/snapshot}
 */
@RestController
@RequestMapping("/api/v1/report-executions")
@Slf4j
@RequiredArgsConstructor
public class ReportExecutionController {

    /**
     * Pattern that matches JDBC connection URLs. Used for desensitisation of the sql field.
     * Example: jdbc:mysql://host:3306/db?user=foo or any jdbc:// prefix.
     */
    private static final Pattern JDBC_PATTERN =
            Pattern.compile("jdbc:[a-zA-Z0-9+.-]+://[^\\s'\"]+", Pattern.CASE_INSENSITIVE);

    private final ReportExecutionMapper executionMapper;
    private final ReportDeliveryRecordMapper deliveryRecordMapper;
    private final DataSetAuthService dataSetAuthService;

    /**
     * Get the execution snapshot for audit replay.
     *
     * <p>
     * Security: requires login (UserHolder throws if unauthenticated). SQL field is desensitised —
     * JDBC connection strings are replaced by {@code [DB:***]}. resultPreview is filtered to
     * columns the current user is allowed to see.
     */
    @GetMapping("/{executionId}/snapshot")
    public ResponseEntity<ExecutionSnapshotResp> getSnapshot(@PathVariable Long executionId,
            HttpServletRequest request, HttpServletResponse response) {

        // Require authentication
        User user = UserHolder.findUser(request, response);

        // 1. Load execution record (TenantSqlInterceptor enforces tenant isolation automatically)
        ReportExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Parse execution_snapshot JSON → ExecutionSnapshotData
        ExecutionSnapshotData snapshotData = parseSnapshot(execution.getExecutionSnapshot());

        // 3. Authorization check (P1): verify the requesting user can view the dataset that
        // produced this execution. This prevents cross-user data leaks within the same tenant.
        Long datasetId = snapshotData != null && snapshotData.getContext() != null
                ? snapshotData.getContext().getDatasetId()
                : null;
        if (datasetId != null && !dataSetAuthService.checkDataSetViewPermission(datasetId, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 4. Load associated delivery records
        List<ReportDeliveryRecordDO> records = loadDeliveryRecords(executionId);

        // 5. Assemble response
        ExecutionSnapshotResp resp =
                buildResponse(executionId, execution, snapshotData, records, user);

        return ResponseEntity.ok(resp);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parse the execution_snapshot JSON into {@link ExecutionSnapshotData}.
     *
     * <p>
     * Supports two formats:
     * <ol>
     * <li><b>New format</b> (wrapper): {@code {"context":{...},"resultPreview":[...]}} — written by
     * the updated orchestrator.</li>
     * <li><b>Legacy format</b> (plain context): the JSON was previously serialised directly from
     * {@link ReportExecutionContext} without a wrapper. We detect this by checking for the
     * {@code "context"} key, and fall back to wrapping the whole JSON as the context with a null
     * resultPreview.</li>
     * </ol>
     */
    private ExecutionSnapshotData parseSnapshot(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return null;
        }
        try {
            // Try new wrapper format first
            Map<String, Object> raw = JsonUtil.toObject(snapshotJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            if (raw != null && raw.containsKey("context")) {
                return JsonUtil.toObject(snapshotJson, ExecutionSnapshotData.class);
            }
            // Legacy format: the JSON is a plain ReportExecutionContext
            ReportExecutionContext ctx =
                    JsonUtil.toObject(snapshotJson, ReportExecutionContext.class);
            return new ExecutionSnapshotData(ctx, null);
        } catch (Exception e) {
            log.warn("Failed to parse execution_snapshot JSON: {}", e.getMessage());
            return null;
        }
    }

    private List<ReportDeliveryRecordDO> loadDeliveryRecords(Long executionId) {
        LambdaQueryWrapper<ReportDeliveryRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportDeliveryRecordDO::getExecutionId, executionId);
        return deliveryRecordMapper.selectList(wrapper);
    }

    private ExecutionSnapshotResp buildResponse(Long executionId, ReportExecutionDO execution,
            ExecutionSnapshotData snapshotData, List<ReportDeliveryRecordDO> records, User user) {

        ExecutionSnapshotResp resp = new ExecutionSnapshotResp();
        resp.setExecutionId(executionId);
        resp.setStatus(execution.getStatus());
        resp.setExecutedAt(execution.getStartTime());
        resp.setDurationMs(execution.getExecutionTimeMs());
        resp.setResultRowCount(execution.getRowCount());
        resp.setTemplateVersion(execution.getTemplateVersion());

        ReportExecutionContext ctx = snapshotData != null ? snapshotData.getContext() : null;
        if (ctx != null) {
            resp.setTemplateName(ctx.getScheduleName());
            resp.setTriggerType(ctx.getSource() != null ? ctx.getSource().name() : null);
            resp.setParams(ctx.getResolvedParams());

            // Prefer the stored rendered SQL (P2 fix); fall back to extracting from queryConfig
            // for legacy snapshots that pre-date this field.
            String rawSql = snapshotData.getRenderedSql() != null ? snapshotData.getRenderedSql()
                    : extractSql(ctx.getQueryConfig());
            resp.setSql(desensitiseSql(rawSql));
        }

        // Result preview: read from the stored snapshot (up to 20 rows captured at execution time)
        // AG-12: columns filtered by current user's permissions
        resp.setResultPreview(buildResultPreview(snapshotData, user));

        // Delivery records
        resp.setDeliveryRecords(toDeliveryItems(records));

        return resp;
    }

    /**
     * Attempt to extract a rendered SQL string from the queryConfig JSON blob. The queryConfig may
     * be a QuerySqlReq (has "sql" field) or a SqlTemplateConfig (has "templateSql" field). For
     * QueryStructReq there is no raw SQL stored, so we return null — the audit trail shows params
     * instead.
     */
    private String extractSql(String queryConfig) {
        if (StringUtils.isBlank(queryConfig)) {
            return null;
        }
        try {
            Map<String, Object> map = JsonUtil.toObject(queryConfig,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            if (map == null) {
                return null;
            }
            // QuerySqlReq stores executed SQL under "sql"
            if (map.containsKey("sql")) {
                Object sql = map.get("sql");
                return sql != null ? sql.toString() : null;
            }
            // SqlTemplateConfig stores the template under "templateSql"
            if (map.containsKey("templateSql")) {
                Object tmpl = map.get("templateSql");
                return tmpl != null ? tmpl.toString() : null;
            }
        } catch (Exception e) {
            log.debug("Unable to extract SQL from queryConfig: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Replace JDBC connection strings with {@code [DB:***]} to prevent leaking host/user/password
     * information to UI consumers.
     */
    private String desensitiseSql(String sql) {
        if (StringUtils.isBlank(sql)) {
            return sql;
        }
        return JDBC_PATTERN.matcher(sql).replaceAll("[DB:***]");
    }

    /**
     * Build result preview rows from the stored snapshot, filtered by the current user's
     * column-level permissions (AG-12). Columns the user cannot access are omitted from each row.
     */
    private List<Map<String, Object>> buildResultPreview(ExecutionSnapshotData snapshotData,
            User user) {
        if (snapshotData == null || snapshotData.getResultPreview() == null
                || snapshotData.getResultPreview().isEmpty()) {
            return null;
        }

        List<Map<String, Object>> rows = snapshotData.getResultPreview();

        // AG-12: apply column-level permission filtering
        ReportExecutionContext ctx = snapshotData.getContext();
        if (ctx != null && ctx.getDatasetId() != null) {
            Set<String> allowedColumns = getAllowedColumns(ctx.getDatasetId(), user);
            if (allowedColumns != null) {
                rows = filterColumns(rows, allowedColumns);
            }
        }

        return rows;
    }

    /**
     * Query the user's authorized columns for the given dataset.
     *
     * <p>
     * Returns {@code null} (no filtering) for admins/superAdmins. Returns an empty set when the
     * user has no column grants — the caller must then return an empty preview. An empty
     * {@code authResList} means no grants, NOT full access (P1 fix for AG-12).
     */
    private Set<String> getAllowedColumns(Long datasetId, User user) {
        // Admins and super-admins see all columns
        if (user.isSuperAdmin()
                || dataSetAuthService.checkDataSetAdminPermission(datasetId, user)) {
            return null;
        }
        try {
            AuthorizedResourceResp authResp =
                    dataSetAuthService.queryAuthorizedResources(datasetId, user);
            if (authResp == null || authResp.getAuthResList() == null) {
                // Cannot determine permissions — fail closed
                return Set.of();
            }
            // Empty list → no column grants → caller will produce empty preview
            return authResp.getAuthResList().stream().map(AuthRes::getName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to query column permissions for dataset={}, user={}: {}", datasetId,
                    user.getName(), e.getMessage());
            // Fail closed on errors
            return Set.of();
        }
    }

    /**
     * Remove columns from each row that are not in the allowed set.
     */
    private List<Map<String, Object>> filterColumns(List<Map<String, Object>> rows,
            Set<String> allowedColumns) {
        return rows.stream().map(row -> {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (allowedColumns.contains(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered;
        }).collect(Collectors.toList());
    }

    private List<ExecutionSnapshotResp.DeliveryRecordItem> toDeliveryItems(
            List<ReportDeliveryRecordDO> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        return records.stream().map(r -> {
            ExecutionSnapshotResp.DeliveryRecordItem item =
                    new ExecutionSnapshotResp.DeliveryRecordItem();
            item.setRecordId(r.getId());
            item.setChannelType(r.getDeliveryType());
            item.setStatus(r.getStatus());
            item.setDeliveredAt(r.getCompletedAt());
            item.setErrorMessage(r.getErrorMessage());
            return item;
        }).collect(Collectors.toList());
    }
}
