# Headless → API Module DTO Boundary Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the stalled `headless/server` → `headless/api` module decoupling by replacing MyBatis-Plus DOs (currently leaked across the module boundary) with plain Req/Resp DTOs, then drop `headless-server` from feishu/chat `pom.xml` so those modules really depend on `headless-api` only.

**Architecture:** Keep all `@TableName` DOs inside `headless-server` (they are persistence internals). Expose Req/Resp DTOs from `headless-api` — these speak the public contract to feishu/chat consumers. Service interfaces in `headless-api` take/return DTOs. Service impls in `headless-server` do DO↔DTO mapping at the boundary using Spring `BeanUtils.copyProperties` + tiny static `fromDO`/`toDO` helpers (no mapper framework — YAGNI).

**Tech Stack:** Java 21, Spring Boot 3.4.x, MyBatis-Plus, Lombok, Maven multi-module. Tests via JUnit 5 + Mockito (existing).

---

## Current Broken State

Review of the working tree found:

- 5 DOs were wrongly moved to `headless-api` and still leak MyBatis annotations across the boundary.
- 3 service interfaces (`ReportScheduleService`, `ReportDeliveryService`, `ReportScheduleConfirmationService`) were moved to `headless-api` but still import `headless.server.*` — they don't compile.
- `TemplateDeployedEvent` in api imports `headless.server.pojo.*` for classes that *were* moved to api — stale imports.
- 3 yaml POJOs were moved to api (`DataModelYamlTpl`, `DimensionYamlTpl`, `MetricYamlTpl`) but transitively referenced yaml siblings (`IdentifyYamlTpl`, `MeasureYamlTpl`, `MetricTypeParamsYamlTpl`, `FieldParamYamlTpl`, `MetricParamYamlTpl`) were left in server — compile fails.
- ~85 dependent `.java` files (feishu, chat, headless/server, launchers) still import the old `headless.server.*` paths for classes that *were* moved.
- `feishu/server/pom.xml` and `chat/server/pom.xml` still declare `headless-server` as a dependency — the whole point of the refactor is to drop this.
- Service interface package layout in `headless-api` is inconsistent: `ChatLayerService`/`SemanticLayerService` live under `api.facade.service.*` while `DataSetService`/`SchemaService`/`Report*Service` live under `api.service.*`. Decision: **keep both packages** — `facade.service` for the external-API-facing "facade" services (existing convention), `service` for internal cross-module services (DataSet, Schema, Report*). No moves needed here.

Baseline compile (`mvn compile -pl launchers/standalone -am`) is RED.

## Out of Scope

- **`ValidDataSetResp` field additions** (`detailLimit`, `aggregateLimit`) — unrelated feature work piggybacking on the refactor. Leave as-is; split into its own commit after this plan lands. Not addressed here.
- **Cleaning up `headless-api`'s fat transitive deps** (mybatis-plus, spring-web, etc. leaked via `common`). Pre-existing and out of scope.
- **Frontend changes** in the working tree — unrelated.

---

## Target File Structure

### New/moved files in `headless/api`

```
headless/api/src/main/java/com/tencent/supersonic/headless/api/
├── pojo/
│   ├── yaml/
│   │   ├── IdentifyYamlTpl.java             (moved from server, new)
│   │   ├── MeasureYamlTpl.java              (moved from server, new)
│   │   ├── MetricTypeParamsYamlTpl.java     (moved from server, new)
│   │   ├── FieldParamYamlTpl.java           (moved from server, new)
│   │   ├── MetricParamYamlTpl.java          (moved from server, new)
│   │   ├── DataModelYamlTpl.java            (exists, imports fixed)
│   │   ├── DimensionYamlTpl.java            (exists, imports fixed)
│   │   └── MetricYamlTpl.java               (exists, imports fixed)
│   ├── request/
│   │   ├── ReportScheduleReq.java           (new DTO)
│   │   ├── ReportDeliveryConfigReq.java     (new DTO)
│   │   └── ReportScheduleConfirmationReq.java (new DTO)
│   └── response/
│       ├── ReportScheduleResp.java          (new DTO)
│       ├── ReportDeliveryConfigResp.java    (new DTO)
│       ├── ReportDeliveryRecordResp.java    (new DTO)
│       ├── ReportExecutionResp.java         (new DTO)
│       └── ReportScheduleConfirmationResp.java (new DTO)
├── event/TemplateDeployedEvent.java         (exists, imports fixed)
└── service/
    ├── ReportScheduleService.java           (exists, rewritten with DTOs)
    ├── ReportDeliveryService.java           (exists, rewritten with DTOs)
    └── ReportScheduleConfirmationService.java (exists, rewritten with DTOs)
```

### Files reverted back to `headless/server`

```
headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/
├── ReportScheduleDO.java              (moved back from api)
├── ReportDeliveryConfigDO.java        (moved back from api)
├── ReportDeliveryRecordDO.java        (moved back from api)
├── ReportExecutionDO.java             (moved back from api)
└── ReportScheduleConfirmationDO.java  (moved back from api)
```

### Files that stay in `headless/api` as-is (just fix imports)

- `ChatLayerService`, `SemanticLayerService` — already clean, no dependent fixes in moved file itself.
- `DataSetService` — already uses DTOs, clean.
- `SchemaService` — needs yaml import paths updated.
- `DeliveryContext` — already clean POJO.
- `ReportExecutionVO`, `SemanticDeployResult`, `SemanticTemplateConfig`, `ModelConfigHelper` — already clean (no old imports).

---

## Phase A — Stabilize compile (DOs back in server, api compiles clean)

### Task A1: Revert DOs back to `headless/server`

**Goal:** All 5 MyBatis-Plus `@TableName` DOs live in `headless-server` again. Mappers (which already import the old path and are currently broken) begin compiling.

**Files:**
- Move: `headless/api/src/main/java/com/tencent/supersonic/headless/api/persistence/dataobject/ReportScheduleDO.java` → `headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/ReportScheduleDO.java`
- Move: `ReportDeliveryConfigDO.java` → server equivalent
- Move: `ReportDeliveryRecordDO.java` → server equivalent
- Move: `ReportExecutionDO.java` → server equivalent
- Move: `ReportScheduleConfirmationDO.java` → server equivalent
- Delete (after moves): `headless/api/src/main/java/com/tencent/supersonic/headless/api/persistence/` (entire subtree, should be empty)

- [ ] **Step 1: Move each of the 5 DO files with `git mv`**

```bash
cd /Users/xudong/git/supersonic
for f in ReportScheduleDO ReportDeliveryConfigDO ReportDeliveryRecordDO ReportExecutionDO ReportScheduleConfirmationDO; do
  git mv "headless/api/src/main/java/com/tencent/supersonic/headless/api/persistence/dataobject/${f}.java" \
         "headless/server/src/main/java/com/tencent/supersonic/headless/server/persistence/dataobject/${f}.java"
done
```

- [ ] **Step 2: Update the `package` declaration in each moved file**

For each file, change line 1 from:
```java
package com.tencent.supersonic.headless.api.persistence.dataobject;
```
to:
```java
package com.tencent.supersonic.headless.server.persistence.dataobject;
```

Use Edit tool, one file at a time. No other changes in these files.

