package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.service.impl.FixedReportServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedReportServiceImplTest {

    @Test
    void deriveConsumptionStatus_availableWhenLatestSuccess() {
        FixedReportVO vo = new FixedReportVO();
        vo.setLatestResultStatus("SUCCESS");
        vo.setResultExpired(false);
        vo.setScheduleCount(1);
        vo.setEnabledScheduleCount(1);
        FixedReportVO.DeliverySummaryItem item = new FixedReportVO.DeliverySummaryItem();
        item.setEnabled(true);
        vo.setDeliveryChannels(java.util.List.of(item));

        String status = FixedReportServiceImpl.deriveConsumptionStatus(vo);
        assertEquals("AVAILABLE", status);
    }

    @Test
    void deriveConsumptionStatus_noResultWhenNeverExecuted() {
        FixedReportVO vo = new FixedReportVO();
        vo.setLatestResultStatus(null);
        vo.setLatestResultTime(null);
        vo.setScheduleCount(0);

        String status = FixedReportServiceImpl.deriveConsumptionStatus(vo);
        assertEquals("NO_RESULT", status);
    }

    @Test
    void deriveConsumptionStatus_expiredWhenResultExpired() {
        FixedReportVO vo = new FixedReportVO();
        vo.setLatestResultStatus("SUCCESS");
        vo.setResultExpired(true);
        vo.setScheduleCount(1);
        vo.setEnabledScheduleCount(1);

        String status = FixedReportServiceImpl.deriveConsumptionStatus(vo);
        assertEquals("EXPIRED", status);
    }

    @Test
    void deriveConsumptionStatus_recentlyFailedWithPreviousSuccess() {
        FixedReportVO vo = new FixedReportVO();
        vo.setLatestResultStatus("FAILED");
        vo.setPreviousSuccessTime(new java.util.Date());
        vo.setResultExpired(false);
        vo.setScheduleCount(1);
        vo.setEnabledScheduleCount(1);

        String status = FixedReportServiceImpl.deriveConsumptionStatus(vo);
        assertEquals("RECENTLY_FAILED", status);
    }

    @Test
    void deriveConsumptionStatus_noDeliveryWhenNoChannels() {
        FixedReportVO vo = new FixedReportVO();
        vo.setLatestResultStatus("SUCCESS");
        vo.setResultExpired(false);
        vo.setScheduleCount(1);
        vo.setEnabledScheduleCount(1);
        vo.setDeliveryChannels(java.util.List.of());

        String status = FixedReportServiceImpl.deriveConsumptionStatus(vo);
        assertEquals("NO_DELIVERY", status);
    }
}
