package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.service.delivery.ReportDownloadTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

@RestController
@RequestMapping("/api/public/reportSchedules")
@RequiredArgsConstructor
public class ReportSchedulePublicController {

    private final ReportExecutionMapper executionMapper;

    @Value("${s2.report-download.signing-secret:${s2.encryption.aes-key:}}")
    private String downloadSigningSecret;

    @GetMapping("/{scheduleId}/executions/{executionId}:download")
    public ResponseEntity<Resource> downloadResult(@PathVariable Long scheduleId,
            @PathVariable Long executionId, @RequestParam Long expires,
            @RequestParam String token) {
        if (!ReportDownloadTokenUtils.isValidToken(downloadSigningSecret, scheduleId, executionId,
                expires, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
