# Headless DTO Boundary Migration — Continuation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the headless DTO boundary migration started in `docs/superpowers/plans/2026-04-14-headless-dto-boundary-migration.md`. Commit the in-progress `headless/server` DTO wiring, migrate the two remaining consumers (`chat/server`, `feishu/server`) to DTOs, then drop the `headless-server` Maven dependency from both consumer modules so the module boundary is structurally enforced.

**Architecture:** The previous plan rewrote the three `Report*Service` interfaces in `headless-api` to speak `Req`/`Resp` DTOs and added `ReportDtoMappers` under `headless-server`. The `Impl` classes and controllers still need their commit, and downstream callers (`ReportScheduleQuery` in chat, and the three handlers + renderer in feishu) still import `Report*DO` classes — which is why `mvn compile -pl launchers/standalone -am` currently breaks at `feishu-server`. After the consumers are switched to the DTO types (field names already match), the two `pom.xml` files can drop their `headless-server` dependency and the build is done.

**Tech Stack:** Java 21, Spring Boot 3.4, MyBatis-Plus `Page<T>`, Mockito, Maven multi-module.

---

## Current state (verified 2026-04-15)

- `mvn compile -pl headless/server` → **BUILD SUCCESS**
- `mvn test-compile -pl headless/server` → **BUILD SUCCESS**
- `mvn compile -pl launchers/standalone -am` → **BUILD FAILURE at feishu-server** (expected: DO references)

Uncommitted WIP on `refactor/headless-dto-boundary`:

| File | Change |
|------|--------|
| `headless/server/.../service/impl/ReportScheduleServiceImpl.java` | B7 — boundary methods map DO↔DTO; `updateSchedule` copies req onto loaded DO preserving server-owned fields; `getExecutionList` splits into DTO surface + private DO helper. |
| `headless/server/.../service/impl/ReportDeliveryServiceImpl.java` | B8 — boundary methods map DO↔DTO; `updateConfig` copies req onto loaded DO preserving immutables; new private `getConfigByIdInternal` / `getConfigsByIdsInternal` helpers for callers that still need DOs. |
| `headless/server/.../service/impl/ReportScheduleConfirmationServiceImpl.java` | B9 — `createPending` / `getLatestPending` map DO↔DTO; new private `getLatestPendingInternal` for `hasPending`. |
| `headless/server/.../service/mapper/ReportDtoMappers.java` | Null-safety: `toRespPage` / `toConfigRespPage` / `toRecordRespPage` / `toExecutionRespPage` return empty page on null input. |
| `headless/server/.../rest/ReportScheduleController.java` | Request/response types switched to DTOs; dropped the manual "strip immutable fields" block in `updateSchedule` — that invariant is now inside the impl. |
| `headless/server/.../rest/ReportDeliveryController.java` | Request/response types switched to DTOs; dropped `setCreatedBy/setTenantId/setUpdatedBy` that leaked DO setters through the controller (impl now owns tenant/audit fields). |
| `headless/server/.../service/impl/DataSetServiceImpl.java` | Internal caller: `Page<ReportScheduleDO>` → `Page<ReportScheduleResp>` (the delete-precheck). |
| `headless/server/.../service/impl/SemanticTemplateServiceImpl.java` | Same pattern as DataSet. |
| `headless/server/.../executor/BuiltinSemanticTemplateInitializer.java` | Expanded wildcard import into explicit imports for `SemanticDeployResult` and `SemanticTemplateConfig` (unrelated housekeeping — this file also uses the `SemanticTemplateConfig.*` nested types). |
| `headless/server/src/test/.../service/ReportScheduleServiceImplTest.java` | Tests updated to call `createSchedule(ReportScheduleReq)` / `updateSchedule(ReportScheduleReq)` and assert on `ReportScheduleResp`. |
| `headless/server/src/test/.../service/ReportDeliveryServiceImplTest.java` | Minor sync to Resp types. |
| `headless/server/src/test/.../rest/ReportDeliveryControllerTest.java` | Payload type updated. |

## Out of scope

- Re-tuning the `ReportDtoMappers` shape (already covered in B6 of the prior plan).
- Any business-logic refactor inside the Impl classes — this plan only finishes the type boundary.
- `ValidDataSetResp` drift (out of scope in the prior plan as well).
- MapStruct, Spring mapper beans, generic mapper interface — YAGNI.

---

## Phase D — Stabilize and commit current `headless/server` WIP