- [ ] **Step 3: Remove the now-empty `headless/api/.../persistence` directory**

```bash
find /Users/xudong/git/supersonic/headless/api/src/main/java/com/tencent/supersonic/headless/api/persistence -type d -empty -delete
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(headless): revert DO moves back to headless-server

DOs are MyBatis-Plus persistence entities and should not cross the
module boundary. Restore them to headless-server; the upcoming DTO
layer in headless-api will be the new cross-module contract."
```

Do NOT compile yet — service interfaces in api still import the old wrong path, so compile is still red. That's expected; it gets fixed in subsequent tasks.

---

### Task A2: Move remaining yaml POJOs from server to api

**Goal:** All 8 `*YamlTpl` POJOs live together in `headless-api`. `DataModelYamlTpl`/`DimensionYamlTpl`/`MetricYamlTpl` compile because their dependencies are in the same package.

**Files:**
- Move: `headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/yaml/IdentifyYamlTpl.java` → `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml/IdentifyYamlTpl.java`
- Move: `MeasureYamlTpl.java` → api equivalent
- Move: `MetricTypeParamsYamlTpl.java` → api equivalent
- Move: `FieldParamYamlTpl.java` → api equivalent
- Move: `MetricParamYamlTpl.java` → api equivalent
- Modify: 8 moved yaml files' package declaration
- Modify: `SchemaService.java` imports (uses these types)

- [ ] **Step 1: `git mv` the 5 remaining yaml files**

```bash
cd /Users/xudong/git/supersonic
for f in IdentifyYamlTpl MeasureYamlTpl MetricTypeParamsYamlTpl FieldParamYamlTpl MetricParamYamlTpl; do
  git mv "headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/yaml/${f}.java" \
         "headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml/${f}.java"
done
```

- [ ] **Step 2: Update `package` declaration in the 5 moved yaml files**

Change line 1 of each from:
```java
package com.tencent.supersonic.headless.server.pojo.yaml;
```
to:
```java
package com.tencent.supersonic.headless.api.pojo.yaml;
```

- [ ] **Step 3: Remove now-empty server yaml directory**

```bash
find /Users/xudong/git/supersonic/headless/server/src/main/java/com/tencent/supersonic/headless/server/pojo/yaml -type d -empty -delete
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(headless): move all yaml POJOs to headless-api

YamlTpl classes are plain POJOs shared with chat/feishu modules.
Consolidate them all in headless-api/pojo/yaml so SchemaService can
be a clean api-layer contract."
```

---

### Task A3: Fix stale imports inside files already in `headless/api`

**Goal:** Every file currently in `headless-api` (moved earlier) that still imports `headless.server.*` gets updated to `headless.api.*`. After this task, `headless-api` module must compile on its own.

