package com.tencent.supersonic.headless.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.ReportExecutionVO;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;

public interface ReportScheduleService {

    ReportScheduleResp createSchedule(ReportScheduleReq req, User user);

    ReportScheduleResp updateSchedule(ReportScheduleReq req, User user);

    void deleteSchedule(Long id, User user);

    ReportScheduleResp getScheduleById(Long id, User user);

    Page<ReportScheduleResp> getScheduleList(Page<ReportScheduleResp> page, Long datasetId,
            Boolean enabled, User user);

    void pauseSchedule(Long id, User user);

    void resumeSchedule(Long id, User user);

    void triggerNow(Long id, User user);

    Page<ReportExecutionResp> getExecutionList(Page<ReportExecutionResp> page, Long scheduleId,
            String status, User user);

    Page<ReportExecutionVO> getExecutionVOList(Page<ReportExecutionVO> page, Long scheduleId,
            String status, User user);

    ReportExecutionResp getExecutionById(Long scheduleId, Long id, User user);

    void executeReport(Long scheduleId, User user);
}