Goal: split the uncommitted WIP into three logical commits (schedule, delivery, confirmation) so `git bisect` can isolate a regression later. `headless/server` already builds and test-compiles clean, so each commit just needs its files staged and the build re-run as a smoke check.

### Task D1: Commit the ReportScheduleService boundary rewrite

**Files to stage:**
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java`
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportScheduleController.java`
- `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportScheduleServiceImplTest.java`
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DataSetServiceImpl.java`
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java`
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/executor/BuiltinSemanticTemplateInitializer.java`

- [ ] **Step 1: Re-read the WIP on each file to confirm scope**

```bash
git diff --stat HEAD -- \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportScheduleController.java \
  headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportScheduleServiceImplTest.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DataSetServiceImpl.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/executor/BuiltinSemanticTemplateInitializer.java
```

Expected: six modified files, no surprises (no extra files dragged in).

- [ ] **Step 2: Verify no stray `ReportScheduleDO` / `ReportExecutionDO` references remain inside `ReportScheduleServiceImpl`'s public overrides**

```
Grep -n 'public.*ReportScheduleDO\|public.*ReportExecutionDO' headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java
```

Expected: no output. (DOs are legal in private helpers; only the `@Override` boundary methods matter.)

- [ ] **Step 3: Confirm the controller no longer mentions `ReportScheduleDO` / `ReportExecutionDO`**

```
Grep -n 'ReportScheduleDO\|ReportExecutionDO' headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportScheduleController.java
```

Expected: no output.

- [ ] **Step 4: Run the touched unit test**

```bash
mvn test -pl headless/server -Dtest=ReportScheduleServiceImplTest
```

Expected: BUILD SUCCESS, all assertions green.

- [ ] **Step 5: Stage and commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportScheduleController.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportScheduleServiceImplTest.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DataSetServiceImpl.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/executor/BuiltinSemanticTemplateInitializer.java

git commit -m "$(cat <<'EOF'
refactor(headless-server): ReportScheduleService speaks DTOs end-to-end

ReportScheduleServiceImpl maps DO<->DTO at the public override boundary;
internal Quartz / mapper code still sees DOs. updateSchedule now loads
the DO and copies caller-provided fields onto it (preserving ownerId,
tenantId, createdBy, createdAt, quartzJobKey, lastExecutionTime,
nextExecutionTime), so the controller no longer needs to null out
immutable fields.

ReportScheduleController request/response types swap to
ReportScheduleReq / ReportScheduleResp / ReportExecutionResp.

DataSetServiceImpl and SemanticTemplateServiceImpl — the two in-module
callers that do a delete-precheck via getScheduleList — pick up the new
Page<ReportScheduleResp> type.

BuiltinSemanticTemplateInitializer: expand the wildcard import that
javac started flagging after the api-module shuffle.
EOF
)"
```

- [ ] **Step 6: Smoke-test the commit**

```bash
mvn compile -pl headless/server -am 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

---

### Task D2: Commit the ReportDeliveryService boundary rewrite

**Files to stage:**
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportDeliveryServiceImpl.java`
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportDeliveryController.java`
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/mapper/ReportDtoMappers.java`
- `headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportDeliveryServiceImplTest.java`
- `headless/server/src/test/java/com/tencent/supersonic/headless/server/rest/ReportDeliveryControllerTest.java`

- [ ] **Step 1: Verify the delivery impl no longer exposes DOs at the boundary**

```
Grep -n '@Override' -A1 headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportDeliveryServiceImpl.java
```

Expected: every `@Override`'d method signature uses `ReportDeliveryConfigReq`, `ReportDeliveryConfigResp`, `ReportDeliveryRecordResp` (or primitive types). DOs only appear in private helpers like `getConfigByIdInternal` / `getConfigsByIdsInternal`.

- [ ] **Step 2: Confirm the controller imports match**

```
Grep -n 'ReportDeliveryConfigDO\|ReportDeliveryRecordDO' headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportDeliveryController.java
```

Expected: no output.

- [ ] **Step 3: Run the touched delivery tests**

```bash
mvn test -pl headless/server -Dtest='ReportDeliveryServiceImplTest,ReportDeliveryControllerTest,FeishuDeliveryChannelTest'
```

Expected: BUILD SUCCESS, green. (`FeishuDeliveryChannelTest` lives in `headless/server` and exercises `ReportDeliveryService` — it should be unaffected because it already worked after the B6 commit, but re-running catches regressions from this batch.)

