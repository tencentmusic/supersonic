package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.headless.api.pojo.request.ReportDeliveryConfigReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryRecordResp;
import com.tencent.supersonic.headless.api.service.ReportDeliveryService;
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
 * REST controller for report delivery configuration management.
 */
@RestController
@RequestMapping("${spring.servlet.api-path:/api}/semantic/delivery")
@Slf4j
@RequiredArgsConstructor
public class ReportDeliveryController {

    private final ReportDeliveryService deliveryService;

    private void assertAdmin(User user) {
        if (user == null || (!user.isSuperAdmin()
                && (user.getIsAdmin() == null || user.getIsAdmin() != 1))) {
            throw new InvalidPermissionException("仅管理员可操作推送配置");
        }
    }

    // ========== Delivery Config CRUD ==========

    @GetMapping("/configs")
    public Page<ReportDeliveryConfigResp> listConfigs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        pageSize = Math.min(pageSize, 200);
        Page<ReportDeliveryConfigResp> page = new Page<>(pageNum, pageSize);
        return deliveryService.getConfigList(page);
    }

    @GetMapping("/configs/{id}")
    public ReportDeliveryConfigResp getConfig(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return deliveryService.getConfigById(id);
    }

    @PostMapping("/configs")
    public ReportDeliveryConfigResp createConfig(@RequestBody ReportDeliveryConfigReq config,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        return deliveryService.createConfig(config, user);
    }

    @PatchMapping("/configs/{id}")
    public ReportDeliveryConfigResp updateConfig(@PathVariable Long id,
            @RequestBody ReportDeliveryConfigReq config, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        config.setId(id);
        return deliveryService.updateConfig(config, user);
    }

    @DeleteMapping("/configs/{id}")
    public Boolean deleteConfig(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        deliveryService.deleteConfig(id);
        return true;
    }

    // ========== Test Delivery ==========

    @PostMapping("/configs/{id}:test")
    public ReportDeliveryRecordResp testConfig(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        assertAdmin(user);
        return deliveryService.testDelivery(id);
    }

    // ========== Delivery Records ==========

    @GetMapping("/records")
    public Page<ReportDeliveryRecordResp> listRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long executionId, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        pageSize = Math.min(pageSize, 200);
        Page<ReportDeliveryRecordResp> page = new Page<>(pageNum, pageSize);
        return deliveryService.getDeliveryRecords(page, configId, scheduleId, executionId);
    }

    @PostMapping("/records/{id}:retry")
    public ReportDeliveryRecordResp retryDelivery(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return deliveryService.retryDelivery(id);
    }

    // ========== Statistics ==========

    @GetMapping("/statistics")
    public ReportDeliveryService.DeliveryStatistics getStatistics(
            @RequestParam(defaultValue = "7") Integer days, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        days = Math.min(days, 365);
        return deliveryService.getStatistics(days);
    }

    @GetMapping("/statistics/daily")
    public java.util.List<ReportDeliveryService.DailyDeliveryStats> getDailyStats(
            @RequestParam(defaultValue = "7") Integer days, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        days = Math.min(days, 365);
        return deliveryService.getDailyStats(days);
    }
}
