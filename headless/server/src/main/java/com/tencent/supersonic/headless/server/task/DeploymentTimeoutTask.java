package com.tencent.supersonic.headless.server.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.SemanticDeploymentDO;
import com.tencent.supersonic.headless.server.persistence.mapper.SemanticDeploymentMapper;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeploymentTimeoutTask {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final SemanticDeploymentMapper deploymentMapper;

    @Scheduled(fixedRate = 300000, initialDelay = 120000)
    public void cleanupStaleDeployments() {
        Date timeoutThreshold = new Date(System.currentTimeMillis() - TIMEOUT_MILLIS);

        QueryWrapper<SemanticDeploymentDO> runningWrapper = new QueryWrapper<>();
        runningWrapper.lambda()
                .eq(SemanticDeploymentDO::getStatus,
                        SemanticDeployment.DeploymentStatus.RUNNING.name())
                .lt(SemanticDeploymentDO::getStartTime, timeoutThreshold);

        QueryWrapper<SemanticDeploymentDO> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.lambda()
                .eq(SemanticDeploymentDO::getStatus,
                        SemanticDeployment.DeploymentStatus.PENDING.name())
                .lt(SemanticDeploymentDO::getCreatedAt, timeoutThreshold);

        int count = markAsFailed(runningWrapper, "部署执行超时") + markAsFailed(pendingWrapper, "部署等待超时");
        if (count > 0) {
            log.info("Marked {} stale deployments as FAILED", count);
        }
    }

    private int markAsFailed(QueryWrapper<SemanticDeploymentDO> wrapper, String errorMsg) {
        List<SemanticDeploymentDO> staleRecords = deploymentMapper.selectList(wrapper);
        for (SemanticDeploymentDO record : staleRecords) {
            record.setStatus(SemanticDeployment.DeploymentStatus.FAILED.name());
            record.setErrorMessage(errorMsg);
            record.setEndTime(new Date());
            record.setActiveLock(null);
            deploymentMapper.updateById(record);
            log.warn("Deployment {} marked FAILED: {}", record.getId(), errorMsg);
        }
        return staleRecords.size();
    }
}