- [ ] **Step 4: Stage and commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportDeliveryServiceImpl.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/ReportDeliveryController.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/service/mapper/ReportDtoMappers.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportDeliveryServiceImplTest.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/rest/ReportDeliveryControllerTest.java

git commit -m "$(cat <<'EOF'
refactor(headless-server): ReportDeliveryService speaks DTOs end-to-end

ReportDeliveryServiceImpl maps DO<->DTO at the public override
boundary. updateConfig loads the existing DO and copies caller-provided
fields onto it (preserving id, tenantId, createdAt, createdBy,
updatedBy, consecutiveFailures, disabledReason). Private helpers
getConfigByIdInternal / getConfigsByIdsInternal preserve DO access for
internal flows (delivery execution, retry).

ReportDeliveryController request/response types swap to
ReportDeliveryConfigReq / ReportDeliveryConfigResp /
ReportDeliveryRecordResp. The controller no longer leaks DO setters
(createdBy / tenantId / updatedBy) — tenancy and audit fields belong in
the impl.

ReportDtoMappers: null-safe Page mappers return empty pages instead of
NPE'ing when the underlying DO page is null (matches MyBatis-Plus
edge case on empty result sets).
EOF
)"
```

---

### Task D3: Commit the ReportScheduleConfirmationService boundary rewrite

**Files to stage:**
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleConfirmationServiceImpl.java`

- [ ] **Step 1: Confirm the scope is just this one file**

```bash
git status --short headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleConfirmationServiceImpl.java
```

Expected: ` M ...ReportScheduleConfirmationServiceImpl.java`.

- [ ] **Step 2: Compile smoke-test**

```bash
mvn compile -pl headless/server -am 2>&1 | tail -5
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleConfirmationServiceImpl.java

git commit -m "$(cat <<'EOF'
refactor(headless-server): ReportScheduleConfirmationService speaks DTOs

createPending and getLatestPending map DO<->DTO at the public override
boundary. hasPending now delegates to a private getLatestPendingInternal
helper so it doesn't spin up an unused DTO conversion per call.
EOF
)"
```

- [ ] **Step 4: Verify WIP tree is empty for `headless/server` main sources**

```bash
git status --short headless/server/src/main | cat
```

Expected: no output (all WIP landed).

---

## Phase E — Migrate downstream consumers to DTOs

