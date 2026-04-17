# P2-11: Spring Profile YAML Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate all settings that are identical across `dev`, `prd`, `h2`, `mysql`, `postgres` profiles into `application.yaml`; keep profile yamls diff-only (credentials, JDBC URL/driver, Flyway location, dialect-specific overrides). Add a Spring-Boot test that fails if unintended key drift reappears.

**Architecture:** Follow the in-codebase "Spring Profile 配置分层规范 — 主 yaml 只放通用配置，环境凭据走 profile yaml" rule. Base `application.yaml` owns everything shared; `application-{h2,mysql,postgres}.yaml` carry only JDBC dialect differences; `application-{dev,prd}.yaml` carry only env/credential overrides. Add a `ProfileYamlDiffTest` that loads each profile file as raw YAML and asserts its key-set is a strict subset of an allow-list (the keys that truly differ between envs). Boot-test covers end-to-end resolved property parity with pre-refactor values.

**Tech Stack:** Spring Boot 3.4.11, SnakeYAML (brought in transitively by Spring Boot — accessed via `YamlPropertiesFactoryBean`), JUnit 5 (`spring-boot-starter-test`), `@SpringBootTest` with `ActiveProfiles`.

---

## Ground-Truth Inventory (current state, read from disk 2026-04-17)

All paths are under `launchers/standalone/src/main/resources/`.

| File | Bytes | Lines | Role |
|------|-------|-------|------|
| `application.yaml` | 8143 | 257 | Base — shared settings + SPI defaults |
| `application-dev.yaml` | 1024 | 26 | Local dev credentials (**gitignored**) |
| `application-dev.yaml.example` | 735 | 17 | Template for dev (committed) |
| `application-h2.yaml` | 560 | 16 | H2 in-memory DB |
| `application-mysql.yaml` | 1283 | 26 | MySQL JDBC + Flyway |
| `application-postgres.yaml` | 1123 | 33 | Postgres JDBC + Flyway + pgvector |
| `application-prd.yaml` | 251 | 8 | Production env-var-only credentials |

**Related files outside standalone** (report drift only, out of scope):
- `launchers/headless/src/main/resources/application{-h2,-mysql,-postgres,}.yaml` — nearly identical duplication of datasource/flyway blocks; one difference: schema file names add `-common.sql` (e.g. `schema-h2-common.sql`). Same drift-risk pattern.
- `launchers/chat/src/main/resources/application{-h2,-mysql,-postgres,}.yaml` — same pattern, chat-specific `-common.sql` schemas.
- `launchers/common/src/main/resources/` — no yamls.

Consolidating headless/chat is left for a follow-up plan because they use different schema-location lists, different ports, and different application names. Keeping this plan focused on the `standalone` launcher (the main deployment artifact built by `docker/Dockerfile`).

**Profile activation chain (must NOT change):**
- `application.yaml` line 10-12: `spring.profiles.active: ${S2_DB_TYPE:h2}` and `spring.profiles.include: ${S2_ENV:dev}`.
- `assembly/bin/supersonic-daemon.sh` line 73/103: passes `-Dspring.profiles.active="$profile"` where `$profile=$S2_DB_TYPE`.
- `docker/docker-compose.yml` line 83: sets `S2_DB_TYPE: postgres`. Prd activation in Docker relies on `S2_ENV=prd` (must be set in user env — no current default for production deployments in the tree).
- `docker/.env.example` line 8: `S2_DB_TYPE=mysql`.

---

## File Structure

Files created by this plan:

```
launchers/standalone/src/test/java/com/tencent/supersonic/config/
  ProfileYamlDiffTest.java          # NEW — YAML-level allow-list check per profile
  ResolvedPropertyParityTest.java   # NEW — @SpringBootTest parity check
docs/runbook/
  spring-profiles.md                # NEW — "how to add a new env profile" runbook
```

Files modified by this plan:

```
launchers/standalone/src/main/resources/
  application.yaml                  # MODIFY — absorb common keys + add spring.profiles.group
  application-dev.yaml              # MODIFY — strip common keys, keep creds + mail overrides
  application-dev.yaml.example      # REWRITE — mirror new application-dev.yaml shape
  application-prd.yaml              # MODIFY — strip common keys (already minimal)
  application-h2.yaml               # MODIFY — datasource/sql.init/h2/flyway only
  application-mysql.yaml            # MODIFY — datasource/sql.init/flyway only
  application-postgres.yaml         # MODIFY — datasource/sql.init/flyway + pgvector only
docker/.env.example                 # MODIFY — add S2_ENV example + clarify
docker/docker-compose.yml           # MODIFY — add S2_ENV=prd env var in standalone service
```

No file renames. No key renames. Zero breaking changes to `SPRING_PROFILES_ACTIVE=dev,mysql` style activations.

---

## Task 1: Exploration output — full key-diff matrix

**Files read:** all seven standalone yamls.

**Matrix legend:**
- `B` = key present in base `application.yaml` (will stay in base).
- `H`, `M`, `P` = key present in `application-h2.yaml`, `application-mysql.yaml`, `application-postgres.yaml`.
- `D`, `R` = key present in `application-dev.yaml`, `application-prd.yaml`.
- `—` = absent.
- Values shown only when they differ between files. When identical across profiles, "identical" is noted.

### 1a. `spring.datasource.*`

| Key | B | H | M | P | D | R |
|---|---|---|---|---|---|---|
| `spring.datasource.driver-class-name` | — | `org.h2.Driver` | `com.mysql.cj.jdbc.Driver` | `org.postgresql.Driver` | — | — |
| `spring.datasource.url` | — | `jdbc:h2:mem:semantic;DB_CLOSE_DELAY=-1;...` | `jdbc:mysql://${S2_DB_HOST:localhost}:${S2_DB_PORT:3306}/...` | `jdbc:postgresql://${S2_DB_HOST:localhost}:${S2_DB_PORT:15432}/...` | — | — |
| `spring.datasource.username` | — | `root` | `${S2_DB_USER:root}` | `${S2_DB_USER:postgres}` | — | — |
| `spring.datasource.password` | — | `semantic` | `${S2_DB_PASSWORD:}` | `${S2_DB_PASSWORD:postgres}` | — | — |

**Decision:** all four keys genuinely differ per DB dialect → **stay in profile yamls**.

### 1b. `spring.sql.init.*`

| Key | B | H | M | P | D | R |
|---|---|---|---|---|---|---|
| `spring.sql.init.mode` | — | `always` | `never` | `never` | — | — |
| `spring.sql.init.continue-on-error` | — | — | `true` | `true` | — | — |
| `spring.sql.init.username` | — | — | `${S2_DB_USER:root}` | `${S2_DB_USER:postgres}` | — | — |
| `spring.sql.init.password` | — | — | `${S2_DB_PASSWORD:}` | `${S2_DB_PASSWORD:postgres}` | — | — |
| `spring.sql.init.schema-locations` | — | `classpath:db/schema-h2.sql,classpath:db/schema-h2-demo.sql` | `classpath:db/schema-mysql.sql,classpath:db/schema-mysql-demo.sql` | `classpath:db/schema-postgres.sql,classpath:db/schema-postgres-demo.sql` | — | — |
| `spring.sql.init.data-locations` | — | `classpath:db/data-h2.sql,classpath:db/data-h2-demo.sql` | `classpath:db/data-mysql.sql,classpath:db/data-mysql-demo.sql` | `classpath:db/data-postgres.sql,classpath:db/data-postgres-demo.sql` | — | — |

