package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncExecutionDO;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.service.DataSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy DataSync API controller.
 * <p>
 * <b>DEPRECATED:</b> This API is deprecated and will be removed in a future version. Please migrate
 * to the new Connection API at <code>/api/v1/connections</code>.
 * </p>
 * <p>
 * Migration guide:
 * <ul>
 * <li>DataSyncConfig → Connection (one-to-one mapping)</li>
 * <li>source_database_id → sourceDatabaseId</li>
 * <li>target_database_id → destinationDatabaseId</li>
 * <li>enabled=true/false → status=ACTIVE/PAUSED</li>
 * <li>sync_config → configuredCatalog</li>
 * </ul>
 *
 * @deprecated Use {@link ConnectionController} instead. This API will be removed in version 2.0.
 */
@RestController
@RequestMapping("/api/v1/dataSyncConfigs")
@Slf4j
@RequiredArgsConstructor
@Deprecated(since = "1.5", forRemoval = true)
public class DataSyncController {

    private static final String DEPRECATION_WARNING =
            "This API is deprecated. Please migrate to /api/v1/connections. "
                    + "See documentation for migration guide.";
    private static final String SUNSET_DATE = "2026-06-01";

    private final DataSyncService dataSyncService;

    private void addDeprecationHeaders(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", SUNSET_DATE);
        response.setHeader("X-Deprecation-Warning", DEPRECATION_WARNING);
        response.setHeader("Link", "</api/v1/connections>; rel=\"successor-version\"");
    }

    @PostMapping
    @Deprecated(since = "1.5", forRemoval = true)
    public DataSyncConfigDO createSyncConfig(@RequestBody DataSyncConfigDO config,
            HttpServletRequest request, HttpServletResponse response) {
        addDeprecationHeaders(response);
        User user = UserHolder.findUser(request, response);
        config.setCreatedBy(user.getName());
        config.setTenantId(user.getTenantId());
        return dataSyncService.createSyncConfig(config);
    }

    @GetMapping
    @Deprecated(since = "1.5", forRemoval = true)
    public Page<DataSyncConfigDO> getSyncConfigList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        return dataSyncService.getSyncConfigList(new Page<>(current, pageSize));
    }

    @GetMapping("/{id}")
    @Deprecated(since = "1.5", forRemoval = true)
    public DataSyncConfigDO getSyncConfigById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        return dataSyncService.getSyncConfigById(id);
    }

    @PatchMapping("/{id}")
    @Deprecated(since = "1.5", forRemoval = true)
    public DataSyncConfigDO updateSyncConfig(@PathVariable Long id,
            @RequestBody DataSyncConfigDO config, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        config.setId(id);
        return dataSyncService.updateSyncConfig(config);
    }

    @DeleteMapping("/{id}")
    @Deprecated(since = "1.5", forRemoval = true)
    public void deleteSyncConfig(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        dataSyncService.deleteSyncConfig(id);
    }

    @PostMapping("/{id}:trigger")
    @Deprecated(since = "1.5", forRemoval = true)
    public void triggerSync(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        dataSyncService.triggerSync(id);
    }

    @PostMapping("/{id}:pause")
    @Deprecated(since = "1.5", forRemoval = true)
    public void pauseSync(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        dataSyncService.pauseSync(id);
    }

    @PostMapping("/{id}:resume")
    @Deprecated(since = "1.5", forRemoval = true)
    public void resumeSync(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        dataSyncService.resumeSync(id);
    }

    @GetMapping("/{configId}/executions")
    @Deprecated(since = "1.5", forRemoval = true)
    public Page<DataSyncExecutionDO> getExecutionList(@PathVariable Long configId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        return dataSyncService.getExecutionList(new Page<>(current, pageSize), configId);
    }

    @PostMapping("/{configId}:discoverSchema")
    @Deprecated(since = "1.5", forRemoval = true)
    public DiscoveredSchema discoverSchema(@PathVariable Long configId, HttpServletRequest request,
            HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        return dataSyncService.discoverSchema(configId);
    }

    /**
     * Discover schema for a database (used in wizard before config is created).
     *
     * @deprecated Use Connection API instead
     */
    @PostMapping(":discoverSchemaByDatabase")
    @Deprecated(since = "1.5", forRemoval = true)
    public DiscoveredSchema discoverSchemaByDatabase(@RequestParam Long databaseId,
            HttpServletRequest request, HttpServletResponse response) {
        addDeprecationHeaders(response);
        UserHolder.findUser(request, response);
        return dataSyncService.discoverSchemaByDatabaseId(databaseId);
    }
}
