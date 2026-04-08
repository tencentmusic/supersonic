# Exception Handling Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing alert system from "fire-and-forget notification" to a full exception handling workflow where alert events can be acknowledged, assigned, resolved, and closed — with status timeline and responsibility tracking.

**Architecture:** Add a `resolution_status` state machine to the existing `s2_alert_event` table (OPEN → CONFIRMED → ASSIGNED → RESOLVED → CLOSED), plus `acknowledged_by`, `assignee_id`, `resolved_by`, and `notes` fields. New REST endpoints handle state transitions. A new frontend `AlertEventDrawer` component provides event-level viewing and actions. The existing `AlertRuleTab` and `AlertExecutionDrawer` gain links to navigate into events.

**Tech Stack:** Java 21 / Spring Boot 3.4 / MyBatis-Plus / JUnit 5 (backend); TypeScript / React / Ant Design (frontend)

---

## File Structure

### Backend — New Files
| File | Responsibility |
|------|---------------|
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/AlertResolutionStatus.java` | Enum for the state machine: OPEN, CONFIRMED, ASSIGNED, RESOLVED, CLOSED |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/AlertEventTransitionReq.java` | Request DTO for state transitions (target status + notes + assigneeId) |
| `launchers/standalone/src/main/resources/db/migration/mysql/V27__alert_event_resolution.sql` | MySQL migration adding resolution columns |
| `launchers/standalone/src/main/resources/db/migration/postgresql/V27__alert_event_resolution.sql` | PostgreSQL migration |
| `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/AlertResolutionStatusTest.java` | State machine transition validation tests |

### Backend — Modified Files
| File | Change |
|------|--------|
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/AlertEventDO.java` | Add resolution fields |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/AlertRuleService.java` | Add `transitionEvent()` and `getEventById()` methods |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertRuleServiceImpl.java` | Implement state transitions with validation |
| `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/AlertRuleController.java` | Add event-level endpoints |

### Frontend — New Files
| File | Responsibility |
|------|---------------|
| `webapp/packages/supersonic-fe/src/pages/TaskCenter/components/AlertEventDrawer.tsx` | Drawer showing event detail + timeline + action buttons |

### Frontend — Modified Files
| File | Change |
|------|--------|
| `webapp/packages/supersonic-fe/src/services/alertRule.ts` | Add event types and transition API functions |
| `webapp/packages/supersonic-fe/src/pages/TaskCenter/components/AlertExecutionDrawer.tsx` | Add link to view events for each execution |
| `webapp/packages/supersonic-fe/src/pages/TaskCenter/AlertRuleTab.tsx` | Add pending events badge count |

---

### Task 1: Database Migration — Resolution Columns

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V27__alert_event_resolution.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V27__alert_event_resolution.sql`

- [ ] **Step 1: Create MySQL migration**

```sql
-- V27__alert_event_resolution.sql
-- Add exception handling workflow columns to alert events

ALTER TABLE s2_alert_event
    ADD COLUMN resolution_status VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CONFIRMED/ASSIGNED/RESOLVED/CLOSED',
    ADD COLUMN acknowledged_by   VARCHAR(100) NULL     COMMENT 'User who confirmed the event',
    ADD COLUMN acknowledged_at   DATETIME     NULL     COMMENT 'When the event was confirmed',
    ADD COLUMN assignee_id       BIGINT       NULL     COMMENT 'User ID of assigned handler',
    ADD COLUMN assigned_at       DATETIME     NULL     COMMENT 'When the event was assigned',
    ADD COLUMN resolved_by       VARCHAR(100) NULL     COMMENT 'User who resolved the event',
    ADD COLUMN resolved_at       DATETIME     NULL     COMMENT 'When the event was resolved',
    ADD COLUMN closed_at         DATETIME     NULL     COMMENT 'When the event was closed',
    ADD COLUMN notes             TEXT         NULL     COMMENT 'Resolution notes / timeline entries';

ALTER TABLE s2_alert_event
    ADD INDEX idx_alert_event_resolution (resolution_status);
```