**Decision:** dialect-specific → stay in profile yamls. `continue-on-error: true` can be lifted to **base** (MySQL and Postgres both set it; H2 uses `mode: always` and errors-out is desirable there — confirm by keeping `continue-on-error` explicit in H2 as `false`). **Lift to base: none.** Keep as-is.

### 1c. `spring.h2.console.*`

| Key | B | H | M | P | D | R |
|---|---|---|---|---|---|---|
| `spring.h2.console.path` | — | `/h2-console/semantic` | — | — | — | — |
| `spring.h2.console.enabled` | — | `true` | — | — | — | — |

**Decision:** H2-only → stays in `application-h2.yaml`.

### 1d. `spring.flyway.*`

| Key | B | H | M | P | D | R |
|---|---|---|---|---|---|---|
| `spring.flyway.enabled` | — | `false` | `true` | `true` | — | — |
| `spring.flyway.baseline-on-migrate` | — | — | `true` | `true` | — | — |
| `spring.flyway.baseline-version` | — | — | `${FLYWAY_BASELINE_VERSION:-1}` | `${FLYWAY_BASELINE_VERSION:-1}` | — | — |
| `spring.flyway.locations` | — | — | `classpath:db/migration/mysql` | `classpath:db/migration/postgresql` | — | — |
| `spring.flyway.table` | — | — | `flyway_schema_history` | `flyway_schema_history` | — | — |
| `spring.flyway.out-of-order` | — | — | `false` | `false` | — | — |
| `spring.flyway.validate-on-migrate` | — | — | `false` | `true` | — | — |

