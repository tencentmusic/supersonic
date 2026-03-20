# ReportSchedule Code Review Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all code-review findings on the natural-language report schedule feature in priority order: testability (constructor injection + Mockito tests), reliability (constants, configurability), and maintainability (decomposition + annotations).

**Architecture:** Replace every `ContextUtils.getBean()` instance-method call in `ReportScheduleQuery` with `@RequiredArgsConstructor` constructor injection + `@PostConstruct` registration, then rewrite the existing `GenericApplicationContext`-based test with pure Mockito and full Anti-Goal coverage. Constant extraction and method decomposition follow as independent cleanup tasks.

**Tech Stack:** Java 21, Spring Boot 3.4.x, Lombok, Mockito 5, JUnit 5, MyBatis-Plus

---

## File Map

| File | Action |
|------|--------|
| `chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java` | Modify — constructor injection, `@PostConstruct`, constant extraction, `handleCreate` decomposition, `@AiGenerated` |
| `chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ScheduleKeywordsTest.java` | Create — pure unit tests for intent scoring / recognition |
| `chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java` | Rewrite — Mockito-based, full Anti-Goal coverage, no GenericApplicationContext |
| `chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/ReportScheduleParser.java` | Modify — add `@AiGenerated` annotation |

---

### Task 1: ScheduleKeywordsTest — pure unit coverage

Tests the pure-static intent scoring logic with no dependencies. Independent of all other tasks.

**Files:**
- Create: `chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ScheduleKeywordsTest.java`

- [ ] **Step 1: Write the test file**

```java
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
    @DisplayName("listScore: list phrase + create verb drops to negative")
    void listScore_createVerbDropsScore() {
        assertTrue(ScheduleKeywords.listScore("我的定时报表每天发给我") < 0);
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
        int withTrigger    = ScheduleKeywords.createScore("每天10:30发我，现在先推一次");
        int withoutTrigger = ScheduleKeywords.createScore("每天10:30发我");
        assertEquals(withoutTrigger + 1, withTrigger);
    }
}
```

- [ ] **Step 2: Run — expect PASS (logic already implemented)**

```bash
mvn test -pl chat/server -Dtest=ScheduleKeywordsTest -am 2>&1 | tail -20
```

Expected: `Tests run: 11, Failures: 0, Errors: 0`

- [ ] **Step 3: Commit**

```bash
git add chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ScheduleKeywordsTest.java
git commit -m "test(schedule): add ScheduleKeywordsTest — pure unit coverage for intent scoring"
```

---

### Task 2: Rewrite ReportScheduleQueryTest with Mockito (write first — TDD)

Write the new Mockito-based tests **before** the constructor injection refactor. They will fail to compile until Task 3 adds the new constructor — that is the intended failing state.

**Files:**
- Modify: `chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java`

- [ ] **Step 1: Replace file contents with Mockito-based tests**