- [ ] **Step 2: Create PostgreSQL migration**

```sql
-- V27__alert_event_resolution.sql
-- Add exception handling workflow columns to alert events

ALTER TABLE s2_alert_event
    ADD COLUMN IF NOT EXISTS resolution_status VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    ADD COLUMN IF NOT EXISTS acknowledged_by   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS acknowledged_at   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS assignee_id       BIGINT,
    ADD COLUMN IF NOT EXISTS assigned_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resolved_by       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS resolved_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS closed_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS notes             TEXT;

CREATE INDEX IF NOT EXISTS idx_alert_event_resolution ON s2_alert_event (resolution_status);
```

- [ ] **Step 3: Compile to verify migrations are picked up**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V27__alert_event_resolution.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V27__alert_event_resolution.sql
git commit -m "feat(alert): add resolution workflow columns to s2_alert_event (V27)"
```

---

### Task 2: Backend — Resolution Status Enum + Transition Request

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/AlertResolutionStatus.java`
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/AlertEventTransitionReq.java`

- [ ] **Step 1: Write the failing test for state transitions**

Create `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/AlertResolutionStatusTest.java`:

```java
package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.pojo.AlertResolutionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertResolutionStatusTest {

    @Test
    void openCanTransitionToConfirmedOrAssigned() {
        assertTrue(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.CONFIRMED));
        assertTrue(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.ASSIGNED));
        assertFalse(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.RESOLVED));
        assertFalse(AlertResolutionStatus.OPEN.canTransitionTo(AlertResolutionStatus.CLOSED));
    }

    @Test
    void confirmedCanTransitionToAssignedOrResolved() {
        assertTrue(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.ASSIGNED));
        assertTrue(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.RESOLVED));
        assertFalse(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.CONFIRMED.canTransitionTo(AlertResolutionStatus.CLOSED));
    }

    @Test
    void assignedCanTransitionToResolved() {
        assertTrue(AlertResolutionStatus.ASSIGNED.canTransitionTo(AlertResolutionStatus.RESOLVED));
        assertFalse(AlertResolutionStatus.ASSIGNED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.ASSIGNED.canTransitionTo(AlertResolutionStatus.CLOSED));
    }

    @Test
    void resolvedCanTransitionToClosed() {
        assertTrue(AlertResolutionStatus.RESOLVED.canTransitionTo(AlertResolutionStatus.CLOSED));
        assertFalse(AlertResolutionStatus.RESOLVED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.RESOLVED.canTransitionTo(AlertResolutionStatus.ASSIGNED));
    }

    @Test
    void closedIsTerminal() {
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.OPEN));
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.CONFIRMED));
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.ASSIGNED));
        assertFalse(AlertResolutionStatus.CLOSED.canTransitionTo(AlertResolutionStatus.RESOLVED));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl headless/server -Dtest=AlertResolutionStatusTest -q 2>&1 | tail -5`
Expected: FAIL — `AlertResolutionStatus` does not exist

- [ ] **Step 3: Create AlertResolutionStatus enum**

```java
package com.tencent.supersonic.headless.server.pojo;

import java.util.Set;

public enum AlertResolutionStatus {
    OPEN(Set.of("CONFIRMED", "ASSIGNED")),
    CONFIRMED(Set.of("ASSIGNED", "RESOLVED")),
    ASSIGNED(Set.of("RESOLVED")),
    RESOLVED(Set.of("CLOSED")),
    CLOSED(Set.of());

    private final Set<String> allowedTargets;

    AlertResolutionStatus(Set<String> allowedTargets) {
        this.allowedTargets = allowedTargets;
    }

    public boolean canTransitionTo(AlertResolutionStatus target) {
        return allowedTargets.contains(target.name());
    }
}
```

- [ ] **Step 4: Create AlertEventTransitionReq**

```java
package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

