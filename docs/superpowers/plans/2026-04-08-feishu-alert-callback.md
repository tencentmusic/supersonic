# Feishu Alert Callback (飞书异常确认/接手回写) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable users to confirm ("我已知悉") or assign-to-self ("我来处理") alert events directly from Feishu interactive cards, writing the state transition back to the core platform.

**Architecture:** Alert delivery cards gain action buttons carrying event context. Feishu's event controller is extended to handle `card.action.trigger` events. A new `CardActionHandler` resolves the user, calls the core API via `SuperSonicApiClient`, and replies with a confirmation card. The existing `AlertRuleService.transitionEvent()` API does the actual state transition — no new backend logic needed.

**Tech Stack:** Java 21, Spring Boot, Feishu Open Platform Card Actions API

---

## File Structure

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `headless/server/.../service/delivery/DeliveryContext.java` | Add alert event ID list |
| Modify | `headless/server/.../service/impl/AlertCheckDispatcher.java` | Pass event IDs to delivery |
| Modify | `headless/server/.../service/delivery/FeishuDeliveryChannel.java` | Add action buttons to alert card |
| Modify | `feishu/server/.../service/SuperSonicApiClient.java` | Add transitionAlertEvent method |
| Modify | `feishu/server/.../service/HttpSuperSonicApiClient.java` | Implement HTTP call |
| Modify | `feishu/server/.../rest/FeishuEventController.java` | Handle card.action.trigger events |
| Modify | `feishu/server/.../service/FeishuBotService.java` | Dispatch card action events |
| Create | `feishu/server/.../handler/CardActionHandler.java` | Process card button actions |

All `headless/server/...` paths expand to `headless/server/src/main/java/com/tencent/supersonic/headless/server/`.
All `feishu/server/...` paths expand to `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/`.

---

### Task 1: Extend DeliveryContext with alert event IDs

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/delivery/DeliveryContext.java`

- [ ] **Step 1: Add alertEventIds field**

Add to the `DeliveryContext` class (which uses `@Data @Builder`):

```java
private List<Long> alertEventIds;
```

Also add the import:

```java
import java.util.List;
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS (Lombok @Builder auto-updates)

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/delivery/DeliveryContext.java
git commit -m "feat: add alertEventIds to DeliveryContext"
```

---

### Task 2: Pass event IDs from AlertCheckDispatcher to delivery

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertCheckDispatcher.java`

- [ ] **Step 1: Collect event IDs after persist, add to DeliveryContext**

In the `persistAndDeliver` method, after the loop that inserts `AlertEventDO` records, collect the generated IDs. Find the section that builds `DeliveryContext` (around lines 240-260) and add `alertEventIds`.

The persist loop looks like:
```java
for (AlertEvaluator.AlertEventCandidate candidate : toDeliver) {
    AlertEventDO event = new AlertEventDO();
    // ... set fields ...
    alertEventMapper.insert(event);
    // After insert, event.getId() is populated by MyBatis-Plus auto-increment
}
```

Change it to collect IDs:
```java
List<Long> eventIds = new ArrayList<>();
for (AlertEvaluator.AlertEventCandidate candidate : toDeliver) {
    AlertEventDO event = new AlertEventDO();
    // ... existing field assignments ...
    alertEventMapper.insert(event);
    eventIds.add(event.getId());
}
```

Then in the `DeliveryContext.builder()` call, add:
```java
.alertEventIds(eventIds)
```

Also add the import at the top of the file:
```java
import java.util.ArrayList;
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/AlertCheckDispatcher.java
git commit -m "feat: pass alert event IDs to delivery context"
```

---

### Task 3: Add action buttons to Feishu alert card

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/delivery/FeishuDeliveryChannel.java`

- [ ] **Step 1: Modify buildAlertCard to include action buttons**

In the `buildAlertCard(DeliveryContext context)` method, before the final `card.put("elements", elements)`, add an action row with two buttons. The buttons carry JSON payloads with the action type and event IDs:

```java
// Add action buttons if alert event IDs are available
if (context.getAlertEventIds() != null && !context.getAlertEventIds().isEmpty()) {
    Map<String, Object> actionDiv = new HashMap<>();
    actionDiv.put("tag", "action");
    List<Object> actions = new ArrayList<>();

    // "我已知悉" button — transitions events to CONFIRMED
    Map<String, Object> confirmBtn = new HashMap<>();
    confirmBtn.put("tag", "button");
    confirmBtn.put("type", "primary");
    confirmBtn.put("text", Map.of("tag", "plain_text", "content", "我已知悉"));
    confirmBtn.put("value", Map.of(
            "action", "alert_confirm",
            "ruleId", context.getAlertRuleId(),
            "eventIds", context.getAlertEventIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(","))));
    actions.add(confirmBtn);

    // "我来处理" button — transitions events to ASSIGNED (with current user as assignee)
    Map<String, Object> assignBtn = new HashMap<>();
    assignBtn.put("tag", "button");
    assignBtn.put("type", "default");
    assignBtn.put("text", Map.of("tag", "plain_text", "content", "我来处理"));
    assignBtn.put("value", Map.of(
            "action", "alert_assign",
            "ruleId", context.getAlertRuleId(),
            "eventIds", context.getAlertEventIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(","))));
    actions.add(assignBtn);

    actionDiv.put("actions", actions);
    elements.add(actionDiv);
}
```

Add the import if not present:
```java
import java.util.stream.Collectors;
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/delivery/FeishuDeliveryChannel.java
git commit -m "feat: add confirm/assign action buttons to Feishu alert cards"
```

---

### Task 4: Add transitionAlertEvent to SuperSonicApiClient

**Files:**
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/SuperSonicApiClient.java`
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/HttpSuperSonicApiClient.java`

- [ ] **Step 1: Add interface method**

In `SuperSonicApiClient.java`, add:

```java
/**
 * Transition an alert event to a new resolution status.
 * @param eventId the alert event ID
 * @param targetStatus one of: CONFIRMED, ASSIGNED, RESOLVED, CLOSED
 * @param assigneeId optional, required for ASSIGNED
 * @param notes optional note text
 * @param user the user performing the action
 */
