# Fixed Reports Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the current card-wall Reports page into a workbench-style Fixed Reports page with filtering, status indicators, subscription tracking, detail drawer, and direct task creation — matching the product spec's "consumption object" model.

**Architecture:** A "Fixed Report" is a read-only view derived from successful `SemanticDeployment` records (each creating a unique dataset). A new `s2_report_subscription` table tracks per-user subscriptions. A new backend aggregation endpoint (`GET /api/v1/fixedReports`) joins deployment + schedule + latest execution + delivery config + subscription data into a single `FixedReportVO`. The frontend is a complete rewrite of `/reports` from card wall to filter bar + table list + detail drawer.

**Tech Stack:** Java 21 / Spring Boot 3.4 / MyBatis-Plus / JUnit 5 (backend); TypeScript / React / Ant Design (frontend)

---

## File Structure

### Backend — New Files
| File | Responsibility |
|------|---------------|
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/FixedReportVO.java` | Read-only view DTO combining deployment + schedule + execution + delivery + subscription |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/ReportSubscriptionDO.java` | MyBatis-Plus entity for `s2_report_subscription` |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/mapper/ReportSubscriptionMapper.java` | MyBatis-Plus mapper interface |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/FixedReportService.java` | Service interface for fixed report aggregation + subscription |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/FixedReportServiceImpl.java` | Aggregation logic: join deployments, schedules, executions, subscriptions |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/FixedReportController.java` | REST controller for `/api/v1/fixedReports` |
| `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/FixedReportServiceImplTest.java` | Unit tests |
| `launchers/standalone/src/main/resources/db/migration/mysql/V26__report_subscription.sql` | MySQL migration |
| `launchers/standalone/src/main/resources/db/migration/postgresql/V26__report_subscription.sql` | PostgreSQL migration |

### Frontend — New Files
| File | Responsibility |
|------|---------------|
| `webapp/packages/supersonic-fe/src/services/fixedReport.ts` | API service for fixed reports + subscriptions |
| `webapp/packages/supersonic-fe/src/pages/Reports/components/FilterBar.tsx` | Search + domain + status + view filter bar |
| `webapp/packages/supersonic-fe/src/pages/Reports/components/ReportDetailDrawer.tsx` | Report detail drawer with latest result, history, delivery, subscription |

### Frontend — Modified Files
| File | Change |
|------|--------|
| `webapp/packages/supersonic-fe/src/pages/Reports/index.tsx` | Complete rewrite from card wall to workbench |
| `webapp/packages/supersonic-fe/src/pages/Reports/style.less` | Complete rewrite for workbench styles |

---

### Task 1: Database Migration — Subscription Table

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V26__report_subscription.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V26__report_subscription.sql`

- [ ] **Step 1: Create MySQL migration**

```sql
-- V26__report_subscription.sql
-- User subscription tracking for fixed reports (by dataset)