Goal: `chat/server` and `feishu/server` no longer import `Report*DO` classes. Field names on Req/Resp DTOs match the DOs verbatim (per the previous plan's self-review), so each change is a type-only rewrite.

### Task E1: Rewrite `chat/server` ReportScheduleQuery + test to use DTOs

**Files:**
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java`
- Modify: `chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java`

**DO references to rewrite (verified by grep):**

`ReportScheduleQuery.java`:
| Line | Current | Target |
|------|---------|--------|
| 24 | `import ...dataobject.ReportDeliveryConfigDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;` |
| 25 | `import ...dataobject.ReportExecutionDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;` |
| 26 | `import ...dataobject.ReportScheduleConfirmationDO;` | `import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;` **and** `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;` |
| 27 | `import ...dataobject.ReportScheduleDO;` | `import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;` **and** `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;` |
| 191 | `ReportScheduleConfirmationDO confirmation = new ReportScheduleConfirmationDO();` | `ReportScheduleConfirmationReq confirmation = new ReportScheduleConfirmationReq();` |
| 257 | `ReportScheduleConfirmationDO pending = confirmationService.getLatestPending(...)` | `ReportScheduleConfirmationResp pending = confirmationService.getLatestPending(...)` |
| 288 | `ReportScheduleDO schedule = new ReportScheduleDO();` | `ReportScheduleReq schedule = new ReportScheduleReq();` |
| 298 | `ReportScheduleDO created = scheduleService.createSchedule(schedule, currentUser);` | `ReportScheduleResp created = scheduleService.createSchedule(schedule, currentUser);` |
| 420 | `Page<ReportScheduleDO> page = new Page<>(1, 20);` | `Page<ReportScheduleResp> page = new Page<>(1, 20);` |
| 421 | `Page<ReportScheduleDO> result = scheduleService.getScheduleList(page, null, null, ...)` | `Page<ReportScheduleResp> result = scheduleService.getScheduleList(page, null, null, ...)` |
| 425 | `for (ReportScheduleDO schedule : result.getRecords())` | `for (ReportScheduleResp schedule : result.getRecords())` |
| 458, 486, 512, 537 | `ReportScheduleDO schedule = scheduleService.getScheduleById(...)` | `ReportScheduleResp schedule = scheduleService.getScheduleById(...)` |
| 563 | `Page<ReportExecutionDO> page = new Page<>(1, 10);` | `Page<ReportExecutionResp> page = new Page<>(1, 10);` |
| 564 | `Page<ReportExecutionDO> result = scheduleService.getExecutionList(page, scheduleId, null, ...)` | `Page<ReportExecutionResp> result = scheduleService.getExecutionList(page, scheduleId, null, ...)` |
| 568 | `for (ReportExecutionDO exec : result.getRecords())` | `for (ReportExecutionResp exec : result.getRecords())` |
| 832 | `List<ReportDeliveryConfigDO> configs = deliveryService.getConfigList(new Page<>(1, 100)).getRecords().stream()...` | `List<ReportDeliveryConfigResp> configs = deliveryService.getConfigList(new Page<>(1, 100)).getRecords().stream()...` |
| 874 | `ReportDeliveryConfigDO config = deliveryService.getConfigById(configId);` | `ReportDeliveryConfigResp config = deliveryService.getConfigById(configId);` |

The `.getRecords()` / `.stream()` chain at line 832 works unchanged — `getConfigList` now returns `Page<ReportDeliveryConfigResp>`, so `.getRecords()` produces `List<ReportDeliveryConfigResp>`. Only the local variable type needs to change.

`ReportScheduleQueryTest.java`:
| Line | Current | Target |
|------|---------|--------|
| 20 | `import ...dataobject.ReportDeliveryConfigDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;` |
| 21 | `import ...dataobject.ReportScheduleConfirmationDO;` | `import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;` and `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;` |
| 129 | `Page<ReportDeliveryConfigDO> emptyPage = new Page<>(1, 100);` | `Page<ReportDeliveryConfigResp> emptyPage = new Page<>(1, 100);` |
| 149 | `ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();` | `ReportDeliveryConfigResp config = new ReportDeliveryConfigResp();` |
| 154 | `Page<ReportDeliveryConfigDO> configPage = new Page<>(1, 100);` | `Page<ReportDeliveryConfigResp> configPage = new Page<>(1, 100);` |
| 164 | `ReportScheduleConfirmationDO[] stored = {null};` | `ReportScheduleConfirmationResp[] stored = {null};` |
| 166 | `ReportScheduleConfirmationDO c = inv.getArgument(0);` — this is a Mockito `thenAnswer` capturing the `createPending(...)` argument. The service signature is now `createPending(ReportScheduleConfirmationReq)`, so the captured argument is `Req`, but the cached value (what `stored[0]` returns from `getLatestPending`) must be `Resp`. Rewrite as: `ReportScheduleConfirmationReq captured = inv.getArgument(0); ReportScheduleConfirmationResp c = new ReportScheduleConfirmationResp(); org.springframework.beans.BeanUtils.copyProperties(captured, c); stored[0] = c; return c;` |
| 208 | `ReportDeliveryConfigDO config = new ReportDeliveryConfigDO();` | `ReportDeliveryConfigResp config = new ReportDeliveryConfigResp();` |
| 213 | `Page<ReportDeliveryConfigDO> configPage = new Page<>(1, 100);` | `Page<ReportDeliveryConfigResp> configPage = new Page<>(1, 100);` |
| 223 | `ReportScheduleConfirmationDO[] stored = {null};` | `ReportScheduleConfirmationResp[] stored = {null};` |
| 225 | Same `inv.getArgument(0)` pattern as line 166 — apply the same rewrite. |

Any `when(...).thenReturn(someDO)` calls that match the service stubs:
- `scheduleService.createSchedule(any(...), any())` — the `any()` matcher no longer matches `ReportScheduleDO.class`; use `any(ReportScheduleReq.class)` or plain `any()`. Return value must be `ReportScheduleResp`.
- `scheduleService.getScheduleById(anyLong(), any())` → returns `ReportScheduleResp`.
- `scheduleService.getScheduleList(any(Page.class), any(), any(), any())` → returns `Page<ReportScheduleResp>`.
- `deliveryService.getConfigList(any(Page.class))` → returns `Page<ReportDeliveryConfigResp>`.
- `deliveryService.getConfigById(anyLong())` → returns `ReportDeliveryConfigResp`.
- `confirmationService.createPending(any(...))` → returns `ReportScheduleConfirmationResp`.
- `confirmationService.getLatestPending(anyLong(), anyInt())` → returns `ReportScheduleConfirmationResp`.

- [ ] **Step 1: Open the production file**

```
Read chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
```

Scan once for every `ReportScheduleDO` / `ReportExecutionDO` / `ReportDeliveryConfigDO` / `ReportScheduleConfirmationDO` token — the table above lists every occurrence, but the file is large, so confirm the grep matches before editing.

- [ ] **Step 2: Rewrite production-file imports and types**

Apply every row from the `ReportScheduleQuery.java` table. Use `Edit` with `replace_all=true` when the exact DO type appears more than once (e.g. `ReportScheduleDO` at lines 458/486/512/537). For each `replace_all`, pre-check uniqueness by reading the local context:

```bash
# Example safety check before bulk-renaming
```
```
Grep -n 'ReportScheduleDO' chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java
```

If the grep shows only variable declarations (not, say, `.class` references or generics that need different handling), a `replace_all` of `ReportScheduleDO` → `ReportScheduleResp` is safe for the local-variable-assignment occurrences. The import line needs a separate edit because it changes package, not just symbol.

- [ ] **Step 3: Rewrite test-file imports, types, and Mockito stubs**

Apply every row from the `ReportScheduleQueryTest.java` table. For the `inv.getArgument(0)` rewrite, use a single `BeanUtils.copyProperties` bridge since field names match. Add `import org.springframework.beans.BeanUtils;` at the top of the test file if not present.

- [ ] **Step 4: Verify chat has no stale DO imports**

```
Grep -n 'com\.tencent\.supersonic\.headless\.server\.persistence\.dataobject\.Report' chat/server
```

Expected: no output.

- [ ] **Step 5: Compile `chat/server`**

```bash
mvn compile -pl chat/server -am 2>&1 | tail -60
```

Expected: BUILD SUCCESS. (If the `-am` pulls in `headless/server` as a reactor dep via the current pom, that's fine — the pom drop happens in Phase F.)

- [ ] **Step 6: Run the touched test**

```bash
mvn test -pl chat/server -Dtest=ReportScheduleQueryTest
```

Expected: all green.

- [ ] **Step 7: Commit**

```bash
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java \
        chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java

git commit -m "$(cat <<'EOF'
refactor(chat): consume Report* services via DTOs

ReportScheduleQuery and its test no longer reference Report*DO classes.
Schedule creation, listing, single-fetch, execution listing, confirmation
pending state, and delivery-config lookup all go through the Req/Resp
DTOs exposed by headless-api. Field accessors are unchanged because DTO
field names match the DOs.

Mockito stubs now match on Req argument types; confirmation test's
thenAnswer bridges captured Req -> Resp via BeanUtils.copyProperties so
the assertion path continues to observe the same persisted state.
EOF
)"
```

---

### Task E2: Rewrite `feishu/server` handlers and renderer to use DTOs

**Files:**
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/CardActionHandler.java`
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/ScheduleMessageHandler.java`
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/render/FeishuCardRenderer.java`
- Modify: `feishu/server/src/test/java/com/tencent/supersonic/feishu/server/handler/CardActionHandlerTest.java`

