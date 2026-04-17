package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.common.storage.StoragePath;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
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
        String key = exportTaskService.getDownloadPath(id);

        // Tenant guard: ensure the file belongs to the caller's tenant
        Long fileTenantId = StoragePath.extractTenantId(key);
        if (fileTenantId != null && currentUser.getTenantId() != null
                && !fileTenantId.equals(currentUser.getTenantId())) {
            return ResponseEntity.status(403).build();
        }

        // Prefer a presigned redirect for cloud storage (OSS/S3)
        String presigned = fileStorage.presignedUrl(key, Duration.ofMinutes(10));
        if (presigned != null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, presigned).build();
        }

        // Fall back to streaming (local storage)
        if (!fileStorage.exists(key)) {
            return ResponseEntity.notFound().build();
        }
        String fileName = key.substring(key.lastIndexOf('/') + 1);
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
