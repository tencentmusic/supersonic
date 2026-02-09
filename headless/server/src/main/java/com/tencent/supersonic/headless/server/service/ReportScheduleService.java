package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;

public interface ReportScheduleService {

    ReportScheduleDO createSchedule(ReportScheduleDO schedule);

    ReportScheduleDO updateSchedule(ReportScheduleDO schedule);

    void deleteSchedule(Long id);

    ReportScheduleDO getScheduleById(Long id);

    Page<ReportScheduleDO> getScheduleList(Page<ReportScheduleDO> page, Long datasetId,
            Boolean enabled);

    void pauseSchedule(Long id);

    void resumeSchedule(Long id);

    void triggerNow(Long id);

    void reschedule(Long id, String newCron);

    Page<ReportExecutionDO> getExecutionList(Page<ReportExecutionDO> page, Long scheduleId,
            String status);

    ReportExecutionDO getExecutionById(Long id);

    void executeReport(Long scheduleId, User user);
}
