package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.service.delivery.ReportDownloadTokenUtils;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSchedulePublicControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadResultShouldReturnFileWhenTokenIsValid() throws Exception {
        ReportExecutionMapper executionMapper = mock(ReportExecutionMapper.class);
        ReportSchedulePublicController controller =
                new ReportSchedulePublicController(executionMapper);
        ReflectionTestUtils.setField(controller, "downloadSigningSecret", "test-secret");

        Path reportFile = Files.writeString(tempDir.resolve("report.xlsx"), "content");
        ReportExecutionDO execution = new ReportExecutionDO();
        execution.setId(9L);
        execution.setScheduleId(8L);
        execution.setResultLocation(reportFile.toString());
        when(executionMapper.selectById(9L)).thenReturn(execution);

        long expires = ReportDownloadTokenUtils.expiresAtEpochSeconds(60);
        String token = ReportDownloadTokenUtils.createToken("test-secret", 8L, 9L, expires);

        ResponseEntity<Resource> response = controller.downloadResult(8L, 9L, expires, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("attachment; filename=\"report.xlsx\"",
                response.getHeaders().getFirst("Content-Disposition"));
    }

    @Test
    void downloadResultShouldRejectInvalidTokenBeforeLoadingExecution() {
        ReportExecutionMapper executionMapper = mock(ReportExecutionMapper.class);
        ReportSchedulePublicController controller =
                new ReportSchedulePublicController(executionMapper);
        ReflectionTestUtils.setField(controller, "downloadSigningSecret", "test-secret");

        ResponseEntity<Resource> response = controller.downloadResult(8L, 9L,
                ReportDownloadTokenUtils.expiresAtEpochSeconds(60), "bad-token");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(executionMapper, never()).selectById(9L);
    }
}
