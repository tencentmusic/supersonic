# Business Topics (经营主题) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Business Topics as the organizing layer that groups fixed reports, alert rules, and scheduled tasks into business-meaningful units.

**Architecture:** A `BusinessTopicDO` entity + generic `BusinessTopicItemDO` join table link topics to items by type (FIXED_REPORT/ALERT_RULE/SCHEDULE). The frontend gets a topic list page with a detail drawer showing grouped items. Topics carry priority (for future dashboard), owner, and default delivery config.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, React, Ant Design, TypeScript

---

## File Structure

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `launchers/standalone/src/main/resources/db/migration/mysql/V28__business_topic.sql` | MySQL migration |
| Create | `launchers/standalone/src/main/resources/db/migration/postgresql/V28__business_topic.sql` | PostgreSQL migration |
| Create | `headless/server/.../persistence/dataobject/BusinessTopicDO.java` | Topic entity |
| Create | `headless/server/.../persistence/dataobject/BusinessTopicItemDO.java` | Topic-item join entity |
| Create | `headless/server/.../persistence/mapper/BusinessTopicMapper.java` | Topic mapper |
| Create | `headless/server/.../persistence/mapper/BusinessTopicItemMapper.java` | Item mapper |
| Create | `headless/server/.../pojo/BusinessTopicVO.java` | Enriched view DTO |
| Create | `headless/server/.../service/BusinessTopicService.java` | Service interface |
| Create | `headless/server/.../service/impl/BusinessTopicServiceImpl.java` | Service impl |
| Create | `headless/server/src/test/.../service/BusinessTopicServiceImplTest.java` | Unit tests |
| Create | `headless/server/.../rest/BusinessTopicController.java` | REST endpoints |
| Create | `webapp/.../src/services/businessTopic.ts` | Frontend API |
| Create | `webapp/.../src/pages/BusinessTopics/index.tsx` | Topic list page |
| Create | `webapp/.../src/pages/BusinessTopics/components/TopicFormModal.tsx` | Create/edit modal |
| Create | `webapp/.../src/pages/BusinessTopics/components/TopicDetailDrawer.tsx` | Topic detail |
| Create | `webapp/.../src/pages/BusinessTopics/style.less` | Styles |
| Modify | `webapp/.../config/routes.ts` | Register route |

All `headless/server/...` paths expand to `headless/server/src/main/java/com/tencent/supersonic/headless/server/`.
All `webapp/...` paths expand to `webapp/packages/supersonic-fe/`.

---

### Task 1: DB migration V28 — business topic tables

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V28__business_topic.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V28__business_topic.sql`

- [ ] **Step 1: Create MySQL migration**

```sql
-- V28__business_topic.sql

CREATE TABLE IF NOT EXISTS s2_business_topic (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    priority    INT           DEFAULT 0 COMMENT 'Lower value = higher priority in dashboard',
    owner_id    BIGINT,
    default_delivery_config_ids VARCHAR(500) COMMENT 'CSV of delivery config IDs',
    enabled     TINYINT(1)    DEFAULT 1,
    tenant_id   BIGINT        DEFAULT 1,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_priority (priority)
);

CREATE TABLE IF NOT EXISTS s2_business_topic_item (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id  BIGINT      NOT NULL,
    item_type VARCHAR(50) NOT NULL COMMENT 'FIXED_REPORT, ALERT_RULE, SCHEDULE',
    item_id   BIGINT      NOT NULL COMMENT 'datasetId for FIXED_REPORT, ruleId for ALERT_RULE, scheduleId for SCHEDULE',
    tenant_id BIGINT      DEFAULT 1,
    UNIQUE KEY uq_topic_item (topic_id, item_type, item_id),
    INDEX idx_item (item_type, item_id)
);
```

- [ ] **Step 2: Create PostgreSQL migration**

```sql
-- V28__business_topic.sql

