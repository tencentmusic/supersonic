package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fixedReports")
@Slf4j
@RequiredArgsConstructor
public class FixedReportController {

    private final FixedReportService fixedReportService;

    @GetMapping
    public List<FixedReportVO> listFixedReports(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String domainName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String view, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return fixedReportService.listFixedReports(user, keyword, domainName, status, view);
    }

    @PostMapping("/{datasetId}/subscription")
    public void subscribe(@PathVariable Long datasetId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        fixedReportService.subscribe(datasetId, user);
    }

    @DeleteMapping("/{datasetId}/subscription")
    public void unsubscribe(@PathVariable Long datasetId, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        fixedReportService.unsubscribe(datasetId, user);
    }

    @GetMapping("/{datasetId}/executions")
    public Page<ReportExecutionDO> getExecutionsByDataset(@PathVariable Long datasetId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<ReportExecutionDO> page = new Page<>(current, pageSize);
        return fixedReportService.getExecutionsByDataset(page, datasetId);
    }
}