@Data
public class AlertEventTransitionReq {
    private AlertResolutionStatus targetStatus;
    private Long assigneeId;
    private String notes;
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -pl headless/server -Dtest=AlertResolutionStatusTest -q 2>&1 | tail -5`
Expected: All 5 tests PASS

- [ ] **Step 6: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/AlertResolutionStatus.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/AlertEventTransitionReq.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/service/AlertResolutionStatusTest.java
git commit -m "feat(alert): add AlertResolutionStatus state machine and transition request DTO"
```

---

### Task 3: Backend — Update AlertEventDO Entity

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/AlertEventDO.java`

- [ ] **Step 1: Add resolution fields to AlertEventDO**

Add these fields after the existing `silenceUntil` field:

```java
    // --- Resolution workflow ---
    private String resolutionStatus;    // OPEN / CONFIRMED / ASSIGNED / RESOLVED / CLOSED
    private String acknowledgedBy;
    private Date acknowledgedAt;
    private Long assigneeId;
    private Date assignedAt;
    private String resolvedBy;
    private Date resolvedAt;
    private Date closedAt;
    private String notes;
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/AlertEventDO.java
git commit -m "feat(alert): add resolution workflow fields to AlertEventDO"
```

---

### Task 4: Backend — Initialize resolutionStatus in AlertCheckDispatcher

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertCheckDispatcher.java`

- [ ] **Step 1: Set OPEN status when creating new events**

Find the section in `AlertCheckDispatcher.dispatch()` where `AlertEventDO` objects are created (the loop that iterates over non-silenced candidates and calls `eventMapper.insert()`). Add one line after the existing field assignments:

```java
eventDO.setResolutionStatus("OPEN");
```

This should be added right before the `eventMapper.insert(eventDO)` call, alongside the existing field assignments like `setDeliveryStatus("PENDING")`, `setSilenceUntil(...)`, etc.

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertCheckDispatcher.java
git commit -m "feat(alert): set resolutionStatus=OPEN on new alert events"
```

---

### Task 5: Backend — Service Layer: Transition Logic + Event Query

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/AlertRuleService.java`
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertRuleServiceImpl.java`

- [ ] **Step 1: Add new methods to AlertRuleService interface**

Add these method signatures:

```java
    AlertEventDO getEventById(Long eventId);

    AlertEventDO transitionEvent(Long eventId, AlertEventTransitionReq req, User user);

    /** Count events in non-terminal states (OPEN, CONFIRMED, ASSIGNED) grouped by ruleId */
    Map<Long, Long> countPendingEventsByRule();
```

- [ ] **Step 2: Implement in AlertRuleServiceImpl**

Add the necessary imports:

```java
import com.tencent.supersonic.headless.server.pojo.AlertEventTransitionReq;
import com.tencent.supersonic.headless.server.pojo.AlertResolutionStatus;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import java.util.Map;
import java.util.HashMap;
```

Add the implementation methods:

```java
    @Override
    public AlertEventDO getEventById(Long eventId) {
        return eventMapper.selectById(eventId);
    }

    @Override
    public AlertEventDO transitionEvent(Long eventId, AlertEventTransitionReq req, User user) {
        AlertEventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new InvalidArgumentException("Alert event not found: " + eventId);
        }

        AlertResolutionStatus current = AlertResolutionStatus.valueOf(
                event.getResolutionStatus() != null ? event.getResolutionStatus() : "OPEN");
        AlertResolutionStatus target = req.getTargetStatus();

        if (!current.canTransitionTo(target)) {
            throw new InvalidArgumentException(
                    String.format("Cannot transition from %s to %s", current, target));
        }

        Date now = new Date();
        String userName = user.getName();

        switch (target) {
            case CONFIRMED:
                event.setAcknowledgedBy(userName);
                event.setAcknowledgedAt(now);
                break;
            case ASSIGNED:
                if (event.getAcknowledgedBy() == null) {
                    // Auto-confirm when directly assigning from OPEN
                    event.setAcknowledgedBy(userName);
                    event.setAcknowledgedAt(now);
                }
                event.setAssigneeId(req.getAssigneeId());
                event.setAssignedAt(now);
                break;
            case RESOLVED:
                event.setResolvedBy(userName);
                event.setResolvedAt(now);
                break;
            case CLOSED:
                event.setClosedAt(now);
                break;
            default:
                break;
        }