void transitionAlertEvent(Long eventId, String targetStatus, Long assigneeId, String notes,
        User user);
```

- [ ] **Step 2: Implement in HttpSuperSonicApiClient**

In `HttpSuperSonicApiClient.java`, add the implementation:

```java
@Override
public void transitionAlertEvent(Long eventId, String targetStatus, Long assigneeId,
        String notes, User user) {
    String url = baseUrl + "/api/v1/alertRules/events/" + eventId + ":transition";
    HttpHeaders headers = buildHeaders(user);
    Map<String, Object> body = new HashMap<>();
    body.put("targetStatus", targetStatus);
    if (assigneeId != null) {
        body.put("assigneeId", assigneeId);
    }
    if (notes != null) {
        body.put("notes", notes);
    }
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
    try {
        restTemplate.postForObject(url, entity, String.class);
    } catch (Exception e) {
        log.error("Failed to transition alert event {}: {}", eventId, e.getMessage());
        throw e;
    }
}
```

Add import if needed:
```java
import java.util.HashMap;
```

- [ ] **Step 3: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/SuperSonicApiClient.java
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/HttpSuperSonicApiClient.java
git commit -m "feat: add transitionAlertEvent to SuperSonicApiClient"
```

---

### Task 5: Create CardActionHandler

**Files:**
- Create: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/CardActionHandler.java`

- [ ] **Step 1: Create handler**

```java
package com.tencent.supersonic.feishu.server.handler;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.server.render.FeishuCardTemplate;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardActionHandler {

    private final SuperSonicApiClient apiClient;
    private final FeishuMessageSender messageSender;

    /**
     * Handle a card action button click.
     *
     * @param actionValue the value map from the button (contains "action", "ruleId", "eventIds")
     * @param user resolved SuperSonic user
     * @param chatId the chat to reply to (open_chat_id from the event)
     */
    public void handle(Map<String, Object> actionValue, User user, String chatId) {
        String action = String.valueOf(actionValue.get("action"));
        String eventIdsStr = String.valueOf(actionValue.getOrDefault("eventIds", ""));
        List<Long> eventIds = Arrays.stream(eventIdsStr.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(Long::valueOf).collect(Collectors.toList());

        if (eventIds.isEmpty()) {
            log.warn("Card action with no event IDs: {}", actionValue);
            return;
        }

        String targetStatus;
        Long assigneeId = null;
        String actionLabel;

        switch (action) {
            case "alert_confirm":
                targetStatus = "CONFIRMED";
                actionLabel = "已确认知悉";
                break;
            case "alert_assign":
                targetStatus = "ASSIGNED";
                assigneeId = user.getId();
                actionLabel = "已接手处理";
                break;
            default:
                log.warn("Unknown card action: {}", action);
                return;
        }

        int success = 0;
        int failed = 0;
        for (Long eventId : eventIds) {
            try {
                apiClient.transitionAlertEvent(eventId, targetStatus, assigneeId,
                        "via Feishu by " + user.getName(), user);
                success++;
            } catch (Exception e) {
                log.warn("Failed to transition event {}: {}", eventId, e.getMessage());
                failed++;
            }
        }

        // Send confirmation reply
        String resultText;
        if (failed == 0) {
            resultText = String.format("✅ %s：共 %d 条异常事件", actionLabel, success);
        } else {
            resultText = String.format("⚠️ %s：成功 %d 条，失败 %d 条", actionLabel, success, failed);
        }

        Map<String, Object> card = new HashMap<>();
        card.put("header", FeishuCardTemplate.buildHeader(actionLabel, "green"));
        card.put("elements", List.of(FeishuCardTemplate.buildMarkdown(resultText)));

        try {
            messageSender.sendCard(chatId, card);
        } catch (Exception e) {
            log.error("Failed to send confirmation card to {}: {}", chatId, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/CardActionHandler.java
git commit -m "feat: add CardActionHandler for Feishu alert actions"
```

---

### Task 6: Handle card.action.trigger in FeishuEventController + FeishuBotService

**Files:**
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/rest/FeishuEventController.java`
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBotService.java`

- [ ] **Step 1: Extend FeishuEventController to accept card action events**

In `FeishuEventController.handleEvent()`, find the event type check that currently only accepts `im.message.receive_v1` and `message`. Extend it to also accept `card.action.trigger`:

Find:
```java
if (!"im.message.receive_v1".equals(eventType) && !"message".equals(eventType)) {
    log.debug("Ignoring event type: {}", eventType);
    writeJson(response, "{\"code\":0}");
    return;
}
```

Replace with:
```java
if (!"im.message.receive_v1".equals(eventType) && !"message".equals(eventType)
        && !"card.action.trigger".equals(eventType)) {
    log.debug("Ignoring event type: {}", eventType);
    writeJson(response, "{\"code\":0}");
    return;
}
```

The existing `botService.handleEventAsync(eventType, event)` call already passes the event type — no other changes needed in the controller.

- [ ] **Step 2: Extend FeishuBotService to dispatch card action events**

In `FeishuBotService.handleEventAsync()`, find the event type check:

```java
if (!"im.message.receive_v1".equals(eventType) && !"message".equals(eventType)) {
    log.debug("Ignoring event type: {}", eventType);
    return;
}
```

Replace with:
```java
if ("card.action.trigger".equals(eventType)) {
    feishuExecutor.execute(() -> {
        try {
            handleCardAction(event);
        } catch (Exception e) {
            log.error("Failed to handle card action", e);
        }
    });
    return;
}
if (!"im.message.receive_v1".equals(eventType) && !"message".equals(eventType)) {
    log.debug("Ignoring event type: {}", eventType);
    return;
}
```

Then add the `handleCardAction` method and the `CardActionHandler` dependency:

Add field:
```java
private final CardActionHandler cardActionHandler;
```

Add method:
```java
@SuppressWarnings("unchecked")
private void handleCardAction(Map<String, Object> event) {
    // Extract card action payload
    // Feishu card action event structure:
    // { "event": { "operator": { "open_id": "..." }, "action": { "value": {...}, "tag": "button" } } }
    Map<String, Object> eventBody = (Map<String, Object>) event.get("event");
    if (eventBody == null) {
        log.warn("Card action event missing 'event' body");
        return;
    }

    Map<String, Object> operator = (Map<String, Object>) eventBody.get("operator");
    String openId = operator != null ? String.valueOf(operator.get("open_id")) : null;
    if (openId == null) {
        log.warn("Card action event missing operator open_id");
        return;
    }

    Map<String, Object> action = (Map<String, Object>) eventBody.get("action");
    if (action == null) {
        log.warn("Card action event missing action");
        return;
    }

    Map<String, Object> actionValue = (Map<String, Object>) action.get("value");
    if (actionValue == null) {
        log.warn("Card action missing value payload");
        return;
    }

    // Resolve user
    FeishuUserMappingService.ResolvedMapping mapping = userMappingService.resolveMapping(openId);
    if (mapping == null || mapping.user() == null) {
        log.warn("Cannot resolve Feishu user for open_id: {}", openId);
        return;
    }

    // Extract chat ID for reply
    // Feishu card action events include open_chat_id in the context
    Map<String, Object> context = (Map<String, Object>) eventBody.get("context");
    String chatId = context != null ? String.valueOf(context.get("open_chat_id")) : null;

    cardActionHandler.handle(actionValue, mapping.user(), chatId);
}
```

Add import:
```java
import com.tencent.supersonic.feishu.server.handler.CardActionHandler;
```

- [ ] **Step 3: Compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/rest/FeishuEventController.java
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBotService.java
git commit -m "feat: handle card.action.trigger events for alert confirm/assign"
```

---

### Task 7: Integration verification

- [ ] **Step 1: Compile full backend**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run existing alert tests to verify no regressions**

Run: `mvn test -pl headless/server -Dtest="AlertResolutionStatusTest" -q`
Expected: 5 tests PASS

- [ ] **Step 3: Verify the complete card action flow end-to-end**

Trace the flow manually through the code:
1. `AlertCheckDispatcher.persistAndDeliver()` — inserts events, collects IDs, passes to delivery
2. `FeishuDeliveryChannel.buildAlertCard()` — renders buttons with eventIds in value payload
3. User clicks button in Feishu → webhook to `FeishuEventController.handleEvent()`
4. Event type `card.action.trigger` → `FeishuBotService.handleCardAction()`
5. Resolve user via `userMappingService.resolveMapping(openId)`
6. `CardActionHandler.handle()` → calls `apiClient.transitionAlertEvent()` per event
7. `HttpSuperSonicApiClient.transitionAlertEvent()` → `POST /api/v1/alertRules/events/{id}:transition`
8. `AlertRuleController` → `AlertRuleServiceImpl.transitionEvent()` (already implemented)
9. Confirmation card sent back via `messageSender.sendCard()`

- [ ] **Step 4: Commit (if any fixes needed)**