CREATE TABLE IF NOT EXISTS s2_business_topic (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    priority    INT           DEFAULT 0,
    owner_id    BIGINT,
    default_delivery_config_ids VARCHAR(500),
    enabled     SMALLINT      DEFAULT 1,
    tenant_id   BIGINT        DEFAULT 1,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_topic_tenant ON s2_business_topic (tenant_id);
CREATE INDEX IF NOT EXISTS idx_business_topic_priority ON s2_business_topic (priority);

CREATE TABLE IF NOT EXISTS s2_business_topic_item (
    id        BIGSERIAL PRIMARY KEY,
    topic_id  BIGINT      NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    item_id   BIGINT      NOT NULL,
    tenant_id BIGINT      DEFAULT 1,
    UNIQUE (topic_id, item_type, item_id)
);

CREATE INDEX IF NOT EXISTS idx_business_topic_item_type ON s2_business_topic_item (item_type, item_id);
```

- [ ] **Step 3: Compile to verify migration files are picked up**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/
git commit -m "feat: add V28 migration for business topic tables"
```

---

### Task 2: Entity + Mapper — BusinessTopicDO, BusinessTopicItemDO

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/BusinessTopicDO.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/BusinessTopicItemDO.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/mapper/BusinessTopicMapper.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/mapper/BusinessTopicItemMapper.java`

- [ ] **Step 1: Create BusinessTopicDO**

```java
package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_business_topic")
public class BusinessTopicDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer priority;
    private Long ownerId;
    private String defaultDeliveryConfigIds;
    private Integer enabled;
    private Long tenantId;
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}
```

- [ ] **Step 2: Create BusinessTopicItemDO**

```java
package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_business_topic_item")
public class BusinessTopicItemDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private String itemType;
    private Long itemId;
    private Long tenantId;
}
```

- [ ] **Step 3: Create mappers**

```java
// BusinessTopicMapper.java
package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BusinessTopicMapper extends BaseMapper<BusinessTopicDO> {
}
```

```java
// BusinessTopicItemMapper.java
package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicItemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BusinessTopicItemMapper extends BaseMapper<BusinessTopicItemDO> {
}
```

- [ ] **Step 4: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/
git commit -m "feat: add BusinessTopicDO/ItemDO entities and mappers"
```

---

