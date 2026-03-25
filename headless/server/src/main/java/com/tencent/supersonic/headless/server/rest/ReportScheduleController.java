package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/api/v1/reportSchedules")
@Slf4j
@RequiredArgsConstructor
public class ReportScheduleController {

    private final ReportScheduleService reportScheduleService;

    @PostMapping
    public ReportScheduleDO createSchedule(@RequestBody ReportScheduleDO schedule,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return reportScheduleService.createSchedule(schedule, user);
    }

    @GetMapping
    public Page<ReportScheduleDO> getScheduleList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long datasetId,
            @RequestParam(required = false) Boolean enabled, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return reportScheduleService.getScheduleList(new Page<>(current, pageSize), datasetId,
                enabled, user);
    }

    @GetMapping("/{id}")
    public ReportScheduleDO getScheduleById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return reportScheduleService.getScheduleById(id, user);
    }

    @PatchMapping("/{id}")
    public ReportScheduleDO updateSchedule(@PathVariable Long id,
            @RequestBody ReportScheduleDO schedule, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        schedule.setId(id);
        // Strip immutable fields — these must never be overwritten by the client
        schedule.setOwnerId(null);
        schedule.setCreatedBy(null);
        schedule.setTenantId(null);
        schedule.setCreatedAt(null);
        return reportScheduleService.updateSchedule(schedule, user);
    }

    @DeleteMapping("/{id}")
    public void deleteSchedule(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        reportScheduleService.deleteSchedule(id, user);
    }

    @PostMapping("/{id}:pause")
    public void pauseSchedule(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        reportScheduleService.pauseSchedule(id, user);
    }

    @PostMapping("/{id}:resume")
    public void resumeSchedule(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        reportScheduleService.resumeSchedule(id, user);
    }

    @PostMapping("/{id}:trigger")
    public void triggerNow(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        reportScheduleService.triggerNow(id, user);
    }

    @PostMapping("/{scheduleId}:execute")
    public void executeReport(@PathVariable Long scheduleId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        reportScheduleService.executeReport(scheduleId, user);
    }

    @GetMapping("/{scheduleId}/executions")
    public Page<ReportExecutionDO> getExecutionList(@PathVariable Long scheduleId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return reportScheduleService.getExecutionList(new Page<>(current, pageSize), scheduleId,
                status, user);
    }

    @GetMapping("/{scheduleId}/executions/{executionId}")
    public ReportExecutionDO getExecutionById(@PathVariable Long scheduleId,
            @PathVariable Long executionId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return reportScheduleService.getExecutionById(scheduleId, executionId, user);
    }

    @GetMapping("/{scheduleId}/executions/{executionId}:download")
    public ResponseEntity<Resource> downloadResult(@PathVariable Long scheduleId,
            @PathVariable Long executionId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        ReportExecutionDO execution =
                reportScheduleService.getExecutionById(scheduleId, executionId, user);
        if (execution == null || execution.getResultLocation() == null) {
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