        event.setResolutionStatus(target.name());

        // Append notes with timestamp
        if (req.getNotes() != null && !req.getNotes().isBlank()) {
            String entry = String.format("[%1$tF %1$tT] %2$s → %3$s by %4$s: %5$s",
                    now, current, target, userName, req.getNotes());
            String existing = event.getNotes();
            event.setNotes(existing != null ? existing + "\n" + entry : entry);
        }

        eventMapper.updateById(event);
        return event;
    }

    @Override
    public Map<Long, Long> countPendingEventsByRule() {
        List<AlertEventDO> pendingEvents = eventMapper.selectList(
                new LambdaQueryWrapper<AlertEventDO>()
                        .in(AlertEventDO::getResolutionStatus, "OPEN", "CONFIRMED", "ASSIGNED"));
        Map<Long, Long> counts = new HashMap<>();
        for (AlertEventDO e : pendingEvents) {
            counts.merge(e.getRuleId(), 1L, Long::sum);
        }
        return counts;
    }
```

- [ ] **Step 3: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/AlertRuleService.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertRuleServiceImpl.java
git commit -m "feat(alert): add event transition logic and pending event count"
```

---

### Task 6: Backend — Controller Endpoints for Events

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/AlertRuleController.java`

- [ ] **Step 1: Add event-level endpoints**

Add these endpoints to `AlertRuleController`:

```java
    @GetMapping("/events/{eventId}")
    public AlertEventDO getEvent(@PathVariable Long eventId) {
        return alertRuleService.getEventById(eventId);
    }

    @PostMapping("/events/{eventId}:transition")
    public AlertEventDO transitionEvent(
            @PathVariable Long eventId,
            @RequestBody AlertEventTransitionReq req,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return alertRuleService.transitionEvent(eventId, req, user);
    }

    @GetMapping("/events/pendingCounts")
    public Map<Long, Long> getPendingEventCounts() {
        return alertRuleService.countPendingEventsByRule();
    }
```

Add the required imports:

```java
import com.tencent.supersonic.headless.server.pojo.AlertEventTransitionReq;
import java.util.Map;
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/AlertRuleController.java
git commit -m "feat(alert): add event detail, transition, and pending count endpoints"
```

---

### Task 7: Frontend — API Service Updates

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/services/alertRule.ts`

- [ ] **Step 1: Add event types and API functions**

Append to the end of `alertRule.ts`:

```typescript
export type ResolutionStatus = 'OPEN' | 'CONFIRMED' | 'ASSIGNED' | 'RESOLVED' | 'CLOSED';

export interface AlertEvent {
  id: number;
  executionId: number;
  ruleId: number;
  conditionIndex: number;
  severity: string;
  alertKey: string;
  dimensionValue?: string;
  metricValue?: number;
  baselineValue?: number;
  deviationPct?: number;
  message?: string;
  deliveryStatus: string;
  silenceUntil?: string;
  resolutionStatus: ResolutionStatus;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  assigneeId?: number;
  assignedAt?: string;
  resolvedBy?: string;
  resolvedAt?: string;
  closedAt?: string;
  notes?: string;
  createdAt?: string;
}

export function getEvents(
  params?: { current?: number; pageSize?: number; ruleId?: number; severity?: string; deliveryStatus?: string },
): Promise<any> {
  return request(`${BASE}/events`, { method: 'GET', params });
}

export function getEventById(eventId: number): Promise<AlertEvent> {
  return request(`${BASE}/events/${eventId}`, { method: 'GET' });
}

export function transitionEvent(
  eventId: number,
  data: { targetStatus: ResolutionStatus; assigneeId?: number; notes?: string },
): Promise<AlertEvent> {
  return request(`${BASE}/events/${eventId}:transition`, { method: 'POST', data });
}

export function getPendingEventCounts(): Promise<Record<number, number>> {
  return request(`${BASE}/events/pendingCounts`, { method: 'GET' });
}
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/services/alertRule.ts
git commit -m "feat(alert): add event types and transition API functions"
```

