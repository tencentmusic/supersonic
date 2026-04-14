package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.api.service.ReportDeliveryService;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertRuleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertEventMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertRuleMapper;
import com.tencent.supersonic.headless.server.pojo.AlertCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertCheckDispatcher {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertExecutionMapper alertExecutionMapper;
    private final AlertEventMapper alertEventMapper;
    private final QueryConfigParser queryConfigParser;
    private final AlertEvaluator alertEvaluator;
    private final SemanticLayerService semanticLayerService;

    @Autowired(required = false)
    private ReportDeliveryService deliveryService;

    public void dispatch(Long ruleId) {
        AlertRuleDO rule = alertRuleMapper.selectById(ruleId);
        if (rule == null) {
            log.warn("Alert rule not found: {}", ruleId);
            return;
        }
        if (!Integer.valueOf(1).equals(rule.getEnabled())) {
            log.info("Alert rule {} is disabled, skipping", ruleId);
            return;
        }

        // Create execution record
        Date startTime = new Date();
        AlertExecutionDO execution = new AlertExecutionDO();
        execution.setRuleId(ruleId);
        execution.setStatus("RUNNING");
        execution.setStartTime(startTime);
        execution.setTenantId(rule.getTenantId());
        alertExecutionMapper.insert(execution);

        try {
            // Parse query config (AG-06 enforced inside parseForAlert)
            SemanticQueryReq queryReq =
                    queryConfigParser.parseForAlert(rule.getQueryConfig(), rule.getDatasetId());

            // Build user context (execute as rule owner)
            User user = new User();
            user.setId(rule.getOwnerId() != null ? rule.getOwnerId() : 0L);
            user.setName(rule.getOwnerId() != null ? "user_" + rule.getOwnerId() : "system");
            user.setTenantId(rule.getTenantId());

            // Execute query
            SemanticQueryResp queryResp = semanticLayerService.queryByReq(queryReq, user);
            List<Map<String, Object>> rows =
                    queryResp.getResultList() != null ? queryResp.getResultList()
                            : Collections.emptyList();

            // Parse and evaluate conditions
            List<AlertCondition> conditions =
                    JsonUtil.toList(rule.getConditions(), AlertCondition.class);
            List<AlertEvaluator.AlertEventCandidate> candidates =
                    alertEvaluator.evaluate(ruleId, rows, conditions);

            // Silence period filter (Task 9)
            Date now = new Date();
            List<AlertEvaluator.AlertEventCandidate> toDeliver = new ArrayList<>();
            int silencedCount = 0;
            for (AlertEvaluator.AlertEventCandidate candidate : candidates) {
                if (isInSilencePeriod(candidate.getAlertKey(), now)) {
                    silencedCount++;
                } else {
                    toDeliver.add(candidate);
                }
            }

            // Persist execution
            execution.setStatus("SUCCESS");
            execution.setEndTime(new Date());
            execution.setTotalRows((long) rows.size());
            execution.setAlertedRows((long) toDeliver.size());
            execution.setSilencedRows((long) silencedCount);
            execution.setExecutionTimeMs(System.currentTimeMillis() - startTime.getTime());
            alertExecutionMapper.updateById(execution);

            // Persist events and deliver
            if (!toDeliver.isEmpty()) {
                int silenceMinutes =
                        rule.getSilenceMinutes() != null ? rule.getSilenceMinutes() : 60;
                Date silenceUntil = new Date(now.getTime() + silenceMinutes * 60_000L);
                persistAndDeliver(rule, execution, toDeliver, now, silenceUntil);
            }

            // Reset consecutive failures on success
            if (rule.getConsecutiveFailures() != null && rule.getConsecutiveFailures() > 0) {
                rule.setConsecutiveFailures(0);
            }
            rule.setLastCheckTime(now);
            alertRuleMapper.updateById(rule);

        } catch (Exception e) {
            log.error("Alert check failed for ruleId={}", ruleId, e);
            execution.setStatus("FAILED");
            execution.setEndTime(new Date());
            execution.setErrorMessage(truncate(e.getMessage(), 2000));
            execution.setExecutionTimeMs(System.currentTimeMillis() - startTime.getTime());
            alertExecutionMapper.updateById(execution);

            // Handle consecutive failures (auto-disable after maxConsecutiveFailures)
            int failures =
                    (rule.getConsecutiveFailures() != null ? rule.getConsecutiveFailures() : 0) + 1;
            rule.setConsecutiveFailures(failures);
            int maxFailures =
                    rule.getMaxConsecutiveFailures() != null ? rule.getMaxConsecutiveFailures() : 5;
            if (failures >= maxFailures) {
                rule.setEnabled(0);
                rule.setDisabledReason("Auto-disabled after " + failures
                        + " consecutive failures. Last error: " + truncate(e.getMessage(), 200));
                log.warn("Alert rule {} auto-disabled after {} consecutive failures", ruleId,
                        failures);
            }
            rule.setLastCheckTime(new Date());
            alertRuleMapper.updateById(rule);
        }
    }

    /**
     * Test run: execute query and evaluate, return candidates without persisting or delivering.
     */
    public List<AlertEvaluator.AlertEventCandidate> testRun(Long ruleId) throws Exception {
        AlertRuleDO rule = alertRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + ruleId);
        }

        SemanticQueryReq queryReq =
                queryConfigParser.parseForAlert(rule.getQueryConfig(), rule.getDatasetId());
        User user = new User();
        user.setId(rule.getOwnerId() != null ? rule.getOwnerId() : 0L);
        user.setName(rule.getOwnerId() != null ? "user_" + rule.getOwnerId() : "system");
        user.setTenantId(rule.getTenantId());

        SemanticQueryResp queryResp = semanticLayerService.queryByReq(queryReq, user);
        List<Map<String, Object>> rows =
                queryResp.getResultList() != null ? queryResp.getResultList()
                        : Collections.emptyList();
        List<AlertCondition> conditions =
                JsonUtil.toList(rule.getConditions(), AlertCondition.class);
        return alertEvaluator.evaluate(ruleId, rows, conditions);
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private boolean isInSilencePeriod(String alertKey, Date now) {
        QueryWrapper<AlertEventDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(AlertEventDO::getAlertKey, alertKey)
                .gt(AlertEventDO::getSilenceUntil, now).orderByDesc(AlertEventDO::getCreatedAt)
                .last("LIMIT 1");
        return alertEventMapper.selectOne(wrapper) != null;
    }

    private void persistAndDeliver(AlertRuleDO rule, AlertExecutionDO execution,
            List<AlertEvaluator.AlertEventCandidate> toDeliver, Date now, Date silenceUntil) {

        // Sort: CRITICAL first, then WARNING
        toDeliver.sort((a, b) -> {
            int aScore = "CRITICAL".equals(a.getSeverity()) ? 0 : 1;
            int bScore = "CRITICAL".equals(b.getSeverity()) ? 0 : 1;
            return Integer.compare(aScore, bScore);
        });

        // Persist each event
        List<Long> eventIds = new ArrayList<>();
        for (AlertEvaluator.AlertEventCandidate candidate : toDeliver) {
            AlertEventDO event = new AlertEventDO();
            event.setExecutionId(execution.getId());
            event.setRuleId(rule.getId());
            event.setConditionIndex(candidate.getConditionIndex());
            event.setSeverity(candidate.getSeverity());
            event.setAlertKey(candidate.getAlertKey());
            event.setDimensionValue(candidate.getDimensionValue());
            event.setMetricValue(candidate.getMetricValue());
            event.setBaselineValue(candidate.getBaselineValue());
            event.setDeviationPct(candidate.getDeviationPct());
            event.setMessage(candidate.getMessage());
            event.setDeliveryStatus("PENDING");
            event.setResolutionStatus("OPEN");
            event.setSilenceUntil(silenceUntil);
            event.setTenantId(rule.getTenantId());
            event.setCreatedAt(now);
            alertEventMapper.insert(event);
            eventIds.add(event.getId());
        }

        // Deliver if service available and delivery configs set
        if (deliveryService == null || StringUtils.isBlank(rule.getDeliveryConfigIds())) {
            log.debug("No delivery service or configs, skipping delivery for rule {}",
                    rule.getId());
            return;
        }

        try {
            // Determine highest severity
            String maxSeverity =
                    toDeliver.stream().anyMatch(e -> "CRITICAL".equals(e.getSeverity()))
                            ? "CRITICAL"
                            : "WARNING";

            // Build alert content message (one line per event)
            StringBuilder contentBuilder = new StringBuilder();
            for (AlertEvaluator.AlertEventCandidate candidate : toDeliver) {
                String severityIcon = "CRITICAL".equals(candidate.getSeverity()) ? "🔴" : "⚠️";
                contentBuilder.append(severityIcon).append(" **").append(candidate.getSeverity())
                        .append("**\n");
                contentBuilder.append(candidate.getMessage()).append("\n\n");
            }

            // Parse delivery config IDs (comma-separated)
            List<Long> configIds = Arrays.stream(rule.getDeliveryConfigIds().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong)
                    .collect(Collectors.toList());

            // Build DeliveryContext with alert fields
            DeliveryContext deliveryContext =
                    DeliveryContext.builder().executionId(execution.getId())
                            .alertRuleId(rule.getId()).alertRuleName(rule.getName())
                            .alertContent(contentBuilder.toString().trim())
                            .alertSeverity(maxSeverity).alertedCount(toDeliver.size())
                            .totalChecked(execution.getTotalRows() != null
                                    ? execution.getTotalRows().intValue()
                                    : 0)
                            .tenantId(rule.getTenantId())
                            .executionTime(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                                    .format(new Date()))
                            .alertEventIds(eventIds).build();

            deliveryService.deliver(configIds, deliveryContext);

            // Update delivery status for events
            alertEventMapper.update(null,
                    new UpdateWrapper<AlertEventDO>().lambda()
                            .eq(AlertEventDO::getExecutionId, execution.getId())
                            .set(AlertEventDO::getDeliveryStatus, "SUCCESS"));

        } catch (Exception e) {
            log.error("Alert delivery failed for rule {}: {}", rule.getId(), e.getMessage(), e);
            alertEventMapper.update(null,
                    new UpdateWrapper<AlertEventDO>().lambda()
                            .eq(AlertEventDO::getExecutionId, execution.getId())
                            .set(AlertEventDO::getDeliveryStatus, "FAILED"));
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