**Files with stale imports (verified via review):**
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleService.java`
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportDeliveryService.java`
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleConfirmationService.java`
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/service/SchemaService.java`
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/event/TemplateDeployedEvent.java`
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml/DataModelYamlTpl.java` (may use unqualified type references — check)
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml/MetricYamlTpl.java` (may use unqualified type references — check)
- `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml/DimensionYamlTpl.java` (check)

⚠️ **Note:** `ReportScheduleService`/`ReportDeliveryService`/`ReportScheduleConfirmationService` will be completely rewritten in Phase B (B3–B5), so their imports don't need fixing now — they will be replaced wholesale. For this task, fix only the files that will **remain as-is in api**: `SchemaService`, `TemplateDeployedEvent`, and the yaml files.

- [ ] **Step 1: Fix `TemplateDeployedEvent.java` imports**

Edit `/Users/xudong/git/supersonic/headless/api/src/main/java/com/tencent/supersonic/headless/api/event/TemplateDeployedEvent.java`:

Replace:
```java
import com.tencent.supersonic.headless.server.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
```
with:
```java
import com.tencent.supersonic.headless.api.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig;
```

- [ ] **Step 2: Fix `SchemaService.java` imports**

Edit `/Users/xudong/git/supersonic/headless/api/src/main/java/com/tencent/supersonic/headless/api/service/SchemaService.java`:

Replace:
```java
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
```
with:
```java
import com.tencent.supersonic.headless.api.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.api.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.api.pojo.yaml.MetricYamlTpl;
```

- [ ] **Step 3: Verify yaml files in api don't reference unqualified missing types**

Use Grep to find any line inside `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml/*.java` that references `IdentifyYamlTpl`, `MeasureYamlTpl`, `MetricTypeParamsYamlTpl`, `FieldParamYamlTpl`, `MetricParamYamlTpl`:

```
Grep pattern="IdentifyYamlTpl|MeasureYamlTpl|MetricTypeParamsYamlTpl|FieldParamYamlTpl|MetricParamYamlTpl" path="headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/yaml" output_mode="content" -n=true
```

These are now in the same package (`headless.api.pojo.yaml`), so unqualified references resolve automatically. No import lines are needed. If any file has a fully-qualified import to `headless.server.pojo.yaml.*`, delete or rewrite it.

- [ ] **Step 4: Compile just the api module to verify it's clean**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS. Note: `ReportScheduleService` / `ReportDeliveryService` / `ReportScheduleConfirmationService` will still reference `com.tencent.supersonic.headless.server.*` which now doesn't exist, so compile will fail on those three files. That's expected — the rewrite in Phase B replaces them. For now, comment out the file contents temporarily OR make those three files empty interface shells to get past compile:

```java
// headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleService.java
package com.tencent.supersonic.headless.api.service;

// STUB — fully rewritten in Phase B (Task B3)
public interface ReportScheduleService {
}
```

Same pattern for `ReportDeliveryService` and `ReportScheduleConfirmationService`. The impls/consumers will be broken downstream, but we don't care — Phase A stops at "api compiles in isolation".

- [ ] **Step 5: Re-run api compile**

```bash
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(headless-api): fix stale imports and stub Report* services

Phase A of headless DTO boundary migration. Fix imports in
TemplateDeployedEvent and SchemaService to point at the in-module
versions of their dependencies. Stub the Report* service interfaces
temporarily so the api module compiles in isolation; they are fully
rewritten with DTO-based signatures in Phase B."
```

---

### Task A4: Bulk-update `headless.server.*` stale imports in `headless-server`, `chat`, `feishu`, `launchers`

**Goal:** Every consumer that imports moved classes by their old path gets the new path. After this task, the whole build compiles (with DOs as the cross-module contract — DTO migration happens in Phase B).

This is a **mechanical** bulk rename. The import pairs to rewrite:

| Old path | New path |
|---|---|
| `com.tencent.supersonic.headless.server.event.TemplateDeployedEvent` | `com.tencent.supersonic.headless.api.event.TemplateDeployedEvent` |
| `com.tencent.supersonic.headless.server.facade.service.ChatLayerService` | `com.tencent.supersonic.headless.api.facade.service.ChatLayerService` |
| `com.tencent.supersonic.headless.server.facade.service.SemanticLayerService` | `com.tencent.supersonic.headless.api.facade.service.SemanticLayerService` |
| `com.tencent.supersonic.headless.server.pojo.ReportExecutionVO` | `com.tencent.supersonic.headless.api.pojo.ReportExecutionVO` |
| `com.tencent.supersonic.headless.server.pojo.SemanticDeployResult` | `com.tencent.supersonic.headless.api.pojo.SemanticDeployResult` |
| `com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig` | `com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig` |
| `com.tencent.supersonic.headless.server.pojo.yaml.` | `com.tencent.supersonic.headless.api.pojo.yaml.` |
| `com.tencent.supersonic.headless.server.service.DataSetService` | `com.tencent.supersonic.headless.api.service.DataSetService` |
| `com.tencent.supersonic.headless.server.service.ReportDeliveryService` | `com.tencent.supersonic.headless.api.service.ReportDeliveryService` |
| `com.tencent.supersonic.headless.server.service.ReportScheduleConfirmationService` | `com.tencent.supersonic.headless.api.service.ReportScheduleConfirmationService` |
| `com.tencent.supersonic.headless.server.service.ReportScheduleService` | `com.tencent.supersonic.headless.api.service.ReportScheduleService` |
| `com.tencent.supersonic.headless.server.service.SchemaService` | `com.tencent.supersonic.headless.api.service.SchemaService` |
| `com.tencent.supersonic.headless.server.service.delivery.DeliveryContext` | `com.tencent.supersonic.headless.api.service.delivery.DeliveryContext` |
| `com.tencent.supersonic.headless.server.utils.ModelConfigHelper` | `com.tencent.supersonic.headless.api.util.ModelConfigHelper` |

**Files to walk:** all `.java` files under `headless/server/src`, `chat/server/src`, `feishu/server/src`, `launchers/standalone/src` (both main and test trees).

⚠️ **Do NOT rewrite DO imports** (`ReportScheduleDO`, `ReportDeliveryConfigDO`, `ReportDeliveryRecordDO`, `ReportExecutionDO`, `ReportScheduleConfirmationDO`). Those live in `headless.server.persistence.dataobject.*` now (after Task A1) — dependents already import them correctly.

- [ ] **Step 1: Run the bulk replace**

Use this ripgrep-driven sed loop:

```bash
cd /Users/xudong/git/supersonic

# Define the pairs
declare -a REPLACEMENTS=(
  "com.tencent.supersonic.headless.server.event.TemplateDeployedEvent|com.tencent.supersonic.headless.api.event.TemplateDeployedEvent"
  "com.tencent.supersonic.headless.server.facade.service.ChatLayerService|com.tencent.supersonic.headless.api.facade.service.ChatLayerService"
  "com.tencent.supersonic.headless.server.facade.service.SemanticLayerService|com.tencent.supersonic.headless.api.facade.service.SemanticLayerService"
  "com.tencent.supersonic.headless.server.pojo.ReportExecutionVO|com.tencent.supersonic.headless.api.pojo.ReportExecutionVO"
  "com.tencent.supersonic.headless.server.pojo.SemanticDeployResult|com.tencent.supersonic.headless.api.pojo.SemanticDeployResult"
  "com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig|com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig"
  "com.tencent.supersonic.headless.server.pojo.yaml.|com.tencent.supersonic.headless.api.pojo.yaml."
  "com.tencent.supersonic.headless.server.service.DataSetService|com.tencent.supersonic.headless.api.service.DataSetService"
  "com.tencent.supersonic.headless.server.service.ReportDeliveryService|com.tencent.supersonic.headless.api.service.ReportDeliveryService"
  "com.tencent.supersonic.headless.server.service.ReportScheduleConfirmationService|com.tencent.supersonic.headless.api.service.ReportScheduleConfirmationService"
  "com.tencent.supersonic.headless.server.service.ReportScheduleService|com.tencent.supersonic.headless.api.service.ReportScheduleService"
  "com.tencent.supersonic.headless.server.service.SchemaService|com.tencent.supersonic.headless.api.service.SchemaService"
  "com.tencent.supersonic.headless.server.service.delivery.DeliveryContext|com.tencent.supersonic.headless.api.service.delivery.DeliveryContext"
  "com.tencent.supersonic.headless.server.utils.ModelConfigHelper|com.tencent.supersonic.headless.api.util.ModelConfigHelper"
)

for pair in "${REPLACEMENTS[@]}"; do
  OLD="${pair%%|*}"
  NEW="${pair##*|}"
  # Find all .java files in the 4 module trees (main + test) that contain the OLD literal,
  # and replace in-place. Use a macOS-compatible sed (BSD sed needs -i '').
  rg --files-with-matches --type java -F "$OLD" \
     headless/server/src chat/server/src feishu/server/src launchers/standalone/src \
     2>/dev/null | while read -r file; do
    sed -i '' "s|${OLD}|${NEW}|g" "$file"
  done
done
```

- [ ] **Step 2: Verify no stale references remain**

```bash
rg -n --type java \
  'com\.tencent\.supersonic\.headless\.server\.(event\.TemplateDeployedEvent|facade\.service\.(Chat|Semantic)LayerService|pojo\.(ReportExecutionVO|SemanticDeployResult|SemanticTemplateConfig|yaml\.)|service\.(DataSetService|ReportDeliveryService|ReportScheduleConfirmationService|ReportScheduleService|SchemaService|delivery\.DeliveryContext)|utils\.ModelConfigHelper)' \
  headless/server/src chat/server/src feishu/server/src launchers/standalone/src
```

Expected: no output.

- [ ] **Step 3: Full compile check**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl launchers/standalone -am 2>&1 | tail -60
```

Expected: **Some errors remain** — specifically, the stubbed `ReportScheduleService`/`ReportDeliveryService`/`ReportScheduleConfirmationService` interfaces have empty bodies, so every call-site and impl that uses their methods will fail. Read the error list. Errors that are NOT from these three stubbed services are bugs in this task — fix them before proceeding. Errors that ARE from the stubs are expected.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(headless): bulk-update stale headless.server.* imports

Dependents across chat, feishu, headless/server, and launchers now
reference moved classes (events, yaml POJOs, facade services, etc.)
via their headless-api paths. Report* service call-sites are still
broken; they're rewritten to consume DTOs in Phase B."
```

---

## Phase B — DTO migration proper

### Task B1: Create Resp DTOs in `headless/api`

**Goal:** 5 new `*Resp` DTOs exist in `headless/api/.../pojo/response/`, each with static `fromDO(DO)` helpers that copy fields. Plain POJOs, no MyBatis annotations, no dependency on `headless-server`.

**Files:**
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/response/ReportScheduleResp.java`
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/response/ReportDeliveryConfigResp.java`
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/response/ReportDeliveryRecordResp.java`
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/response/ReportExecutionResp.java`
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/response/ReportScheduleConfirmationResp.java`

⚠️ **No `fromDO` methods inside these classes** — that would force `headless-api` to import `headless-server`, which defeats the whole refactor. Field shape only. Mapping lives in `headless-server` (Task B6).

- [ ] **Step 1: Create `ReportScheduleResp.java`**

```java
package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportScheduleResp {
    private Long id;
    private String name;
    private Long datasetId;
    private String queryConfig;
    private String outputFormat;
    private String cronExpression;
    private Boolean enabled;
    private Long ownerId;
    private Integer retryCount;
    private Integer retryInterval;
    private Long templateVersion;
    private String deliveryConfigIds;
    private String quartzJobKey;
    private Date lastExecutionTime;
    private Date nextExecutionTime;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private Long tenantId;
}
```

- [ ] **Step 2: Create `ReportDeliveryConfigResp.java`**

```java
package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportDeliveryConfigResp {
    private Long id;
    private String name;
    private String deliveryType;
    private String deliveryConfig;
    private Boolean enabled;
    private String description;
    private Integer consecutiveFailures;
    private Integer maxConsecutiveFailures;
    private String disabledReason;
    private Long tenantId;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;
}
```

- [ ] **Step 3: Create `ReportDeliveryRecordResp.java`**

```java
package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportDeliveryRecordResp {
    private Long id;
    private String deliveryKey;
    private Long scheduleId;
    private Long executionId;
    private Long configId;
    private String deliveryType;
    private String status;
    private String fileLocation;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Date nextRetryAt;
    private Date startedAt;
    private Date completedAt;
    private Long deliveryTimeMs;
    private Long tenantId;
    private Date createdAt;
}
```

- [ ] **Step 4: Create `ReportExecutionResp.java`**

```java
package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportExecutionResp {
    private Long id;
    private Long scheduleId;
    private Integer attempt;
    private String status;
    private Date startTime;
    private Date endTime;
    private String resultLocation;
    private String errorMessage;
    private Long rowCount;
    private String sqlHash;
    private Long tenantId;
    private String executionSnapshot;
    private Long templateVersion;
    private String engineVersion;
    private Long scanRows;
    private Long executionTimeMs;
    private Long ioBytes;
}
```

Note: this is a **plain, flat** execution DTO — mirrors the DO. The enriched `ReportExecutionVO` (with `templateName`, `channelTypes`, `deliveryRollup`, etc.) already exists at `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/ReportExecutionVO.java` and is unchanged. The two coexist: `ReportExecutionResp` for plain list/get endpoints, `ReportExecutionVO` for enriched UI views.

- [ ] **Step 5: Create `ReportScheduleConfirmationResp.java`**

```java
package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportScheduleConfirmationResp {
    private Long id;
    private String confirmToken;
    private Long userId;
    private Integer chatId;
    private String actionType;
    private Long sourceQueryId;
    private Integer sourceParseId;
    private Long sourceDataSetId;
    private String payloadJson;
    private String status;
    private Date expireAt;
    private Date createdAt;
    private Long tenantId;
}
```

- [ ] **Step 6: Compile the api module**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/response/Report*.java
git commit -m "feat(headless-api): add Report* Resp DTOs for cross-module contracts

Plain POJOs mirroring the DO shape. These replace DOs as the
cross-module data shape for feishu/chat consumers. Mapping between
DOs and DTOs stays in headless-server."
```

---

### Task B2: Create Req DTOs in `headless/api`

**Files:**
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/request/ReportScheduleReq.java`
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/request/ReportDeliveryConfigReq.java`
- Create: `headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/request/ReportScheduleConfirmationReq.java`

These are mutable "build" DTOs used by callers to pass create/update inputs. They include `id` (nullable for create, non-null for update) and only the fields a caller can set — exclude server-owned fields like `createdAt`, `tenantId`, `quartzJobKey`.

- [ ] **Step 1: Create `ReportScheduleReq.java`**

```java
package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class ReportScheduleReq {
    private Long id;                   // null for create
    private String name;
    private Long datasetId;
    private String queryConfig;
    private String outputFormat;
    private String cronExpression;
    private Boolean enabled;
    private Long ownerId;
    private Integer retryCount;
    private Integer retryInterval;
    private Long templateVersion;
    private String deliveryConfigIds;
}
```

- [ ] **Step 2: Create `ReportDeliveryConfigReq.java`**

```java
package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class ReportDeliveryConfigReq {
    private Long id;                   // null for create
    private String name;
    private String deliveryType;
    private String deliveryConfig;
    private Boolean enabled;
    private String description;
    private Integer maxConsecutiveFailures;
}
```

- [ ] **Step 3: Create `ReportScheduleConfirmationReq.java`**

```java
package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

import java.util.Date;

@Data
public class ReportScheduleConfirmationReq {
    private String confirmToken;
    private Long userId;
    private Integer chatId;
    private String actionType;
    private Long sourceQueryId;
    private Integer sourceParseId;
    private Long sourceDataSetId;
    private String payloadJson;
    private Date expireAt;
}
```

- [ ] **Step 4: Compile**

```bash
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add headless/api/src/main/java/com/tencent/supersonic/headless/api/pojo/request/Report*.java
git commit -m "feat(headless-api): add Report* Req DTOs for create/update inputs"
```

---

### Task B3: Rewrite `ReportScheduleService` interface with DTOs

**File:** `/Users/xudong/git/supersonic/headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleService.java`

- [ ] **Step 1: Replace the stubbed file contents**

```java
package com.tencent.supersonic.headless.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.ReportExecutionVO;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;

public interface ReportScheduleService {

    ReportScheduleResp createSchedule(ReportScheduleReq req, User user);

    ReportScheduleResp updateSchedule(ReportScheduleReq req, User user);

    void deleteSchedule(Long id, User user);

    ReportScheduleResp getScheduleById(Long id, User user);

    Page<ReportScheduleResp> getScheduleList(Page<ReportScheduleResp> page, Long datasetId,
            Boolean enabled, User user);

    void pauseSchedule(Long id, User user);

    void resumeSchedule(Long id, User user);

    void triggerNow(Long id, User user);

    void reschedule(Long id, String newCron);

    Page<ReportExecutionResp> getExecutionList(Page<ReportExecutionResp> page, Long scheduleId,
            String status, User user);

    Page<ReportExecutionVO> getExecutionVOList(Page<ReportExecutionResp> page, Long scheduleId,
            String status, User user);

    ReportExecutionResp getExecutionById(Long scheduleId, Long id, User user);

    void executeReport(Long scheduleId, User user);
}
```

- [ ] **Step 2: Verify api compiles**

```bash
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleService.java
git commit -m "refactor(headless-api): ReportScheduleService speaks DTOs"
```

---

### Task B4: Rewrite `ReportDeliveryService` interface with DTOs

**File:** `/Users/xudong/git/supersonic/headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportDeliveryService.java`

- [ ] **Step 1: Replace the stubbed file contents**

```java
package com.tencent.supersonic.headless.api.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.api.pojo.request.ReportDeliveryConfigReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryRecordResp;
import com.tencent.supersonic.headless.api.service.delivery.DeliveryContext;

import java.util.List;

/**
 * Service for managing report delivery configurations and executing deliveries.
 */
