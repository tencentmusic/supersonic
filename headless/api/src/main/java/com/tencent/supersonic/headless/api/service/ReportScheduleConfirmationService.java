package com.tencent.supersonic.headless.api.service;

import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;

public interface ReportScheduleConfirmationService {

    ReportScheduleConfirmationResp createPending(ReportScheduleConfirmationReq req);

    ReportScheduleConfirmationResp getLatestPending(Long userId, Integer chatId);

    boolean hasPending(Long userId, Integer chatId);

    void updateStatus(Long id, String status);
}