```java
package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import com.tencent.supersonic.headless.server.service.ReportScheduleConfirmationService;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportScheduleQuery — intent handling and two-step confirmation flow")
class ReportScheduleQueryTest {

    @Mock private ReportScheduleService scheduleService;
    @Mock private ReportScheduleConfirmationService confirmationService;
    @Mock private ChatManageService chatManageService;
    @Mock private DataSetService dataSetService;
    @Mock private ReportDeliveryService deliveryService;

    private ReportScheduleQuery query;

    @BeforeEach
    void setUp() {
        query = new ReportScheduleQuery(
                scheduleService, confirmationService, chatManageService,
                dataSetService, deliveryService);
    }

    // ── Anti-Goal AG-01: cross-tenant isolation ─────────────────────────

    @Test
    @DisplayName("AG-01: getScheduleById returns null for cross-tenant id → ERROR_SCHEDULE_NOT_FOUND")
    void cancelWithUnknownId_returnsNotFound() {
        // TenantSqlInterceptor filters cross-tenant rows → null from service
        when(scheduleService.getScheduleById(999L)).thenReturn(null);

        query.setParseInfo(buildParseInfo("取消报表 #999", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("#999"));
    }

    @Test
    @DisplayName("AG-01: pause on cross-tenant id (null return) → error, not silent skip")
    void pauseWithUnknownId_returnsNotFound() {
        when(scheduleService.getScheduleById(42L)).thenReturn(null);

        query.setParseInfo(buildParseInfo("暂停报表 #42", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("#42"));
    }

    // ── CREATE intent — validation paths ──────────────────────────────

    @Test
    @DisplayName("CREATE without frequency → ERROR_SPECIFY_FREQUENCY, no confirmation saved")
    void createWithoutFrequency_returnsError() {
        query.setParseInfo(buildParseInfo("基于刚才那个报表，推给我", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_SPECIFY_FREQUENCY, resp.getMessage());
        verify(confirmationService, never()).createPending(any());
    }

    @Test
    @DisplayName("CREATE without prior schedulable query → ERROR_SPECIFY_REPORT_CONTENT")
    void createWithoutPreviousQuery_returnsError() {
        when(chatManageService.getChatQueries(anyInt())).thenReturn(List.of());

        query.setParseInfo(buildParseInfo("每天9点发给我", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_SPECIFY_REPORT_CONTENT, resp.getMessage());
        verify(confirmationService, never()).createPending(any());
    }

    @Test
    @DisplayName("CREATE without delivery config → ERROR_NO_DELIVERY_CONFIG")
    void createWithoutDeliveryConfig_returnsError() {
        when(chatManageService.getChatQueries(anyInt()))
                .thenReturn(List.of(buildPreviousQuery(5001L, buildSchedulableParseInfo(1))));

        Page<ReportDeliveryConfigDO> emptyPage = new Page<>(1, 100);
        emptyPage.setRecords(List.of());
        when(deliveryService.getConfigList(any())).thenReturn(emptyPage);

        DataSetResp ds = new DataSetResp(); ds.setId(1L); ds.setName("订单");
        when(dataSetService.getDataSet(1L)).thenReturn(ds);

        query.setParseInfo(buildParseInfo("每天9点发给我", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_NO_DELIVERY_CONFIG, resp.getMessage());
    }

    // ── CREATE → CONFIRM two-step flow ────────────────────────────────

    @Test
    @DisplayName("CREATE builds pending confirmation; CONFIRM executes and returns schedule id")
    void createThenConfirm_createsSchedule() {
        // Arrange: previous query with schedulable parse
        when(chatManageService.getChatQueries(anyInt()))
                .thenReturn(List.of(buildPreviousQuery(5001L, buildSchedulableParseInfo(1))));

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(9L); config.setName("飞书群"); config.setEnabled(true); config.setTenantId(10L);
        Page<ReportDeliveryConfigDO> configPage = new Page<>(1, 100);
        configPage.setRecords(List.of(config));
        when(deliveryService.getConfigList(any())).thenReturn(configPage);
        when(deliveryService.getConfigById(9L)).thenReturn(config);

        DataSetResp ds = new DataSetResp(); ds.setId(1L); ds.setName("订单明细");
        when(dataSetService.getDataSet(1L)).thenReturn(ds);

        // Capture stored confirmation
        ReportScheduleConfirmationDO[] stored = {null};
        doAnswer(inv -> {
            ReportScheduleConfirmationDO c = inv.getArgument(0);
            c.setId(1L); c.setStatus("PENDING"); stored[0] = c;
            return c;
        }).when(confirmationService).createPending(any());

        // Act: CREATE
        query.setParseInfo(buildParseInfo("基于刚才那个报表，每天10:30推送给我", 88, 6001L, 1001L, "tester", 10L));
        QueryResult createResult = query.build();
        ReportScheduleResp createResp = (ReportScheduleResp) createResult.getResponse();

        assertTrue(createResp.isSuccess());
        assertTrue(createResp.isNeedConfirm());
        assertEquals("0 30 10 * * ?", createResp.getCronExpression());
        verify(confirmationService).createPending(any());

        // Arrange: confirmation lookup
        when(confirmationService.getLatestPending(1001L, 88)).thenReturn(stored[0]);

        ReportScheduleDO created = new ReportScheduleDO();
        created.setId(123L);
        when(scheduleService.createSchedule(any())).thenReturn(created);

        // Act: CONFIRM
        query.setParseInfo(buildParseInfo("确认", 88, 6002L, 1001L, "tester", 10L));
        QueryResult confirmResult = query.build();
        ReportScheduleResp confirmResp = (ReportScheduleResp) confirmResult.getResponse();

        assertTrue(confirmResp.isSuccess());
        assertEquals(123L, confirmResp.getScheduleId());
        verify(scheduleService).createSchedule(any());
        verify(scheduleService, never()).triggerNow(any());
    }

    @Test
    @DisplayName("CREATE with triggerNow phrase → confirmation stores triggerNow=true, confirm triggers execution")
    void createWithTriggerNow_triggersAfterConfirm() {
        when(chatManageService.getChatQueries(anyInt()))
                .thenReturn(List.of(buildPreviousQuery(5001L, buildSchedulableParseInfo(1))));

        ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();
        config.setId(9L); config.setName("飞书群"); config.setEnabled(true); config.setTenantId(10L);
        Page<ReportDeliveryConfigDO> configPage = new Page<>(1, 100);
        configPage.setRecords(List.of(config));
        when(deliveryService.getConfigList(any())).thenReturn(configPage);
        when(deliveryService.getConfigById(9L)).thenReturn(config);

        DataSetResp ds = new DataSetResp(); ds.setId(1L); ds.setName("订单");
        when(dataSetService.getDataSet(1L)).thenReturn(ds);

        ReportScheduleConfirmationDO[] stored = {null};
        doAnswer(inv -> {
            ReportScheduleConfirmationDO c = inv.getArgument(0);
            c.setId(2L); c.setStatus("PENDING"); stored[0] = c;
            return c;
        }).when(confirmationService).createPending(any());

        query.setParseInfo(buildParseInfo("基于刚才那个报表，每天10:30推送，现在先发一次", 88, 6001L, 1001L, "tester", 10L));
        QueryResult createResult = query.build();
        ReportScheduleResp createResp = (ReportScheduleResp) createResult.getResponse();

        assertTrue(Boolean.TRUE.equals(createResp.getConfirmAction().getParams().get("triggerNow")));

        when(confirmationService.getLatestPending(1001L, 88)).thenReturn(stored[0]);
        ReportScheduleDO created = new ReportScheduleDO(); created.setId(124L);
        when(scheduleService.createSchedule(any())).thenReturn(created);

        query.setParseInfo(buildParseInfo("确认", 88, 6002L, 1001L, "tester", 10L));
        QueryResult confirmResult = query.build();
        ReportScheduleResp confirmResp = (ReportScheduleResp) confirmResult.getResponse();

        assertTrue(confirmResp.isSuccess());
        verify(scheduleService).triggerNow(124L);
        assertTrue(confirmResp.getMessage().contains("已触发一次立即执行"));
    }

    @Test
    @DisplayName("CONFIRM with no pending → ERROR_NO_PENDING")
    void confirmWithNoPending_returnsError() {
        when(confirmationService.getLatestPending(1L, 1)).thenReturn(null);

        query.setParseInfo(buildParseInfo("确认", 1, 200L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertFalse(resp.isSuccess());
        assertEquals(ScheduleMessages.ERROR_NO_PENDING, resp.getMessage());
    }

    // ── LIST ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LIST with no schedules → LIST_EMPTY message")
    void list_empty() {
        Page<ReportScheduleDO> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        when(scheduleService.getScheduleList(any(), isNull(), isNull())).thenReturn(emptyPage);

        query.setParseInfo(buildParseInfo("我的定时报表有哪些", 1, 100L, 1L, "u", 10L));
        QueryResult result = query.build();
        ReportScheduleResp resp = (ReportScheduleResp) result.getResponse();

        assertTrue(resp.isSuccess());
        assertEquals(ScheduleMessages.LIST_EMPTY, resp.getMessage());
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private SemanticParseInfo buildParseInfo(String queryText, Integer chatId, Long queryId,
            Long userId, String userName, Long tenantId) {
        PluginParseResult pr = new PluginParseResult();
        pr.setQueryText(queryText); pr.setChatId(chatId); pr.setQueryId(queryId);
        pr.setUserId(userId); pr.setUserName(userName); pr.setTenantId(tenantId);

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode(ReportScheduleQuery.QUERY_MODE);
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.CONTEXT, pr);
        parseInfo.setProperties(props);
        return parseInfo;
    }

    private QueryResp buildPreviousQuery(Long questionId, SemanticParseInfo parseInfo) {
        QueryResp q = new QueryResp();
        q.setQuestionId(questionId);
        q.setQueryText("历史查询");
        q.setParseInfos(List.of(parseInfo));
        return q;
    }

    private SemanticParseInfo buildSchedulableParseInfo(int id) {
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSetId(1L); dataSet.setName("订单明细"); dataSet.setBizName("order_detail");

        SchemaElement metric = new SchemaElement();
        metric.setName("订单数"); metric.setBizName("order_cnt"); metric.setDefaultAgg("COUNT");

        SemanticParseInfo pi = new SemanticParseInfo();
        pi.setId(id); pi.setQueryType(QueryType.AGGREGATE);
        pi.setAggType(AggregateTypeEnum.NONE); pi.setDataSet(dataSet);
        pi.getMetrics().add(metric);
        return pi;
    }
}
```