public interface ReportDeliveryService {

    // ========== Config CRUD ==========

    ReportDeliveryConfigResp createConfig(ReportDeliveryConfigReq req);

    ReportDeliveryConfigResp updateConfig(ReportDeliveryConfigReq req);

    void deleteConfig(Long id);

    ReportDeliveryConfigResp getConfigById(Long id);

    Page<ReportDeliveryConfigResp> getConfigList(Page<ReportDeliveryConfigResp> page);

    List<ReportDeliveryConfigResp> getConfigsByIds(List<Long> ids);

    // ========== Delivery Execution ==========

    /**
     * Deliver a report to multiple channels.
     */
    List<ReportDeliveryRecordResp> deliver(List<Long> configIds, DeliveryContext context);

    /**
     * Test delivery configuration by sending a test message.
     */
    ReportDeliveryRecordResp testDelivery(Long configId);

    // ========== Delivery Records ==========

    Page<ReportDeliveryRecordResp> getDeliveryRecords(Page<ReportDeliveryRecordResp> page,
            Long configId, Long scheduleId, Long executionId);

    /**
     * Retry a failed delivery.
     */
    ReportDeliveryRecordResp retryDelivery(Long recordId);

    // ========== Statistics ==========

    DeliveryStatistics getStatistics(Integer days);