**DO references to rewrite (verified by grep + current compile errors):**

`CardActionHandler.java`:
| Line | Current | Target |
|------|---------|--------|
| 10 | `import ...dataobject.ReportScheduleDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;` |
| 137 | `ReportScheduleDO schedule = reportScheduleService.getScheduleById(scheduleId, user);` | `ReportScheduleResp schedule = reportScheduleService.getScheduleById(scheduleId, user);` |

`ScheduleMessageHandler.java`:
| Line | Current | Target |
|------|---------|--------|
| 13 | `import ...dataobject.ReportDeliveryConfigDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;` |
| 14 | `import ...dataobject.ReportScheduleDO;` | `import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;` and `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;` |
| 81–82 | `Page<ReportScheduleDO> page = ...reportScheduleService.getScheduleList(...)` | `Page<ReportScheduleResp> page = ...reportScheduleService.getScheduleList(...)` |
| 122 | `ReportScheduleDO schedule = new ReportScheduleDO();` | `ReportScheduleReq schedule = new ReportScheduleReq();` |
| 134 | `ReportScheduleDO created = reportScheduleService.createSchedule(schedule, user);` | `ReportScheduleResp created = reportScheduleService.createSchedule(schedule, user);` |
| 245 | `List<ReportDeliveryConfigDO> configs = ...` | `List<ReportDeliveryConfigResp> configs = ...` |

Also check whether the `created` result at line 134 is then passed to `FeishuCardRenderer.renderScheduleCreatedCard(...)` — it is. That renderer method signature also changes in the next table, so these edits must happen together (one commit).

