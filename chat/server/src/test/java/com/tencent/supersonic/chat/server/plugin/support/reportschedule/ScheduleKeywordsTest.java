package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScheduleKeywords — intent scoring and recognition")
class ScheduleKeywordsTest {

    // ── preferCreate ────────────────────────────────────────────────────

    @Test
    @DisplayName("hard-create: context reference + create verb → true without scoring")
    void hardCreate_contextPlusVerb() {
        assertTrue(ScheduleKeywords.preferCreate("基于刚才那个报表，每天9点发给我"));
    }

    @Test
    @DisplayName("hard-create: context + frequency + time → true")
    void hardCreate_contextFrequencyTime() {
        assertTrue(ScheduleKeywords.preferCreate("基于刚才查询结果，每天下午3点推送给我"));
    }

    @Test
    @DisplayName("score-create: frequency + time ≥3 → true")
    void scoreCreate_frequencyPlusTime() {
        assertTrue(ScheduleKeywords.preferCreate("每天10:30发我"));
    }

    @Test
    @DisplayName("ambiguous action verb alone does NOT trigger create")
    void ambiguousVerbAlone_doesNotTriggerCreate() {
        // AG-guard: "帮我查数据发给我" should not produce a schedule intent
        assertFalse(ScheduleKeywords.preferCreate("帮我查数据发给我"));
    }

    @Test
    @DisplayName("CREATE_ACTION verb alone (no frequency) does not reach threshold")
    void createActionVerbWithoutFrequency_belowThreshold() {
        assertFalse(ScheduleKeywords.preferCreate("把这个发给我"));
    }

    // ── preferList ──────────────────────────────────────────────────────

    @Test
    @DisplayName("hard-list: exact list phrase without create signals → true")
    void hardList_exactPhrase() {
        assertTrue(ScheduleKeywords.preferList("我的定时报表有哪些"));
        assertTrue(ScheduleKeywords.preferList("查看报表任务"));
    }

    @Test
    @DisplayName("list phrase + create verb → NOT list (create wins)")
    void listPhrasePlusCreateVerb_notList() {
        assertFalse(ScheduleKeywords.preferList("我的定时报表每天发给我"));
    }

    // ── createScore / listScore ──────────────────────────────────────────

    @Test
    @DisplayName("createScore: verb(2) + freq(2) + time(1) = 5")
    void createScore_verbFreqTime() {
        assertEquals(5, ScheduleKeywords.createScore("每天10:30发给我"));
    }

    @Test
    @DisplayName("listScore: exact list phrase scores ≥3")
    void listScore_explicitPhrase() {
        assertTrue(ScheduleKeywords.listScore("我的定时报表") >= 3);
    }

    @Test
    @DisplayName("list phrase + create verb → preferList returns false (create wins)")
    void listScore_createVerbPreventsListIntent() {
        // listScore = 3(phrase) + 1(prefix+suffix) - 4(create verb) = 0, not ≥ 3 → false
        assertFalse(ScheduleKeywords.preferList("我的定时报表每天发给我"));
    }

    // ── edge cases ──────────────────────────────────────────────────────

    @Test
    @DisplayName("null input returns false / 0 without NPE")
    void nullInput_safe() {
        assertFalse(ScheduleKeywords.preferCreate(null));
        assertFalse(ScheduleKeywords.preferList(null));
        assertEquals(0, ScheduleKeywords.createScore(null));
    }

    @Test
    @DisplayName("TRIGGER_NOW in text adds +1 to createScore")
    void triggerNow_boostsCreateScore() {
        int withTrigger = ScheduleKeywords.createScore("每天10:30发我，现在先推一次");
        int withoutTrigger = ScheduleKeywords.createScore("每天10:30发我");
        assertEquals(withoutTrigger + 1, withTrigger);
    }
}