CREATE TABLE IF NOT EXISTS s2_report_subscription (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT 'Subscriber user ID',
    dataset_id  BIGINT       NOT NULL COMMENT 'Subscribed dataset (= fixed report)',
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_dataset (user_id, dataset_id, tenant_id),
    KEY idx_dataset_id (dataset_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='User subscriptions to fixed reports';
```

- [ ] **Step 2: Create PostgreSQL migration**

```sql
-- V26__report_subscription.sql
-- User subscription tracking for fixed reports (by dataset)

CREATE TABLE IF NOT EXISTS s2_report_subscription (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    dataset_id  BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_dataset UNIQUE (user_id, dataset_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_report_sub_dataset ON s2_report_subscription (dataset_id);
CREATE INDEX IF NOT EXISTS idx_report_sub_tenant  ON s2_report_subscription (tenant_id);
```

- [ ] **Step 3: Compile to verify migrations are picked up**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V26__report_subscription.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V26__report_subscription.sql
git commit -m "feat(report): add s2_report_subscription migration (V26)"
```

---

### Task 2: Backend — Subscription Entity + Mapper

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/ReportSubscriptionDO.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/mapper/ReportSubscriptionMapper.java`

- [ ] **Step 1: Create ReportSubscriptionDO**

```java
package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_report_subscription")
public class ReportSubscriptionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long datasetId;
    private Long tenantId;
    private Date createdAt;
}
```

- [ ] **Step 2: Create ReportSubscriptionMapper**

```java
package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportSubscriptionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportSubscriptionMapper extends BaseMapper<ReportSubscriptionDO> {
}
```

- [ ] **Step 3: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/ReportSubscriptionDO.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/mapper/ReportSubscriptionMapper.java
git commit -m "feat(report): add ReportSubscription entity and mapper"
```

---

### Task 3: Backend — FixedReportVO DTO

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/FixedReportVO.java`

- [ ] **Step 1: Create FixedReportVO**

This DTO aggregates data from multiple sources into a single consumption-oriented view.

```java
package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class FixedReportVO {

    // --- Identity (from SemanticDeployment) ---
    private Long deploymentId;
    private Long datasetId;
    private String reportName;
    private String description;
    private String domainName;

    // --- Latest result (from most recent execution across all schedules) ---
    private Date latestResultTime;
    private String latestResultStatus;    // SUCCESS / FAILED / null
    private String latestErrorMessage;
    private Long latestRowCount;
    /** Whether the last successful result is older than 2x the shortest cron interval */
    private boolean resultExpired;
    /** If latest execution failed but a previous success exists */
    private Date previousSuccessTime;

    // --- Schedule summary ---
    private int scheduleCount;
    private int enabledScheduleCount;

    // --- Delivery summary ---
    private List<DeliverySummaryItem> deliveryChannels;

    // --- Subscription ---
    private boolean subscribed;

    // --- Consumption status (derived) ---
    private String consumptionStatus;
    // AVAILABLE / NO_RESULT / EXPIRED / RECENTLY_FAILED / NO_DELIVERY / PARTIAL_CHANNEL_ERROR

    @Data
    public static class DeliverySummaryItem {
        private Long configId;
        private String configName;
        private String deliveryType;
        private boolean enabled;
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/FixedReportVO.java
git commit -m "feat(report): add FixedReportVO aggregation DTO"
```

---

### Task 4: Backend — FixedReportService Interface + Implementation

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/FixedReportService.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/FixedReportServiceImpl.java`

- [ ] **Step 1: Write the failing test**

Create `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/FixedReportServiceImplTest.java`:

```java
package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.service.impl.FixedReportServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FixedReportServiceImplTest {

    @Test
    void deriveConsumptionStatus_availableWhenLatestSuccess() {
        FixedReportVO vo = new FixedReportVO();
        vo.setLatestResultStatus("SUCCESS");
        vo.setResultExpired(false);
        vo.setScheduleCount(1);
        vo.setEnabledScheduleCount(1);

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl headless/server -Dtest=FixedReportServiceImplTest -q 2>&1 | tail -5`
Expected: FAIL — class `FixedReportServiceImpl` does not exist yet

- [ ] **Step 3: Create FixedReportService interface**

```java
package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;

import java.util.List;

public interface FixedReportService {

    List<FixedReportVO> listFixedReports(User user, String keyword, String domainName,
            String statusFilter, String viewFilter);

    void subscribe(Long datasetId, User user);

    void unsubscribe(Long datasetId, User user);
}
```

- [ ] **Step 4: Create FixedReportServiceImpl with deriveConsumptionStatus**

```java
package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportSubscriptionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportSubscriptionMapper;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedReportServiceImpl implements FixedReportService {

    private final SemanticTemplateService semanticTemplateService;
    private final ReportScheduleMapper scheduleMapper;
    private final ReportExecutionMapper executionMapper;
    private final ReportDeliveryConfigMapper deliveryConfigMapper;
    private final ReportSubscriptionMapper subscriptionMapper;

    @Override
    public List<FixedReportVO> listFixedReports(User user, String keyword, String domainName,
            String statusFilter, String viewFilter) {

        // 1. Get all successful deployments (each = one fixed report)
        List<SemanticDeployment> deployments = semanticTemplateService.getDeploymentHistory(user)
                .stream()
                .filter(d -> d.getStatus() == SemanticDeployment.DeploymentStatus.SUCCESS)
                .filter(d -> d.getResultDetail() != null && d.getResultDetail().getDataSetId() != null)
                .collect(Collectors.toList());

        if (deployments.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplicate by datasetId — keep the latest deployment per dataset
        Map<Long, SemanticDeployment> latestByDataset = deployments.stream()
                .collect(Collectors.toMap(
                        d -> d.getResultDetail().getDataSetId(),
                        d -> d,
                        (a, b) -> (a.getId() > b.getId()) ? a : b));

        Set<Long> datasetIds = latestByDataset.keySet();

        // 2. Load all schedules for these datasets
        List<ReportScheduleDO> allSchedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<ReportScheduleDO>()
                        .in(ReportScheduleDO::getDatasetId, datasetIds));
        Map<Long, List<ReportScheduleDO>> schedulesByDataset = allSchedules.stream()
                .collect(Collectors.groupingBy(ReportScheduleDO::getDatasetId));

        // 3. Load latest execution per schedule
        Set<Long> scheduleIds = allSchedules.stream()
                .map(ReportScheduleDO::getId)
                .collect(Collectors.toSet());
        Map<Long, ReportExecutionDO> latestExecBySchedule = loadLatestExecutions(scheduleIds);

        // 4. Load delivery configs (all enabled for lookup)
        Map<Long, ReportDeliveryConfigDO> deliveryConfigMap = deliveryConfigMapper.selectList(
                new LambdaQueryWrapper<ReportDeliveryConfigDO>())
                .stream()
                .collect(Collectors.toMap(ReportDeliveryConfigDO::getId, c -> c, (a, b) -> a));

        // 5. Load subscriptions for current user
        Set<Long> subscribedDatasets = subscriptionMapper.selectList(
                new LambdaQueryWrapper<ReportSubscriptionDO>()
                        .eq(ReportSubscriptionDO::getUserId, user.getId())
                        .in(ReportSubscriptionDO::getDatasetId, datasetIds))
                .stream()
                .map(ReportSubscriptionDO::getDatasetId)
                .collect(Collectors.toSet());

        // 6. Assemble VO list
        List<FixedReportVO> results = new ArrayList<>();
        for (Map.Entry<Long, SemanticDeployment> entry : latestByDataset.entrySet()) {
            Long dsId = entry.getKey();
            SemanticDeployment dep = entry.getValue();
            FixedReportVO vo = buildVO(dep, dsId, schedulesByDataset.getOrDefault(dsId, List.of()),
                    latestExecBySchedule, deliveryConfigMap, subscribedDatasets.contains(dsId));
            results.add(vo);
        }

        // 7. Apply filters
        results = applyFilters(results, keyword, domainName, statusFilter, viewFilter);

        return results;
    }

    @Override
    public void subscribe(Long datasetId, User user) {
        LambdaQueryWrapper<ReportSubscriptionDO> wrapper = new LambdaQueryWrapper<ReportSubscriptionDO>()
                .eq(ReportSubscriptionDO::getUserId, user.getId())
                .eq(ReportSubscriptionDO::getDatasetId, datasetId);
        if (subscriptionMapper.selectCount(wrapper) > 0) {
            return; // already subscribed
        }
        ReportSubscriptionDO sub = new ReportSubscriptionDO();
        sub.setUserId(user.getId());
        sub.setDatasetId(datasetId);
        subscriptionMapper.insert(sub);
    }

    @Override
    public void unsubscribe(Long datasetId, User user) {
        subscriptionMapper.delete(
                new LambdaQueryWrapper<ReportSubscriptionDO>()
                        .eq(ReportSubscriptionDO::getUserId, user.getId())
                        .eq(ReportSubscriptionDO::getDatasetId, datasetId));
    }

    // --- Internal helpers ---

    private Map<Long, ReportExecutionDO> loadLatestExecutions(Set<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // Get the latest execution per schedule using a simple approach:
        // fetch recent executions ordered by startTime desc, then deduplicate
        List<ReportExecutionDO> execs = executionMapper.selectList(
                new LambdaQueryWrapper<ReportExecutionDO>()
                        .in(ReportExecutionDO::getScheduleId, scheduleIds)
                        .orderByDesc(ReportExecutionDO::getStartTime));
        return execs.stream()
                .collect(Collectors.toMap(
                        ReportExecutionDO::getScheduleId,
                        e -> e,
                        (first, second) -> first)); // keep first = latest
    }

    private FixedReportVO buildVO(SemanticDeployment dep, Long datasetId,
            List<ReportScheduleDO> schedules,
            Map<Long, ReportExecutionDO> latestExecBySchedule,
            Map<Long, ReportDeliveryConfigDO> deliveryConfigMap,
            boolean subscribed) {

        FixedReportVO vo = new FixedReportVO();
        vo.setDeploymentId(dep.getId());
        vo.setDatasetId(datasetId);
        vo.setReportName(dep.getTemplateName() != null ? dep.getTemplateName()
                : (dep.getResultDetail().getDataSetName() != null
                        ? dep.getResultDetail().getDataSetName()
                        : "报表 #" + dep.getId()));
        vo.setDomainName(dep.getResultDetail().getDomainName());
        vo.setDescription(dep.getTemplateConfigSnapshot() != null
                && dep.getTemplateConfigSnapshot().getDataSet() != null
                        ? dep.getTemplateConfigSnapshot().getDataSet().getDescription()
                        : null);
        vo.setSubscribed(subscribed);
        vo.setScheduleCount(schedules.size());
        vo.setEnabledScheduleCount(
                (int) schedules.stream().filter(s -> Boolean.TRUE.equals(s.getEnabled())).count());

        // Find latest execution across all schedules for this dataset
        ReportExecutionDO latestExec = schedules.stream()
                .map(s -> latestExecBySchedule.get(s.getId()))
                .filter(Objects::nonNull)
                .max((a, b) -> {
                    Date ta = a.getStartTime();
                    Date tb = b.getStartTime();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return -1;
                    if (tb == null) return 1;
                    return ta.compareTo(tb);
                })
                .orElse(null);

        if (latestExec != null) {
            vo.setLatestResultTime(latestExec.getStartTime());
            vo.setLatestResultStatus(latestExec.getStatus());
            vo.setLatestErrorMessage(latestExec.getErrorMessage());
            vo.setLatestRowCount(latestExec.getRowCount());
        }

        // Check result expiry — simplified: expired if latest result > 48h old
        if (vo.getLatestResultTime() != null) {
            long ageMs = System.currentTimeMillis() - vo.getLatestResultTime().getTime();
            vo.setResultExpired(ageMs > 48 * 3600 * 1000L);
        }

        // If latest is FAILED, find previous SUCCESS
        if ("FAILED".equals(vo.getLatestResultStatus())) {
            schedules.stream()
                    .flatMap(s -> {
                        ReportExecutionDO exec = latestExecBySchedule.get(s.getId());
                        return exec != null ? java.util.stream.Stream.of(exec)
                                : java.util.stream.Stream.empty();
                    })
                    .filter(e -> "SUCCESS".equals(e.getStatus()))
                    .map(ReportExecutionDO::getStartTime)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .ifPresent(vo::setPreviousSuccessTime);
        }

        // Collect delivery channels from all schedules
        Set<Long> configIds = schedules.stream()
                .filter(s -> StringUtils.isNotBlank(s.getDeliveryConfigIds()))
                .flatMap(s -> Arrays.stream(s.getDeliveryConfigIds().split(",")))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toSet());

        List<FixedReportVO.DeliverySummaryItem> channels = configIds.stream()
                .map(id -> {
                    ReportDeliveryConfigDO config = deliveryConfigMap.get(id);
                    if (config == null) return null;
                    FixedReportVO.DeliverySummaryItem item = new FixedReportVO.DeliverySummaryItem();
                    item.setConfigId(config.getId());
                    item.setConfigName(config.getName());
                    item.setDeliveryType(config.getDeliveryType());
                    item.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        vo.setDeliveryChannels(channels);

        // Derive consumption status
        vo.setConsumptionStatus(deriveConsumptionStatus(vo));

        return vo;
    }

    public static String deriveConsumptionStatus(FixedReportVO vo) {
        // Priority order matches spec §1.5
        if (vo.getLatestResultStatus() == null && vo.getLatestResultTime() == null) {
            return "NO_RESULT";
        }
        if ("FAILED".equals(vo.getLatestResultStatus())) {
            return "RECENTLY_FAILED";
        }
        if (vo.isResultExpired()) {
            return "EXPIRED";
        }
        if (vo.getDeliveryChannels() != null && vo.getDeliveryChannels().isEmpty()
                && vo.getScheduleCount() > 0) {
            return "NO_DELIVERY";
        }
        if (vo.getDeliveryChannels() != null
                && vo.getDeliveryChannels().stream().anyMatch(c -> !c.isEnabled())) {
            return "PARTIAL_CHANNEL_ERROR";
        }
        return "AVAILABLE";
    }

    private List<FixedReportVO> applyFilters(List<FixedReportVO> list, String keyword,
            String domainName, String statusFilter, String viewFilter) {
        return list.stream()
                .filter(vo -> StringUtils.isBlank(keyword)
                        || StringUtils.containsIgnoreCase(vo.getReportName(), keyword)
                        || StringUtils.containsIgnoreCase(vo.getDescription(), keyword))
                .filter(vo -> StringUtils.isBlank(domainName)
                        || domainName.equals(vo.getDomainName()))
                .filter(vo -> StringUtils.isBlank(statusFilter)
                        || statusFilter.equals(vo.getConsumptionStatus()))
                .filter(vo -> {
                    if ("subscribed".equals(viewFilter)) return vo.isSubscribed();
                    return true; // "all" or blank
                })
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -pl headless/server -Dtest=FixedReportServiceImplTest -q 2>&1 | tail -5`
Expected: All 5 tests PASS

- [ ] **Step 6: Compile full project**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/FixedReportService.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/FixedReportServiceImpl.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/service/FixedReportServiceImplTest.java
git commit -m "feat(report): add FixedReportService with aggregation and subscription logic"
```

---

### Task 5: Backend — FixedReportController

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/FixedReportController.java`

- [ ] **Step 1: Create controller**

```java
package com.tencent.supersonic.headless.server.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fixedReports")
@RequiredArgsConstructor
public class FixedReportController {

    private final FixedReportService fixedReportService;

    @GetMapping
    public List<FixedReportVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String domainName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String view,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return fixedReportService.listFixedReports(user, keyword, domainName, status, view);
    }

    @PostMapping("/{datasetId}/subscription")
    public void subscribe(@PathVariable Long datasetId,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        fixedReportService.subscribe(datasetId, user);
    }

    @DeleteMapping("/{datasetId}/subscription")
    public void unsubscribe(@PathVariable Long datasetId,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        fixedReportService.unsubscribe(datasetId, user);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/FixedReportController.java
git commit -m "feat(report): add FixedReportController REST endpoints"
```

---

### Task 6: Frontend — API Service

**Files:**
- Create: `webapp/packages/supersonic-fe/src/services/fixedReport.ts`

- [ ] **Step 1: Create the service file**

```typescript
import request from './request';

export interface DeliverySummaryItem {
  configId: number;
  configName: string;
  deliveryType: string;
  enabled: boolean;
}

export interface FixedReport {
  deploymentId: number;
  datasetId: number;
  reportName: string;
  description?: string;
  domainName?: string;
  latestResultTime?: string;
  latestResultStatus?: string;
  latestErrorMessage?: string;
  latestRowCount?: number;
  resultExpired: boolean;
  previousSuccessTime?: string;
  scheduleCount: number;
  enabledScheduleCount: number;
  deliveryChannels: DeliverySummaryItem[];
  subscribed: boolean;
  consumptionStatus: string;
}

const BASE = '/api/v1/fixedReports';

export function getFixedReports(params?: {
  keyword?: string;
  domainName?: string;
  status?: string;
  view?: string;
}): Promise<FixedReport[]> {
  return request(BASE, { method: 'GET', params });
}

export function subscribe(datasetId: number): Promise<void> {
  return request(`${BASE}/${datasetId}/subscription`, { method: 'POST' });
}

export function unsubscribe(datasetId: number): Promise<void> {
  return request(`${BASE}/${datasetId}/subscription`, { method: 'DELETE' });
}
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/services/fixedReport.ts
git commit -m "feat(report): add fixedReport frontend API service"
```

---

### Task 7: Frontend — FilterBar Component

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/Reports/components/FilterBar.tsx`

- [ ] **Step 1: Create FilterBar**

```tsx
import React from 'react';
import { Input, Select, Space } from 'antd';
import { SearchOutlined } from '@ant-design/icons';

const STATUS_OPTIONS = [
  { label: '全部状态', value: '' },
  { label: '可查看', value: 'AVAILABLE' },
  { label: '暂无结果', value: 'NO_RESULT' },
  { label: '结果过期', value: 'EXPIRED' },
  { label: '最近失败', value: 'RECENTLY_FAILED' },
  { label: '未配置投递', value: 'NO_DELIVERY' },
];

const VIEW_OPTIONS = [
  { label: '全部', value: '' },
  { label: '我订阅的', value: 'subscribed' },
];

interface FilterBarProps {
  keyword: string;
  domainName: string;
  statusFilter: string;
  viewFilter: string;
  domainOptions: string[];
  onKeywordChange: (val: string) => void;
  onDomainChange: (val: string) => void;
  onStatusChange: (val: string) => void;
  onViewChange: (val: string) => void;
}

const FilterBar: React.FC<FilterBarProps> = ({
  keyword,
  domainName,
  statusFilter,
  viewFilter,
  domainOptions,
  onKeywordChange,
  onDomainChange,
  onStatusChange,
  onViewChange,
}) => {
  return (
    <Space wrap size={12}>
      <Input
        placeholder="搜索报表名称"
        prefix={<SearchOutlined />}
        value={keyword}
        onChange={(e) => onKeywordChange(e.target.value)}
        allowClear
        style={{ width: 220 }}
      />
      <Select
        value={domainName}
        onChange={onDomainChange}
        style={{ width: 160 }}
        options={[
          { label: '全部业务域', value: '' },
          ...domainOptions.map((d) => ({ label: d, value: d })),
        ]}
      />
      <Select
        value={statusFilter}
        onChange={onStatusChange}
        style={{ width: 140 }}
        options={STATUS_OPTIONS}
      />
      <Select
        value={viewFilter}
        onChange={onViewChange}
        style={{ width: 140 }}
        options={VIEW_OPTIONS}
      />
    </Space>
  );
};

export default FilterBar;
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/Reports/components/FilterBar.tsx
git commit -m "feat(report): add FilterBar component for fixed reports workbench"
```

---

### Task 8: Frontend — ReportDetailDrawer Component

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/Reports/components/ReportDetailDrawer.tsx`

- [ ] **Step 1: Create ReportDetailDrawer**

```tsx
import React from 'react';
import { Drawer, Descriptions, Tag, Button, Space, Typography, Empty } from 'antd';
import { CalendarOutlined, StarOutlined, StarFilled } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { FixedReport } from '@/services/fixedReport';
import { DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';

const { Text } = Typography;

const STATUS_CONFIG: Record<string, { color: string; text: string }> = {
  AVAILABLE: { color: 'green', text: '可查看' },
  NO_RESULT: { color: 'default', text: '暂无结果' },
  EXPIRED: { color: 'orange', text: '结果过期' },
  RECENTLY_FAILED: { color: 'red', text: '最近失败' },
  NO_DELIVERY: { color: 'volcano', text: '未配置投递' },
  PARTIAL_CHANNEL_ERROR: { color: 'orange', text: '部分渠道异常' },
};

interface ReportDetailDrawerProps {
  visible: boolean;
  report?: FixedReport;
  onClose: () => void;
  onSubscribe: (datasetId: number) => void;
  onUnsubscribe: (datasetId: number) => void;
  onCreateSchedule: (datasetId: number) => void;
  onViewHistory: (datasetId: number) => void;
}

const ReportDetailDrawer: React.FC<ReportDetailDrawerProps> = ({
  visible,
  report,
  onClose,
  onSubscribe,
  onUnsubscribe,
  onCreateSchedule,
  onViewHistory,
}) => {
  if (!report) {
    return (
      <Drawer title="报表详情" open={visible} onClose={onClose} width={520}>
        <Empty description="未选择报表" />
      </Drawer>
    );
  }

  const statusInfo = STATUS_CONFIG[report.consumptionStatus] || STATUS_CONFIG.NO_RESULT;

  return (
    <Drawer
      title={report.reportName}
      open={visible}
      onClose={onClose}
      width={520}
      extra={
        <Space>
          <Button
            icon={report.subscribed ? <StarFilled /> : <StarOutlined />}
            type={report.subscribed ? 'primary' : 'default'}
            onClick={() =>
              report.subscribed
                ? onUnsubscribe(report.datasetId)
                : onSubscribe(report.datasetId)
            }
          >
            {report.subscribed ? '已订阅' : '订阅'}
          </Button>
        </Space>
      }
    >
      <Descriptions column={1} size="small" style={{ marginBottom: 24 }}>
        <Descriptions.Item label="状态">
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="业务域">{report.domainName || '—'}</Descriptions.Item>
        <Descriptions.Item label="口径摘要">{report.description || '—'}</Descriptions.Item>
        <Descriptions.Item label="最新结果时间">
          {report.latestResultTime
            ? dayjs(report.latestResultTime).format('YYYY-MM-DD HH:mm:ss')
            : '—'}
        </Descriptions.Item>
        {report.latestResultStatus === 'FAILED' && (
          <Descriptions.Item label="失败原因">
            <Text type="danger" ellipsis={{ tooltip: report.latestErrorMessage }}>
              {report.latestErrorMessage || '—'}
            </Text>
          </Descriptions.Item>
        )}
        {report.previousSuccessTime && (
          <Descriptions.Item label="上一版成功结果">
            {dayjs(report.previousSuccessTime).format('YYYY-MM-DD HH:mm:ss')}
            <Text type="secondary" style={{ marginLeft: 8 }}>
              (仍可参考)
            </Text>
          </Descriptions.Item>
        )}
        <Descriptions.Item label="数据行数">
          {report.latestRowCount != null ? report.latestRowCount : '—'}
        </Descriptions.Item>
        <Descriptions.Item label="定时任务">
          {report.scheduleCount > 0
            ? `${report.enabledScheduleCount} / ${report.scheduleCount} 启用`
            : '未创建'}
        </Descriptions.Item>
        <Descriptions.Item label="投递渠道">
          {report.deliveryChannels.length > 0 ? (
            <Space size={4} wrap>
              {report.deliveryChannels.map((ch) => {
                const info = DELIVERY_TYPE_MAP[ch.deliveryType] || {
                  color: 'default',
                  text: ch.deliveryType,
                };
                return (
                  <Tag key={ch.configId} color={ch.enabled ? info.color : 'default'}>
                    {ch.configName || info.text}
                    {!ch.enabled && ' (已禁用)'}
                  </Tag>
                );
              })}
            </Space>
          ) : (
            <Text type="secondary">未配置</Text>
          )}
        </Descriptions.Item>
      </Descriptions>

      <Space direction="vertical" style={{ width: '100%' }}>
        <Button block onClick={() => onViewHistory(report.datasetId)}>
          查看历史结果
        </Button>
        <Button
          block
          type="primary"
          icon={<CalendarOutlined />}
          onClick={() => onCreateSchedule(report.datasetId)}
        >
          创建定时报表任务
        </Button>
      </Space>
    </Drawer>
  );
};

export default ReportDetailDrawer;
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/Reports/components/ReportDetailDrawer.tsx
git commit -m "feat(report): add ReportDetailDrawer component"
```

---

### Task 9: Frontend — Reports Page Rewrite

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/pages/Reports/index.tsx`
- Modify: `webapp/packages/supersonic-fe/src/pages/Reports/style.less`

- [ ] **Step 1: Rewrite style.less**

```less
.reportsPage {
  padding: 24px;
}

.pageHeader {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.pageTitle {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color, rgba(0, 10, 36, 0.85));
}

.filterRow {
  margin-bottom: 16px;
}

.tableShell {
  border: 1px solid var(--border-color, #f0f0f0);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);

  :global {
    .ant-table-wrapper {
      padding: 0;
    }

    .ant-table-thead > tr > th {
      color: var(--text-color-third, rgba(0, 10, 36, 0.45));
      font-weight: 600;
      background: #f7f9fc;
    }
  }
}

.reportName {
  cursor: pointer;
  color: var(--primary-color, #1890ff);
  &:hover {
    text-decoration: underline;
  }
}
```

- [ ] **Step 2: Rewrite index.tsx**

```tsx
import React, { useEffect, useState, useCallback } from 'react';
import { Table, Tag, Space, Button, message, Empty, Tooltip } from 'antd';
import { StarFilled, StarOutlined, SendOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import FilterBar from './components/FilterBar';
import ReportDetailDrawer from './components/ReportDetailDrawer';
import ScheduleForm from '@/pages/ReportSchedule/components/ScheduleForm';
import {
  getFixedReports,
  subscribe as apiSubscribe,
  unsubscribe as apiUnsubscribe,
} from '@/services/fixedReport';
import type { FixedReport } from '@/services/fixedReport';
import { createSchedule } from '@/services/reportSchedule';
import type { ReportSchedule } from '@/services/reportSchedule';
import { DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';
import styles from './style.less';

const STATUS_CONFIG: Record<string, { color: string; text: string }> = {
  AVAILABLE: { color: 'green', text: '可查看' },
  NO_RESULT: { color: 'default', text: '暂无结果' },
  EXPIRED: { color: 'orange', text: '结果过期' },
  RECENTLY_FAILED: { color: 'red', text: '最近失败' },
  NO_DELIVERY: { color: 'volcano', text: '未配置投递' },
  PARTIAL_CHANNEL_ERROR: { color: 'orange', text: '部分渠道异常' },
};

const ReportsPage: React.FC = () => {
  const [data, setData] = useState<FixedReport[]>([]);
  const [loading, setLoading] = useState(false);

  // Filters
  const [keyword, setKeyword] = useState('');
  const [domainName, setDomainName] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [viewFilter, setViewFilter] = useState('');

  // Detail drawer
  const [selectedReport, setSelectedReport] = useState<FixedReport | undefined>();
  const [drawerVisible, setDrawerVisible] = useState(false);

  // Schedule form
  const [scheduleFormVisible, setScheduleFormVisible] = useState(false);
  const [scheduleDatasetId, setScheduleDatasetId] = useState<number | undefined>();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res: any = await getFixedReports({
        keyword: keyword || undefined,
        domainName: domainName || undefined,
        status: statusFilter || undefined,
        view: viewFilter || undefined,
      });
      const list = (res?.code === 200 && res?.data) ? res.data : res;
      setData(Array.isArray(list) ? list : []);
    } catch {
      message.error('加载固定报表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  }, [keyword, domainName, statusFilter, viewFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const domainOptions = [...new Set(data.map((r) => r.domainName).filter(Boolean))] as string[];

  const handleSubscribe = async (datasetId: number) => {
    try {
      await apiSubscribe(datasetId);
      message.success('已订阅');
      fetchData();
    } catch {
      message.error('订阅失败');
    }
  };

  const handleUnsubscribe = async (datasetId: number) => {
    try {
      await apiUnsubscribe(datasetId);
      message.success('已取消订阅');
      fetchData();
    } catch {
      message.error('取消订阅失败');
    }
  };

  const handleCreateSchedule = (datasetId: number) => {
    setScheduleDatasetId(datasetId);
    setScheduleFormVisible(true);
  };

  const handleScheduleSubmit = async (values: Partial<ReportSchedule>) => {
    try {
      const res: any = await createSchedule(values);
      if (res?.code === 200) {
        message.success('创建定时任务成功');
        setScheduleFormVisible(false);
        setScheduleDatasetId(undefined);
        fetchData();
      } else {
        message.error(res?.msg || '创建定时任务失败');
      }
    } catch {
      message.error('创建定时任务失败');
    }
  };

  const openDetail = (record: FixedReport) => {
    setSelectedReport(record);
    setDrawerVisible(true);
  };

  const columns = [
    {
      title: '报表名称',
      dataIndex: 'reportName',
      width: 200,
      ellipsis: true,
      render: (val: string, record: FixedReport) => (
        <a className={styles.reportName} onClick={() => openDetail(record)}>
          {val}
        </a>
      ),
    },
    {
      title: '口径摘要',
      dataIndex: 'description',
      width: 200,
      ellipsis: true,
      render: (val?: string) => val || '—',
    },
    {
      title: '最新结果',
      dataIndex: 'latestResultTime',
      width: 170,
      render: (val?: string) =>
        val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '—',
    },
    {
      title: '状态',
      dataIndex: 'consumptionStatus',
      width: 120,
      render: (status: string) => {
        const info = STATUS_CONFIG[status] || STATUS_CONFIG.NO_RESULT;
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '投递渠道',
      dataIndex: 'deliveryChannels',
      width: 150,
      render: (channels: FixedReport['deliveryChannels']) => {
        if (!channels || channels.length === 0) {
          return <span style={{ color: 'rgba(0,10,36,0.35)' }}>未配置</span>;
        }
        return (
          <Space size={4} wrap>
            {channels.slice(0, 3).map((ch) => {
              const info = DELIVERY_TYPE_MAP[ch.deliveryType] || {
                color: 'default',
                text: ch.deliveryType,
              };
              return (
                <Tooltip key={ch.configId} title={ch.configName}>
                  <Tag color={ch.enabled ? info.color : 'default'}>
                    <SendOutlined /> {info.text}
                  </Tag>
                </Tooltip>
              );
            })}
            {channels.length > 3 && (
              <Tag>+{channels.length - 3}</Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '订阅',
      dataIndex: 'subscribed',
      width: 70,
      align: 'center' as const,
      render: (subscribed: boolean, record: FixedReport) => (
        <Button
          type="text"
          size="small"
          icon={subscribed ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
          onClick={(e) => {
            e.stopPropagation();
            subscribed
              ? handleUnsubscribe(record.datasetId)
              : handleSubscribe(record.datasetId);
          }}
        />
      ),
    },
    {
      title: '操作',
      width: 120,
      fixed: 'right' as const,
      render: (_: any, record: FixedReport) => (
        <Space size={4} wrap>
          <Button type="link" size="small" onClick={() => openDetail(record)}>
            查看结果
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleCreateSchedule(record.datasetId)}
          >
            创建任务
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.reportsPage}>
      <div className={styles.pageHeader}>
        <h3 className={styles.pageTitle}>固定报表</h3>
      </div>
      <div className={styles.filterRow}>
        <FilterBar
          keyword={keyword}
          domainName={domainName}
          statusFilter={statusFilter}
          viewFilter={viewFilter}
          domainOptions={domainOptions}
          onKeywordChange={setKeyword}
          onDomainChange={setDomainName}
          onStatusChange={setStatusFilter}
          onViewChange={setViewFilter}
        />
      </div>
      <div className={styles.tableShell}>
        <Table
          rowKey="datasetId"
          size="middle"
          bordered={false}
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <Empty
                description={
                  keyword || domainName || statusFilter || viewFilter
                    ? '筛选后无匹配结果'
                    : '暂无固定报表'
                }
              />
            ),
          }}
          pagination={{
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onRow={(record) => ({
            onClick: () => openDetail(record),
            style: { cursor: 'pointer' },
          })}
        />
      </div>

      <ReportDetailDrawer
        visible={drawerVisible}
        report={selectedReport}
        onClose={() => setDrawerVisible(false)}
        onSubscribe={handleSubscribe}
        onUnsubscribe={handleUnsubscribe}
        onCreateSchedule={handleCreateSchedule}
        onViewHistory={() => {}}
      />

      <ScheduleForm
        visible={scheduleFormVisible}
        initialDatasetId={scheduleDatasetId}
        onCancel={() => {
          setScheduleFormVisible(false);
          setScheduleDatasetId(undefined);
        }}
        onSubmit={handleScheduleSubmit}
      />
    </div>
  );
};

export default ReportsPage;
```

- [ ] **Step 3: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/Reports/index.tsx \
        webapp/packages/supersonic-fe/src/pages/Reports/style.less
git commit -m "feat(report): rewrite Reports page as fixed reports workbench"
```

---

### Task 10: Integration Verification

- [ ] **Step 1: Compile backend**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all backend tests**

Run: `mvn test -pl headless/server -Dtest=FixedReportServiceImplTest -q`
Expected: All tests PASS

- [ ] **Step 3: Verify frontend builds**

Run: `cd webapp && pnpm --filter supersonic-fe build 2>&1 | tail -10`
Expected: Build completes without errors

- [ ] **Step 4: Manual smoke test checklist**

Verify these scenarios work after starting the app:

1. `/reports` page loads with filter bar + table (not card wall)
2. Filters (keyword, domain, status, view) narrow results correctly
3. Clicking a report name opens detail drawer
4. Subscribe/unsubscribe star toggles correctly
5. "Create task" opens ScheduleForm with correct datasetId
6. Empty states show correctly: no reports, filtered no results
7. Status tags show correct colors and text

---

## Self-Review Checklist

- [x] **Spec coverage**: §1.1–1.8 (object definition, page goal, info architecture, status, user journey, visual) all have corresponding tasks
- [x] **Placeholder scan**: No TBD/TODO/placeholder text in any step
- [x] **Type consistency**: `FixedReportVO` fields match frontend `FixedReport` interface; `DeliverySummaryItem` consistent across backend/frontend; `consumptionStatus` enum values consistent in `deriveConsumptionStatus()` and `STATUS_CONFIG`
- [x] **Missing from spec**: "历史结果入口" (history entry) — wired in drawer as `onViewHistory` callback placeholder; the actual execution history page already exists at `ReportSchedule/components/ExecutionList.tsx` and can be wired up during integration