---

### Task 8: Frontend — AlertEventDrawer Component

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/TaskCenter/components/AlertEventDrawer.tsx`

- [ ] **Step 1: Create the drawer component**

```tsx
import React, { useEffect, useState } from 'react';
import {
  Drawer,
  Table,
  Tag,
  Button,
  Space,
  Popconfirm,
  Input,
  message,
  Empty,
  Descriptions,
  Typography,
  InputNumber,
} from 'antd';
import dayjs from 'dayjs';
import type { AlertEvent, ResolutionStatus } from '@/services/alertRule';
import { getEvents, transitionEvent } from '@/services/alertRule';

const { Text } = Typography;
const { TextArea } = Input;

const RESOLUTION_STATUS_MAP: Record<string, { color: string; text: string }> = {
  OPEN: { color: 'red', text: '待确认' },
  CONFIRMED: { color: 'orange', text: '已确认' },
  ASSIGNED: { color: 'blue', text: '已接手' },
  RESOLVED: { color: 'green', text: '已恢复' },
  CLOSED: { color: 'default', text: '已关闭' },
};

const SEVERITY_MAP: Record<string, { color: string; text: string }> = {
  CRITICAL: { color: 'red', text: '严重' },
  WARNING: { color: 'orange', text: '警告' },
};

const ALLOWED_TRANSITIONS: Record<string, { label: string; target: ResolutionStatus; danger?: boolean }[]> = {
  OPEN: [
    { label: '确认已知', target: 'CONFIRMED' },
    { label: '接手处理', target: 'ASSIGNED' },
  ],
  CONFIRMED: [
    { label: '接手处理', target: 'ASSIGNED' },
    { label: '标记恢复', target: 'RESOLVED' },
  ],
  ASSIGNED: [
    { label: '标记恢复', target: 'RESOLVED' },
  ],
  RESOLVED: [
    { label: '关闭', target: 'CLOSED' },
  ],
  CLOSED: [],
};

interface AlertEventDrawerProps {
  visible: boolean;
  ruleId?: number;
  ruleName?: string;
  executionId?: number;
  onClose: () => void;
}