- [ ] **Step 2: Run — expect COMPILE FAILURE** (constructor doesn't exist yet)

```bash
mvn test-compile -pl chat/server -am 2>&1 | grep "error:" | head -5
```

Expected: `error: constructor ReportScheduleQuery in class ReportScheduleQuery cannot be applied to given types`

- [ ] **Step 3: Commit the failing test**

```bash
git add chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java
git commit -m "test(schedule): rewrite ReportScheduleQueryTest with Mockito + Anti-Goal coverage [wip — fails until Task 3]"
```

---

### Task 3: Constructor injection refactor in ReportScheduleQuery

Replace all 14 instance-method `ContextUtils.getBean()` calls with injected `private final` fields. Keep the one call in the static `hasPendingConfirmation()` method as-is.

**Files:**
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java`

- [ ] **Step 1: Replace class header and constructor**

Change the class declaration from:
```java
@Slf4j
@Component
public class ReportScheduleQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "REPORT_SCHEDULE";
    private static final SimpleDateFormat DATE_FORMATTER = ...;
    private static final long CONFIRMATION_EXPIRE_MS = 5 * 60 * 1000;

    public ReportScheduleQuery() {
        PluginQueryManager.register(QUERY_MODE, this);
    }
```

To:
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduleQuery extends PluginSemanticQuery {

    public static final String QUERY_MODE = "REPORT_SCHEDULE";
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long CONFIRMATION_EXPIRE_MS = 5 * 60 * 1000;

    private final ReportScheduleService scheduleService;
    private final ReportScheduleConfirmationService confirmationService;
    private final ChatManageService chatManageService;
    private final DataSetService dataSetService;
    private final ReportDeliveryService deliveryService;

    @PostConstruct
    public void register() {
        PluginQueryManager.register(QUERY_MODE, this);
    }
```

Add `@PostConstruct` import: `import jakarta.annotation.PostConstruct;`
Add Lombok import: `import lombok.RequiredArgsConstructor;`
Remove `import com.tencent.supersonic.common.util.ContextUtils;` if no other references exist — but keep it for `hasPendingConfirmation()`.

- [ ] **Step 2: Replace all instance-method ContextUtils.getBean() calls**

For each method, replace the local `ContextUtils.getBean(XxxService.class)` call with the injected field. The mapping is:

| Method | Old call | New usage |
|--------|----------|-----------|
| `savePendingConfirmation` (line 159) | `ContextUtils.getBean(ReportScheduleConfirmationService.class)` | `this.confirmationService` |
| `handleConfirm` (line 225) | same | `this.confirmationService` |
| `executeCreate` (line 260) | `ContextUtils.getBean(ReportScheduleService.class)` | `this.scheduleService` |
| `executeCancel` (line 303) | same | `this.scheduleService` |
| `handleList` (line 380) | same | `this.scheduleService` |
| `handleCancel` (line 421) | same | `this.scheduleService` |
| `handlePause` (line 449) | same | `this.scheduleService` |
| `handleResume` (line 475) | same | `this.scheduleService` |
| `handleTrigger` (line 500) | same | `this.scheduleService` |
| `handleStatus` (line 526) | same | `this.scheduleService` |
| `resolveSubscriptionSource` (line 665) | `ContextUtils.getBean(ChatManageService.class)` | `this.chatManageService` |
| `resolveDeliveryConfigIds` (line 752) | `ContextUtils.getBean(ReportDeliveryService.class)` | `this.deliveryService` |
| `getDataSetName` (line 781) | `ContextUtils.getBean(DataSetService.class)` | `this.dataSetService` |
| `resolveChannelName` (line 797) | `ContextUtils.getBean(ReportDeliveryService.class)` | `this.deliveryService` |

For `savePendingConfirmation`, also remove the null-guard `if (confirmationService != null)` — injected field is never null.

- [ ] **Step 3: Compile**

```bash
mvn compile -pl chat/server -am 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Run tests — expect PASS**

```bash
mvn test -pl chat/server -Dtest="ReportScheduleQueryTest,ScheduleKeywordsTest" -am 2>&1 | tail -20
```

Expected: `Tests run: 19, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
git commit -m "refactor(schedule): replace ContextUtils.getBean with constructor injection + @PostConstruct"
```

---

### Task 4: Extract DEFAULT_RETRY_COUNT constant + configurable CONFIRMATION_EXPIRE_MS

**Files:**
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java`

- [ ] **Step 1: Extract constants**

Replace the magic number in `executeCreate` (line 271: `schedule.setRetryCount(3)`) and the hardcoded expiry:

```java
// Before — in class body:
private static final long CONFIRMATION_EXPIRE_MS = 5 * 60 * 1000;

// After — in class body:
private static final int DEFAULT_RETRY_COUNT = 3;

@Value("${s2.schedule.confirmation.expire-ms:300000}")
private long confirmationExpireMs;
```

Update `executeCreate` to use `DEFAULT_RETRY_COUNT`:
```java
schedule.setRetryCount(DEFAULT_RETRY_COUNT);
```

Update `savePendingConfirmation` to use `confirmationExpireMs` instead of `CONFIRMATION_EXPIRE_MS`.

- [ ] **Step 2: Update test setUp to set confirmationExpireMs**

In `ReportScheduleQueryTest.setUp()`, add after constructing `query`:
```java
org.springframework.test.util.ReflectionTestUtils.setField(query, "confirmationExpireMs", 300_000L);
```

- [ ] **Step 3: Run tests**

```bash
mvn test -pl chat/server -Dtest="ReportScheduleQueryTest,ScheduleKeywordsTest" -am 2>&1 | tail -10
```

Expected: `Tests run: 19, Failures: 0, Errors: 0`

- [ ] **Step 4: Commit**

```bash
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
git add chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java
git commit -m "refactor(schedule): extract DEFAULT_RETRY_COUNT constant, make confirmation expiry configurable via @Value"
```

---

### Task 5: Decompose handleCreate into focused sub-methods

`handleCreate` is 65 lines. Split into: validation → source resolution → params building → response building.

**Files:**
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java`

- [ ] **Step 1: Extract validateAndResolveSource helper**

Pull the source/queryConfig/deliveryConfig validation block (lines 320–337) into:

```java
/**
 * Validates subscription source and returns it, or returns an error response.
 * Returns null if validation passes; non-null means return the error immediately.
 */
private ReportScheduleResp validateSource(ReportSubscriptionSource source,
        String deliveryConfigIds) {
    if (source == null || source.getSourceDataSetId() == null) {
        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                .message(ERROR_SPECIFY_REPORT_CONTENT).needConfirm(false).build();
    }
    if (StringUtils.isBlank(source.getQueryConfigSnapshot())) {
        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                .message(ERROR_UNSUPPORTED_REPORT_CONTEXT).needConfirm(false).build();
    }
    if (StringUtils.isBlank(deliveryConfigIds)) {
        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                .message(ERROR_NO_DELIVERY_CONFIG).needConfirm(false).build();
    }
    return null;
}
```

- [ ] **Step 2: Extract buildCreateParams helper**

Pull the `HashMap` construction (lines 345–361) into:

```java
private Map<String, Object> buildCreateParams(Long datasetId, String cronExpression,
        String queryConfig, String outputFormat, String deliveryConfigIds,
        String scheduleName, PluginParseResult pluginParseResult,
        ReportSubscriptionSource source, boolean triggerNow) {
    Map<String, Object> params = new HashMap<>();
    params.put("datasetId", datasetId);
    params.put("cronExpression", cronExpression);
    params.put("queryText", pluginParseResult.getQueryText());
    params.put("queryConfig", queryConfig);
    params.put("outputFormat", outputFormat);
    params.put("deliveryConfigIds", deliveryConfigIds);
    params.put("scheduleName", scheduleName);
    params.put("ownerId", pluginParseResult.getUserId());
    params.put("tenantId", pluginParseResult.getTenantId());
    params.put("createdBy", pluginParseResult.getUserName());
    params.put("triggerNow", triggerNow);
    params.put("sourceQueryId", source.getSourceQueryId());
    params.put("sourceParseId", source.getSourceParseId());
    params.put("sourceDataSetId", source.getSourceDataSetId());
    params.put("sourceSummary", source.getSummaryText());
    return params;
}
```

- [ ] **Step 3: Rewrite handleCreate as orchestrator (~25 lines)**

```java
private ReportScheduleResp handleCreate(String queryText, Integer chatId,
        PluginParseResult pluginParseResult, Long currentUserId) {
    String cronExpression = parseCronExpression(queryText);
    if (cronExpression == null) {
        return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(false)
                .message(ERROR_SPECIFY_FREQUENCY).needConfirm(false).build();
    }

    ReportSubscriptionSource source = resolveSubscriptionSource(pluginParseResult);
    String deliveryConfigIds = resolveDeliveryConfigIds(pluginParseResult);
    ReportScheduleResp validationError = validateSource(source, deliveryConfigIds);
    if (validationError != null) {
        return validationError;
    }

    String cronDescription = describeCron(cronExpression);
    String outputFormat = parseOutputFormat(queryText);
    String dataSetName = getDataSetName(source.getSourceDataSetId());
    String scheduleName = buildScheduleName(dataSetName, cronDescription);
    boolean triggerNow = TRIGGER_NOW.stream().anyMatch(queryText::contains);

    Map<String, Object> params = buildCreateParams(source.getSourceDataSetId(), cronExpression,
            source.getQueryConfigSnapshot(), outputFormat, deliveryConfigIds,
            scheduleName, pluginParseResult, source, triggerNow);

    savePendingConfirmation(chatId, ScheduleIntent.CREATE, params, currentUserId,
            pluginParseResult.getTenantId(), source);

    String displayName = StringUtils.isNotBlank(source.getSummaryText()) ? source.getSummaryText()
            : dataSetName != null ? dataSetName : String.valueOf(source.getSourceDataSetId());
    String confirmMsg = triggerNow
            ? String.format(CONFIRM_CREATE_WITH_TRIGGER, displayName, cronDescription)
            : String.format(CONFIRM_CREATE, displayName, cronDescription);

    return ReportScheduleResp.builder().intent(ScheduleIntent.CREATE).success(true)
            .message(confirmMsg).needConfirm(true)
            .confirmAction(ReportScheduleResp.ConfirmAction.builder()
                    .action("CREATE_SCHEDULE").params(params).build())
            .cronExpression(cronExpression).cronDescription(cronDescription).build();
}
```

- [ ] **Step 4: Run tests**

```bash
mvn test -pl chat/server -Dtest="ReportScheduleQueryTest,ScheduleKeywordsTest" -am 2>&1 | tail -10
```

Expected: `Tests run: 19, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
git commit -m "refactor(schedule): decompose handleCreate into validateSource + buildCreateParams helpers"
```

---

### Task 6: Add explanatory comments and @AiGenerated annotations

**Files:**
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java`
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/ReportScheduleParser.java`

- [ ] **Step 1: Add class-level Javadoc to ReportScheduleQuery**

```java
/**
 * Plugin query for natural-language report scheduling. Handles create, list, cancel, pause,
 * resume, trigger, and status intents via a two-step confirmation flow.
 *
 * <p>AI-generated logic — reviewed 2026-03-20. Key invariants:
 * <ul>
 *   <li>Intent extraction is keyword-based (no LLM) — see {@link ScheduleKeywords}.
 *   <li>CREATE always goes through pending confirmation before persistence.
 *   <li>Tenant isolation is delegated to {@code TenantSqlInterceptor} at the DB layer.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduleQuery extends PluginSemanticQuery {
```

- [ ] **Step 2: Add inline comments for non-obvious decisions**

In `extractIntent()`, before the TRIGGER block at the bottom, add:
```java
// TRIGGER checked last: "立即"/"现在" also appear in TRIGGER_NOW (CREATE context).
// preferCreate() is evaluated first; TRIGGER only fires when a #ID is present.
```

In `ScheduleKeywords.preferCreate()` (or inline in `extractIntent` where the threshold is applied), add:
```java
// Score threshold ≥3: a single CREATE verb alone scores 2 — too ambiguous.
// At least verb+frequency (4) or context+verb (4) is required.
```

In `hasPendingConfirmation()`, add:
```java
// Static utility: cannot use the injected confirmationService field.
// ContextUtils.getBean() is intentional here — this method is called from
// ReportScheduleParser (a different bean), not from instance methods.
```

- [ ] **Step 3: Add class-level Javadoc to ReportScheduleParser**

```java
/**
 * Keyword-based parser for report schedule intents. Runs without LLM and without a
 * DB-registered plugin, so scheduled-report commands work for any agent out of the box.
 *
 * <p>AI-generated — reviewed 2026-03-20.
 */
public class ReportScheduleParser implements ChatQueryParser {
```

- [ ] **Step 4: Run full test suite to confirm nothing broken**

```bash
mvn test -pl chat/server -Dtest="ReportScheduleQueryTest,ScheduleKeywordsTest" -am 2>&1 | tail -10
```

Expected: `Tests run: 19, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/ReportScheduleParser.java
git commit -m "docs(schedule): add class-level Javadoc and inline rationale comments for extractIntent asymmetry and scoring thresholds"
```

---

## Summary

| Task | Priority | Risk | Effort |
|------|----------|------|--------|
| 1 — ScheduleKeywordsTest | 🔴 Critical | None | 15 min |
| 2 — Rewrite tests (write first) | 🔴 Critical | Low | 20 min |
| 3 — Constructor injection | 🔴 Critical | Low (mechanical) | 20 min |
| 4 — Constants + @Value | 🟡 General | None | 10 min |
| 5 — handleCreate decomposition | 🟡 General | Low | 15 min |
| 6 — Comments + @AiGenerated | 🔵 Suggestion | None | 10 min |

**Anti-Goals covered by tests:**
- AG-01 (cross-tenant isolation): Tasks 2+3 — cancel/pause with cross-tenant null return
- AG-05 (no reflection): Task 3 — `ContextUtils.getBean()` removed from instance methods
- CREATE without source / frequency / delivery: Task 2
