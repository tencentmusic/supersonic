package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;

public interface ReportScheduleConfirmationService {

    ReportScheduleConfirmationDO createPending(ReportScheduleConfirmationDO confirmation);

    ReportScheduleConfirmationDO getLatestPending(Long userId, Integer chatId);

    boolean hasPending(Long userId, Integer chatId);

    void updateStatus(Long id, String status);
}
