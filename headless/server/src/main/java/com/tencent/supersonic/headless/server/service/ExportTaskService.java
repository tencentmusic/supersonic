package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.server.persistence.dataobject.ExportTaskDO;

public interface ExportTaskService {

    ExportTaskDO submitExportTask(ExportTaskDO task);

    Page<ExportTaskDO> getTaskList(Page<ExportTaskDO> page, Long userId);

    ExportTaskDO getTaskById(Long id);

    void cancelTask(Long id);

    String getDownloadPath(Long id);
}
