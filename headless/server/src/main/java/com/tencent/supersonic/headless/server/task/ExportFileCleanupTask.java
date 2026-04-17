package com.tencent.supersonic.headless.server.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.storage.FileStorage;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ExportTaskMapper;
import com.tencent.supersonic.headless.server.pojo.ExportTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExportFileCleanupTask {

    private final ExportTaskMapper exportTaskMapper;
    private final FileStorage fileStorage;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredExportFiles() {
        QueryWrapper<ExportTaskDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ExportTaskDO::getStatus, ExportTaskStatus.SUCCESS.name())
                .lt(ExportTaskDO::getExpireTime, new Date())
                .isNotNull(ExportTaskDO::getFileLocation);

        List<ExportTaskDO> expiredTasks = exportTaskMapper.selectList(wrapper);
        for (ExportTaskDO task : expiredTasks) {
            try {
                fileStorage.delete(task.getFileLocation());
                log.info("Deleted expired export file: {}", task.getFileLocation());
            } catch (Exception e) {
                log.warn("Failed to delete export file: {}", task.getFileLocation(), e);
            }
            task.setStatus(ExportTaskStatus.EXPIRED.name());
            exportTaskMapper.updateById(task);
        }
        if (!expiredTasks.isEmpty()) {
            log.info("Cleaned up {} expired export tasks", expiredTasks.size());
        }
    }
}