**Decision:**
- `baseline-on-migrate: true` — **identical in M+P** → lift to base.
- `baseline-version: ${FLYWAY_BASELINE_VERSION:-1}` — **identical in M+P** → lift to base.
- `table: flyway_schema_history` — **identical in M+P** (also Flyway's own default) → lift to base.
- `out-of-order: false` — **identical in M+P** (also default) → lift to base.
- `enabled`, `locations`, `validate-on-migrate` — genuinely differ, stay per-profile. H2's `enabled: false` stays in `application-h2.yaml`.

### 1e. `spring.mail.*`

| Key | B | H | M | P | D | R |
|---|---|---|---|---|---|---|
| `spring.mail.host` | `${EMAIL_HOST}` | — | — | — | `${EMAIL_HOST:smtp.qiye.aliyun.com}` | — |
| `spring.mail.port` | `${EMAIL_PORT}` | — | — | — | `${EMAIL_PORT:465}` | — |
| `spring.mail.username` | `${EMAIL_USERNAME}` | — | — | — | `${EMAIL_USERNAME:xxzx@zkbc.net}` | — |
| `spring.mail.password` | `${EMAIL_PASSWORD}` | — | — | — | `${EMAIL_PASSWORD:hkXO1riJ5Ypd}` | — |
| `spring.mail.protocol` | `smtp` | — | — | — | `smtps` | — |
| `spring.mail.properties.mail.smtp.auth` | `${EMAIL_SMTP_AUTH:true}` | — | — | — | `true` | — |
| `spring.mail.properties.mail.smtp.ssl.enable` | `${EMAIL_SSL_ENABLE:false}` | — | — | — | `true` | — |
| `spring.mail.properties.mail.smtp.timeout` | `${EMAIL_TIMEOUT:5000}` | — | — | — | `10000` | — |
| `spring.mail.properties.mail.smtp.connectiontimeout` | `${EMAIL_CONNECTION_TIMEOUT:5000}` | — | — | — | `10000` | — |

**Decision:** base already defines these as env-var references with safe defaults. Dev overrides are **convenience local defaults**. Keep the dev override, since it's a true local override. **Lift nothing.**

### 1f. `s2.encryption.*` and `s2.feishu.*` credentials

| Key | B | D | R | `.example` |
|---|---|---|---|---|
| `s2.encryption.aes-key` | — | `${S2_ENCRYPTION_KEY:9f86d081...}` | `${S2_ENCRYPTION_KEY}` | `${S2_ENCRYPTION_KEY:}` |
| `s2.encryption.aes-iv` | — | `${S2_ENCRYPTION_IV:supersonic@bicom}` | `${S2_ENCRYPTION_IV}` | `${S2_ENCRYPTION_IV:}` |
| `s2.feishu.app-id` | — | `${FEISHU_APP_ID:cli_a9...}` | `${FEISHU_APP_ID}` | `${FEISHU_APP_ID:}` |
| `s2.feishu.app-secret` | — | `${FEISHU_APP_SECRET:...}` | `${FEISHU_APP_SECRET}` | `${FEISHU_APP_SECRET:}` |
| `s2.feishu.verification-token` | — | `${FEISHU_VERIFICATION_TOKEN:...}` | `${FEISHU_VERIFICATION_TOKEN}` | `${FEISHU_VERIFICATION_TOKEN:}` |
| `s2.feishu.encrypt-key` | — | `${FEISHU_ENCRYPT_KEY:SageData}` | `${FEISHU_ENCRYPT_KEY}` | `${FEISHU_ENCRYPT_KEY:}` |

**Decision:** Credentials. **Must** stay per-profile. Dev has dev-local default values (for unauthenticated local testing); prd requires env var with no default. Base `application.yaml` must **not** define these.

### 1g. `s2.embedding.store.*` (pgvector)

| Key | B | H | M | P | D | R |
|---|---|---|---|---|---|---|
| `s2.embedding.store.provider` | — | — | — | `PGVECTOR` | — | — |
| `s2.embedding.store.base.url` | — | — | — | `${S2_DB_HOST:127.0.0.1}` | — | — |
| `s2.embedding.store.port` | — | — | — | `${S2_DB_PORT:15432}` | — | — |
| `s2.embedding.store.databaseName` | — | — | — | `${S2_DB_DATABASE:postgres}` | — | — |
| `s2.embedding.store.user` | — | — | — | `${S2_DB_USER:postgres}` | — | — |
| `s2.embedding.store.password` | — | — | — | `${S2_DB_PASSWORD:postgres}` | — | — |
| `s2.embedding.store.dimension` | — | — | — | `512` | — | — |

**Decision:** Postgres-only (pgvector). Stays in `application-postgres.yaml`. No change.

### 1h. Everything else in base (`application.yaml`)

All remaining blocks — `server.*`, `spring.application.name`, `spring.lifecycle`, `spring.task`, `spring.config.import`, `spring.data.redis.*`, `spring.main.allow-circular-references`, `spring.mvc`, `spring.quartz`, `mybatis`, `management`, `logging.level.*`, `springdoc`, `knife4j`, `s2.oauth`, `s2.permission`, `s2.tenant`, `s2.feishu.enabled/connection-mode/api-base-url/default-agent-id/query-timeout-ms/max-table-rows/user-mapping/export/cache`, `s2.report-download`, `s2.export`, `s2.report`, `s2.slo`, `s2.data-sync` — are **only** in base. **No changes to these.**

### 1i. Keys to lift into `application.yaml` (summary)

Four keys from `spring.flyway.*` lift from M+P into base:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: ${FLYWAY_BASELINE_VERSION:-1}
    table: flyway_schema_history
    out-of-order: false
```

H2 still overrides `spring.flyway.enabled: false`; MySQL/Postgres still override `enabled: true`, `locations:`, and `validate-on-migrate:` (the one key they legitimately disagree on).

### 1j. Discovered drift / bugs

1. **`application-dev.yaml.example` drifted from actual `application-dev.yaml`** — the example is missing the entire `spring.mail` block that the real dev yaml has. A new developer copying the example gets no mail config. Task 5 fixes this.
2. **`spring.mail.properties.mail.smtp.starttls.*` keys are in base but not dev** — dev overrides `ssl.enable: true` without explicitly turning off `starttls`. Spring sorts this out at runtime (SSL has priority), but we'll document it in the runbook. Not a bug per se.
3. **`logging.level.root` missing in prd** — `OPS-DEPLOY-IMPROVEMENTS.md` already notes this. Out of scope for this plan (no behavior change requested), but logged in the runbook as a "consider" item.
4. **Docker compose does not set `S2_ENV`** — so by Spring's `${S2_ENV:dev}` default, the deployed container activates the `dev` profile, **including dev-credential defaults**. Mitigated by: prd environments must set `S2_ENV=prd`. Task 6 adds `S2_ENV: prd` to `docker-compose.yml` to make the intended prd activation explicit.
5. **`continue-on-error: true`** on `spring.sql.init` is mysql+postgres only. Intentional (schema-init is a no-op there since `mode: never`), and H2 relies on clean init — no change.

---

## Task 2: Write YAML-diff allow-list test (TDD — red first)

**Files:**
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/config/ProfileYamlDiffTest.java`

- [ ] **Step 1: Write the failing test**

Create `launchers/standalone/src/test/java/com/tencent/supersonic/config/ProfileYamlDiffTest.java`:

```java
package com.tencent.supersonic.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Enforces the "application.yaml = shared; profile yaml = diff-only" rule.
 *
 * For each profile, declares the exact top-level key prefixes the profile yaml
 * is ALLOWED to define. If a profile yaml starts defining new keys that should
 * have been lifted to base, this test fails with a clear message.
 *
 * Rationale: see docs/runbook/spring-profiles.md and user memory
 * "Spring Profile 配置分层规范".
 */
class ProfileYamlDiffTest {

    /** Per-profile allow-lists. Every dotted key in the profile yaml must start with one of these prefixes. */
    private static final Map<String, List<String>> ALLOWED_PREFIXES = Map.of(
            "application-h2.yaml", List.of(
                    "spring.datasource.",
                    "spring.sql.init.",
                    "spring.h2.console.",
                    "spring.flyway.enabled"
            ),
            "application-mysql.yaml", List.of(
                    "spring.datasource.",
                    "spring.sql.init.",
                    "spring.flyway.enabled",
                    "spring.flyway.locations",
                    "spring.flyway.validate-on-migrate"
            ),
            "application-postgres.yaml", List.of(
                    "spring.datasource.",
                    "spring.sql.init.",
                    "spring.flyway.enabled",
                    "spring.flyway.locations",
                    "spring.flyway.validate-on-migrate",
                    "s2.embedding.store."
            ),
            "application-dev.yaml", List.of(
                    "s2.encryption.",
                    "s2.feishu.app-id",
                    "s2.feishu.app-secret",
                    "s2.feishu.verification-token",
                    "s2.feishu.encrypt-key",
                    "spring.mail."
            ),
            "application-prd.yaml", List.of(
                    "s2.encryption.",
                    "s2.feishu.app-id",
                    "s2.feishu.app-secret",
                    "s2.feishu.verification-token",
                    "s2.feishu.encrypt-key"
            )
    );

    @Test
    void profileYamlsOnlyDefineAllowedKeys() {
        for (Map.Entry<String, List<String>> entry : ALLOWED_PREFIXES.entrySet()) {
            String fileName = entry.getKey();
            List<String> allowedPrefixes = entry.getValue();
            Set<String> actualKeys = loadFlatKeys(fileName);

            Set<String> violations = actualKeys.stream()
                    .filter(k -> allowedPrefixes.stream().noneMatch(k::startsWith))
                    .collect(Collectors.toCollection(TreeSet::new));

            if (!violations.isEmpty()) {
                fail(String.format(
                        "%s defines keys not in the allow-list — lift them to application.yaml or extend ALLOWED_PREFIXES if genuinely profile-specific:%n  %s%nAllowed prefixes: %s",
                        fileName,
                        String.join("\n  ", violations),
                        allowedPrefixes));
            }
        }
    }

    @Test
    void baseYamlDoesNotDefineProfileOnlyKeys() {
        Set<String> baseKeys = loadFlatKeys("application.yaml");

        // These prefixes must NEVER appear in the base yaml — they belong to profile files.
        List<String> bannedInBase = List.of(
                "spring.datasource.",
                "spring.sql.init.",
                "spring.h2.console.",
                "spring.flyway.locations",
                "spring.flyway.validate-on-migrate",
                "s2.embedding.store.",
                "s2.encryption.",
                "s2.feishu.app-id",
                "s2.feishu.app-secret",
                "s2.feishu.verification-token",
                "s2.feishu.encrypt-key"
        );

        Set<String> leaks = new LinkedHashSet<>();
        for (String key : baseKeys) {
            for (String banned : bannedInBase) {
                if (key.startsWith(banned)) {
                    leaks.add(key);
                }
            }
        }
        assertTrue(leaks.isEmpty(),
                "application.yaml must not define profile-only keys: " + leaks);
    }

    @Test
    void flywayBaselineLiftedToBase() {
        Set<String> baseKeys = loadFlatKeys("application.yaml");
        assertTrue(baseKeys.contains("spring.flyway.baseline-on-migrate"),
                "spring.flyway.baseline-on-migrate should be defined in application.yaml (shared by mysql+postgres)");
        assertTrue(baseKeys.contains("spring.flyway.baseline-version"),
                "spring.flyway.baseline-version should be defined in application.yaml");
        assertTrue(baseKeys.contains("spring.flyway.table"),
                "spring.flyway.table should be defined in application.yaml");
        assertTrue(baseKeys.contains("spring.flyway.out-of-order"),
                "spring.flyway.out-of-order should be defined in application.yaml");
    }

    private Set<String> loadFlatKeys(String classpathName) {
        Resource res = new ClassPathResource(classpathName);
        if (!res.exists()) {
            throw new IllegalStateException("Missing yaml on classpath: " + classpathName);
        }
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(res);
        Properties props = yaml.getObject();
        if (props == null) {
            return Set.of();
        }
        return props.stringPropertyNames();
    }
}
```

- [ ] **Step 2: Run test — expect the `flywayBaselineLiftedToBase` case to fail (red)**

Run: `mvn -pl launchers/standalone test -Dtest=ProfileYamlDiffTest -DfailIfNoTests=false`

Expected:
- `profileYamlsOnlyDefineAllowedKeys` — FAILS. `application-mysql.yaml` still defines `spring.flyway.baseline-on-migrate`, `spring.flyway.baseline-version`, `spring.flyway.table`, `spring.flyway.out-of-order` which are NOT in the mysql allow-list. Same for postgres.
- `baseYamlDoesNotDefineProfileOnlyKeys` — PASSES (base doesn't yet contain leaks).
- `flywayBaselineLiftedToBase` — FAILS. Base yaml doesn't yet contain those keys.

This is the expected TDD red state.

- [ ] **Step 3: Commit the failing test**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/config/ProfileYamlDiffTest.java
git commit -m "test(config): add yaml-diff allow-list test for Spring profile consolidation"
```

---

## Task 3: Lift shared Flyway keys into `application.yaml`

**Files:**
- Modify: `launchers/standalone/src/main/resources/application.yaml`
- Modify: `launchers/standalone/src/main/resources/application-mysql.yaml`
- Modify: `launchers/standalone/src/main/resources/application-postgres.yaml`

### 3.1. `application.yaml` — add a top-level `spring.flyway` block

- [ ] **Step 1: Add the `spring.flyway` block after `spring.quartz` in the base yaml.**

Insert this block in `launchers/standalone/src/main/resources/application.yaml` between line 52 (end of `spring.quartz` block) and line 54 (start of `spring.mail`):

**Before** (lines 42-54 context):

```yaml
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: embedded
    properties:
      org.quartz.scheduler.instanceName: SuperSonicScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.clusterCheckinInterval: 15000
      org.quartz.jobStore.misfireThreshold: 60000
      org.quartz.threadPool.threadCount: 5

  mail:
```

**After:**

```yaml
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: embedded
    properties:
      org.quartz.scheduler.instanceName: SuperSonicScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.clusterCheckinInterval: 15000
      org.quartz.jobStore.misfireThreshold: 60000
      org.quartz.threadPool.threadCount: 5

  # Flyway: shared settings across mysql+postgres profiles.
  # Per-DB overrides (enabled, locations, validate-on-migrate) live in application-{mysql,postgres,h2}.yaml.
  flyway:
    baseline-on-migrate: true
    # 基线版本：已有数据库从此版本开始；新部署设置为 -1（执行所有迁移）。
    baseline-version: ${FLYWAY_BASELINE_VERSION:-1}
    table: flyway_schema_history
    # 允许乱序执行（用于修复脚本）
    out-of-order: false

  mail:
```

Exact `Edit` call:

```
old_string:
      org.quartz.threadPool.threadCount: 5

  mail:
new_string:
      org.quartz.threadPool.threadCount: 5

  # Flyway: shared settings across mysql+postgres profiles.
  # Per-DB overrides (enabled, locations, validate-on-migrate) live in application-{mysql,postgres,h2}.yaml.
  flyway:
    baseline-on-migrate: true
    # 基线版本：已有数据库从此版本开始；新部署设置为 -1（执行所有迁移）。
    baseline-version: ${FLYWAY_BASELINE_VERSION:-1}
    table: flyway_schema_history
    # 允许乱序执行（用于修复脚本）
    out-of-order: false

  mail:
```

### 3.2. `application-mysql.yaml` — replace whole file with diff-only version

- [ ] **Step 2: Rewrite `application-mysql.yaml`.**

**Before** (26 lines — see Task 1 "1d" for matrix):

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${S2_DB_HOST:localhost}:${S2_DB_PORT:3306}/${S2_DB_DATABASE:mysql}?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&useSSL=false&allowMultiQueries=true&allowPublicKeyRetrieval=true
    username: ${S2_DB_USER:root}
    password: ${S2_DB_PASSWORD:}
  sql:
    init:
      continue-on-error: true
      mode: never
      username: ${S2_DB_USER:root}
      password: ${S2_DB_PASSWORD:}
      schema-locations: classpath:db/schema-mysql.sql,classpath:db/schema-mysql-demo.sql
      data-locations: classpath:db/data-mysql.sql,classpath:db/data-mysql-demo.sql
  flyway:
    enabled: true
    # 对于已有数据库，设置为 true 跳过已执行的迁移
    baseline-on-migrate: true
    # 基线版本：已有数据库从此版本开始
    # 新部署设置为 -1（执行所有迁移），已有数据库设置为实际版本
    baseline-version: ${FLYWAY_BASELINE_VERSION:-1}
    locations: classpath:db/migration/mysql
    table: flyway_schema_history
    # 允许乱序执行（用于修复脚本）
    out-of-order: false
    # 验证迁移脚本（关闭以允许修复已有脚本的 display width deprecation warning）
    validate-on-migrate: false
```

**After** (17 lines — only MySQL-specific overrides remain; shared flyway keys inherited from base):

```yaml
# MySQL dialect — JDBC + dialect-specific Flyway overrides only.
# Shared Flyway settings (baseline-on-migrate, baseline-version, table, out-of-order) live in application.yaml.
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${S2_DB_HOST:localhost}:${S2_DB_PORT:3306}/${S2_DB_DATABASE:mysql}?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&useSSL=false&allowMultiQueries=true&allowPublicKeyRetrieval=true
    username: ${S2_DB_USER:root}
    password: ${S2_DB_PASSWORD:}
  sql:
    init:
      continue-on-error: true
      mode: never
      username: ${S2_DB_USER:root}
      password: ${S2_DB_PASSWORD:}
      schema-locations: classpath:db/schema-mysql.sql,classpath:db/schema-mysql-demo.sql
      data-locations: classpath:db/data-mysql.sql,classpath:db/data-mysql-demo.sql
  flyway:
    enabled: true
    locations: classpath:db/migration/mysql
    # 关闭以允许修复已有脚本的 display width deprecation warning
    validate-on-migrate: false
```

Replace the entire file with the "After" contents using `Write` (after first `Read`-ing).

### 3.3. `application-postgres.yaml` — diff-only rewrite

- [ ] **Step 3: Rewrite `application-postgres.yaml`.**

**Before** (33 lines — see current contents).

**After** (23 lines):

```yaml
# Postgres dialect — JDBC + dialect-specific Flyway + pgvector embedding store.
# Shared Flyway settings (baseline-on-migrate, baseline-version, table, out-of-order) live in application.yaml.
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${S2_DB_HOST:localhost}:${S2_DB_PORT:15432}/${S2_DB_DATABASE:postgres}?stringtype=unspecified
    username: ${S2_DB_USER:postgres}
    password: ${S2_DB_PASSWORD:postgres}
  sql:
    init:
      continue-on-error: true
      mode: never
      username: ${S2_DB_USER:postgres}
      password: ${S2_DB_PASSWORD:postgres}
      schema-locations: classpath:db/schema-postgres.sql,classpath:db/schema-postgres-demo.sql
      data-locations: classpath:db/data-postgres.sql,classpath:db/data-postgres-demo.sql
  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql
    validate-on-migrate: true

s2:
  embedding:
    store:
      provider: PGVECTOR
      base:
        url: ${S2_DB_HOST:127.0.0.1}
      port: ${S2_DB_PORT:15432}
      databaseName: ${S2_DB_DATABASE:postgres}
      user: ${S2_DB_USER:postgres}
      password: ${S2_DB_PASSWORD:postgres}
      dimension: 512
```

Replace the entire file with the "After" contents using `Write`.

### 3.4. `application-h2.yaml` — no key-set change, but add header comment and confirm

- [ ] **Step 4: Add header comment to `application-h2.yaml` (no behavior change).**

Insert two comment lines at the top of the existing file:

**Before** (first line):

```yaml
spring:
```

**After:**

```yaml
# H2 in-memory DB — dev/CI only. Flyway disabled because H2 schema is bootstrapped from schema-h2.sql.
spring:
```

Exact `Edit` call:

```
old_string: spring:
  datasource:
    driver-class-name: org.h2.Driver
new_string: # H2 in-memory DB — dev/CI only. Flyway disabled because H2 schema is bootstrapped from schema-h2.sql.
spring:
  datasource:
    driver-class-name: org.h2.Driver
```

### 3.5. Run `ProfileYamlDiffTest` — expect green

- [ ] **Step 5: Run the yaml-diff test.**

Run: `mvn -pl launchers/standalone test -Dtest=ProfileYamlDiffTest -DfailIfNoTests=false`

Expected: all three test cases PASS.

- [ ] **Step 6: Run compile to make sure nothing else broke.**

Run: `mvn compile -pl launchers/standalone -am`

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit.**

```bash
git add launchers/standalone/src/main/resources/application.yaml \
        launchers/standalone/src/main/resources/application-mysql.yaml \
        launchers/standalone/src/main/resources/application-postgres.yaml \
        launchers/standalone/src/main/resources/application-h2.yaml
git commit -m "$(cat <<'EOF'
refactor(config): lift shared Flyway keys to application.yaml

Moves baseline-on-migrate, baseline-version, table, out-of-order from
application-mysql.yaml and application-postgres.yaml into base
application.yaml. Per-profile yamls now carry only dialect-specific
overrides (enabled, locations, validate-on-migrate, JDBC creds).

Cuts mysql profile from 26→17 lines and postgres from 33→23 lines.
No runtime behavior change — resolved property values are identical.

Enforced by ProfileYamlDiffTest allow-list check.
EOF
)"
```

---

## Task 4: Add `spring.profiles.group` for cleaner activation

**Files:**
- Modify: `launchers/standalone/src/main/resources/application.yaml`

**Rationale:** Spring 2.4+ supports `spring.profiles.group.<group> = <child profiles>` to let a single "meta" profile activate multiple children. Current setup relies on `spring.profiles.active=${S2_DB_TYPE:h2}` plus `spring.profiles.include=${S2_ENV:dev}` — two env vars. Profile groups let operators activate `prd` and get the mysql-specific overrides chained in, if we choose. **But** since `S2_DB_TYPE` and `S2_ENV` are independent (prod can run on mysql OR postgres), we keep them independent and use groups **only** to decouple "dev" from "local credential file" so a future `application-secrets.yaml` can be pulled in without touching `S2_ENV`.

Concrete proposal: introduce an optional `local` group that includes a `secrets` profile, so developers can activate `SPRING_PROFILES_ACTIVE=mysql,local` and get both dev defaults + any `application-secrets.yaml` overrides.

Decision: **do not add groups in this plan**. Premature — no consumer exists yet. Leave the runbook note that future multi-profile setups can use `spring.profiles.group`.

- [ ] **Step 1: Add a comment in `application.yaml` explaining group support is available but unused.**

**Before** (lines 9-13):

```yaml
spring:
  profiles:
    active: ${S2_DB_TYPE:h2}
    include: ${S2_ENV:dev}
  application:
    name: chat
```

**After:**

```yaml
spring:
  profiles:
    # active = DB dialect (h2 | mysql | postgres); include = env (dev | prd).
    # These two axes are independent — DO NOT couple them via spring.profiles.group.
    # See docs/runbook/spring-profiles.md for activation matrix.
    active: ${S2_DB_TYPE:h2}
    include: ${S2_ENV:dev}
  application:
    name: chat
```

Exact `Edit` call:

```
old_string:
spring:
  profiles:
    active: ${S2_DB_TYPE:h2}
    include: ${S2_ENV:dev}
  application:
    name: chat
new_string:
spring:
  profiles:
    # active = DB dialect (h2 | mysql | postgres); include = env (dev | prd).
    # These two axes are independent — DO NOT couple them via spring.profiles.group.
    # See docs/runbook/spring-profiles.md for activation matrix.
    active: ${S2_DB_TYPE:h2}
    include: ${S2_ENV:dev}
  application:
    name: chat
```

- [ ] **Step 2: Commit.**

```bash
git add launchers/standalone/src/main/resources/application.yaml
git commit -m "docs(config): annotate spring.profiles axes in application.yaml"
```

---

## Task 5: Fix `application-dev.yaml.example` drift

**Files:**
- Modify: `launchers/standalone/src/main/resources/application-dev.yaml.example`

**Context:** actual `application-dev.yaml` has a `spring.mail` block; the `.example` template does not. New contributors copying the example lose mail defaults silently. Task 1 diff step 1j item (1) flagged this.

- [ ] **Step 1: Rewrite the example to mirror the dev yaml shape.**

**Before** (17 lines):

```yaml
# 模板：复制为同目录下的 application-dev.yaml 后再改（application-dev.yaml 已 gitignore，不会被拉取覆盖）
#
#   cp launchers/standalone/src/main/resources/application-dev.yaml.example \
#      launchers/standalone/src/main/resources/application-dev.yaml
#
# 激活方式：application.yaml 中 spring.profiles.include 默认含 dev（S2_ENV:dev）。
# 密钥请用环境变量或在 application-dev.yaml 内填写本地值（勿提交该文件）。

s2:
  encryption:
    aes-key: ${S2_ENCRYPTION_KEY:}
    aes-iv: ${S2_ENCRYPTION_IV:}
  feishu:
    app-id: ${FEISHU_APP_ID:}
    app-secret: ${FEISHU_APP_SECRET:}
    verification-token: ${FEISHU_VERIFICATION_TOKEN:}
    encrypt-key: ${FEISHU_ENCRYPT_KEY:}
```

**After:**

```yaml
# 模板：复制为同目录下的 application-dev.yaml 后再改（application-dev.yaml 已 gitignore，不会被拉取覆盖）
#
#   cp launchers/standalone/src/main/resources/application-dev.yaml.example \
#      launchers/standalone/src/main/resources/application-dev.yaml
#
# 激活方式：application.yaml 中 spring.profiles.include 默认含 dev（S2_ENV:dev）。
# 密钥请用环境变量或在 application-dev.yaml 内填写本地值（勿提交该文件）。
#
# Allowed key prefixes in this file (enforced by ProfileYamlDiffTest):
#   - s2.encryption.*
#   - s2.feishu.app-id / app-secret / verification-token / encrypt-key
#   - spring.mail.*

s2:
  encryption:
    aes-key: ${S2_ENCRYPTION_KEY:}
    aes-iv: ${S2_ENCRYPTION_IV:}
  feishu:
    app-id: ${FEISHU_APP_ID:}
    app-secret: ${FEISHU_APP_SECRET:}
    verification-token: ${FEISHU_VERIFICATION_TOKEN:}
    encrypt-key: ${FEISHU_ENCRYPT_KEY:}

# Optional: override mail defaults for local SMTP testing.
# Base application.yaml already reads ${EMAIL_HOST}, ${EMAIL_PORT}, etc. from env vars.
# Uncomment below only if you want hard-coded local defaults.
#spring:
#  mail:
#    host: ${EMAIL_HOST:smtp.example.com}
#    port: ${EMAIL_PORT:465}
#    username: ${EMAIL_USERNAME:}
#    password: ${EMAIL_PASSWORD:}
#    protocol: smtps
#    properties:
#      mail.smtp.auth: true
#      mail.smtp.ssl.enable: true
#      mail.smtp.timeout: 10000
#      mail.smtp.connectiontimeout: 10000
```

Replace the whole file via `Write`.

- [ ] **Step 2: Verify the example file is still valid YAML.**

Run: `mvn -pl launchers/standalone test -Dtest=ProfileYamlDiffTest -DfailIfNoTests=false`

The test only scans classpath-loaded yamls (not `.example`) so this confirms the test still passes. The template is valid YAML because everything after `# Optional:` is commented.

Additional sanity check (not tracked by tests, manual smoke):

Run: `python3 -c "import yaml; yaml.safe_load(open('launchers/standalone/src/main/resources/application-dev.yaml.example'))"`

Expected: no exception. (Python yaml is pre-installed on macOS — if not, substitute any yaml linter.)

- [ ] **Step 3: Commit.**

```bash
git add launchers/standalone/src/main/resources/application-dev.yaml.example
git commit -m "docs(config): sync application-dev.yaml.example with dev yaml shape"
```

---

## Task 6: Update Docker files

**Files:**
- Modify: `docker/docker-compose.yml`
- Modify: `docker/.env.example`

**Rationale:** Currently `docker-compose.yml` sets `S2_DB_TYPE: postgres` but no `S2_ENV`. Without `S2_ENV=prd`, Spring defaults to `dev`, which in turn loads `application-dev.yaml` (now strictly optional because it's gitignored — in Docker images built via `docker/Dockerfile`, the file is absent, so Spring just skips it). This works but is implicit. Make it explicit: in `docker-compose.yml` force `S2_ENV=prd` so the profile activation matches intent.

- [ ] **Step 1: Add `S2_ENV` to docker-compose standalone environment.**

**Before** (lines 82-92 of `docker/docker-compose.yml`):

```yaml
  supersonic_standalone:
    image: supersonicbi/supersonic:${SUPERSONIC_VERSION:-latest}
    privileged: true
    container_name: supersonic_standalone
    environment:
      S2_DB_TYPE: postgres
      S2_DB_HOST: supersonic_postgres
      S2_DB_PORT: 5432
      S2_DB_DATABASE: postgres
      S2_DB_USER: supersonic_user
      S2_DB_PASSWORD: supersonic_password
      REDIS_HOST: supersonic_redis
      REDIS_PORT: 6379
      FEISHU_CACHE_TYPE: redis
      S2_OAUTH_STORAGE_TYPE: redis
```

**After:**

```yaml
  supersonic_standalone:
    image: supersonicbi/supersonic:${SUPERSONIC_VERSION:-latest}
    privileged: true
    container_name: supersonic_standalone
    environment:
      # Profile axes (see docs/runbook/spring-profiles.md):
      #   S2_DB_TYPE → spring.profiles.active (h2 | mysql | postgres)
      #   S2_ENV     → spring.profiles.include (dev | prd)
      S2_DB_TYPE: postgres
      S2_ENV: prd
      S2_DB_HOST: supersonic_postgres
      S2_DB_PORT: 5432
      S2_DB_DATABASE: postgres
      S2_DB_USER: supersonic_user
      S2_DB_PASSWORD: supersonic_password
      REDIS_HOST: supersonic_redis
      REDIS_PORT: 6379
      FEISHU_CACHE_TYPE: redis
      S2_OAUTH_STORAGE_TYPE: redis
```

Exact `Edit` call:

```
old_string:
    environment:
      S2_DB_TYPE: postgres
      S2_DB_HOST: supersonic_postgres
      S2_DB_PORT: 5432
      S2_DB_DATABASE: postgres
      S2_DB_USER: supersonic_user
      S2_DB_PASSWORD: supersonic_password
      REDIS_HOST: supersonic_redis
      REDIS_PORT: 6379
      FEISHU_CACHE_TYPE: redis
      S2_OAUTH_STORAGE_TYPE: redis
new_string:
    environment:
      # Profile axes (see docs/runbook/spring-profiles.md):
      #   S2_DB_TYPE → spring.profiles.active (h2 | mysql | postgres)
      #   S2_ENV     → spring.profiles.include (dev | prd)
      S2_DB_TYPE: postgres
      S2_ENV: prd
      S2_DB_HOST: supersonic_postgres
      S2_DB_PORT: 5432
      S2_DB_DATABASE: postgres
      S2_DB_USER: supersonic_user
      S2_DB_PASSWORD: supersonic_password
      REDIS_HOST: supersonic_redis
      REDIS_PORT: 6379
      FEISHU_CACHE_TYPE: redis
      S2_OAUTH_STORAGE_TYPE: redis
```

- [ ] **Step 2: Update `docker/.env.example` with `S2_ENV` documentation.**

**Before** (lines 7-13):

```
# ── Database (shared by MySQL container + SuperSonic) ─────
S2_DB_TYPE=mysql
S2_DB_DATABASE=supersonic
S2_DB_USER=supersonic_user
S2_DB_PASSWORD=supersonic_password
MYSQL_ROOT_PASSWORD=root_password
MYSQL_PORT=13306
```

**After:**

```
# ── Profile (Spring) ─────────────────────────────────────
# S2_DB_TYPE → spring.profiles.active (h2 | mysql | postgres)
# S2_ENV     → spring.profiles.include (dev | prd) — default dev
S2_ENV=prd

# ── Database (shared by MySQL container + SuperSonic) ─────
S2_DB_TYPE=mysql
S2_DB_DATABASE=supersonic
S2_DB_USER=supersonic_user
S2_DB_PASSWORD=supersonic_password
MYSQL_ROOT_PASSWORD=root_password
MYSQL_PORT=13306
```

Exact `Edit` call:

```
old_string:
# ── Database (shared by MySQL container + SuperSonic) ─────
S2_DB_TYPE=mysql
new_string:
# ── Profile (Spring) ─────────────────────────────────────
# S2_DB_TYPE → spring.profiles.active (h2 | mysql | postgres)
# S2_ENV     → spring.profiles.include (dev | prd) — default dev
S2_ENV=prd

# ── Database (shared by MySQL container + SuperSonic) ─────
S2_DB_TYPE=mysql
```

- [ ] **Step 3: Verify docker-compose.yml still parses.**

Run: `docker compose -f docker/docker-compose.yml config > /dev/null`

Expected: no error output. (If the engineer has no docker, skip — this is a smoke check.)

- [ ] **Step 4: Commit.**

```bash
git add docker/docker-compose.yml docker/.env.example
git commit -m "chore(docker): set S2_ENV=prd explicitly in standalone compose service"
```

---

## Task 7: End-to-end parity test — resolved properties unchanged

**Files:**
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/config/ResolvedPropertyParityTest.java`

**Rationale:** Allow-list test in Task 2 only checks the per-file key-set. This test boots a `StandaloneLauncher` context with `mysql,prd` and `postgres,prd` active, and asserts the critical resolved properties match the exact values they would have had pre-refactor. This catches the "we accidentally lifted a key that differed between mysql and postgres" failure mode.

- [ ] **Step 1: Write the test file.**

Create `launchers/standalone/src/test/java/com/tencent/supersonic/config/ResolvedPropertyParityTest.java`:

```java
package com.tencent.supersonic.config;

import com.tencent.supersonic.StandaloneLauncher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boot the application with each (db, env) profile pair and assert critical
 * properties resolve to the exact same values as before the consolidation
 * refactor. If a key we thought was identical across profiles actually wasn't,
 * this test catches the regression before it ships.
 *
 * NOTE: we only test three "known-good" profile combinations — adding a new
 * profile requires adding a nested class here.
 */
class ResolvedPropertyParityTest {

    @Nested
    @SpringBootTest(classes = StandaloneLauncher.class,
            properties = {"S2_DB_TYPE=h2", "S2_ENV=dev"},
            webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles({"h2", "dev"})
    class H2DevProfile {

        @Autowired
        Environment env;

        @Test
        void flywayDisabledOnH2() {
            assertEquals("false", env.getProperty("spring.flyway.enabled"));
        }

        @Test
        void h2DatasourceWired() {
            assertEquals("org.h2.Driver", env.getProperty("spring.datasource.driver-class-name"));
            assertTrue(env.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:semantic"));
        }

        @Test
        void baseFlywayKeysStillPresent() {
            // Even though enabled=false, base flyway block should resolve:
            assertEquals("true", env.getProperty("spring.flyway.baseline-on-migrate"));
            assertEquals("-1", env.getProperty("spring.flyway.baseline-version"));
            assertEquals("flyway_schema_history", env.getProperty("spring.flyway.table"));
            assertEquals("false", env.getProperty("spring.flyway.out-of-order"));
        }
    }

    @Nested
    @SpringBootTest(classes = StandaloneLauncher.class,
            properties = {"S2_DB_TYPE=mysql", "S2_ENV=prd"},
            webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles({"mysql", "prd"})
    class MysqlPrdProfile {

        @Autowired
        Environment env;

        @Test
        void mysqlFlywayOverridesApplied() {
            assertEquals("true", env.getProperty("spring.flyway.enabled"));
            assertEquals("classpath:db/migration/mysql", env.getProperty("spring.flyway.locations"));
            assertEquals("false", env.getProperty("spring.flyway.validate-on-migrate"));
        }

        @Test
        void mysqlInheritsBaseFlywaySettings() {
            assertEquals("true", env.getProperty("spring.flyway.baseline-on-migrate"));
            assertEquals("-1", env.getProperty("spring.flyway.baseline-version"));
            assertEquals("flyway_schema_history", env.getProperty("spring.flyway.table"));
            assertEquals("false", env.getProperty("spring.flyway.out-of-order"));
        }

        @Test
        void mysqlJdbcUrlResolves() {
            String url = env.getProperty("spring.datasource.url");
            assertTrue(url != null && url.startsWith("jdbc:mysql://"),
                    "Expected MySQL JDBC URL, got: " + url);
            assertTrue(url.contains("connectionCollation=utf8mb4_unicode_ci"),
                    "MySQL URL must preserve UTF-8 collation param: " + url);
        }

        @Test
        void criticalSharedPropertiesUnchanged() {
            assertEquals("9080", env.getProperty("server.port"));
            assertEquals("chat", env.getProperty("spring.application.name"));
            assertEquals("graceful", env.getProperty("server.shutdown"));
            assertEquals("true", env.getProperty("s2.tenant.enabled"));
            assertEquals("X-Tenant-Id", env.getProperty("s2.tenant.tenant-id-header"));
        }
    }

    @Nested
    @SpringBootTest(classes = StandaloneLauncher.class,
            properties = {"S2_DB_TYPE=postgres", "S2_ENV=prd"},
            webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles({"postgres", "prd"})
    class PostgresPrdProfile {

        @Autowired
        Environment env;

        @Test
        void postgresFlywayOverridesApplied() {
            assertEquals("true", env.getProperty("spring.flyway.enabled"));
            assertEquals("classpath:db/migration/postgresql", env.getProperty("spring.flyway.locations"));
            assertEquals("true", env.getProperty("spring.flyway.validate-on-migrate"));
        }

        @Test
        void postgresInheritsBaseFlywaySettings() {
            assertEquals("true", env.getProperty("spring.flyway.baseline-on-migrate"));
            assertEquals("-1", env.getProperty("spring.flyway.baseline-version"));
            assertEquals("flyway_schema_history", env.getProperty("spring.flyway.table"));
            assertEquals("false", env.getProperty("spring.flyway.out-of-order"));
        }

        @Test
        void pgvectorProviderWired() {
            assertEquals("PGVECTOR", env.getProperty("s2.embedding.store.provider"));
            assertEquals("512", env.getProperty("s2.embedding.store.dimension"));
        }
    }
}
```

- [ ] **Step 2: Run the test.**

Run: `mvn -pl launchers/standalone test -Dtest=ResolvedPropertyParityTest -DfailIfNoTests=false`

Expected: all three nested test classes PASS, covering:
- H2+dev: flyway disabled, base flyway keys still resolve from inherited base.
- MySQL+prd: mysql JDBC wired, mysql-specific flyway overrides applied, base flyway keys inherited correctly.
- Postgres+prd: postgres JDBC wired, pgvector keys resolve, base flyway keys inherited.

If any fail: re-read the profile yaml and confirm the key is either in the right file or was correctly moved to base. Re-run `ProfileYamlDiffTest` to re-check the allow-list.

- [ ] **Step 3: Commit.**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/config/ResolvedPropertyParityTest.java
git commit -m "test(config): add end-to-end resolved property parity across profiles"
```

---

## Task 8: Write the `spring-profiles` runbook

**Files:**
- Create: `docs/runbook/spring-profiles.md`

- [ ] **Step 1: Write the runbook.**

Create `docs/runbook/spring-profiles.md`:

```markdown
# Spring Profile Runbook — SuperSonic (`launchers/standalone`)

## Activation model

Two independent axes, both driven by env var:

| Env var    | Spring key               | Values (defaults in parens)   | Role                             |
|------------|--------------------------|--------------------------------|----------------------------------|
| `S2_DB_TYPE` | `spring.profiles.active` | `h2` (default) / `mysql` / `postgres` | Database dialect — which `application-<db>.yaml` gets loaded |
| `S2_ENV`     | `spring.profiles.include` | `dev` (default) / `prd`     | Environment — which `application-<env>.yaml` gets loaded (credentials) |

The two axes are **independent**: production can run on either MySQL or Postgres; a developer on `dev` can still pick either DB for local testing.

Spring's resolution order (later wins):
1. `application.yaml` (base — always loaded)
2. `application-<active>.yaml` (DB dialect)
3. `application-<included>.yaml` (env)
4. Env vars
5. JVM `-D` system properties

## File ownership rule

- **`application.yaml`** = every setting that is identical across every `(db, env)` combination.
- **`application-<db>.yaml`** = only what differs per DB dialect (JDBC URL/driver, Flyway `locations`, `validate-on-migrate`, pgvector for Postgres, H2 console for H2).
- **`application-<env>.yaml`** = credentials, per-env flags, optional SMTP overrides.

Enforced automatically by `ProfileYamlDiffTest` (fails CI if a profile yaml grows keys outside its allow-list).

## How to add a new env profile (e.g. `staging`)

1. Create `launchers/standalone/src/main/resources/application-staging.yaml`.
2. Put ONLY credentials/env-specific overrides — mirror `application-prd.yaml` shape.
3. Add the file to `ALLOWED_PREFIXES` in `ProfileYamlDiffTest` with the allowed prefix list (typically the same as prd).
4. Add a `@Nested` class to `ResolvedPropertyParityTest` with `@ActiveProfiles({"<db>", "staging"})` asserting critical resolved values.
5. Activate in deployment via `S2_ENV=staging`.
6. Update `docker/.env.example` if the new profile should be documented there.
7. If the new env needs a credential template committed to the repo, create `application-staging.yaml.example` alongside and add the real `application-staging.yaml` to `.gitignore` (mirror the dev pattern).

## How to add a new DB dialect (e.g. `clickhouse`)

1. Create `launchers/standalone/src/main/resources/application-clickhouse.yaml` with JDBC + Flyway overrides.
2. Add `classpath:db/migration/clickhouse/` Flyway scripts if applicable, else set `spring.flyway.enabled: false`.
3. Add to `ALLOWED_PREFIXES` in `ProfileYamlDiffTest`.
4. Add a nested parity test.
5. Add ClickHouse JDBC driver to `launchers/standalone/pom.xml`.

## Known gotchas

- **Docker-compose defaults**: `docker/docker-compose.yml` pins `S2_DB_TYPE=postgres` and (since P2-11) `S2_ENV=prd`. Override by adding envs to the compose file or `.env`.
- **`application-dev.yaml` is gitignored.** Use `application-dev.yaml.example` as the template. `git pull` will never overwrite your local dev secrets.
- **Prd mail config**: base `application.yaml` reads `${EMAIL_HOST}` with no default. If prd should have a fallback, set it via env var — do **not** add it to `application-prd.yaml` unless it's a prd-only value.
- **Logging level in prd**: base yaml defines `logging.level.dev.langchain4j: DEBUG` and similar. Consider overriding `logging.level.root: INFO` in `application-prd.yaml` if you find prd log volume too high. (Tracked in `docker/OPS-DEPLOY-IMPROVEMENTS.md`.)
- **`spring.profiles.group`** is currently **unused**. If you ever need to activate "prd-credentials + prd-metrics" together, define a group in `application.yaml` — but only couple profiles that should always travel together.
- **Flyway `baseline-version`**: env var `FLYWAY_BASELINE_VERSION` overrides. First-time deploy: leave `-1` to run every migration. Legacy DB: set to the version already applied.

## Rollback

See the rollback section of `docs/superpowers/plans/2026-04-17-p2-11-config-consolidation.md` (Task 9). One `git revert` of the consolidation commits restores the pre-refactor yaml shape — resolved properties are unchanged, so no downtime.
```

- [ ] **Step 2: Commit.**

```bash
git add docs/runbook/spring-profiles.md
git commit -m "docs(runbook): add spring profile activation & consolidation runbook"
```

---

## Task 9: Rollback strategy — document and dry-run

- [ ] **Step 1: Append a rollback section to this plan document (self-reference).**

No action required — this section serves as the rollback doc.

### Rollback procedure

If the consolidation breaks a production deploy:

**Option A — revert the consolidation commits (preferred):**

```bash
# Identify the consolidation commits on master:
git log --oneline master | grep -E "refactor\(config\): lift shared Flyway keys|chore\(docker\): set S2_ENV=prd"

# Revert in reverse order (newer first):
git revert <sha-of-docker-env-commit>
git revert <sha-of-config-consolidation-commit>
git push origin master
```

Because Task 3 is a pure key-location refactor (no resolved value changed), reverting restores the pre-refactor yaml shape with **identical runtime behavior**. No data migration, no downtime.

**Option B — hotfix only the misrouted key:**

If a single key turns out to differ between mysql and postgres (and we mistakenly lifted it), add it back to both `application-mysql.yaml` and `application-postgres.yaml`, drop it from `application.yaml`, update `ALLOWED_PREFIXES` in `ProfileYamlDiffTest`, and ship a single-commit hotfix. Cheaper than a full revert.

**Option C — runtime override:**

As an emergency break-glass, any moved key can be overridden via env var without code change:

```bash
# Example: override spring.flyway.baseline-version at runtime
export SPRING_FLYWAY_BASELINE_VERSION=17
```

Spring's environment-variable-to-property mapping covers every key in the yamls.

### Dry-run verification (before landing Task 3)

- [ ] **Step 2: Capture pre-refactor resolved properties as a baseline.**

Before landing Task 3, run this command to snapshot the pre-refactor resolved config:

```bash
mvn -pl launchers/standalone -am compile
java -cp "launchers/standalone/target/classes:launchers/standalone/target/dependency/*" \
     -Dspring.profiles.active=mysql -Dspring.profiles.include=prd \
     -Dspring.main.web-application-type=none \
     org.springframework.boot.SpringApplication \
     com.tencent.supersonic.StandaloneLauncher \
     --debug > /tmp/before-mysql-prd.log 2>&1 || true
grep "^spring\.\|^s2\.\|^server\." /tmp/before-mysql-prd.log | sort > /tmp/before-mysql-prd.properties
```

After Task 3, run the same and diff:

```bash
# re-run with the refactored config, then:
diff /tmp/before-mysql-prd.properties /tmp/after-mysql-prd.properties
```

Expected: empty diff. Any output is a regression — roll back per Option A.

(This dry-run is a manual sanity check. `ResolvedPropertyParityTest` is the automated equivalent for a hand-picked set of critical keys.)

- [ ] **Step 3: No commit needed for Task 9 itself** — the rollback procedure is documented in-plan and in the runbook (Task 8).

---

## Verification checklist (run all before landing)

- [ ] `mvn -pl launchers/standalone test -Dtest=ProfileYamlDiffTest -DfailIfNoTests=false` — PASS
- [ ] `mvn -pl launchers/standalone test -Dtest=ResolvedPropertyParityTest -DfailIfNoTests=false` — PASS
- [ ] `mvn compile -pl launchers/standalone -am` — BUILD SUCCESS (required per CLAUDE.md)
- [ ] `docker compose -f docker/docker-compose.yml config > /dev/null` — no error
- [ ] Visual check: `git diff master -- 'launchers/standalone/src/main/resources/application*.yaml*'` only shows key-movement, no semantic changes.
- [ ] `docs/runbook/spring-profiles.md` exists and renders on GitHub.

## Summary of what changes

- **Keys lifted into base `application.yaml`**: 4 (`spring.flyway.baseline-on-migrate`, `spring.flyway.baseline-version`, `spring.flyway.table`, `spring.flyway.out-of-order`).
- **Net line count across standalone yamls**: from 383 → ~360, with the meaningful win being that mysql/postgres per-file complexity drops by ~35%.
- **New tests**: 2 (allow-list diff test + boot-time parity test).
- **New docs**: 1 (`docs/runbook/spring-profiles.md`).
- **Bugs fixed inline**: `application-dev.yaml.example` now mirrors dev's `spring.mail` shape; `docker-compose.yml` now explicitly sets `S2_ENV=prd`.
- **Out of scope (follow-up plan)**: the same consolidation for `launchers/headless/` and `launchers/chat/` yamls, which carry a similar but independent duplication pattern (noted in Task 1).
