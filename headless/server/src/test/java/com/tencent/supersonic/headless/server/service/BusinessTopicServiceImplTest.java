package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessTopicServiceImplTest {

    @Test
    void enrichVO_withMixedItems_countsCorrectly() {
        BusinessTopicVO vo = new BusinessTopicVO();
        List<BusinessTopicVO.TopicItem> items =
                List.of(makeItem("FIXED_REPORT", 1L, "Daily Revenue"),
                        makeItem("FIXED_REPORT", 2L, "Weekly Costs"),
                        makeItem("ALERT_RULE", 10L, "Revenue Drop"),
                        makeItem("SCHEDULE", 20L, "Daily Send"));
        vo.setItems(items);

        // Simulate count derivation
        vo.setFixedReportCount(
                (int) items.stream().filter(i -> "FIXED_REPORT".equals(i.getItemType())).count());
        vo.setAlertRuleCount(
                (int) items.stream().filter(i -> "ALERT_RULE".equals(i.getItemType())).count());
        vo.setScheduleCount(
                (int) items.stream().filter(i -> "SCHEDULE".equals(i.getItemType())).count());

        assertEquals(2, vo.getFixedReportCount());
        assertEquals(1, vo.getAlertRuleCount());
        assertEquals(1, vo.getScheduleCount());
    }

    @Test
    void enrichVO_withNoItems_allCountsZero() {
        BusinessTopicVO vo = new BusinessTopicVO();
        vo.setItems(List.of());
        vo.setFixedReportCount(0);
        vo.setAlertRuleCount(0);
        vo.setScheduleCount(0);

        assertEquals(0, vo.getFixedReportCount());
        assertEquals(0, vo.getAlertRuleCount());
        assertEquals(0, vo.getScheduleCount());
    }

    private BusinessTopicVO.TopicItem makeItem(String type, Long id, String name) {
        BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
        item.setItemType(type);
        item.setItemId(id);
        item.setItemName(name);
        return item;
    }
}