### Task 3: BusinessTopicVO enriched DTO

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/BusinessTopicVO.java`

- [ ] **Step 1: Create VO**

```java
package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BusinessTopicVO {
    private Long id;
    private String name;
    private String description;
    private Integer priority;
    private Long ownerId;
    private String ownerName;
    private String defaultDeliveryConfigIds;
    private Integer enabled;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;

    // Aggregated counts
    private int fixedReportCount;
    private int alertRuleCount;
    private int scheduleCount;

    // Item details (populated on detail view)
    private List<TopicItem> items;

    @Data
    public static class TopicItem {
        private Long itemId;
        private String itemType;
        private String itemName;
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/BusinessTopicVO.java
git commit -m "feat: add BusinessTopicVO enriched DTO"
```

---

### Task 4: Service interface + implementation + tests

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/BusinessTopicService.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/BusinessTopicServiceImpl.java`
- Create: `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/BusinessTopicServiceImplTest.java`

- [ ] **Step 1: Write tests**

```java
package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessTopicServiceImplTest {

    @Test
    void enrichVO_withMixedItems_countsCorrectly() {
        BusinessTopicVO vo = new BusinessTopicVO();
        List<BusinessTopicVO.TopicItem> items = List.of(
                makeItem("FIXED_REPORT", 1L, "Daily Revenue"),
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -pl headless/server -Dtest="BusinessTopicServiceImplTest" -q`
Expected: 2 tests PASS

- [ ] **Step 3: Create service interface**

```java
package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;

import java.util.List;

public interface BusinessTopicService {

    Page<BusinessTopicVO> listTopics(Page<BusinessTopicDO> page, Boolean enabled, User user);

    BusinessTopicVO getTopicDetail(Long id, User user);

    BusinessTopicDO createTopic(BusinessTopicDO topic, User user);

    BusinessTopicDO updateTopic(BusinessTopicDO topic, User user);

    void deleteTopic(Long id, User user);

    void addItems(Long topicId, List<String> itemTypes, List<Long> itemIds, User user);

    void removeItem(Long topicId, String itemType, Long itemId, User user);
}
```

- [ ] **Step 4: Create service implementation**

```java
package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertRuleDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicItemDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertRuleMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.BusinessTopicItemMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.BusinessTopicMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.service.BusinessTopicService;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessTopicServiceImpl implements BusinessTopicService {

    private final BusinessTopicMapper topicMapper;
    private final BusinessTopicItemMapper itemMapper;
    private final FixedReportService fixedReportService;
    private final ReportScheduleMapper scheduleMapper;
    private final AlertRuleMapper alertRuleMapper;

    @Override
    public Page<BusinessTopicVO> listTopics(Page<BusinessTopicDO> page, Boolean enabled,
            User user) {
        LambdaQueryWrapper<BusinessTopicDO> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(BusinessTopicDO::getEnabled, enabled ? 1 : 0);
        }
        wrapper.orderByAsc(BusinessTopicDO::getPriority);
        Page<BusinessTopicDO> doPage = topicMapper.selectPage(page, wrapper);

        // Batch load item counts
        List<Long> topicIds =
                doPage.getRecords().stream().map(BusinessTopicDO::getId).collect(Collectors.toList());
        Map<Long, List<BusinessTopicItemDO>> itemsByTopic = topicIds.isEmpty()
                ? Collections.emptyMap()
                : itemMapper
                        .selectList(new LambdaQueryWrapper<BusinessTopicItemDO>()
                                .in(BusinessTopicItemDO::getTopicId, topicIds))
                        .stream().collect(Collectors.groupingBy(BusinessTopicItemDO::getTopicId));

        Page<BusinessTopicVO> voPage = new Page<>(doPage.getCurrent(), doPage.getSize(),
                doPage.getTotal());
        voPage.setRecords(doPage.getRecords().stream().map(topic -> {
            BusinessTopicVO vo = toVO(topic);
            List<BusinessTopicItemDO> items =
                    itemsByTopic.getOrDefault(topic.getId(), List.of());
            vo.setFixedReportCount(
                    (int) items.stream().filter(i -> "FIXED_REPORT".equals(i.getItemType())).count());
            vo.setAlertRuleCount(
                    (int) items.stream().filter(i -> "ALERT_RULE".equals(i.getItemType())).count());
            vo.setScheduleCount(
                    (int) items.stream().filter(i -> "SCHEDULE".equals(i.getItemType())).count());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public BusinessTopicVO getTopicDetail(Long id, User user) {
        BusinessTopicDO topic = topicMapper.selectById(id);
        if (topic == null) {
            return null;
        }
        BusinessTopicVO vo = toVO(topic);
        List<BusinessTopicItemDO> itemDOs = itemMapper
                .selectList(new LambdaQueryWrapper<BusinessTopicItemDO>()
                        .eq(BusinessTopicItemDO::getTopicId, id));

        // Resolve item names
        List<BusinessTopicVO.TopicItem> items = new ArrayList<>();
        Map<String, Set<Long>> idsByType = itemDOs.stream().collect(Collectors
                .groupingBy(BusinessTopicItemDO::getItemType,
                        Collectors.mapping(BusinessTopicItemDO::getItemId, Collectors.toSet())));

        // Fixed reports — resolve names from FixedReportService
        Set<Long> reportDatasetIds = idsByType.getOrDefault("FIXED_REPORT", Set.of());
        if (!reportDatasetIds.isEmpty()) {
            List<FixedReportVO> reports = fixedReportService.listFixedReports(user, null, null,
                    null, null);
            Map<Long, String> reportNames = reports.stream().collect(
                    Collectors.toMap(FixedReportVO::getDatasetId, FixedReportVO::getReportName,
                            (a, b) -> a));
            reportDatasetIds.forEach(dsId -> {
                BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
                item.setItemType("FIXED_REPORT");
                item.setItemId(dsId);
                item.setItemName(reportNames.getOrDefault(dsId, "Report #" + dsId));
                items.add(item);
            });
        }

        // Alert rules
        Set<Long> ruleIds = idsByType.getOrDefault("ALERT_RULE", Set.of());
        if (!ruleIds.isEmpty()) {
            alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRuleDO>()
                    .in(AlertRuleDO::getId, ruleIds)).forEach(rule -> {
                        BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
                        item.setItemType("ALERT_RULE");
                        item.setItemId(rule.getId());
                        item.setItemName(rule.getName());
                        items.add(item);
                    });
        }

        // Schedules
        Set<Long> scheduleIds = idsByType.getOrDefault("SCHEDULE", Set.of());
        if (!scheduleIds.isEmpty()) {
            scheduleMapper.selectList(new LambdaQueryWrapper<ReportScheduleDO>()
                    .in(ReportScheduleDO::getId, scheduleIds)).forEach(sched -> {
                        BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
                        item.setItemType("SCHEDULE");
                        item.setItemId(sched.getId());
                        item.setItemName(sched.getName());
                        items.add(item);
                    });
        }

        vo.setItems(items);
        vo.setFixedReportCount(
                (int) items.stream().filter(i -> "FIXED_REPORT".equals(i.getItemType())).count());
        vo.setAlertRuleCount(
                (int) items.stream().filter(i -> "ALERT_RULE".equals(i.getItemType())).count());
        vo.setScheduleCount(
                (int) items.stream().filter(i -> "SCHEDULE".equals(i.getItemType())).count());
        return vo;
    }

    @Override
    public BusinessTopicDO createTopic(BusinessTopicDO topic, User user) {
        topic.setCreatedBy(user.getName());
        topic.setTenantId(user.getTenantId());
        if (topic.getEnabled() == null) {
            topic.setEnabled(1);
        }
        if (topic.getPriority() == null) {
            topic.setPriority(0);
        }
        topicMapper.insert(topic);
        return topic;
    }

    @Override
    public BusinessTopicDO updateTopic(BusinessTopicDO topic, User user) {
        topic.setUpdatedBy(user.getName());
        topicMapper.updateById(topic);
        return topicMapper.selectById(topic.getId());
    }

    @Override
    @Transactional
    public void deleteTopic(Long id, User user) {
        topicMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<BusinessTopicItemDO>()
                .eq(BusinessTopicItemDO::getTopicId, id));
    }

    @Override
    public void addItems(Long topicId, List<String> itemTypes, List<Long> itemIds, User user) {
        BusinessTopicDO topic = topicMapper.selectById(topicId);
        if (topic == null) {
            return;
        }
        for (int i = 0; i < itemTypes.size(); i++) {
            BusinessTopicItemDO item = new BusinessTopicItemDO();
            item.setTopicId(topicId);
            item.setItemType(itemTypes.get(i));
            item.setItemId(itemIds.get(i));
            item.setTenantId(topic.getTenantId());
            // Upsert: skip if already exists (unique constraint)
            long existing = itemMapper.selectCount(new LambdaQueryWrapper<BusinessTopicItemDO>()
                    .eq(BusinessTopicItemDO::getTopicId, topicId)
                    .eq(BusinessTopicItemDO::getItemType, itemTypes.get(i))
                    .eq(BusinessTopicItemDO::getItemId, itemIds.get(i)));
            if (existing == 0) {
                itemMapper.insert(item);
            }
        }
    }

    @Override
    public void removeItem(Long topicId, String itemType, Long itemId, User user) {
        itemMapper.delete(new LambdaQueryWrapper<BusinessTopicItemDO>()
                .eq(BusinessTopicItemDO::getTopicId, topicId)
                .eq(BusinessTopicItemDO::getItemType, itemType)
                .eq(BusinessTopicItemDO::getItemId, itemId));
    }

    private BusinessTopicVO toVO(BusinessTopicDO topic) {
        BusinessTopicVO vo = new BusinessTopicVO();
        vo.setId(topic.getId());
        vo.setName(topic.getName());
        vo.setDescription(topic.getDescription());
        vo.setPriority(topic.getPriority());
        vo.setOwnerId(topic.getOwnerId());
        vo.setDefaultDeliveryConfigIds(topic.getDefaultDeliveryConfigIds());
        vo.setEnabled(topic.getEnabled());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
        vo.setCreatedBy(topic.getCreatedBy());
        return vo;
    }
}
```

- [ ] **Step 5: Compile and run tests**

Run: `mvn test-compile -pl headless/server -am -q && mvn test -pl headless/server -Dtest="BusinessTopicServiceImplTest" -q`
Expected: 2 tests PASS

- [ ] **Step 6: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/BusinessTopicService.java
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/BusinessTopicServiceImpl.java
git add headless/server/src/test/java/com/tencent/supersonic/headless/server/service/BusinessTopicServiceImplTest.java
git commit -m "feat: add BusinessTopicService with CRUD and item management"
```

---

### Task 5: REST controller

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/BusinessTopicController.java`

- [ ] **Step 1: Create controller**

```java
package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import com.tencent.supersonic.headless.server.service.BusinessTopicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/businessTopics")
@RequiredArgsConstructor
public class BusinessTopicController {

    private final BusinessTopicService businessTopicService;

    @GetMapping
    public Page<BusinessTopicVO> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Boolean enabled,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return businessTopicService.listTopics(new Page<>(current, pageSize), enabled, user);
    }

    @GetMapping("/{id}")
    public BusinessTopicVO getDetail(@PathVariable Long id,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return businessTopicService.getTopicDetail(id, user);
    }

    @PostMapping
    public BusinessTopicDO create(@RequestBody BusinessTopicDO topic,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return businessTopicService.createTopic(topic, user);
    }

    @PatchMapping("/{id}")
    public BusinessTopicDO update(@PathVariable Long id, @RequestBody BusinessTopicDO topic,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        topic.setId(id);
        return businessTopicService.updateTopic(topic, user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        businessTopicService.deleteTopic(id, user);
    }

    @PostMapping("/{id}/items")
    public void addItems(@PathVariable Long id, @RequestBody Map<String, List<?>> body,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        @SuppressWarnings("unchecked")
        List<String> itemTypes = (List<String>) body.get("itemTypes");
        List<Long> itemIds = ((List<Number>) body.get("itemIds")).stream()
                .map(Number::longValue).toList();
        businessTopicService.addItems(id, itemTypes, itemIds, user);
    }

    @DeleteMapping("/{id}/items/{itemType}/{itemId}")
    public void removeItem(@PathVariable Long id, @PathVariable String itemType,
            @PathVariable Long itemId,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        businessTopicService.removeItem(id, itemType, itemId, user);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/BusinessTopicController.java
git commit -m "feat: add BusinessTopicController REST endpoints"
```

---

### Task 6: Frontend API service

**Files:**
- Create: `webapp/packages/supersonic-fe/src/services/businessTopic.ts`

- [ ] **Step 1: Create service**

```typescript
import request from './request';

export interface TopicItem {
  itemId: number;
  itemType: 'FIXED_REPORT' | 'ALERT_RULE' | 'SCHEDULE';
  itemName: string;
}

export interface BusinessTopic {
  id?: number;
  name: string;
  description?: string;
  priority?: number;
  ownerId?: number;
  ownerName?: string;
  defaultDeliveryConfigIds?: string;
  enabled?: number;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;

  fixedReportCount?: number;
  alertRuleCount?: number;
  scheduleCount?: number;
  items?: TopicItem[];
}

const BASE = '/api/v1/businessTopics';

export function getTopicList(params?: { current?: number; pageSize?: number; enabled?: boolean }) {
  return request(BASE, { method: 'GET', params });
}

export function getTopicDetail(id: number) {
  return request(`${BASE}/${id}`, { method: 'GET' });
}

export function createTopic(data: Partial<BusinessTopic>) {
  return request(BASE, { method: 'POST', data });
}

export function updateTopic(id: number, data: Partial<BusinessTopic>) {
  return request(`${BASE}/${id}`, { method: 'PATCH', data });
}

export function deleteTopic(id: number) {
  return request(`${BASE}/${id}`, { method: 'DELETE' });
}

export function addTopicItems(
  topicId: number,
  itemTypes: string[],
  itemIds: number[],
) {
  return request(`${BASE}/${topicId}/items`, {
    method: 'POST',
    data: { itemTypes, itemIds },
  });
}

export function removeTopicItem(topicId: number, itemType: string, itemId: number) {
  return request(`${BASE}/${topicId}/items/${itemType}/${itemId}`, { method: 'DELETE' });
}
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/services/businessTopic.ts
git commit -m "feat: add businessTopic.ts frontend API service"
```

---

### Task 7: TopicFormModal component

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/BusinessTopics/components/TopicFormModal.tsx`

- [ ] **Step 1: Create component**

```tsx
import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Switch } from 'antd';
import type { BusinessTopic } from '@/services/businessTopic';

interface TopicFormModalProps {
  visible: boolean;
  record?: BusinessTopic;
  onCancel: () => void;
  onSubmit: (values: Partial<BusinessTopic>) => void;
}

const TopicFormModal: React.FC<TopicFormModalProps> = ({
  visible,
  record,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      if (record) {
        form.setFieldsValue({
          name: record.name,
          description: record.description,
          priority: record.priority ?? 0,
          enabled: record.enabled !== 0,
        });
      } else {
        form.resetFields();
      }
    }
  }, [visible, record]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit({
      ...values,
      enabled: values.enabled ? 1 : 0,
    });
  };

  return (
    <Modal
      title={record ? '编辑经营主题' : '新建经营主题'}
      open={visible}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
      width={520}
    >
      <Form form={form} layout="vertical" initialValues={{ priority: 0, enabled: true }}>
        <Form.Item
          name="name"
          label="主题名称"
          rules={[{ required: true, message: '请输入主题名称' }]}
        >
          <Input maxLength={200} placeholder="如：每日收入看板" />
        </Form.Item>
        <Form.Item name="description" label="说明">
          <Input.TextArea rows={3} placeholder="描述该主题关注的业务范围" />
        </Form.Item>
        <Form.Item
          name="priority"
          label="优先级"
          tooltip="数值越小优先级越高，驾驶舱按此排序"
        >
          <InputNumber min={0} max={999} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TopicFormModal;
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/BusinessTopics/components/TopicFormModal.tsx
git commit -m "feat: add TopicFormModal component"
```

---

### Task 8: TopicDetailDrawer component

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/BusinessTopics/components/TopicDetailDrawer.tsx`

- [ ] **Step 1: Create component**

```tsx
import React, { useEffect, useState } from 'react';
import { Drawer, Descriptions, Table, Tag, Space, Button, Popconfirm, message, Empty } from 'antd';
import dayjs from 'dayjs';
import type { BusinessTopic, TopicItem } from '@/services/businessTopic';
import { getTopicDetail, removeTopicItem } from '@/services/businessTopic';

const ITEM_TYPE_MAP: Record<string, { color: string; text: string }> = {
  FIXED_REPORT: { color: 'blue', text: '固定报表' },
  ALERT_RULE: { color: 'red', text: '告警规则' },
  SCHEDULE: { color: 'green', text: '定时任务' },
};

interface TopicDetailDrawerProps {
  visible: boolean;
  topicId?: number;
  onClose: () => void;
  onItemRemoved?: () => void;
}

const TopicDetailDrawer: React.FC<TopicDetailDrawerProps> = ({
  visible,
  topicId,
  onClose,
  onItemRemoved,
}) => {
  const [topic, setTopic] = useState<BusinessTopic | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchDetail = async () => {
    if (!topicId) return;
    setLoading(true);
    try {
      const res: any = await getTopicDetail(topicId);
      const data = res?.data ?? res;
      setTopic(data);
    } catch {
      message.error('加载主题详情失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && topicId) {
      fetchDetail();
    }
  }, [visible, topicId]);

  const handleRemoveItem = async (itemType: string, itemId: number) => {
    if (!topicId) return;
    try {
      await removeTopicItem(topicId, itemType, itemId);
      message.success('已移除');
      fetchDetail();
      onItemRemoved?.();
    } catch {
      message.error('移除失败');
    }
  };

  const itemColumns = [
    {
      title: '类型',
      dataIndex: 'itemType',
      width: 120,
      render: (type: string) => {
        const info = ITEM_TYPE_MAP[type] || { color: 'default', text: type };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '名称',
      dataIndex: 'itemName',
      ellipsis: true,
    },
    {
      title: '操作',
      width: 80,
      render: (_: any, record: TopicItem) => (
        <Popconfirm title="确认移除?" onConfirm={() => handleRemoveItem(record.itemType, record.itemId)} okText="确认" cancelText="取消">
          <Button type="link" size="small" danger>
            移除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Drawer
      title={topic?.name || '主题详情'}
      open={visible}
      onClose={onClose}
      width={640}
      loading={loading}
    >
      {topic && (
        <>
          <Descriptions column={2} size="small" style={{ marginBottom: 24 }}>
            <Descriptions.Item label="说明" span={2}>
              {topic.description || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="优先级">{topic.priority}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={topic.enabled ? 'green' : 'default'}>
                {topic.enabled ? '启用' : '停用'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="固定报表">{topic.fixedReportCount} 个</Descriptions.Item>
            <Descriptions.Item label="告警规则">{topic.alertRuleCount} 个</Descriptions.Item>
            <Descriptions.Item label="定时任务">{topic.scheduleCount} 个</Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {topic.createdAt ? dayjs(topic.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
            </Descriptions.Item>
          </Descriptions>

          <h4 style={{ marginBottom: 12 }}>关联对象</h4>
          <Table
            rowKey={(r) => `${r.itemType}-${r.itemId}`}
            columns={itemColumns}
            dataSource={topic.items || []}
            size="small"
            bordered={false}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无关联对象" /> }}
          />
        </>
      )}
    </Drawer>
  );
};

export default TopicDetailDrawer;
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/BusinessTopics/components/TopicDetailDrawer.tsx
git commit -m "feat: add TopicDetailDrawer component"
```

---

### Task 9: BusinessTopics page + styles + route

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/BusinessTopics/index.tsx`
- Create: `webapp/packages/supersonic-fe/src/pages/BusinessTopics/style.less`
- Modify: `webapp/packages/supersonic-fe/config/routes.ts`

- [ ] **Step 1: Create style.less**

```less
.topicsPage {
  padding: 24px;
}

.pageHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pageTitle {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color, rgba(0, 10, 36, 0.85));
}
```

- [ ] **Step 2: Create index.tsx**

```tsx
import React, { useEffect, useState } from 'react';
import { Table, Tag, Space, Button, Popconfirm, message, Empty } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import TopicFormModal from './components/TopicFormModal';
import TopicDetailDrawer from './components/TopicDetailDrawer';
import {
  getTopicList,
  createTopic,
  updateTopic,
  deleteTopic,
} from '@/services/businessTopic';
import type { BusinessTopic } from '@/services/businessTopic';
import taskStyles from '../TaskCenter/style.less';
import styles from './style.less';