    List<DailyDeliveryStats> getDailyStats(Integer days);

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class DeliveryStatistics {
        private long totalDeliveries;
        private long successCount;
        private long failedCount;
        private long pendingCount;
        private double successRate;
        private java.util.Map<String, Long> countByType;
        private java.util.Map<String, Double> successRateByType;
        private Double avgDeliveryTimeMs;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class DailyDeliveryStats {
        private String date;
        private long total;
        private long success;
        private long failed;
        private double successRate;
    }
}
```

- [ ] **Step 2: Compile api**

```bash
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportDeliveryService.java
git commit -m "refactor(headless-api): ReportDeliveryService speaks DTOs"
```

---

### Task B5: Rewrite `ReportScheduleConfirmationService` interface with DTOs

**File:** `/Users/xudong/git/supersonic/headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleConfirmationService.java`

- [ ] **Step 1: Replace the stubbed file contents**

```java
package com.tencent.supersonic.headless.api.service;

import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;

public interface ReportScheduleConfirmationService {

    ReportScheduleConfirmationResp createPending(ReportScheduleConfirmationReq req);

    ReportScheduleConfirmationResp getLatestPending(Long userId, Integer chatId);

    boolean hasPending(Long userId, Integer chatId);

    void updateStatus(Long id, String status);
}
```

- [ ] **Step 2: Compile api**

```bash
mvn compile -pl headless/api -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add headless/api/src/main/java/com/tencent/supersonic/headless/api/service/ReportScheduleConfirmationService.java
git commit -m "refactor(headless-api): ReportScheduleConfirmationService speaks DTOs"
```

---

### Task B6: Add `DtoMappers` utility in `headless-server` for DO↔DTO conversion

**Rationale:** Rather than peppering `BeanUtils.copyProperties` calls through every service impl, put the 5 mappings (DO→Resp) + 3 reverse mappings (Req→DO) in one place. One static utility class, no Spring bean, no framework.

**File:** `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/mapper/ReportDtoMappers.java` (NEW)

- [ ] **Step 1: Create `ReportDtoMappers.java`**

```java
package com.tencent.supersonic.headless.server.service.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.api.pojo.request.ReportDeliveryConfigReq;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryRecordResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DO ↔ DTO mapping helpers for Report* service boundaries. One-way field
 * copies via {@link BeanUtils#copyProperties}; property names must match
 * between the DO and DTO. Do not add transformation logic here — if a field
 * needs computed/derived values, the impl should enrich the DTO after the
 * mapping call.
 */
public final class ReportDtoMappers {

    private ReportDtoMappers() {}

    // ========== ReportSchedule ==========

    public static ReportScheduleResp toResp(ReportScheduleDO src) {
        if (src == null) return null;
        ReportScheduleResp dst = new ReportScheduleResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static ReportScheduleDO toDO(ReportScheduleReq src) {
        if (src == null) return null;
        ReportScheduleDO dst = new ReportScheduleDO();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static Page<ReportScheduleResp> toRespPage(Page<ReportScheduleDO> src) {
        Page<ReportScheduleResp> dst =
                new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(src.getRecords().stream()
                .map(ReportDtoMappers::toResp)
                .collect(Collectors.toList()));
        return dst;
    }

    // ========== ReportDeliveryConfig ==========

    public static ReportDeliveryConfigResp toResp(ReportDeliveryConfigDO src) {
        if (src == null) return null;
        ReportDeliveryConfigResp dst = new ReportDeliveryConfigResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static ReportDeliveryConfigDO toDO(ReportDeliveryConfigReq src) {
        if (src == null) return null;
        ReportDeliveryConfigDO dst = new ReportDeliveryConfigDO();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static List<ReportDeliveryConfigResp> toConfigResps(List<ReportDeliveryConfigDO> src) {
        return src.stream().map(ReportDtoMappers::toResp).collect(Collectors.toList());
    }

    public static Page<ReportDeliveryConfigResp> toConfigRespPage(
            Page<ReportDeliveryConfigDO> src) {
        Page<ReportDeliveryConfigResp> dst =
                new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(toConfigResps(src.getRecords()));
        return dst;
    }

    // ========== ReportDeliveryRecord ==========

    public static ReportDeliveryRecordResp toResp(ReportDeliveryRecordDO src) {
        if (src == null) return null;
        ReportDeliveryRecordResp dst = new ReportDeliveryRecordResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static List<ReportDeliveryRecordResp> toRecordResps(
            List<ReportDeliveryRecordDO> src) {
        return src.stream().map(ReportDtoMappers::toResp).collect(Collectors.toList());
    }

    public static Page<ReportDeliveryRecordResp> toRecordRespPage(
            Page<ReportDeliveryRecordDO> src) {
        Page<ReportDeliveryRecordResp> dst =
                new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(toRecordResps(src.getRecords()));
        return dst;
    }

    // ========== ReportExecution ==========

    public static ReportExecutionResp toResp(ReportExecutionDO src) {
        if (src == null) return null;
        ReportExecutionResp dst = new ReportExecutionResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static Page<ReportExecutionResp> toExecutionRespPage(
            Page<ReportExecutionDO> src) {
        Page<ReportExecutionResp> dst =
                new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(src.getRecords().stream()
                .map(ReportDtoMappers::toResp)
                .collect(Collectors.toList()));
        return dst;
    }

    // ========== ReportScheduleConfirmation ==========

    public static ReportScheduleConfirmationResp toResp(ReportScheduleConfirmationDO src) {
        if (src == null) return null;
        ReportScheduleConfirmationResp dst = new ReportScheduleConfirmationResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static ReportScheduleConfirmationDO toDO(ReportScheduleConfirmationReq src) {
        if (src == null) return null;
        ReportScheduleConfirmationDO dst = new ReportScheduleConfirmationDO();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/mapper/ReportDtoMappers.java
git commit -m "feat(headless-server): add ReportDtoMappers for DO<->DTO at service boundary"
```

---

### Task B7: Update `ReportScheduleServiceImpl` to map at the boundary

**File:** `/Users/xudong/git/supersonic/headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java`

**Strategy:** Keep all internal logic, DB access, and helper methods on DOs. At each **public method** boundary, accept Req/primitive inputs, build/fetch DOs internally, and return `ReportDtoMappers.toResp(...)` at the exit. Callers never see a DO.

- [ ] **Step 1: Read the current impl to understand internal structure**

```
Read headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java
```

Note: this file is large. Identify every method that overrides `ReportScheduleService` — those are the boundary methods to modify.

- [ ] **Step 2: Update the method signatures to match the new interface (Task B3)**

For each overridden method, change the signature and wrap internal flow with mapper calls. Example for `createSchedule`:

Before (uses DO):
```java
@Override
public ReportScheduleDO createSchedule(ReportScheduleDO schedule, User user) {
    validate(schedule);
    schedule.setCreatedBy(user.getName());
    schedule.setCreatedAt(new Date());
    save(schedule);
    scheduleQuartzJob(schedule);
    return schedule;
}
```

After (uses DTOs at boundary):
```java
@Override
public ReportScheduleResp createSchedule(ReportScheduleReq req, User user) {
    ReportScheduleDO schedule = ReportDtoMappers.toDO(req);
    validate(schedule);
    schedule.setCreatedBy(user.getName());
    schedule.setCreatedAt(new Date());
    save(schedule);
    scheduleQuartzJob(schedule);
    return ReportDtoMappers.toResp(schedule);
}
```

Apply the same pattern to `updateSchedule`, `getScheduleById`, `getScheduleList`, `getExecutionList`, `getExecutionVOList`, `getExecutionById`. The `getExecutionVOList` method is special — it builds `ReportExecutionVO` internally (enriched), the input page type changes to `Page<ReportExecutionResp>` but internally you create a `Page<ReportExecutionDO>` with the same `current`/`size` for the DB query:

```java
@Override
public Page<ReportExecutionVO> getExecutionVOList(Page<ReportExecutionResp> page,
        Long scheduleId, String status, User user) {
    Page<ReportExecutionDO> doPage = new Page<>(page.getCurrent(), page.getSize());
    // ... existing internal flow that builds Page<ReportExecutionVO> ...
    return voPage;
}
```

Methods that don't return a DO (`deleteSchedule`, `pauseSchedule`, `resumeSchedule`, `triggerNow`, `reschedule`, `executeReport`) don't need signature changes — keep as-is.

Add these imports to the top of the file:
```java
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.server.service.mapper.ReportDtoMappers;
```

- [ ] **Step 3: Compile**

```bash
mvn compile -pl launchers/standalone -am 2>&1 | tail -80
```

Consumers of the service (chat, feishu) will still fail to compile — expected. Errors in `ReportScheduleServiceImpl.java` itself are bugs in this task and must be fixed.

- [ ] **Step 4: Update `ReportScheduleServiceImplTest`**

Test file: `/Users/xudong/git/supersonic/headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportScheduleServiceImplTest.java`

For each test that calls `createSchedule(DO, user)` / `updateSchedule(DO, user)`, construct a `ReportScheduleReq` instead. For assertions on return values, the return type is now `ReportScheduleResp` — field accessors remain the same (Lombok getters). Update types throughout.

- [ ] **Step 5: Run the test**

```bash
mvn test -pl headless/server -Dtest=ReportScheduleServiceImplTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleServiceImpl.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/service/ReportScheduleServiceImplTest.java
git commit -m "refactor(headless-server): ReportScheduleServiceImpl maps DOs to DTOs at boundary"
```

---

### Task B8: Update `ReportDeliveryServiceImpl` to map at the boundary

**File:** `/Users/xudong/git/supersonic/headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportDeliveryServiceImpl.java`

Same pattern as Task B7. Boundary methods map DO↔DTO; internal flow stays on DOs.

Key methods to update (matching the new interface from Task B4):
- `createConfig(ReportDeliveryConfigReq req) -> ReportDeliveryConfigResp`
- `updateConfig(ReportDeliveryConfigReq req) -> ReportDeliveryConfigResp`
- `getConfigById(Long id) -> ReportDeliveryConfigResp`
- `getConfigList(Page<ReportDeliveryConfigResp> page) -> Page<ReportDeliveryConfigResp>`
- `getConfigsByIds(List<Long> ids) -> List<ReportDeliveryConfigResp>`
- `deliver(List<Long>, DeliveryContext) -> List<ReportDeliveryRecordResp>`
- `testDelivery(Long) -> ReportDeliveryRecordResp`
- `getDeliveryRecords(...) -> Page<ReportDeliveryRecordResp>`
- `retryDelivery(Long) -> ReportDeliveryRecordResp`

- [ ] **Step 1: Read the current impl**

```
Read headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportDeliveryServiceImpl.java
```

- [ ] **Step 2: Apply DTO mapping to each boundary method**

Example for `createConfig`:
```java
@Override
public ReportDeliveryConfigResp createConfig(ReportDeliveryConfigReq req) {
    ReportDeliveryConfigDO config = ReportDtoMappers.toDO(req);
    validate(config);
    config.setCreatedAt(new Date());
    save(config);
    return ReportDtoMappers.toResp(config);
}
```

For `getConfigList`:
```java
@Override
public Page<ReportDeliveryConfigResp> getConfigList(Page<ReportDeliveryConfigResp> page) {
    Page<ReportDeliveryConfigDO> doPage = new Page<>(page.getCurrent(), page.getSize());
    // ...existing query logic using doPage...
    return ReportDtoMappers.toConfigRespPage(doPage);
}
```

Add imports:
```java
import com.tencent.supersonic.headless.api.pojo.request.ReportDeliveryConfigReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryRecordResp;
import com.tencent.supersonic.headless.server.service.mapper.ReportDtoMappers;
```

- [ ] **Step 3: Update dependents inside `headless/server`**

`ReportExecutionOrchestrator`, `DeliveryRetryTask`, `FeishuDeliveryChannel`, and any other internal caller of `ReportDeliveryService` now sees `List<ReportDeliveryRecordResp>` instead of `List<ReportDeliveryRecordDO>`. Two options:
1. Update the internal caller to work with `Resp` types.
2. Inside the impl, expose a package-private method returning DOs for internal callers.

Prefer option 1 (clean). If a caller specifically needs DO-only fields that aren't on Resp, bridge through a mapper or add the field to Resp.

- [ ] **Step 4: Update `ReportDeliveryServiceImplTest` and `FeishuDeliveryChannelTest`**

Existing test files to sync:
- `/Users/xudong/git/supersonic/headless/server/src/test/java/com/tencent/supersonic/headless/server/service/delivery/FeishuDeliveryChannelTest.java`
- (Find any `ReportDeliveryServiceImplTest` — may or may not exist)

Update mock setups to return Resp types where the service interface says Resp.

- [ ] **Step 5: Compile + run affected tests**

```bash
mvn compile -pl launchers/standalone -am 2>&1 | tail -60
mvn test -pl headless/server -Dtest=FeishuDeliveryChannelTest,ReportDeliveryServiceImplTest
```

- [ ] **Step 6: Commit**

```bash
git add headless/server/
git commit -m "refactor(headless-server): ReportDeliveryServiceImpl maps DOs to DTOs at boundary"
```

---

### Task B9: Update `ReportScheduleConfirmationServiceImpl` to map at the boundary

**File:** `/Users/xudong/git/supersonic/headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleConfirmationServiceImpl.java`

Small, straightforward.

- [ ] **Step 1: Read and rewrite**

Signatures per Task B5:
- `createPending(ReportScheduleConfirmationReq req) -> ReportScheduleConfirmationResp`
- `getLatestPending(Long userId, Integer chatId) -> ReportScheduleConfirmationResp`
- `hasPending(Long, Integer) -> boolean` (unchanged)
- `updateStatus(Long id, String status) -> void` (unchanged)

Example:
```java
@Override
public ReportScheduleConfirmationResp createPending(ReportScheduleConfirmationReq req) {
    ReportScheduleConfirmationDO confirmation = ReportDtoMappers.toDO(req);
    confirmation.setStatus("PENDING");
    confirmation.setCreatedAt(new Date());
    save(confirmation);
    return ReportDtoMappers.toResp(confirmation);
}
```

Add imports:
```java
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.server.service.mapper.ReportDtoMappers;
```

- [ ] **Step 2: Compile**

```bash
mvn compile -pl launchers/standalone -am 2>&1 | tail -60
```

- [ ] **Step 3: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ReportScheduleConfirmationServiceImpl.java
git commit -m "refactor(headless-server): ReportScheduleConfirmationServiceImpl maps DOs to DTOs at boundary"
```

---

### Task B10: Update `chat/server` consumers

**Goal:** `chat/server` no longer imports any `Report*DO` class. It speaks DTOs via the `headless-api` service interfaces.

**Files:**
- `/Users/xudong/git/supersonic/chat/server/src/main/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQuery.java`
- `/Users/xudong/git/supersonic/chat/server/src/test/java/com/tencent/supersonic/chat/server/plugin/support/reportschedule/ReportScheduleQueryTest.java`

- [ ] **Step 1: Read `ReportScheduleQuery.java` to map every DO reference**

Known DO uses from grep results:
- Line 191: `new ReportScheduleConfirmationDO()` → `new ReportScheduleConfirmationReq()`
- Line 257: `ReportScheduleConfirmationDO pending = confirmationService.getLatestPending(...)` → `ReportScheduleConfirmationResp pending = ...`
- Line 288: `new ReportScheduleDO()` → `new ReportScheduleReq()`
- Line 298: `ReportScheduleDO created = scheduleService.createSchedule(schedule, currentUser)` → `ReportScheduleResp created = ...`
- Line 420-421: `Page<ReportScheduleDO>` → `Page<ReportScheduleResp>`
- Line 425: `for (ReportScheduleDO schedule : ...)` → `for (ReportScheduleResp schedule : ...)`
- Lines 458, 486, 512, 537: `ReportScheduleDO schedule = scheduleService.getScheduleById(...)` → `ReportScheduleResp schedule = ...`
- Line 563-568: `Page<ReportExecutionDO>` and `for (ReportExecutionDO exec : ...)` → `Page<ReportExecutionResp>` and `ReportExecutionResp`
- Line 832: `List<ReportDeliveryConfigDO> configs = deliveryService.getConfigList(...)` → `List<ReportDeliveryConfigResp>` (note: `getConfigList` returns `Page`; this line uses `.stream()`/chain. Update accordingly.)
- Line 874: `ReportDeliveryConfigDO config = deliveryService.getConfigById(configId)` → `ReportDeliveryConfigResp config = ...`

Rewrite imports:
```java
// Remove
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;

// Add
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
```

Field accessors stay the same because DTO field names match DO field names. Only the TYPE changes.

- [ ] **Step 2: Update `ReportScheduleQueryTest.java`**

Test file uses DOs for Mockito stubs. Replace all DO types with the corresponding Req/Resp DTO types. `when(service.createSchedule(any(ReportScheduleDO.class), any(User.class))).thenReturn(...DO)` becomes `when(service.createSchedule(any(ReportScheduleReq.class), any(User.class))).thenReturn(...Resp)`. Do the same for confirmation service and delivery service stubs.

- [ ] **Step 3: Verify chat has no stale DO imports**

```bash
rg -n 'com\.tencent\.supersonic\.headless\.server\.persistence\.dataobject\.Report' chat/
```

Expected: no output.

- [ ] **Step 4: Compile + run chat tests**

```bash
mvn compile -pl launchers/standalone -am 2>&1 | tail -60
mvn test -pl chat/server -Dtest=ReportScheduleQueryTest
```

- [ ] **Step 5: Commit**

```bash
git add chat/server/
git commit -m "refactor(chat): consume Report* services via DTOs

Drops all ReportScheduleDO / ReportExecutionDO / ReportDeliveryConfigDO /
ReportScheduleConfirmationDO references from chat/server. Plugin and tests
now use Req/Resp DTOs via headless-api."
```

---

### Task B11: Update `feishu/server` consumers

**Files:**
- `/Users/xudong/git/supersonic/feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/CardActionHandler.java`
- `/Users/xudong/git/supersonic/feishu/server/src/main/java/com/tencent/supersonic/feishu/server/handler/ScheduleMessageHandler.java`
- `/Users/xudong/git/supersonic/feishu/server/src/main/java/com/tencent/supersonic/feishu/server/render/FeishuCardRenderer.java`
- `/Users/xudong/git/supersonic/feishu/server/src/test/java/com/tencent/supersonic/feishu/server/handler/CardActionHandlerTest.java` (currently untracked — check `git status`)

- [ ] **Step 1: Read each file and rewrite DO references**

Same pattern as Task B10. Key call-sites:

`CardActionHandler.java` line 137:
```java
// Before
ReportScheduleDO schedule = reportScheduleService.getScheduleById(scheduleId, user);
// After
ReportScheduleResp schedule = reportScheduleService.getScheduleById(scheduleId, user);
```

`ScheduleMessageHandler.java` — multiple:
- Line 81: `Page<ReportScheduleDO>` → `Page<ReportScheduleResp>`
- Line 122: `new ReportScheduleDO()` → `new ReportScheduleReq()`
- Line 134: `ReportScheduleDO created = reportScheduleService.createSchedule(...)` → `ReportScheduleResp created = ...`
- Line 245: `List<ReportDeliveryConfigDO>` → `List<ReportDeliveryConfigResp>`

`FeishuCardRenderer.java` — method signatures use DOs:
- Line 374: `public Map<String, Object> renderScheduleCreatedCard(ReportScheduleDO schedule, ...)` → `renderScheduleCreatedCard(ReportScheduleResp schedule, ...)`
- Line 391: `public Map<String, Object> renderScheduleListCard(List<ReportScheduleDO> schedules)` → `renderScheduleListCard(List<ReportScheduleResp> schedules)`
- Line 397: `for (ReportScheduleDO s : schedules)` → `for (ReportScheduleResp s : schedules)`

Update imports in each file:
```java
// Remove any
import com.tencent.supersonic.headless.server.persistence.dataobject.Report*DO;

// Add
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
```

- [ ] **Step 2: Update `CardActionHandlerTest.java` (untracked) if it exists**

Check `git status` — if there's an untracked test file for CardActionHandler, update its DO references to Resp types.

- [ ] **Step 3: Verify no stale DO imports in feishu**

```bash
rg -n 'com\.tencent\.supersonic\.headless\.server\.persistence\.dataobject\.Report' feishu/
```

Expected: no output.

- [ ] **Step 4: Compile + run feishu tests**

```bash
mvn compile -pl launchers/standalone -am 2>&1 | tail -60
mvn test -pl feishu/server
```

Expected: BUILD SUCCESS + tests green.

- [ ] **Step 5: Commit**

```bash
git add feishu/server/
git commit -m "refactor(feishu): consume Report* services via DTOs

Handlers and renderer now work with ReportScheduleResp /
ReportDeliveryConfigResp DTOs from headless-api. No more DO leakage."
```

---

### Task B12: Full repository compile + all test run

**Goal:** Confirm the whole project compiles and all existing tests still pass before altering poms.

- [ ] **Step 1: Full compile**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl launchers/standalone -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Full test-compile**

```bash
mvn test-compile -pl launchers/standalone -am
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run touched test classes**

```bash
mvn test -pl headless/server -Dtest='ReportScheduleServiceImplTest,ReportExecutionOrchestratorTest,FeishuDeliveryChannelTest,QueryConfigParserTest'
mvn test -pl chat/server -Dtest=ReportScheduleQueryTest
mvn test -pl feishu/server
```

Expected: all green.

- [ ] **Step 4: Commit (if any last adjustments)**

If this task required no further changes, skip. Otherwise:

```bash
git commit -m "refactor(headless): finalize DTO boundary migration — all green"
```

---

## Phase C — Enforce module isolation (drop `headless-server` dependency)

### Task C1: Drop `headless-server` dependency from `feishu/server/pom.xml`

**File:** `/Users/xudong/git/supersonic/feishu/server/pom.xml`

- [ ] **Step 1: Read the current pom** (specifically the dependency for `headless-server`)

```
Read /Users/xudong/git/supersonic/feishu/server/pom.xml
```

Locate the `<dependency>...<artifactId>headless-server</artifactId>...</dependency>` block (around line 49-54 per review).

- [ ] **Step 2: Remove the block (and any comment tied to it)**

Use Edit to delete the entire `<dependency>` block, including the leading comment if it references the migration.

- [ ] **Step 3: Compile**

```bash
mvn compile -pl feishu/server -am 2>&1 | tail -80
```

Expected outcomes:
- **If BUILD SUCCESS:** feishu/server is cleanly decoupled. Proceed.
- **If BUILD FAILURE:** Each error names a class still leaked from headless-server. Triage: either (a) the class genuinely belongs in headless-api and should be moved, or (b) feishu was using an internal that should be behind an interface.

For each unresolved class, decide:
1. If it's a POJO/DTO/interface that makes sense in the public contract → move it to `headless-api` (new subtask, mini-migration, same pattern as Task A1-A4).
2. If it's an implementation class → feishu must stop using it directly; add an interface to `headless-api` or remove the usage.

Iterate until compile is green.

- [ ] **Step 4: Run feishu tests**

```bash
mvn test -pl feishu/server
```

- [ ] **Step 5: Commit**

```bash
git add feishu/server/pom.xml
git commit -m "refactor(feishu): drop headless-server dependency

feishu/server now depends only on headless-api. All report service
access is through DTO-based interfaces; no more persistence leakage."
```

---

### Task C2: Drop `headless-server` dependency from `chat/server/pom.xml`

**File:** `/Users/xudong/git/supersonic/chat/server/pom.xml`

- [ ] **Step 1: Read and remove the `headless-server` dependency block**

Same approach as Task C1.

- [ ] **Step 2: Compile**

```bash
mvn compile -pl chat/server -am 2>&1 | tail -80
```

Same triage as C1 — iterate until green.

- [ ] **Step 3: Run chat tests**

```bash
mvn test -pl chat/server
```

- [ ] **Step 4: Commit**

```bash
git add chat/server/pom.xml
git commit -m "refactor(chat): drop headless-server dependency

chat/server now depends only on headless-api."
```

---

### Task C3: Final gate — full build + test-compile + sanity test

- [ ] **Step 1: Full build from scratch**

```bash
cd /Users/xudong/git/supersonic
mvn clean compile -pl launchers/standalone -am
```

- [ ] **Step 2: Full test-compile**

```bash
mvn test-compile -pl launchers/standalone -am
```

- [ ] **Step 3: Run all touched test classes**

```bash
mvn test -pl headless/server
mvn test -pl chat/server
mvn test -pl feishu/server
mvn test -pl launchers/standalone -Dtest='!*IntegrationTest'
```

Expected: all green.

- [ ] **Step 4: Verify the decoupling actually holds**

```bash
cd /Users/xudong/git/supersonic
# No code file under feishu/server or chat/server should reference headless.server.*
rg -n 'com\.tencent\.supersonic\.headless\.server\.' feishu/server/src chat/server/src | grep -v '/target/'
```

Expected: no output. If output exists, a leak remains — must be fixed before merging.

- [ ] **Step 5: Verify poms are clean**

```bash
rg -n 'headless-server' feishu/server/pom.xml chat/server/pom.xml
```

Expected: no output.

- [ ] **Step 6: Final commit (if any touch-ups)**

If any final adjustments were made:
```bash
git commit -m "refactor(headless): complete DTO boundary migration"
```

---

## Notes for the implementer

- **Do NOT batch commits across phases.** Each task has its own commit so bisect works if something regresses.
- **Compile fails at the boundary of each task are expected** when intermediate state leaves consumers broken. Each task explicitly calls out whether compile is expected to pass or fail, and what the expected errors look like. Read the error list, not just the exit code.
- **Preserve tests — don't delete them.** Every signature change in tests is a type rewrite, not a behavior change. If a test no longer makes sense because the API simplified, that is a discussion, not a delete.
- **If a field doesn't exist on a DTO but the consumer needs it:** add it to the DTO and to the mapper. Don't fake it with a getter in the impl.
- **The `ReportExecutionVO` / `ReportExecutionResp` split is intentional.** The plain Resp mirrors the DO for list/get endpoints; the enriched VO exists for UI flows that need `templateName`, `channelTypes`, etc. Do not collapse them.
- **YAGNI:** No MapStruct. No Spring-managed mapper beans. No interfaces for the mapper. Just static methods in `ReportDtoMappers`.
- **The build gate is `mvn compile -pl launchers/standalone -am`.** CLAUDE.md requires this after every Java change.

## Self-review checklist (ran before saving)

- [x] Phase A covers the stalled-migration compile fixes (revert DOs, move yaml, fix api imports, bulk rename dependents).
- [x] Phase B does the DTO migration narrowly (5 Resp, 3 Req, 3 interface rewrites, 3 impl rewrites, 2 consumer updates).
- [x] Phase C delivers the actual win condition (pom drop).
- [x] Every DTO lists every DO field (verified by Read of each DO).
- [x] Every service method in every rewritten interface has the same method names and argument counts as before (type-only changes).
- [x] Consumer update tasks enumerate every specific line from the review.
- [x] Mapper lives in headless-server, not headless-api (direction is safe — server depends on api).
- [x] `ReportExecutionResp` vs `ReportExecutionVO` coexistence is explicit (no drift).
- [x] No placeholder/TBD/TODO text anywhere.
- [x] `ValidDataSetResp` drift is explicitly out of scope (documented in "Out of Scope").

---

**Estimated effort:** Phase A ~1-2h (mechanical), Phase B ~3-4h (writing DTOs and rewriting impls is routine but numerous), Phase C ~1-2h (most time goes to iterating on unexpected compile failures after the pom drop). Total ~6-8h of focused work.