const AlertEventDrawer: React.FC<AlertEventDrawerProps> = ({
  visible,
  ruleId,
  ruleName,
  executionId,
  onClose,
}) => {
  const [data, setData] = useState<AlertEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  // Transition form state
  const [transitionNotes, setTransitionNotes] = useState('');
  const [assigneeId, setAssigneeId] = useState<number | undefined>();
  const [transitioning, setTransitioning] = useState<number | null>(null);

  // Detail expansion
  const [expandedEvent, setExpandedEvent] = useState<AlertEvent | null>(null);

  const fetchData = async (current = 1, pageSize = 20) => {
    if (!ruleId) return;
    setLoading(true);
    try {
      const params: any = { current, pageSize, ruleId };
      const res = await getEvents(params);
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch {
      message.error('加载异常事件失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && ruleId) {
      fetchData();
    }
  }, [visible, ruleId]);

  const handleTransition = async (eventId: number, target: ResolutionStatus) => {
    setTransitioning(eventId);
    try {
      await transitionEvent(eventId, {
        targetStatus: target,
        assigneeId: target === 'ASSIGNED' ? assigneeId : undefined,
        notes: transitionNotes || undefined,
      });
      message.success('状态已更新');
      setTransitionNotes('');
      setAssigneeId(undefined);
      fetchData(pagination.current, pagination.pageSize);
    } catch (e: any) {
      message.error(e?.data?.msg || '操作失败');
    } finally {
      setTransitioning(null);
    }
  };

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '严重度',
      dataIndex: 'severity',
      width: 80,
      render: (val: string) => {
        const info = SEVERITY_MAP[val] || { color: 'default', text: val };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '维度值',
      dataIndex: 'dimensionValue',
      width: 120,
      ellipsis: true,
    },
    {
      title: '指标值',
      dataIndex: 'metricValue',
      width: 100,
      render: (val?: number) => (val != null ? val.toFixed(2) : '-'),
    },
    {
      title: '处置状态',
      dataIndex: 'resolutionStatus',
      width: 100,
      render: (status: string) => {
        const info = RESOLUTION_STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '确认人',
      dataIndex: 'acknowledgedBy',
      width: 100,
      render: (val?: string) => val || '-',
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: AlertEvent) => {
        const actions = ALLOWED_TRANSITIONS[record.resolutionStatus] || [];
        if (actions.length === 0) return <Text type="secondary">已完结</Text>;
        return (
          <Space size={4} wrap>
            {actions.map((action) => (
              <Popconfirm
                key={action.target}
                title={
                  <div>
                    <div style={{ marginBottom: 8 }}>{`确认${action.label}？`}</div>
                    {action.target === 'ASSIGNED' && (
                      <div style={{ marginBottom: 8 }}>
                        <Text type="secondary">接手人 ID：</Text>
                        <InputNumber
                          size="small"
                          value={assigneeId}
                          onChange={(v) => setAssigneeId(v ?? undefined)}
                          style={{ width: 100 }}
                        />
                      </div>
                    )}
                    <TextArea
                      rows={2}
                      placeholder="备注（可选）"
                      value={transitionNotes}
                      onChange={(e) => setTransitionNotes(e.target.value)}
                    />
                  </div>
                }
                onConfirm={() => handleTransition(record.id, action.target)}
                okText="确认"
                cancelText="取消"
                onCancel={() => {
                  setTransitionNotes('');
                  setAssigneeId(undefined);
                }}
              >
                <Button
                  type="link"
                  size="small"
                  danger={action.danger}
                  loading={transitioning === record.id}
                >
                  {action.label}
                </Button>
              </Popconfirm>
            ))}
          </Space>
        );
      },
    },
  ];

  const expandedRowRender = (record: AlertEvent) => (
    <Descriptions column={2} size="small" bordered>
      <Descriptions.Item label="消息">{record.message || '-'}</Descriptions.Item>
      <Descriptions.Item label="基线值">
        {record.baselineValue != null ? record.baselineValue.toFixed(2) : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="偏差率">
        {record.deviationPct != null ? `${record.deviationPct.toFixed(1)}%` : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="投递状态">{record.deliveryStatus}</Descriptions.Item>
      <Descriptions.Item label="确认时间">
        {record.acknowledgedAt ? dayjs(record.acknowledgedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="接手时间">
        {record.assignedAt ? dayjs(record.assignedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="恢复时间">
        {record.resolvedAt ? dayjs(record.resolvedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="关闭时间">
        {record.closedAt ? dayjs(record.closedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      {record.notes && (
        <Descriptions.Item label="处理记录" span={2}>
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 12 }}>{record.notes}</pre>
        </Descriptions.Item>
      )}
    </Descriptions>
  );

  return (
    <Drawer
      title={`异常事件 - ${ruleName || ''}`}
      open={visible}
      onClose={onClose}
      width={960}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        bordered={false}
        scroll={{ x: 'max-content' }}
        size="middle"
        expandable={{
          expandedRowRender,
          rowExpandable: () => true,
        }}
        locale={{
          emptyText: <Empty description="暂无异常事件" />,
        }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => fetchData(page, size),
        }}
      />
    </Drawer>
  );
};

export default AlertEventDrawer;
```

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/TaskCenter/components/AlertEventDrawer.tsx
git commit -m "feat(alert): add AlertEventDrawer with resolution workflow actions"
```

---

### Task 9: Frontend — Wire AlertEventDrawer into AlertRuleTab

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/pages/TaskCenter/AlertRuleTab.tsx`

- [ ] **Step 1: Add event drawer state and import**

Add import at the top of `AlertRuleTab.tsx`:

```typescript
import AlertEventDrawer from './components/AlertEventDrawer';
import { getPendingEventCounts } from '@/services/alertRule';
```

Add state variables alongside existing state:

```typescript
const [eventDrawer, setEventDrawer] = useState<{
  visible: boolean;
  ruleId?: number;
  name?: string;
}>({ visible: false });
const [pendingCounts, setPendingCounts] = useState<Record<number, number>>({});
```

- [ ] **Step 2: Fetch pending event counts**

Add a `fetchPendingCounts` function and call it in the existing `useEffect`:

```typescript
const fetchPendingCounts = async () => {
  try {
    const res: any = await getPendingEventCounts();
    const counts = res?.data ?? res;
    setPendingCounts(counts || {});
  } catch {
    // silent — badge counts are non-critical
  }
};
```

Call `fetchPendingCounts()` alongside the existing `fetchData()` in the `useEffect`.

- [ ] **Step 3: Add "View Events" button to the operations column**

In the existing operations column render function, add a button between "View Executions" and "Delete":

```tsx
<Button
  type="link"
  size="small"
  onClick={() =>
    setEventDrawer({ visible: true, ruleId: record.id, name: record.name })
  }
>
  异常事件
  {pendingCounts[record.id!] ? (
    <Tag color="red" style={{ marginLeft: 4, fontSize: 11 }}>
      {pendingCounts[record.id!]}
    </Tag>
  ) : null}
</Button>
```

- [ ] **Step 4: Add AlertEventDrawer to JSX**

Below the existing `AlertExecutionDrawer`, add:

```tsx
<AlertEventDrawer
  visible={eventDrawer.visible}
  ruleId={eventDrawer.ruleId}
  ruleName={eventDrawer.name}
  onClose={() => {
    setEventDrawer({ visible: false });
    fetchPendingCounts();
  }}
/>
```

- [ ] **Step 5: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/TaskCenter/AlertRuleTab.tsx
git commit -m "feat(alert): wire AlertEventDrawer into AlertRuleTab with pending event badges"
```

---

### Task 10: Integration Verification

- [ ] **Step 1: Compile backend**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all alert-related tests**

Run: `mvn test -pl headless/server -Dtest="AlertResolutionStatusTest,AlertEvaluatorTest" -q`
Expected: All tests PASS

- [ ] **Step 3: Verify frontend builds**

Run: `cd webapp && pnpm --filter supersonic-fe build 2>&1 | tail -10`
Expected: Build completes without errors

- [ ] **Step 4: Manual smoke test checklist**

Verify these scenarios after starting the app:

1. New alert events have `resolution_status = OPEN`
2. `GET /api/v1/alertRules/events?ruleId=X` returns events with resolution fields
3. `POST /api/v1/alertRules/events/{id}:transition` with `{"targetStatus":"CONFIRMED"}` works
4. Invalid transitions (e.g., OPEN → RESOLVED) return 400
5. Notes are appended with timestamps
6. "异常事件" button appears in AlertRuleTab with pending count badge
7. AlertEventDrawer shows events with action buttons
8. Expanding a row shows full event details including timeline
9. Transition actions update status and refresh the table

---

## Self-Review Checklist

- [x] **Spec coverage**: §5.5 (异常处置流) — OPEN/CONFIRMED/ASSIGNED/RESOLVED/CLOSED states, responsibility tracking (acknowledgedBy, assigneeId, resolvedBy), processing records (notes with timestamps), all implemented
- [x] **Placeholder scan**: No TBD/TODO/placeholder text in any step — all code is complete
- [x] **Type consistency**: `AlertResolutionStatus` enum values match frontend `ResolutionStatus` type; `AlertEventTransitionReq` Java class matches frontend `transitionEvent()` params; `AlertEventDO` field names match frontend `AlertEvent` interface using camelCase (MyBatis-Plus auto-maps underscored columns)
- [x] **Spec gap**: §5.5 mentions "影响主题与相关报表" — this depends on Business Topics (Plan #3) and is intentionally deferred. The current implementation handles event-level workflow which is the core capability.