`FeishuCardRenderer.java`:
| Line | Current | Target |
|------|---------|--------|
| 11 | `import ...dataobject.ReportScheduleDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;` |
| 374 | `public Map<String, Object> renderScheduleCreatedCard(ReportScheduleDO schedule, ...)` | `public Map<String, Object> renderScheduleCreatedCard(ReportScheduleResp schedule, ...)` |
| 391 | `public Map<String, Object> renderScheduleListCard(List<ReportScheduleDO> schedules)` | `public Map<String, Object> renderScheduleListCard(List<ReportScheduleResp> schedules)` |
| 397 | `for (ReportScheduleDO s : schedules)` | `for (ReportScheduleResp s : schedules)` |

`CardActionHandlerTest.java`:
| Line | Current | Target |
|------|---------|--------|
| 9 | `import ...dataobject.ReportScheduleDO;` | `import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;` |
| 66 | `when(reportScheduleService.getScheduleById(8L, owner)).thenReturn(new ReportScheduleDO());` | `when(reportScheduleService.getScheduleById(8L, owner)).thenReturn(new ReportScheduleResp());` |

- [ ] **Step 1: Edit `CardActionHandler.java`**

Two edits: replace the import line, and replace the local type at line 137. Both have a single occurrence so no `replace_all` needed.

- [ ] **Step 2: Edit `ScheduleMessageHandler.java`**

Four edits plus two import swaps. `ReportScheduleDO` appears three times (lines 81, 122, 134) across three distinct syntactic positions (generic, constructor, local-var declaration); the bulk `replace_all` is still safe because target types differ — `Page<ReportScheduleDO>` → `Page<ReportScheduleResp>`, `new ReportScheduleDO()` → `new ReportScheduleReq()`, `ReportScheduleDO created = ...` → `ReportScheduleResp created = ...`. Use three targeted `Edit` calls instead of `replace_all` because two of them map to `Resp` and one to `Req`.

- [ ] **Step 3: Edit `FeishuCardRenderer.java`**

Four edits (one import, two signatures, one loop). Use individual `Edit` calls.

**Data-access compatibility check:** inside `renderScheduleCreatedCard` / `renderScheduleListCard`, the renderer reads fields off the schedule object (`getName()`, `getCronExpression()`, etc.). Since `ReportScheduleResp` has the same Lombok getters as `ReportScheduleDO`, no body changes are needed — only the parameter type.

- [ ] **Step 4: Edit `CardActionHandlerTest.java`**

Two edits (import + stubbed return value).

- [ ] **Step 5: Verify feishu has no stale DO imports**

```
Grep -n 'com\.tencent\.supersonic\.headless\.server\.persistence\.dataobject\.Report' feishu/server
```

Expected: no output.

- [ ] **Step 6: Compile `feishu/server`**

```bash
mvn compile -pl feishu/server -am 2>&1 | tail -60
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Run the feishu tests**

```bash
mvn test -pl feishu/server
```

Expected: all green. If `CardActionHandlerTest` was previously untracked / newly added — `git status` will show it as either `M` (tracked) or `??` (new); if new, include it in the commit.

- [ ] **Step 8: Commit**

```bash
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/CardActionHandler.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/ScheduleMessageHandler.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/render/FeishuCardRenderer.java \
        feishu/server/src/test/java/com/tencent/supersonic/feishu/server/handler/CardActionHandlerTest.java

git commit -m "$(cat <<'EOF'
refactor(feishu): consume Report* services via DTOs

CardActionHandler, ScheduleMessageHandler, and FeishuCardRenderer no
longer reference ReportScheduleDO or ReportDeliveryConfigDO. They use
ReportScheduleReq / ReportScheduleResp / ReportDeliveryConfigResp from
headless-api. Renderer method signatures change accordingly; field
accessors are unchanged because DTOs mirror DO field names.
EOF
)"
```

---

### Task E3: Full project compile + test-compile gate before dropping the pom dependency

Goal: prove the entire repository builds cleanly with all DTO rewrites in place, before touching any Maven metadata. This is the hard verification gate — if this fails, a consumer still has a stale DO reference.

- [ ] **Step 1: Full compile**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl launchers/standalone -am 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Full test-compile**

```bash
mvn test-compile -pl launchers/standalone -am 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run the test classes touched by this migration**

