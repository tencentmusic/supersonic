package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.common.lock.DistributedLock;
import com.tencent.supersonic.common.lock.DistributedLockProvider;
import com.tencent.supersonic.common.lock.LockAcquisitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class QuartzJobManager {

    private final Scheduler scheduler;
    private final DistributedLockProvider lockProvider;

    public String createJob(String group, String keyPrefix, Long id, Class<? extends Job> jobClass,
            String cronExpression, JobDataMap jobDataMap) {
        String jobKeyName = keyPrefix + id;
        String fullKey = group + "." + jobKeyName;
        DistributedLock lock = lockProvider.obtain("quartz:" + fullKey);
        if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            throw new LockAcquisitionException("Failed to acquire lock for job: " + fullKey);
        }
        try {
            JobDetail job = JobBuilder.newJob(jobClass).withIdentity(jobKeyName, group)
                    .usingJobData(jobDataMap).storeDurably().build();

            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobKeyName, group)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionFireAndProceed())
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            log.error("Failed to create Quartz job group={}, key={}", group, jobKeyName, e);
            throw new RuntimeException("Failed to create Quartz job", e);
        } finally {
            lock.unlock();
        }
        return fullKey;
    }

    public void pauseJob(String quartzJobKey) {
        JobKey jobKey = resolveJobKey(quartzJobKey);
        try {
            scheduler.pauseJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to pause job: " + quartzJobKey, e);
        }
    }

    public void resumeJob(String quartzJobKey) {
        JobKey jobKey = resolveJobKey(quartzJobKey);
        try {
            scheduler.resumeJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to resume job: " + quartzJobKey, e);
        }
    }

    public void deleteJob(String quartzJobKey) {
        DistributedLock lock = lockProvider.obtain("quartz:" + quartzJobKey);
        if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            throw new LockAcquisitionException("Failed to acquire lock for job: " + quartzJobKey);
        }
        try {
            JobKey jobKey = resolveJobKey(quartzJobKey);
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.warn("Failed to delete Quartz job: {}", quartzJobKey, e);
        } finally {
            lock.unlock();
        }
    }

    public boolean jobExists(String quartzJobKey) {
        if (quartzJobKey == null) {
            return false;
        }
        try {
            return scheduler.checkExists(resolveJobKey(quartzJobKey));
        } catch (SchedulerException e) {
            log.warn("Failed to check job existence: {}", quartzJobKey, e);
            return false;
        }
    }

    /**
     * Cleans up any orphaned trigger or job with the same identity, then creates a fresh job +
     * trigger. Safe to call when the Quartz state is partially corrupted (e.g. trigger exists but
     * job is gone, or both are missing).
     */
    public String recreateJob(String group, String keyPrefix, Long id,
            Class<? extends Job> jobClass, String cronExpression, JobDataMap jobDataMap) {
        String jobKeyName = keyPrefix + id;
        String fullKey = group + "." + jobKeyName;
        DistributedLock lock = lockProvider.obtain("quartz:" + fullKey);
        if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            throw new LockAcquisitionException("Failed to acquire lock for job: " + fullKey);
        }
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobKeyName, group);
            JobKey jobKey = JobKey.jobKey(jobKeyName, group);
            // Remove any orphaned state before recreating
            if (scheduler.checkExists(triggerKey)) {
                scheduler.unscheduleJob(triggerKey);
            }
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            JobDetail job = JobBuilder.newJob(jobClass).withIdentity(jobKeyName, group)
                    .usingJobData(jobDataMap).storeDurably().build();
            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobKeyName, group)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionFireAndProceed())
                    .build();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            log.error("Failed to recreate Quartz job group={}, key={}", group, jobKeyName, e);
            throw new RuntimeException("Failed to recreate Quartz job: " + fullKey, e);
        } finally {
            lock.unlock();
        }
        return fullKey;
    }

    public void triggerJob(String quartzJobKey) {
        JobKey jobKey = resolveJobKey(quartzJobKey);
        try {
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to trigger job: " + quartzJobKey, e);
        }
    }

    public void rescheduleJob(String quartzJobKey, String newCron) {
        DistributedLock lock = lockProvider.obtain("quartz:" + quartzJobKey);
        if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            throw new LockAcquisitionException("Failed to acquire lock for job: " + quartzJobKey);
        }
        try {
            JobKey jobKey = resolveJobKey(quartzJobKey);
            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
            CronTrigger newTrigger = TriggerBuilder
                    .newTrigger().withIdentity(triggerKey).withSchedule(CronScheduleBuilder
                            .cronSchedule(newCron).withMisfireHandlingInstructionFireAndProceed())
                    .build();
            scheduler.rescheduleJob(triggerKey, newTrigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to reschedule job: " + quartzJobKey, e);
        } finally {
            lock.unlock();
        }
    }

    private JobKey resolveJobKey(String quartzJobKey) {
        if (quartzJobKey == null) {
            throw new IllegalArgumentException("quartzJobKey must not be null");
        }
        int dotIndex = quartzJobKey.indexOf('.');
        if (dotIndex > 0) {
            String group = quartzJobKey.substring(0, dotIndex);
            String name = quartzJobKey.substring(dotIndex + 1);
            return JobKey.jobKey(name, group);
        }
        return JobKey.jobKey(quartzJobKey);
    }
}
