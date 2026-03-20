package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.headless.server.manager.QuartzJobManager;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertRuleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertEventMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertRuleMapper;
import com.tencent.supersonic.headless.server.service.AlertRuleService;
import com.tencent.supersonic.headless.server.task.AlertCheckJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRuleDO>
        implements AlertRuleService {

    private static final String GROUP = "ALERT";
    private static final String KEY_PREFIX = "alert_";
    private static final int MAX_RULES_PER_TENANT = 50;
    private static final long MIN_CRON_INTERVAL_MS = 5 * 60 * 1000L; // AG-07: 5 minutes

    private final QuartzJobManager quartzJobManager;
    private final AlertExecutionMapper alertExecutionMapper;
    private final AlertEventMapper alertEventMapper;
    private final AlertCheckDispatcher alertCheckDispatcher;

    public AlertRuleServiceImpl(QuartzJobManager quartzJobManager,
            AlertExecutionMapper alertExecutionMapper, AlertEventMapper alertEventMapper,
            AlertCheckDispatcher alertCheckDispatcher) {
        this.quartzJobManager = quartzJobManager;
        this.alertExecutionMapper = alertExecutionMapper;
        this.alertEventMapper = alertEventMapper;
        this.alertCheckDispatcher = alertCheckDispatcher;
    }

    @Override
    public AlertRuleDO createRule(AlertRuleDO rule) {
        // Validation
        validateCronInterval(rule.getCronExpression()); // AG-07
        validateTenantRuleCount(rule.getTenantId()); // AG-10

        rule.setCreatedAt(new Date());
        rule.setUpdatedAt(new Date());
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (rule.getSilenceMinutes() == null) {
            rule.setSilenceMinutes(60);
        }
        if (rule.getRetryCount() == null) {
            rule.setRetryCount(2);
        }
        if (rule.getRetryInterval() == null) {
            rule.setRetryInterval(30);
        }
        if (rule.getMaxConsecutiveFailures() == null) {
            rule.setMaxConsecutiveFailures(5);
        }
        rule.setConsecutiveFailures(0);

        baseMapper.insert(rule);

        if (Integer.valueOf(1).equals(rule.getEnabled())) {
            registerQuartzJob(rule);
        }

        return rule;
    }

    @Override
    public AlertRuleDO updateRule(AlertRuleDO rule) {
        AlertRuleDO existing = baseMapper.selectById(rule.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Alert rule not found: " + rule.getId());
        }

        if (rule.getCronExpression() != null
                && !rule.getCronExpression().equals(existing.getCronExpression())) {
            validateCronInterval(rule.getCronExpression());
        }

        rule.setUpdatedAt(new Date());
        baseMapper.updateById(rule);

        // Reschedule if cron changed
        if (rule.getCronExpression() != null && existing.getQuartzJobKey() != null) {
            quartzJobManager.rescheduleJob(existing.getQuartzJobKey(), rule.getCronExpression());
        }

        return rule;
    }

    @Override
    public void deleteRule(Long id) {
        AlertRuleDO rule = baseMapper.selectById(id);
        if (rule == null) {
            return;
        }
        if (rule.getQuartzJobKey() != null) {
            quartzJobManager.deleteJob(rule.getQuartzJobKey());
        }
        baseMapper.deleteById(id);
    }

    @Override
    public AlertRuleDO getRuleById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public Page<AlertRuleDO> getRuleList(Page<AlertRuleDO> page, Long datasetId, Boolean enabled) {
        QueryWrapper<AlertRuleDO> wrapper = new QueryWrapper<>();
        if (datasetId != null) {
            wrapper.lambda().eq(AlertRuleDO::getDatasetId, datasetId);
        }
        if (enabled != null) {
            wrapper.lambda().eq(AlertRuleDO::getEnabled, enabled ? 1 : 0);
        }
        wrapper.lambda().orderByDesc(AlertRuleDO::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public void pauseRule(Long id) {
        AlertRuleDO rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + id);
        }
        if (rule.getQuartzJobKey() != null) {
            quartzJobManager.pauseJob(rule.getQuartzJobKey());
        }
        rule.setEnabled(0);
        rule.setUpdatedAt(new Date());
        baseMapper.updateById(rule);
    }

    @Override
    public void resumeRule(Long id) {
        AlertRuleDO rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + id);
        }
        // Clear disabled_reason and consecutive_failures when manually resumed
        rule.setEnabled(1);
        rule.setDisabledReason(null);
        rule.setConsecutiveFailures(0);
        rule.setUpdatedAt(new Date());
        if (rule.getQuartzJobKey() != null) {
            quartzJobManager.resumeJob(rule.getQuartzJobKey());
        } else {
            registerQuartzJob(rule);
        }
        baseMapper.updateById(rule);
    }

    @Override
    public void triggerNow(Long id) {
        AlertRuleDO rule = baseMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + id);
        }
        if (rule.getQuartzJobKey() != null) {
            quartzJobManager.triggerJob(rule.getQuartzJobKey());
        } else {
            // Fallback: run directly if no Quartz job registered
            alertCheckDispatcher.dispatch(id);
        }
    }

    @Override
    public List<AlertEvaluator.AlertEventCandidate> testRun(Long id) throws Exception {
        return alertCheckDispatcher.testRun(id);
    }

    @Override
    public Page<AlertExecutionDO> getExecutionList(Page<AlertExecutionDO> page, Long ruleId,
            String status) {
        QueryWrapper<AlertExecutionDO> wrapper = new QueryWrapper<>();
        if (ruleId != null) {
            wrapper.lambda().eq(AlertExecutionDO::getRuleId, ruleId);
        }
        if (status != null) {
            wrapper.lambda().eq(AlertExecutionDO::getStatus, status);
        }
        wrapper.lambda().orderByDesc(AlertExecutionDO::getStartTime);
        return alertExecutionMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<AlertEventDO> getEventList(Page<AlertEventDO> page, Long ruleId, String severity,
            String deliveryStatus) {
        QueryWrapper<AlertEventDO> wrapper = new QueryWrapper<>();
        if (ruleId != null) {
            wrapper.lambda().eq(AlertEventDO::getRuleId, ruleId);
        }
        if (severity != null) {
            wrapper.lambda().eq(AlertEventDO::getSeverity, severity);
        }
        if (deliveryStatus != null) {
            wrapper.lambda().eq(AlertEventDO::getDeliveryStatus, deliveryStatus);
        }
        wrapper.lambda().orderByDesc(AlertEventDO::getCreatedAt);
        return alertEventMapper.selectPage(page, wrapper);
    }

    // === Validation methods ===

    private void validateCronInterval(String cronExpression) {
        if (cronExpression == null) {
            throw new IllegalArgumentException("cronExpression is required");
        }
        try {
            org.quartz.CronExpression cron = new org.quartz.CronExpression(cronExpression);
            Date now = new Date();
            Date next1 = cron.getNextValidTimeAfter(now);
            if (next1 == null) {
                throw new IllegalArgumentException(
                        "Cron expression produces no fire times: " + cronExpression);
            }
            Date next2 = cron.getNextValidTimeAfter(next1);
            if (next2 == null) {
                throw new IllegalArgumentException(
                        "Cron expression produces less than 2 fire times: " + cronExpression);
            }
            long diffMs = next2.getTime() - next1.getTime();
            if (diffMs < MIN_CRON_INTERVAL_MS) {
                throw new IllegalArgumentException(
                        "AG-07: Alert cron interval must be >= 5 minutes (got " + (diffMs / 60000)
                                + " minutes): " + cronExpression);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid cron expression: " + cronExpression + " — " + e.getMessage());
        }
    }

    private void validateTenantRuleCount(Long tenantId) {
        long count = baseMapper.selectCount(
                new QueryWrapper<AlertRuleDO>().lambda().eq(AlertRuleDO::getTenantId, tenantId));
        if (count >= MAX_RULES_PER_TENANT) {
            throw new IllegalArgumentException("AG-10: Tenant " + tenantId
                    + " has reached the maximum alert rules limit (" + MAX_RULES_PER_TENANT + ")");
        }
    }

    private void registerQuartzJob(AlertRuleDO rule) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("ruleId", rule.getId());
        jobDataMap.put("tenantId", rule.getTenantId());

        String quartzJobKey = quartzJobManager.createJob(GROUP, KEY_PREFIX, rule.getId(),
                AlertCheckJob.class, rule.getCronExpression(), jobDataMap);

        rule.setQuartzJobKey(quartzJobKey);
        baseMapper.updateById(rule);
    }
}