```bash
mvn test -pl headless/server -Dtest='ReportScheduleServiceImplTest,ReportDeliveryServiceImplTest,ReportDeliveryControllerTest,FeishuDeliveryChannelTest,ReportExecutionOrchestratorTest' 2>&1 | tail -30
mvn test -pl chat/server -Dtest=ReportScheduleQueryTest 2>&1 | tail -30
mvn test -pl feishu/server 2>&1 | tail -30
```

Expected: all green. If any red, fix in-task — do not proceed to Phase F.

- [ ] **Step 4: Verify repo-wide that no `headless.server.*.dataobject.Report*DO` reference leaks into `chat/server/src` or `feishu/server/src`**

```
Grep -n 'com\.tencent\.supersonic\.headless\.server\.persistence\.dataobject\.Report' chat/server/src feishu/server/src
```

Expected: no output. A single hit is a blocker.

- [ ] **Step 5: No commit needed** — this task is a gate, not a change. If any touch-up was required to get the build green, commit it under a short `fix(...)` message before moving on.

---

## Phase F — Drop `headless-server` from consumer poms

Goal: structurally enforce the boundary. After this phase, `chat/server` and `feishu/server` only depend on `headless-api`. A future compile will fail immediately if anyone re-imports a `headless.server.*` class.

### Task F1: Drop `headless-server` dependency from `feishu/server/pom.xml`

**File:** `/Users/xudong/git/supersonic/feishu/server/pom.xml` (the dependency comment + block currently lives around lines 49–54: `<!-- headless-server: ReportScheduleService, ... --> <dependency>...<artifactId>headless-server</artifactId>...</dependency>`)

- [ ] **Step 1: Read the pom around the dependency block**

```
Read /Users/xudong/git/supersonic/feishu/server/pom.xml
```

Locate the comment + `<dependency>` pair.

- [ ] **Step 2: Delete the comment line and the entire `<dependency>...</dependency>` block**

Use `Edit` with the exact multi-line comment+dependency text as `old_string` and an empty-string (or a single blank line if indentation matters) as `new_string`. The block is uniquely identified by its `<artifactId>headless-server</artifactId>` so the match is unambiguous.

- [ ] **Step 3: Compile `feishu/server`**

```bash
mvn compile -pl feishu/server -am 2>&1 | tail -60
```

Expected outcomes:
- **BUILD SUCCESS:** feishu/server is cleanly decoupled — proceed.
- **BUILD FAILURE with `package com.tencent.supersonic.headless.server... does not exist`:** A hidden import survived Phase E. Rerun the grep from Task E3 step 4 — it should show the surviving reference now that the reactor no longer papers over it with a transitive compile-classpath entry. Fix the specific class and recompile. Iterate until green.

Do **not** rescue the build by re-adding the dependency.

- [ ] **Step 4: Run feishu tests**

```bash
mvn test -pl feishu/server
```

Expected: all green.

- [ ] **Step 5: Commit**

```bash
git add feishu/server/pom.xml
git commit -m "$(cat <<'EOF'
refactor(feishu): drop headless-server Maven dependency

feishu/server now depends only on headless-api. All report service
access goes through the DTO-typed interfaces in headless-api; no more
persistence-layer leakage across the module boundary.
EOF
)"
```

---

### Task F2: Drop `headless-server` dependency from `chat/server/pom.xml`

**File:** `/Users/xudong/git/supersonic/chat/server/pom.xml` (dependency block around line 28–32: `<dependency>...<artifactId>headless-server</artifactId>...</dependency>`)

- [ ] **Step 1: Read the pom around the dependency block**

```
Read /Users/xudong/git/supersonic/chat/server/pom.xml
```

- [ ] **Step 2: Delete the `<dependency>` block**

Same pattern as F1 — `Edit` with the exact block as `old_string`. Include any leading comment if one exists.

- [ ] **Step 3: Compile `chat/server`**

```bash
mvn compile -pl chat/server -am 2>&1 | tail -60
```

Same triage as F1 step 3. Iterate until BUILD SUCCESS.

- [ ] **Step 4: Run chat tests**

```bash
mvn test -pl chat/server -Dtest=ReportScheduleQueryTest
```

Expected: green. (Full `mvn test -pl chat/server` is run in F3.)

- [ ] **Step 5: Commit**

```bash
git add chat/server/pom.xml
git commit -m "$(cat <<'EOF'
refactor(chat): drop headless-server Maven dependency

chat/server now depends only on headless-api. Completes the DTO
boundary enforcement — both downstream modules are now structurally
prevented from importing headless-server internals.
EOF
)"
```