const BusinessTopicsPage: React.FC = () => {
  const [data, setData] = useState<BusinessTopic[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  // Form modal
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<BusinessTopic | undefined>();

  // Detail drawer
  const [detailDrawer, setDetailDrawer] = useState<{ visible: boolean; topicId?: number }>({
    visible: false,
  });

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res: any = await getTopicList({ current, pageSize });
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch {
      message.error('加载经营主题失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: BusinessTopic) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleFormSubmit = async (values: Partial<BusinessTopic>) => {
    try {
      if (editRecord?.id) {
        await updateTopic(editRecord.id, values);
        message.success('更新成功');
      } else {
        await createTopic(values);
        message.success('创建成功');
      }
      setFormVisible(false);
      fetchData(pagination.current, pagination.pageSize);
    } catch {
      message.error(editRecord ? '更新失败' : '创建失败');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTopic(id);
      message.success('已删除');
      fetchData(pagination.current, pagination.pageSize);
    } catch {
      message.error('删除失败');
    }
  };

  const columns = [
    {
      title: '主题名称',
      dataIndex: 'name',
      width: 200,
      ellipsis: true,
      render: (val: string, record: BusinessTopic) => (
        <a onClick={() => setDetailDrawer({ visible: true, topicId: record.id })}>
          {val}
        </a>
      ),
    },
    {
      title: '说明',
      dataIndex: 'description',
      width: 200,
      ellipsis: true,
      render: (val?: string) => val || '—',
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 80,
      align: 'center' as const,
    },
    {
      title: '固定报表',
      dataIndex: 'fixedReportCount',
      width: 90,
      align: 'center' as const,
      render: (val: number) => val || 0,
    },
    {
      title: '告警规则',
      dataIndex: 'alertRuleCount',
      width: 90,
      align: 'center' as const,
      render: (val: number) => val || 0,
    },
    {
      title: '定时任务',
      dataIndex: 'scheduleCount',
      width: 90,
      align: 'center' as const,
      render: (val: number) => val || 0,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (enabled: number) => (
        <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '停用'}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (val?: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '—'),
    },
    {
      title: '操作',
      width: 140,
      fixed: 'right' as const,
      render: (_: any, record: BusinessTopic) => (
        <Space size={4} wrap>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除该主题？关联对象将被解绑。"
            onConfirm={() => handleDelete(record.id!)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.topicsPage}>
      <div className={styles.pageHeader}>
        <h3 className={styles.pageTitle}>经营主题</h3>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建主题
        </Button>
      </div>
      <div className={taskStyles.tableShell}>
        <Table
          rowKey="id"
          size="middle"
          bordered={false}
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="暂无经营主题" /> }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => fetchData(page, size),
          }}
        />
      </div>

      <TopicFormModal
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />

      <TopicDetailDrawer
        visible={detailDrawer.visible}
        topicId={detailDrawer.topicId}
        onClose={() => setDetailDrawer({ visible: false })}
        onItemRemoved={() => fetchData(pagination.current, pagination.pageSize)}
      />
    </div>
  );
};

export default BusinessTopicsPage;
```

- [ ] **Step 3: Register route**

In `webapp/packages/supersonic-fe/config/routes.ts`, add **before** the `/reports` route entry:

```typescript
  {
    path: '/business-topics',
    name: 'businessTopics',
    component: './BusinessTopics',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
```

- [ ] **Step 4: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/BusinessTopics/
git add webapp/packages/supersonic-fe/config/routes.ts
git commit -m "feat: add BusinessTopics page with list, form, and detail drawer"
```

---

### Task 10: Integration verification

- [ ] **Step 1: Compile backend**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run tests**

Run: `mvn test -pl headless/server -Dtest="BusinessTopicServiceImplTest" -q`
Expected: 2 tests PASS

- [ ] **Step 3: Verify frontend TypeScript**

Run: `cd webapp/packages/supersonic-fe && npx --package=typescript tsc --noEmit 2>&1 | grep -v "tsconfig" | head -20`
Expected: No errors from our new files

- [ ] **Step 4: Commit (if any fixes needed)**
