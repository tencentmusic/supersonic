package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import com.tencent.supersonic.headless.server.service.ExportTaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/exportTasks")
@Slf4j
@RequiredArgsConstructor
public class ExportTaskController {

    private final ExportTaskService exportTaskService;
    private final FileStorage fileStorage;

    @PostMapping
    public ExportTaskDO submitExportTask(@RequestBody ExportTaskDO task, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        task.setUserId(user.getId());
        task.setTenantId(user.getTenantId());
        return exportTaskService.submitExportTask(task);
    }

    @GetMapping
    public Page<ExportTaskDO> getTaskList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return exportTaskService.getTaskList(new Page<>(current, pageSize), user.getId());
    }

    @GetMapping("/{id}")
    public ExportTaskDO getTaskById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return exportTaskService.getTaskById(id);
    }

    @GetMapping("/{id}:download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User currentUser = UserHolder.findUser(request, response);

        // Fix A: use DB-authoritative tenantId — avoids null bypass for legacy absolute-path keys
        ExportTaskDO taskMeta = exportTaskService.getTaskById(id);
        if (taskMeta == null) {
            return ResponseEntity.notFound().build();
        }
        if (currentUser.getTenantId() != null && taskMeta.getTenantId() != null
                && !currentUser.getTenantId().equals(taskMeta.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        if (!ExportTaskStatus.SUCCESS.name().equals(taskMeta.getStatus())) {
            return ResponseEntity.status(409).build();
        }
        String key = taskMeta.getFileLocation();

        // Fix B: check existence before generating presigned URL so callers get a clean 404
        if (!fileStorage.exists(key)) {
            return ResponseEntity.notFound().build();
        }

        // Prefer a presigned redirect for cloud storage (OSS/S3)
        String presigned = fileStorage.presignedUrl(key, Duration.ofMinutes(10));
        if (presigned != null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, presigned).build();
        }

        // Fall back to streaming (local storage)
        // Fix C: sanitize filename to prevent Content-Disposition header injection
        String rawName = key.substring(key.lastIndexOf('/') + 1);
        String fileName = rawName.replaceAll("[^A-Za-z0-9._\\-]", "_");
        InputStream stream = fileStorage.download(key);
        Resource resource = new InputStreamResource(stream);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public void cancelTask(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        exportTaskService.cancelTask(id);
    }
}