---

### Task F3: Final gate — full build, test, and decoupling verification

- [ ] **Step 1: Clean build from scratch**

```bash
cd /Users/xudong/git/supersonic
mvn clean compile -pl launchers/standalone -am 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`. A clean build (not incremental) rules out stale `target/` artifacts masking a missing dependency.

- [ ] **Step 2: Full test-compile**

```bash
mvn test-compile -pl launchers/standalone -am 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run all module test suites touched by this migration**

```bash
mvn test -pl headless/server 2>&1 | tail -20
mvn test -pl chat/server 2>&1 | tail -20
mvn test -pl feishu/server 2>&1 | tail -20
```

Expected: all green. Skip `launchers/standalone` integration tests — those need a live DB; the compile gate in step 2 already exercises wiring.

- [ ] **Step 4: Verify decoupling actually holds in source**

```
Grep -n 'com\.tencent\.supersonic\.headless\.server\.' chat/server/src feishu/server/src
```

Expected: no output. A single hit means a `headless.server.*` class leaked back in — fix and re-run.

- [ ] **Step 5: Verify poms are clean**

```
Grep -n 'headless-server' chat/server/pom.xml feishu/server/pom.xml
```

Expected: no output.

- [ ] **Step 6: Confirm working tree is clean**

```bash
git status --short
```

Expected: empty (or only `.context/` untracked, which existed before this plan).

- [ ] **Step 7: If any final touch-up was needed, commit it**

```bash
git commit -m "refactor(headless): complete DTO boundary migration"
```

If no touch-up: skip. This task is a gate, not a change.

---

## Notes for the implementer

- **Do not merge commits across phases.** Each task has its own commit so `git bisect` can localize a regression.
- **`headless/server` currently builds clean** (verified 2026-04-15). Phase D commits are mechanical — the code already works; they just land the WIP. If Phase D fails at the smoke step, inspect the actual git state and fix in-task rather than blindly retrying.
- **Field names on DTOs match DOs.** Every rewrite in Phase E is type-only; if you find yourself renaming a getter, stop and re-read the DTO definition — it should have the same accessor.
- **`BeanUtils.copyProperties` is already imported in `ReportScheduleServiceImpl` and `ReportDeliveryServiceImpl`** as part of the WIP. If `ReportScheduleQueryTest` needs it for the confirmation bridge, add the import.
- **Mockito `any(ReportScheduleDO.class)` style stubs must switch to the matching DTO type** or plain `any()`. Leaving the old argument-class matcher in place will compile but silently fail to match at runtime.
- **The build gate is `mvn compile -pl launchers/standalone -am`** — CLAUDE.md requires this after every Java change.
- **Phase F compile failures are the informative ones.** When the dependency drops, the compiler refuses transitive `headless.server.*` access — that's the whole point. If you see `package ... does not exist`, treat it as the boundary enforcement doing its job, not as a reason to restore the dependency.
- **`ReportExecutionVO` vs `ReportExecutionResp` coexistence is intentional.** The plain Resp mirrors the DO for list/get endpoints; the enriched VO exists for UI flows. Do not collapse them.
- **YAGNI:** No MapStruct, no mapper beans, no interface extraction. The static `ReportDtoMappers` is the whole story.

## Self-review checklist

- [x] Every uncommitted file in `git status` is accounted for in Phase D (schedule impl+ctrl+tests+callers, delivery impl+ctrl+tests+mapper, confirmation impl, plus the incidental `BuiltinSemanticTemplateInitializer` housekeeping which is bundled with D1).
- [x] Phase E enumerates every line-level `Report*DO` reference confirmed by grep on the current working tree — no "similar to task N" shortcuts.
- [x] Phase F has explicit triage paths for the expected failure modes (leaked import + pom drop).
- [x] Types introduced in one task (e.g. `ReportScheduleResp` in D1) are consistent with types used in later tasks (E1/E2).
- [x] Every commit can stand alone: headless/server D-commits don't depend on E; E-commits don't depend on F; each leaves the reactor either building (D, E1 after E2, F1 after F2) or explicitly noted as "expected fail until F completes".
- [x] No TBD / placeholder / "add appropriate error handling" / stray TODO.

---

**Estimated effort:** Phase D ~30 min (mostly `git add` + smoke tests), Phase E ~1–2 h (mechanical type swaps but two consumer files + tests each), Phase F ~30–60 min (most time spent iterating if a hidden `headless.server.*` import survived). Total ~2–4 h of focused work.
