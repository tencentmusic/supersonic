package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionVO;

public interface ReportScheduleService {

    ReportScheduleDO createSchedule(ReportScheduleDO schedule, User user);

    ReportScheduleDO updateSchedule(ReportScheduleDO schedule, User user);

    void deleteSchedule(Long id, User user);

    ReportScheduleDO getScheduleById(Long id, User user);

    Page<ReportScheduleDO> getScheduleList(Page<ReportScheduleDO> page, Long datasetId,
            Boolean enabled, User user);

    void pauseSchedule(Long id, User user);

    void resumeSchedule(Long id, User user);

    void triggerNow(Long id, User user);

    void reschedule(Long id, String newCron);

    Page<ReportExecutionDO> getExecutionList(Page<ReportExecutionDO> page, Long scheduleId,
            String status, User user);

    Page<ReportExecutionVO> getExecutionVOList(Page<ReportExecutionDO> page, Long scheduleId,
            String status, User user);

    ReportExecutionDO getExecutionById(Long scheduleId, Long id, User user);

    void executeReport(Long scheduleId, User user);
}
