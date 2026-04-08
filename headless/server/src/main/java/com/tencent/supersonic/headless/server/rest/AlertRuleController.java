package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertRuleDO;
import com.tencent.supersonic.headless.server.pojo.AlertEventTransitionReq;
import com.tencent.supersonic.headless.server.service.AlertRuleService;
import com.tencent.supersonic.headless.server.service.impl.AlertEvaluator;
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
@RequestMapping("/api/v1/alertRules")
@Slf4j
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @PostMapping
    public AlertRuleDO createRule(@RequestBody AlertRuleDO rule, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        rule.setCreatedBy(user.getName());
        rule.setOwnerId(user.getId());
        rule.setTenantId(user.getTenantId());
        return alertRuleService.createRule(rule);
    }

    @GetMapping
    public Page<AlertRuleDO> getRuleList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long datasetId,
            @RequestParam(required = false) Boolean enabled, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return alertRuleService.getRuleList(new Page<>(current, pageSize), datasetId, enabled);
    }

    @GetMapping("/{id}")
    public AlertRuleDO getRuleById(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return alertRuleService.getRuleById(id);
    }

    @PatchMapping("/{id}")
    public AlertRuleDO updateRule(@PathVariable Long id, @RequestBody AlertRuleDO rule,
            HttpServletRequest request, HttpServletResponse response) {
        UserHolder.findUser(request, response);
        rule.setId(id);
        return alertRuleService.updateRule(rule);
    }

    @DeleteMapping("/{id}")
    public void deleteRule(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        alertRuleService.deleteRule(id);
    }

    @PostMapping("/{id}:pause")
    public void pauseRule(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        alertRuleService.pauseRule(id);
    }

    @PostMapping("/{id}:resume")
    public void resumeRule(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        alertRuleService.resumeRule(id);
    }

    @PostMapping("/{id}:trigger")
    public void triggerNow(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        alertRuleService.triggerNow(id);
    }

    @PostMapping("/{id}:test")
    public List<AlertEvaluator.AlertEventCandidate> testRun(@PathVariable Long id,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserHolder.findUser(request, response);
        return alertRuleService.testRun(id);
    }

    @GetMapping("/{ruleId}/executions")
    public Page<AlertExecutionDO> getExecutionList(@PathVariable Long ruleId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return alertRuleService.getExecutionList(new Page<>(current, pageSize), ruleId, status);
    }

    @GetMapping("/events")
    public Page<AlertEventDO> getEventList(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String deliveryStatus, HttpServletRequest request,
            HttpServletResponse response) {
        UserHolder.findUser(request, response);
        return alertRuleService.getEventList(new Page<>(current, pageSize), ruleId, severity,
                deliveryStatus);
    }

    @GetMapping("/events/pendingCounts")
    public Map<Long, Long> getPendingEventCounts() {
        return alertRuleService.countPendingEventsByRule();
    }

    @GetMapping("/events/{eventId}")
    public AlertEventDO getEvent(@PathVariable Long eventId) {
        return alertRuleService.getEventById(eventId);
    }

    @PostMapping("/events/{eventId}:transition")
    public AlertEventDO transitionEvent(@PathVariable Long eventId,
            @RequestBody AlertEventTransitionReq req, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return alertRuleService.transitionEvent(eventId, req, user);
    }
}
