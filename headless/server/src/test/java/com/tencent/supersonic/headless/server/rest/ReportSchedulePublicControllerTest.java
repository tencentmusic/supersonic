package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.headless.api.util.ReportDownloadTokenUtils;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.service.delivery.ReportDownloadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSchedulePublicControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadResultShouldBypassLoginInterceptor() throws Exception {
        assertTrue(
                ReportSchedulePublicController.class
                        .getMethod("downloadResult", Long.class, Long.class, Long.class,
                                String.class, Long.class)
                        .isAnnotationPresent(AuthenticationIgnore.class));
    }

    @Test
    void downloadResultShouldReturnFileWhenTokenIsValid() throws Exception {
        ReportExecutionMapper executionMapper = mock(ReportExecutionMapper.class);
        ReportSchedulePublicController controller = new ReportSchedulePublicController(
                executionMapper, tenantConfig(), downloadProps("test-secret"));

        Path reportFile = Files.writeString(tempDir.resolve("report.xlsx"), "content");
        ReportExecutionDO execution = new ReportExecutionDO();
        execution.setId(9L);
        execution.setScheduleId(8L);
        execution.setResultLocation(reportFile.toString());
        when(executionMapper.selectById(9L)).thenReturn(execution);

        long expires = ReportDownloadTokenUtils.expiresAtEpochSeconds(60);
        String token = ReportDownloadTokenUtils.createToken("test-secret", 8L, 9L, expires, 1L);

        ResponseEntity<Resource> response = controller.downloadResult(8L, 9L, expires, token, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.startsWith("attachment"),
                "Content-Disposition should start with 'attachment', got: " + contentDisposition);
        assertTrue(contentDisposition.contains("report.xlsx"),
                "Content-Disposition should contain filename, got: " + contentDisposition);
    }

    @Test
    void downloadResultShouldRejectExpiredToken() throws Exception {
        ReportExecutionMapper executionMapper = mock(ReportExecutionMapper.class);
        ReportSchedulePublicController controller = new ReportSchedulePublicController(
                executionMapper, tenantConfig(), downloadProps("test-secret"));

        // expiresAt is 1 second in the past → token has expired
        long expiredAt = ReportDownloadTokenUtils.expiresAtEpochSeconds(0) - 1;
        String token = ReportDownloadTokenUtils.createToken("test-secret", 8L, 9L, expiredAt, 1L);

        ResponseEntity<Resource> response = controller.downloadResult(8L, 9L, expiredAt, token, 1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(executionMapper, never()).selectById(9L);
    }

    @Test
    void downloadResultShouldRejectCrossTenantTokenReplay() throws Exception {
        ReportExecutionMapper executionMapper = mock(ReportExecutionMapper.class);
        ReportSchedulePublicController controller = new ReportSchedulePublicController(
                executionMapper, tenantConfig(), downloadProps("test-secret"));

        // Token signed for tenant 1 — presented with tenantId=2 → HMAC mismatch → 403
        long expires = ReportDownloadTokenUtils.expiresAtEpochSeconds(60);
        String tokenForTenant1 =
                ReportDownloadTokenUtils.createToken("test-secret", 8L, 9L, expires, 1L);

        ResponseEntity<Resource> response =
                controller.downloadResult(8L, 9L, expires, tokenForTenant1, 2L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(executionMapper, never()).selectById(9L);
    }

    @Test
    void downloadResultShouldRejectInvalidTokenBeforeLoadingExecution() {
        ReportExecutionMapper executionMapper = mock(ReportExecutionMapper.class);
        ReportSchedulePublicController controller = new ReportSchedulePublicController(
                executionMapper, tenantConfig(), downloadProps("test-secret"));

        ResponseEntity<Resource> response = controller.downloadResult(8L, 9L,
                ReportDownloadTokenUtils.expiresAtEpochSeconds(60), "bad-token", null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(executionMapper, never()).selectById(9L);
    }

    private TenantConfig tenantConfig() {
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setDefaultTenantId(1L);
        return tenantConfig;
    }

    private ReportDownloadProperties downloadProps(String secret) {
        ReportDownloadProperties p = new ReportDownloadProperties();
        ReflectionTestUtils.setField(p, "signingSecret", secret);
        ReflectionTestUtils.setField(p, "tokenTtlSeconds", 604800L);
        return p;
    }
}
