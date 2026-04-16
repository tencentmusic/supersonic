package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.headless.api.util.ReportDownloadTokenUtils;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.service.delivery.ReportDownloadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/public/reportSchedules")
@RequiredArgsConstructor
public class ReportSchedulePublicController {

    private final ReportExecutionMapper executionMapper;
    private final TenantConfig tenantConfig;
    private final ReportDownloadProperties downloadProperties;

    @AuthenticationIgnore
    @GetMapping("/{scheduleId}/executions/{executionId}:download")
    public ResponseEntity<Resource> downloadResult(@PathVariable Long scheduleId,
            @PathVariable Long executionId, @RequestParam Long expires, @RequestParam String token,
            @RequestParam(required = false) Long tenantId) {
        // Resolve effective tenantId: URL param takes priority; fall back to TenantConfig default
        // (single-tenant deployments omit tenantId or send 0; both resolve to defaultTenantId).
        Long effectiveTenantId =
                (tenantId != null && tenantId > 0) ? tenantId : tenantConfig.getDefaultTenantId();

        if (!ReportDownloadTokenUtils.isValidToken(downloadProperties.getSigningSecret(),
                scheduleId, executionId, expires, token, effectiveTenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Establish TenantContext so TenantSqlInterceptor injects WHERE tenant_id = ?
        Long previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(effectiveTenantId);

            ReportExecutionDO execution = executionMapper.selectById(executionId);
            if (execution == null || !scheduleId.equals(execution.getScheduleId())
                    || execution.getResultLocation() == null) {
                return ResponseEntity.notFound().build();
            }
            File file = new File(execution.getResultLocation());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.builder("attachment")
                            .filename(file.getName(), StandardCharsets.UTF_8).build().toString())
                    .body(resource);
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }
}
