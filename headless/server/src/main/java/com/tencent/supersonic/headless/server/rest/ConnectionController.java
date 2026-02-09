package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ConnectionEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DataSyncExecutionDO;
import com.tencent.supersonic.headless.server.pojo.ConfiguredCatalog;
import com.tencent.supersonic.headless.server.pojo.DiscoveredSchema;
import com.tencent.supersonic.headless.server.pojo.SchemaChange;
import com.tencent.supersonic.headless.server.service.ConnectionService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/connections")
@Slf4j
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping
    public ConnectionDO createConnection(@RequestBody ConnectionDO connection,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return connectionService.createConnection(connection, user);
    }

    @GetMapping
    public Page<ConnectionDO> listConnections(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long sourceDbId,
            @RequestParam(required = false) Long destDbId,
            @RequestParam(required = false) String status, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return connectionService.listConnections(new Page<>(current, pageSize), sourceDbId,
                destDbId, status);
    }

    @GetMapping("/{id}")
    public ConnectionDO getConnectionById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return connectionService.getConnectionById(id);
    }

    @PatchMapping("/{id}")
    public ConnectionDO updateConnection(@PathVariable Long id,
            @RequestBody ConnectionDO connection, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return connectionService.updateConnection(id, connection, user);
    }

    @DeleteMapping("/{id}")
    public void deleteConnection(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        connectionService.deleteConnection(id, user);
    }

    @PostMapping("/{id}:pause")
    public void pauseConnection(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        connectionService.pauseConnection(id, user);
    }

    @PostMapping("/{id}:resume")
    public void resumeConnection(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        connectionService.resumeConnection(id, user);
    }

    @PostMapping("/{id}:deprecate")
    public void deprecateConnection(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        String reason = body != null ? body.get("reason") : null;
        connectionService.deprecateConnection(id, reason, user);
    }

    @PostMapping("/{id}:sync")
    public void triggerSync(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        connectionService.triggerSync(id, user);
    }

    @PostMapping("/{id}:resetState")
    public void resetState(@PathVariable Long id,
            @RequestBody(required = false) Map<String, List<String>> body,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        List<String> streamNames = body != null ? body.get("streams") : null;
        connectionService.resetState(id, streamNames, user);
    }

    @PostMapping("/{id}:discoverSchema")
    public DiscoveredSchema discoverSchema(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return connectionService.discoverSchema(id, user);
    }

    @GetMapping("/{id}/schemaChanges")
    public SchemaChange detectSchemaChanges(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return connectionService.detectSchemaChanges(id);
    }

    @PostMapping("/{id}:applySchemaChanges")
    public void applySchemaChanges(@PathVariable Long id, @RequestBody ConfiguredCatalog catalog,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        connectionService.applySchemaChanges(id, catalog, user);
    }

    @GetMapping("/{id}/timeline")
    public Page<ConnectionEventDO> getTimeline(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String eventType, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return connectionService.getTimeline(id, new Page<>(current, pageSize), eventType);
    }

    @GetMapping("/{id}/jobs")
    public Page<DataSyncExecutionDO> getJobHistory(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return connectionService.getJobHistory(id, new Page<>(current, pageSize));
    }
}
