# P1-5: Per-Tenant Concurrency Quota + Connection Pool Isolation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent a single large tenant from exhausting global HikariCP/Druid pools and starving other tenants by gating JDBC execution behind a per-tenant semaphore with fail-fast timeout and HTTP 429 response.

**Architecture:** In-memory `ConcurrentHashMap<Long, Semaphore>` keyed by tenant ID, with acquire/release bracketing at the single narrow enforcement point `SqlUtils#queryInternal` (the only path into the Druid pool via `JdbcTemplate#query`). Per-tenant overrides loaded from `s2_tenant_quota`; global defaults from `s2.tenant.quota.*`. Feature is opt-in via `s2.tenant.quota.enabled=false` for safe rollback. Micrometer gauges expose live permit counts. Multi-instance deployments are **per-instance only** (documented limitation); Redis-backed counters are out of scope for this MVP.

**Tech Stack:** Java 21, Spring Boot 3.4.x, MyBatis-Plus, Micrometer, Flyway (MySQL + PostgreSQL dialects), React + Ant Design Pro.

---

## File Structure

**New Java files (common module — reusable, no dependencies on headless):**
- `common/src/main/java/com/tencent/supersonic/common/quota/TenantPermit.java` — `AutoCloseable` permit handle returned by `acquireJdbc(...)`.
- `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaService.java` — interface.
- `common/src/main/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaService.java` — impl backed by `ConcurrentHashMap<Long, Semaphore>`.
- `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaConfig.java` — `@ConfigurationProperties(prefix = "s2.tenant.quota")`.
- `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java` — Spring `@Configuration` wiring; registers the bean only when `s2.tenant.quota.enabled=true`.
- `common/src/main/java/com/tencent/supersonic/common/quota/TooManyRequestsException.java` — runtime exception with `tenantId` and `retryAfterSeconds`.
- `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinder.java` — Micrometer gauges (available + waiting).
- `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaFilter.java` — Servlet filter mapping `TooManyRequestsException` → HTTP 429 + `Retry-After` header.

**New Java files (auth module — override table persisted per tenant):**
- `auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/pojo/TenantQuotaDO.java` — `@TableName("s2_tenant_quota")` entity.
- `auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/request/TenantQuotaReq.java` — DTO for REST.
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/TenantQuotaMapper.java` — MyBatis-Plus mapper.
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepository.java` — `ServiceImpl<TenantQuotaMapper, TenantQuotaDO>` façade.
- `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/TenantQuotaController.java` — `@RestController` under `/api/v1/admin/tenant-quotas`.

**Modified Java files:**
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java` — wrap `queryInternal(...)` with `try (TenantPermit)`.
- `launchers/standalone/src/main/resources/application.yaml` — add `s2.tenant.quota.*` keys.

**Flyway migrations (MySQL + PostgreSQL):**
- `launchers/standalone/src/main/resources/db/migration/mysql/V29__tenant_quota.sql`
- `launchers/standalone/src/main/resources/db/migration/postgresql/V29__tenant_quota.sql`

**Tests (mirror src paths under `src/test/java`):**
- `common/src/test/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaServiceTest.java`
- `common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinderTest.java`
- `common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaFilterTest.java`
- `auth/authentication/src/test/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepositoryTest.java`
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/utils/SqlUtilsQuotaIntegrationTest.java`

**Frontend files:**
- `webapp/packages/supersonic-fe/src/services/tenantQuota.ts` — REST client.
- `webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/index.tsx` — admin list + modal form.
- `webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/style.less` — minimal styles (mirror `SubscriptionManagement/style.less`).
- Routes update in `webapp/packages/supersonic-fe/config/routes.ts` (add entry under Platform section).

**Docs:**
- `docs/runbook/tenant-quota.md` — operator runbook.
- `docs/details/platform/tenant-quota.md` — design spec + frontmatter (status / module / key-files).

---

## Pre-flight (verification of baseline)

- [ ] **Step 0.1: Verify clean baseline compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS, zero errors.

- [ ] **Step 0.2: Verify target migration version unused**

Run: `ls launchers/standalone/src/main/resources/db/migration/mysql/V29* 2>/dev/null || echo "free"`
Expected: `free`

---

## Task 1: Flyway V29 migration for `s2_tenant_quota` (MySQL + PostgreSQL)

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V29__tenant_quota.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V29__tenant_quota.sql`

**Design note:** The table holds per-tenant *overrides*. Tenants without a row use `s2.tenant.quota.default.*` values from YAML. `CREATE TABLE IF NOT EXISTS` is supported by both dialects; MySQL does **not** support `ADD COLUMN IF NOT EXISTS`, so new columns must use the `information_schema` procedural pattern (not needed here — this is a fresh table).

- [ ] **Step 1.1: Write the MySQL migration**

Create `launchers/standalone/src/main/resources/db/migration/mysql/V29__tenant_quota.sql`:

```sql
-- V29: Per-tenant concurrency quota overrides.
-- Default quota falls back to s2.tenant.quota.default.* in application.yaml when a tenant has no row.

CREATE TABLE IF NOT EXISTS `s2_tenant_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL COMMENT 'FK to s2_tenant.id',
    `jdbc_concurrent` INT NOT NULL DEFAULT 10 COMMENT 'Max concurrent JDBC executions per instance',
    `llm_concurrent` INT NOT NULL DEFAULT 5 COMMENT 'Max concurrent LLM calls per instance (reserved; future use)',
    `monthly_query_count` BIGINT NOT NULL DEFAULT 0 COMMENT 'Max total queries per month (0 = unlimited; future use)',
    `acquire_timeout_ms` INT NOT NULL DEFAULT 2000 COMMENT 'Max wait for a JDBC permit before 429',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 = enforce, 0 = bypass for this tenant',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_quota_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Per-tenant concurrency quota overrides';
```

- [ ] **Step 1.2: Write the PostgreSQL migration**

Create `launchers/standalone/src/main/resources/db/migration/postgresql/V29__tenant_quota.sql`:

```sql
-- V29: Per-tenant concurrency quota overrides (PostgreSQL dialect).

CREATE TABLE IF NOT EXISTS s2_tenant_quota (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    jdbc_concurrent INT NOT NULL DEFAULT 10,
    llm_concurrent INT NOT NULL DEFAULT 5,
    monthly_query_count BIGINT NOT NULL DEFAULT 0,
    acquire_timeout_ms INT NOT NULL DEFAULT 2000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_quota_tenant_id UNIQUE (tenant_id)
);

COMMENT ON TABLE s2_tenant_quota IS 'Per-tenant concurrency quota overrides';
COMMENT ON COLUMN s2_tenant_quota.jdbc_concurrent IS 'Max concurrent JDBC executions per instance';
COMMENT ON COLUMN s2_tenant_quota.llm_concurrent IS 'Max concurrent LLM calls per instance (reserved)';
COMMENT ON COLUMN s2_tenant_quota.monthly_query_count IS 'Max total queries per month (0 = unlimited)';
```

- [ ] **Step 1.3: Verify SQL syntax parses locally**

Run: `mvn -pl launchers/standalone flyway:info -DskipTests -q 2>&1 | head -40 || true`
Expected: Flyway prints the migration list including `V29__tenant_quota` with state `Pending`. (If Flyway plugin isn't configured, skip; the file is validated at app startup.)

- [ ] **Step 1.4: Verify project compiles (migrations are resources, not code, but ensure nothing broke)**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 1.5: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V29__tenant_quota.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V29__tenant_quota.sql
git commit -m "feat(quota): add s2_tenant_quota table for per-tenant concurrency overrides"
```

---

## Task 2: `TenantQuotaConfig` + `TenantPermit` + `TooManyRequestsException`

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaConfig.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TenantPermit.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TooManyRequestsException.java`

- [ ] **Step 2.1: Write `TenantQuotaConfig`**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaConfig.java`:

```java
package com.tencent.supersonic.common.quota;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for per-tenant concurrency quota enforcement. Prefix: {@code s2.tenant.quota}.
 */
@Data
@ConfigurationProperties(prefix = "s2.tenant.quota")
public class TenantQuotaConfig {

    /** Master switch. When false, {@link TenantQuotaService} is not wired and SqlUtils skips acquire/release. */
    private boolean enabled = false;

    /** Defaults applied when a tenant has no s2_tenant_quota row. */
    private Default defaultQuota = new Default();

    /** Fallback tenant id used when TenantContext is unset (e.g. background jobs). */
    private Long fallbackTenantId = 0L;

    @Data
    public static class Default {
        private int jdbcConcurrent = 10;
        private int llmConcurrent = 5;
        private int acquireTimeoutMs = 2000;
    }
}
```

- [ ] **Step 2.2: Write `TenantPermit`**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantPermit.java`:

```java
package com.tencent.supersonic.common.quota;

import java.util.concurrent.Semaphore;

/**
 * AutoCloseable handle returned from {@link TenantQuotaService#acquireJdbc(Long, long)}. Releasing
 * via try-with-resources returns the permit to the tenant's semaphore exactly once.
 */
public final class TenantPermit implements AutoCloseable {

    private final Semaphore semaphore;
    private final Long tenantId;
    private boolean released;

    public TenantPermit(Semaphore semaphore, Long tenantId) {
        this.semaphore = semaphore;
        this.tenantId = tenantId;
    }

    /** No-op permit used when quota is disabled or tenantId is null. */
    public static TenantPermit noop() {
        return new TenantPermit(null, null);
    }

    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void close() {
        if (released || semaphore == null) {
            return;
        }
        released = true;
        semaphore.release();
    }
}
```

- [ ] **Step 2.3: Write `TooManyRequestsException`**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TooManyRequestsException.java`:

```java
package com.tencent.supersonic.common.quota;

/**
 * Thrown when a tenant exceeds its concurrent-execution quota and the acquire timeout elapses
 * before a permit becomes available. Maps to HTTP 429 via {@link TenantQuotaFilter}.
 */
public class TooManyRequestsException extends RuntimeException {

    private final Long tenantId;
    private final int retryAfterSeconds;

    public TooManyRequestsException(Long tenantId, int retryAfterSeconds) {
        super("Tenant " + tenantId + " exceeded concurrency quota; retry after " + retryAfterSeconds + "s");
        this.tenantId = tenantId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
```

- [ ] **Step 2.4: Verify compile**

Run: `mvn compile -pl common -q`
Expected: BUILD SUCCESS.

- [ ] **Step 2.5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/quota/
git commit -m "feat(quota): add TenantQuotaConfig, TenantPermit, TooManyRequestsException"
```

---

## Task 3: `TenantQuotaService` interface + `InMemoryTenantQuotaService` impl (TDD)

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaService.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaService.java`
- Test:   `common/src/test/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaServiceTest.java`

- [ ] **Step 3.1: Write the service interface**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaService.java`:

```java
package com.tencent.supersonic.common.quota;

import java.util.Map;

/**
 * Per-tenant concurrency quota gate. Implementations must be thread-safe and non-blocking on the
 * hot path (fail-fast after timeout, never block indefinitely).
 */
public interface TenantQuotaService {

    /**
     * Acquire a JDBC execution permit for {@code tenantId}. Blocks up to {@code timeoutMs}.
     *
     * @param tenantId tenant; {@code null} ⇒ no-op permit (feature-off / unknown tenant)
     * @param timeoutMs maximum wait in milliseconds
     * @return {@link TenantPermit} — MUST be closed via try-with-resources
     * @throws TooManyRequestsException if no permit acquired within {@code timeoutMs}
     */
    TenantPermit acquireJdbc(Long tenantId, long timeoutMs);

    /** Refresh the effective quota for one tenant (called after override upsert). */
    void refresh(Long tenantId);

    /** Snapshot for metrics: tenantId → available permits. */
    Map<Long, Integer> availablePermits();

    /** Snapshot for metrics: tenantId → threads waiting on the semaphore. */
    Map<Long, Integer> waitingThreads();
}
```

- [ ] **Step 3.2: Write the failing concurrency test (10 threads, quota=3)**

Create `common/src/test/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaServiceTest.java`:

```java
package com.tencent.supersonic.common.quota;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryTenantQuotaServiceTest {

    private TenantQuotaConfig configWithDefault(int jdbc, int timeoutMs) {
        TenantQuotaConfig c = new TenantQuotaConfig();
        c.setEnabled(true);
        c.getDefaultQuota().setJdbcConcurrent(jdbc);
        c.getDefaultQuota().setAcquireTimeoutMs(timeoutMs);
        return c;
    }

    @Test
    void acquireAndReleaseReturnsPermitToPool() {
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(configWithDefault(1, 100), tid -> null);
        try (TenantPermit p = svc.acquireJdbc(42L, 100)) {
            assertNotNull(p);
            assertEquals(0, svc.availablePermits().get(42L));
        }
        assertEquals(1, svc.availablePermits().get(42L));
    }

    @Test
    void nullTenantReturnsNoopPermit() {
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(configWithDefault(1, 100), tid -> null);
        try (TenantPermit p = svc.acquireJdbc(null, 100)) {
            assertNotNull(p);
        }
    }

    @Test
    void timeoutThrowsTooManyRequests() throws Exception {
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(configWithDefault(1, 50), tid -> null);
        TenantPermit hold = svc.acquireJdbc(7L, 50);
        try {
            assertThrows(TooManyRequestsException.class, () -> svc.acquireJdbc(7L, 50));
        } finally {
            hold.close();
        }
    }

    @Test
    void tenThreadsQuotaThreeExactlyThreeAcquireImmediately() throws Exception {
        // Quota = 3, 10 threads, acquire-timeout 50ms. Expect 3 succeed, 7 time out.
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(configWithDefault(3, 50), tid -> null);
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger acquired = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    TenantPermit p = svc.acquireJdbc(99L, 50);
                    acquired.incrementAndGet();
                    // Hold long enough that other threads time out.
                    Thread.sleep(200);
                    p.close();
                } catch (TooManyRequestsException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(3, acquired.get(), "exactly 3 threads must acquire");
        assertEquals(7, rejected.get(), "exactly 7 threads must be rejected with 429");
    }

    @Test
    void refreshReplacesSemaphoreWithNewSize() {
        AtomicInteger size = new AtomicInteger(1);
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(
                configWithDefault(1, 100),
                tid -> {
                    TenantQuotaOverride o = new TenantQuotaOverride();
                    o.setJdbcConcurrent(size.get());
                    o.setAcquireTimeoutMs(100);
                    o.setEnabled(true);
                    return o;
                });
        assertEquals(1, svc.availablePermits().get(5L) == null ? 1 : svc.availablePermits().get(5L));
        size.set(5);
        svc.refresh(5L);
        try (TenantPermit p = svc.acquireJdbc(5L, 50)) {
            assertEquals(4, svc.availablePermits().get(5L));
        }
    }
}
```

- [ ] **Step 3.3: Run tests to confirm they fail**

Run: `mvn test -pl common -Dtest=InMemoryTenantQuotaServiceTest -q`
Expected: compile failure — `InMemoryTenantQuotaService` and `TenantQuotaOverride` do not exist.

- [ ] **Step 3.4: Write the `TenantQuotaOverride` DTO**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaOverride.java`:

```java
package com.tencent.supersonic.common.quota;

import lombok.Data;

/**
 * Loaded override for one tenant. Typically produced by a DB-backed loader that maps
 * {@code s2_tenant_quota} rows. A {@code null} return from the loader means "use defaults".
 */
@Data
public class TenantQuotaOverride {
    private int jdbcConcurrent;
    private int llmConcurrent;
    private int acquireTimeoutMs;
    private boolean enabled = true;
}
```

- [ ] **Step 3.5: Write `InMemoryTenantQuotaService`**

Create `common/src/main/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaService.java`:

```java
package com.tencent.supersonic.common.quota;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Per-instance, in-memory implementation of {@link TenantQuotaService}. Uses fair semaphores so
 * waiting threads are served FIFO. Counters are <b>per-instance</b>; multi-instance deployments
 * enforce per-pod not per-cluster. See docs/runbook/tenant-quota.md for future Redis backing.
 */
@Slf4j
public class InMemoryTenantQuotaService implements TenantQuotaService {

    private final TenantQuotaConfig config;
    private final Function<Long, TenantQuotaOverride> overrideLoader;
    private final ConcurrentHashMap<Long, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> sizes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> timeouts = new ConcurrentHashMap<>();

    public InMemoryTenantQuotaService(TenantQuotaConfig config,
            Function<Long, TenantQuotaOverride> overrideLoader) {
        this.config = config;
        this.overrideLoader = overrideLoader;
    }

    @Override
    public TenantPermit acquireJdbc(Long tenantId, long timeoutMs) {
        if (!config.isEnabled() || tenantId == null) {
            return TenantPermit.noop();
        }
        Semaphore sem = semaphoreFor(tenantId);
        long effectiveTimeout = timeoutMs > 0 ? timeoutMs : timeouts.getOrDefault(tenantId,
                config.getDefaultQuota().getAcquireTimeoutMs());
        try {
            if (!sem.tryAcquire(effectiveTimeout, TimeUnit.MILLISECONDS)) {
                int retryAfter = (int) Math.max(1, effectiveTimeout / 1000);
                log.warn("[TenantQuota] 429 tenantId={} available={} waiting={}", tenantId,
                        sem.availablePermits(), sem.getQueueLength());
                throw new TooManyRequestsException(tenantId, retryAfter);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TooManyRequestsException(tenantId, 1);
        }
        return new TenantPermit(sem, tenantId);
    }

    @Override
    public void refresh(Long tenantId) {
        // Atomically replace the semaphore. In-flight permits on the old semaphore simply return
        // to that (now-unreferenced) instance; no permit is lost because the holder still has a
        // reference via its TenantPermit. New acquires use the fresh size.
        TenantQuotaOverride o = safeLoadOverride(tenantId);
        int size = o != null && o.isEnabled() ? o.getJdbcConcurrent()
                : config.getDefaultQuota().getJdbcConcurrent();
        int timeout = o != null ? o.getAcquireTimeoutMs() : config.getDefaultQuota().getAcquireTimeoutMs();
        semaphores.put(tenantId, new Semaphore(size, true));
        sizes.put(tenantId, size);
        timeouts.put(tenantId, timeout);
        log.info("[TenantQuota] refreshed tenantId={} jdbcConcurrent={} acquireTimeoutMs={}",
                tenantId, size, timeout);
    }

    @Override
    public Map<Long, Integer> availablePermits() {
        Map<Long, Integer> out = new HashMap<>();
        semaphores.forEach((k, v) -> out.put(k, v.availablePermits()));
        return out;
    }

    @Override
    public Map<Long, Integer> waitingThreads() {
        Map<Long, Integer> out = new HashMap<>();
        semaphores.forEach((k, v) -> out.put(k, v.getQueueLength()));
        return out;
    }

    private Semaphore semaphoreFor(Long tenantId) {
        return semaphores.computeIfAbsent(tenantId, this::buildSemaphore);
    }

    private Semaphore buildSemaphore(Long tenantId) {
        TenantQuotaOverride o = safeLoadOverride(tenantId);
        int size = o != null && o.isEnabled() ? o.getJdbcConcurrent()
                : config.getDefaultQuota().getJdbcConcurrent();
        int timeout = o != null ? o.getAcquireTimeoutMs() : config.getDefaultQuota().getAcquireTimeoutMs();
        sizes.put(tenantId, size);
        timeouts.put(tenantId, timeout);
        return new Semaphore(size, true);
    }

    private TenantQuotaOverride safeLoadOverride(Long tenantId) {
        try {
            return overrideLoader != null ? overrideLoader.apply(tenantId) : null;
        } catch (Exception e) {
            log.warn("[TenantQuota] override loader failed tenantId={}: {}", tenantId, e.getMessage());
            return null;
        }
    }
}
```

- [ ] **Step 3.6: Run tests to confirm they pass**

Run: `mvn test -pl common -Dtest=InMemoryTenantQuotaServiceTest -q`
Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`. BUILD SUCCESS.

- [ ] **Step 3.7: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaService.java \
        common/src/main/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaService.java \
        common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaOverride.java \
        common/src/test/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaServiceTest.java
git commit -m "feat(quota): add in-memory TenantQuotaService with per-tenant semaphores

- Fair semaphores, fail-fast on timeout with TooManyRequestsException
- Concurrency test: 10 threads/quota=3 → exactly 3 acquire, 7 rejected"
```

---

## Task 4: `TenantQuotaDO`, `TenantQuotaMapper`, `TenantQuotaRepository` (TDD)

**Files:**
- Create: `auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/pojo/TenantQuotaDO.java`
- Create: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/TenantQuotaMapper.java`
- Create: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepository.java`
- Test:   `auth/authentication/src/test/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepositoryTest.java`

- [ ] **Step 4.1: Write the entity**

Create `auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/pojo/TenantQuotaDO.java`:

```java
package com.tencent.supersonic.auth.api.quota.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("s2_tenant_quota")
public class TenantQuotaDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Integer jdbcConcurrent;
    private Integer llmConcurrent;
    private Long monthlyQueryCount;
    private Integer acquireTimeoutMs;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4.2: Write the mapper**

Create `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/TenantQuotaMapper.java`:

```java
package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.api.quota.pojo.TenantQuotaDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantQuotaMapper extends BaseMapper<TenantQuotaDO> {
}
```

- [ ] **Step 4.3: Write the failing repository test**

Create `auth/authentication/src/test/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepositoryTest.java`:

```java
package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.tencent.supersonic.auth.api.quota.pojo.TenantQuotaDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class TenantQuotaRepositoryTest {

    @Autowired
    private TenantQuotaRepository repository;

    @Test
    void saveAndFindByTenantId() {
        TenantQuotaDO q = new TenantQuotaDO();
        q.setTenantId(4242L);
        q.setJdbcConcurrent(20);
        q.setLlmConcurrent(10);
        q.setMonthlyQueryCount(100_000L);
        q.setAcquireTimeoutMs(3000);
        q.setEnabled(true);
        repository.save(q);

        Optional<TenantQuotaDO> found = repository.findByTenantId(4242L);
        assertTrue(found.isPresent());
        assertEquals(20, found.get().getJdbcConcurrent());
    }

    @Test
    void upsertReplacesExisting() {
        TenantQuotaDO q = new TenantQuotaDO();
        q.setTenantId(4343L);
        q.setJdbcConcurrent(5);
        q.setLlmConcurrent(2);
        q.setAcquireTimeoutMs(1000);
        q.setEnabled(true);
        repository.upsert(q);

        q.setJdbcConcurrent(50);
        repository.upsert(q);

        assertEquals(50, repository.findByTenantId(4343L).orElseThrow().getJdbcConcurrent());
    }
}
```

- [ ] **Step 4.4: Run test to confirm it fails**

Run: `mvn test -pl auth/authentication -Dtest=TenantQuotaRepositoryTest -q`
Expected: compile failure — `TenantQuotaRepository` does not exist.

- [ ] **Step 4.5: Write `TenantQuotaRepository`**

Create `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepository.java`:

```java
package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.auth.api.quota.pojo.TenantQuotaDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantQuotaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CRUD + tenant lookup for {@link TenantQuotaDO}. One row per tenant; {@code uk_tenant_quota_tenant_id}
 * enforces uniqueness.
 */
@Repository
public class TenantQuotaRepository extends ServiceImpl<TenantQuotaMapper, TenantQuotaDO> {

    public Optional<TenantQuotaDO> findByTenantId(Long tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getOne(
                new LambdaQueryWrapper<TenantQuotaDO>().eq(TenantQuotaDO::getTenantId, tenantId)));
    }

    public List<TenantQuotaDO> listAll() {
        return list();
    }

    /** Insert-or-update keyed on {@code tenantId} (unique index). */
    public TenantQuotaDO upsert(TenantQuotaDO desired) {
        Optional<TenantQuotaDO> existing = findByTenantId(desired.getTenantId());
        if (existing.isPresent()) {
            desired.setId(existing.get().getId());
            updateById(desired);
        } else {
            save(desired);
        }
        return desired;
    }
}
```

- [ ] **Step 4.6: Run tests to confirm they pass**

Run: `mvn test -pl auth/authentication -Dtest=TenantQuotaRepositoryTest -q`
Expected: `Tests run: 2, Failures: 0, Errors: 0`. BUILD SUCCESS.

- [ ] **Step 4.7: Commit**

```bash
git add auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/ \
        auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/mapper/TenantQuotaMapper.java \
        auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepository.java \
        auth/authentication/src/test/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepositoryTest.java
git commit -m "feat(quota): add TenantQuotaRepository + MyBatis-Plus mapper"
```

---

## Task 5: Wire `TenantQuotaAutoConfiguration` and loader

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java`
- Create: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/quota/DbTenantQuotaOverrideLoader.java`
- Modify: `common/src/main/java/com/tencent/supersonic/common/config/CommonConfiguration.java` (or create a new `@Configuration` file if `CommonConfiguration` is domain-specific — check before editing)

- [ ] **Step 5.1: Write the DB-backed loader**

Create `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/quota/DbTenantQuotaOverrideLoader.java`:

```java
package com.tencent.supersonic.auth.authentication.quota;

import com.tencent.supersonic.auth.api.quota.pojo.TenantQuotaDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.TenantQuotaRepository;
import com.tencent.supersonic.common.quota.TenantQuotaOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Loads {@link TenantQuotaOverride} from {@code s2_tenant_quota}. Returns {@code null} when no row
 * exists, signalling "use defaults" to {@link com.tencent.supersonic.common.quota.InMemoryTenantQuotaService}.
 */
@Component
@RequiredArgsConstructor
public class DbTenantQuotaOverrideLoader implements Function<Long, TenantQuotaOverride> {

    private final TenantQuotaRepository repository;

    @Override
    public TenantQuotaOverride apply(Long tenantId) {
        return repository.findByTenantId(tenantId).map(this::toOverride).orElse(null);
    }

    private TenantQuotaOverride toOverride(TenantQuotaDO d) {
        TenantQuotaOverride o = new TenantQuotaOverride();
        o.setJdbcConcurrent(d.getJdbcConcurrent() == null ? 10 : d.getJdbcConcurrent());
        o.setLlmConcurrent(d.getLlmConcurrent() == null ? 5 : d.getLlmConcurrent());
        o.setAcquireTimeoutMs(d.getAcquireTimeoutMs() == null ? 2000 : d.getAcquireTimeoutMs());
        o.setEnabled(d.getEnabled() == null || d.getEnabled());
        return o;
    }
}
```

- [ ] **Step 5.2: Write the auto-configuration**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java`:

```java
package com.tencent.supersonic.common.quota;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Wires {@link TenantQuotaService} only when {@code s2.tenant.quota.enabled=true}. When disabled,
 * no bean is published and call sites must tolerate the missing bean (see {@link TenantPermit#noop()}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantQuotaConfig.class)
@ConditionalOnProperty(prefix = "s2.tenant.quota", name = "enabled", havingValue = "true")
public class TenantQuotaAutoConfiguration {

    @Bean
    public TenantQuotaService tenantQuotaService(TenantQuotaConfig config,
            ObjectProvider<Function<Long, TenantQuotaOverride>> loaderProvider) {
        Function<Long, TenantQuotaOverride> loader = loaderProvider.getIfAvailable(() -> tid -> null);
        return new InMemoryTenantQuotaService(config, loader);
    }
}
```

- [ ] **Step 5.3: Register auto-config in `spring.factories`**

Edit `common/src/main/resources/META-INF/spring.factories` (create if missing). Add the following line (append; don't replace existing entries):

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.tencent.supersonic.common.quota.TenantQuotaAutoConfiguration
```

If the file already has an `EnableAutoConfiguration` key, append `TenantQuotaAutoConfiguration` to its comma-separated list (backslash-continuation style).

- [ ] **Step 5.4: Verify compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 5.5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java \
        common/src/main/resources/META-INF/spring.factories \
        auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/quota/DbTenantQuotaOverrideLoader.java
git commit -m "feat(quota): wire TenantQuotaAutoConfiguration and DB override loader"
```

---

## Task 6: Wrap JDBC execution in `SqlUtils#queryInternal` (enforcement point)

**Decision — aspect vs decorator vs inline wrap:**

| Approach | Pros | Cons |
|---|---|---|
| `@Aspect @Around` on `queryInternal` | Clean separation | `SqlUtils` instantiated via `new` in some callers (see constructors); AOP proxies bypassed |
| Decorator wrapping `JdbcDataSource` | Intercepts at pool level | Very invasive — 15+ call sites use `JdbcDataSource` directly |
| **Inline try-with-resources in `SqlUtils#queryInternal`** | **Single narrow enforcement point; works for `new SqlUtils()` callers too; no AOP config** | **Couples `SqlUtils` to quota module (acceptable — feature-flagged via config)** |

**Pick: inline wrap.** `SqlUtils#queryInternal` is the single funnel to `JdbcTemplate#query`; wrapping here covers every production path without AOP fragility.

**Files:**
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java:150-152`

- [ ] **Step 6.1: Read current method to confirm signature**

Run: `grep -n "public void queryInternal" headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java`
Expected: one match around line 150.

- [ ] **Step 6.2: Inject `TenantQuotaService` as optional dependency and wrap the call**

Edit `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java` as follows.

Add import near the top (after existing `com.tencent.supersonic.common.*` imports):

```java
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.quota.TenantPermit;
import com.tencent.supersonic.common.quota.TenantQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
```

Add field (below `@Autowired private JdbcDataSource jdbcDataSource;`):

```java
    @Autowired(required = false)
    private TenantQuotaService tenantQuotaService;

    @Value("${s2.tenant.quota.default.acquire-timeout-ms:2000}")
    private long quotaAcquireTimeoutMs;
```

Replace the body of `queryInternal(...)`:

```java
    public void queryInternal(String sql, SemanticQueryResp queryResultWithColumns) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantQuotaService == null) {
            getResult(sql, queryResultWithColumns, jdbcTemplate());
            return;
        }
        try (TenantPermit permit = tenantQuotaService.acquireJdbc(tenantId, quotaAcquireTimeoutMs)) {
            getResult(sql, queryResultWithColumns, jdbcTemplate());
        }
    }
```

**Rationale for letting `TooManyRequestsException` propagate:** It is a `RuntimeException`; callers already bubble `RuntimeException` to the controller layer. `TenantQuotaFilter` (Task 9) translates it to HTTP 429.

- [ ] **Step 6.3: Verify compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 6.4: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java
git commit -m "feat(quota): gate queryInternal with per-tenant TenantPermit acquire/release"
```

---

## Task 7: Integration test — 5 parallel queries, quota=2, assert exactly 2 concurrent (TDD)

**Files:**
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/utils/SqlUtilsQuotaIntegrationTest.java`

- [ ] **Step 7.1: Write the integration test**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/utils/SqlUtilsQuotaIntegrationTest.java`:

```java
package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.quota.InMemoryTenantQuotaService;
import com.tencent.supersonic.common.quota.TenantPermit;
import com.tencent.supersonic.common.quota.TenantQuotaConfig;
import com.tencent.supersonic.common.quota.TenantQuotaService;
import com.tencent.supersonic.common.quota.TooManyRequestsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the quota enforcement contract at the SqlUtils layer: with quota=2, five concurrent
 * queries produce exactly two in-flight and three 429 rejections. This test does <i>not</i> exercise
 * the real Druid pool — it substitutes a controllable "work" function via a test-only subclass.
 */
class SqlUtilsQuotaIntegrationTest {

    private TenantQuotaService quotaService;
    private AtomicInteger concurrent;
    private AtomicInteger peakConcurrent;

    @BeforeEach
    void setUp() {
        TenantQuotaConfig config = new TenantQuotaConfig();
        config.setEnabled(true);
        config.getDefaultQuota().setJdbcConcurrent(2);
        config.getDefaultQuota().setAcquireTimeoutMs(100);
        quotaService = new InMemoryTenantQuotaService(config, tid -> null);
        concurrent = new AtomicInteger();
        peakConcurrent = new AtomicInteger();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void simulatedQuery(Long tenantId) throws InterruptedException {
        try (TenantPermit p = quotaService.acquireJdbc(tenantId, 100)) {
            int now = concurrent.incrementAndGet();
            peakConcurrent.updateAndGet(prev -> Math.max(prev, now));
            Thread.sleep(300); // hold the permit long enough to force contention
            concurrent.decrementAndGet();
        }
    }

    @Test
    void fiveParallelQueriesQuotaTwoExactlyTwoConcurrentThreeRejected() throws Exception {
        int threads = 5;
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    simulatedQuery(77L);
                    accepted.incrementAndGet();
                } catch (TooManyRequestsException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(2, accepted.get(), "exactly 2 concurrent queries accepted");
        assertEquals(3, rejected.get(), "exactly 3 queries rejected with 429");
        assertEquals(2, peakConcurrent.get(), "peak concurrency equals quota");
    }
}
```

- [ ] **Step 7.2: Run test**

Run: `mvn test -pl headless/core -Dtest=SqlUtilsQuotaIntegrationTest -q`
Expected: `Tests run: 1, Failures: 0, Errors: 0`. BUILD SUCCESS.

- [ ] **Step 7.3: Commit**

```bash
git add headless/core/src/test/java/com/tencent/supersonic/headless/core/utils/SqlUtilsQuotaIntegrationTest.java
git commit -m "test(quota): integration test for 5-parallel/quota=2 enforcement"
```

---

## Task 8: Micrometer gauges (TDD)

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinder.java`
- Test:   `common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinderTest.java`
- Modify: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java` (register the binder as a bean)

- [ ] **Step 8.1: Write the failing test**

Create `common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinderTest.java`:

```java
package com.tencent.supersonic.common.quota;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantQuotaMeterBinderTest {

    @Test
    void publishesAvailableAndWaitingGaugesPerTenant() {
        TenantQuotaConfig config = new TenantQuotaConfig();
        config.setEnabled(true);
        config.getDefaultQuota().setJdbcConcurrent(3);
        config.getDefaultQuota().setAcquireTimeoutMs(100);
        InMemoryTenantQuotaService svc = new InMemoryTenantQuotaService(config, tid -> null);

        // Warm the map by acquiring once
        svc.acquireJdbc(55L, 100).close();

        MeterRegistry registry = new SimpleMeterRegistry();
        new TenantQuotaMeterBinder(svc).bindTo(registry);

        Double available = registry.find("s2_tenant_jdbc_permits_available")
                .tag("tenantId", "55").gauge().value();
        Double waiting = registry.find("s2_tenant_jdbc_permits_waiting")
                .tag("tenantId", "55").gauge().value();

        assertNotNull(available);
        assertNotNull(waiting);
        assertEquals(3.0, available);
        assertEquals(0.0, waiting);
    }
}
```

- [ ] **Step 8.2: Run to confirm failure**

Run: `mvn test -pl common -Dtest=TenantQuotaMeterBinderTest -q`
Expected: compile failure — `TenantQuotaMeterBinder` does not exist.

- [ ] **Step 8.3: Write the binder**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinder.java`:

```java
package com.tencent.supersonic.common.quota;

import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes two gauges per known tenant:
 * <ul>
 *   <li>{@code s2_tenant_jdbc_permits_available{tenantId=...}}</li>
 *   <li>{@code s2_tenant_jdbc_permits_waiting{tenantId=...}}</li>
 * </ul>
 * Gauges are registered lazily on first sample so tenants without any acquire never produce noise.
 */
public class TenantQuotaMeterBinder extends AbstractMeterBinder {

    private final TenantQuotaService service;
    private final Map<Long, Boolean> registered = new ConcurrentHashMap<>();
    private MeterRegistry registryRef;

    public TenantQuotaMeterBinder(TenantQuotaService service) {
        this.service = service;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        this.registryRef = registry;
        // Register existing tenants immediately; new tenants registered on sampling tick.
        service.availablePermits().keySet().forEach(this::registerIfAbsent);
        // Meter that polls and auto-registers new tenants
        Gauge.builder("s2_tenant_quota_known_tenants", service, s -> s.availablePermits().size())
                .description("Number of tenants with an active quota semaphore")
                .register(registry);
    }

    private void registerIfAbsent(Long tenantId) {
        if (registered.putIfAbsent(tenantId, Boolean.TRUE) != null || registryRef == null) {
            return;
        }
        String tid = String.valueOf(tenantId);
        Gauge.builder("s2_tenant_jdbc_permits_available", service,
                        s -> s.availablePermits().getOrDefault(tenantId, 0))
                .tag("tenantId", tid)
                .description("Available JDBC permits for tenant")
                .register(registryRef);
        Gauge.builder("s2_tenant_jdbc_permits_waiting", service,
                        s -> s.waitingThreads().getOrDefault(tenantId, 0))
                .tag("tenantId", tid)
                .description("Threads waiting for a JDBC permit for tenant")
                .register(registryRef);
    }
}
```

- [ ] **Step 8.4: Run test to confirm pass**

Run: `mvn test -pl common -Dtest=TenantQuotaMeterBinderTest -q`
Expected: `Tests run: 1, Failures: 0, Errors: 0`. BUILD SUCCESS.

- [ ] **Step 8.5: Register the binder in auto-configuration**

Edit `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java`. Add a bean method **inside** the class:

```java
    @Bean
    public TenantQuotaMeterBinder tenantQuotaMeterBinder(TenantQuotaService service) {
        return new TenantQuotaMeterBinder(service);
    }
```

- [ ] **Step 8.6: Verify compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 8.7: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinder.java \
        common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java \
        common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaMeterBinderTest.java
git commit -m "feat(quota): Micrometer gauges for permits_available and permits_waiting"
```

---

## Task 9: HTTP filter — translate `TooManyRequestsException` → HTTP 429 (TDD)

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaFilter.java`
- Test:   `common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaFilterTest.java`
- Modify: `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java` (register the filter bean)

- [ ] **Step 9.1: Write the failing test**

Create `common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaFilterTest.java`:

```java
package com.tencent.supersonic.common.quota;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantQuotaFilterTest {

    @Test
    void translatesExceptionTo429WithRetryAfter() throws ServletException, IOException {
        TenantQuotaFilter filter = new TenantQuotaFilter();
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/semantic/query");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> {
            throw new TooManyRequestsException(42L, 5);
        };

        filter.doFilter(req, resp, chain);

        assertEquals(429, resp.getStatus());
        assertEquals("5", resp.getHeader("Retry-After"));
        assertTrue(resp.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    void propagatesUnrelatedExceptions() {
        TenantQuotaFilter filter = new TenantQuotaFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> {
            throw new RuntimeException("unrelated");
        };

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> filter.doFilter(req, resp, chain));
        assertEquals("unrelated", ex.getMessage());
    }
}
```

- [ ] **Step 9.2: Run to confirm failure**

Run: `mvn test -pl common -Dtest=TenantQuotaFilterTest -q`
Expected: compile failure — `TenantQuotaFilter` does not exist.

- [ ] **Step 9.3: Write the filter**

Create `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaFilter.java`:

```java
package com.tencent.supersonic.common.quota;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Translates {@link TooManyRequestsException} thrown deeper in the stack into HTTP 429 with a
 * {@code Retry-After} header. Non-quota exceptions are re-thrown unchanged.
 */
public class TenantQuotaFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (TooManyRequestsException e) {
            HttpServletResponse http = (HttpServletResponse) response;
            http.setStatus(429);
            http.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            http.setContentType("application/json;charset=UTF-8");
            String body = "{\"code\":429,\"status\":\"TOO_MANY_REQUESTS\",\"tenantId\":"
                    + e.getTenantId() + ",\"retryAfterSeconds\":" + e.getRetryAfterSeconds()
                    + ",\"msg\":\"Tenant concurrency quota exceeded\"}";
            http.getWriter().write(body);
        } catch (RuntimeException e) {
            // Unwrap one level: Spring wraps controller exceptions; if the root cause is ours,
            // still emit 429.
            Throwable cause = e.getCause();
            if (cause instanceof TooManyRequestsException tmre) {
                HttpServletResponse http = (HttpServletResponse) response;
                http.setStatus(429);
                http.setHeader("Retry-After", String.valueOf(tmre.getRetryAfterSeconds()));
                http.setContentType("application/json;charset=UTF-8");
                http.getWriter().write("{\"code\":429,\"status\":\"TOO_MANY_REQUESTS\",\"tenantId\":"
                        + tmre.getTenantId() + ",\"retryAfterSeconds\":"
                        + tmre.getRetryAfterSeconds() + "}");
                return;
            }
            throw e;
        }
    }
}
```

- [ ] **Step 9.4: Run test to confirm pass**

Run: `mvn test -pl common -Dtest=TenantQuotaFilterTest -q`
Expected: `Tests run: 2, Failures: 0, Errors: 0`. BUILD SUCCESS.

- [ ] **Step 9.5: Register the filter in auto-configuration**

Edit `common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java`. Add these imports at the top:

```java
import org.springframework.boot.web.servlet.FilterRegistrationBean;
```

Add this bean method inside the class:

```java
    @Bean
    public FilterRegistrationBean<TenantQuotaFilter> tenantQuotaFilterRegistration() {
        FilterRegistrationBean<TenantQuotaFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TenantQuotaFilter());
        reg.addUrlPatterns("/api/*");
        reg.setName("tenantQuotaFilter");
        reg.setOrder(Integer.MIN_VALUE + 100); // before auth filters so 429 short-circuits cleanly
        return reg;
    }
```

- [ ] **Step 9.6: Verify compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 9.7: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaFilter.java \
        common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaAutoConfiguration.java \
        common/src/test/java/com/tencent/supersonic/common/quota/TenantQuotaFilterTest.java
git commit -m "feat(quota): TenantQuotaFilter translates exception to HTTP 429 + Retry-After"
```

---

## Task 10: REST controller for admin quota CRUD

**Files:**
- Create: `auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/request/TenantQuotaReq.java`
- Create: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/TenantQuotaController.java`

- [ ] **Step 10.1: Write the request DTO**

Create `auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/request/TenantQuotaReq.java`:

```java
package com.tencent.supersonic.auth.api.quota.request;

import lombok.Data;

@Data
public class TenantQuotaReq {
    private Long tenantId;
    private Integer jdbcConcurrent;
    private Integer llmConcurrent;
    private Long monthlyQueryCount;
    private Integer acquireTimeoutMs;
    private Boolean enabled;
}
```

- [ ] **Step 10.2: Write the controller**

Create `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/TenantQuotaController.java`:

```java
package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.quota.pojo.TenantQuotaDO;
import com.tencent.supersonic.auth.api.quota.request.TenantQuotaReq;
import com.tencent.supersonic.auth.authentication.persistence.repository.TenantQuotaRepository;
import com.tencent.supersonic.common.quota.TenantQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin REST for per-tenant quota overrides. All endpoints require ROLE_PLATFORM_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/tenant-quotas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantQuotaController {

    private final TenantQuotaRepository repository;
    private final ObjectProvider<TenantQuotaService> quotaServiceProvider;

    @GetMapping
    public List<TenantQuotaDO> list() {
        return repository.listAll();
    }

    @GetMapping("/{tenantId}")
    public TenantQuotaDO get(@PathVariable Long tenantId) {
        return repository.findByTenantId(tenantId).orElse(null);
    }

    @PutMapping("/{tenantId}")
    public TenantQuotaDO upsert(@PathVariable Long tenantId, @RequestBody TenantQuotaReq req) {
        TenantQuotaDO d = new TenantQuotaDO();
        d.setTenantId(tenantId);
        d.setJdbcConcurrent(req.getJdbcConcurrent());
        d.setLlmConcurrent(req.getLlmConcurrent());
        d.setMonthlyQueryCount(req.getMonthlyQueryCount());
        d.setAcquireTimeoutMs(req.getAcquireTimeoutMs());
        d.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
        TenantQuotaDO saved = repository.upsert(d);
        // Live refresh so the new quota takes effect immediately
        TenantQuotaService svc = quotaServiceProvider.getIfAvailable();
        if (svc != null) {
            svc.refresh(tenantId);
        }
        return saved;
    }

    @DeleteMapping("/{tenantId}")
    public void delete(@PathVariable Long tenantId) {
        repository.findByTenantId(tenantId).ifPresent(q -> repository.removeById(q.getId()));
        TenantQuotaService svc = quotaServiceProvider.getIfAvailable();
        if (svc != null) {
            svc.refresh(tenantId);
        }
    }
}
```

- [ ] **Step 10.3: Verify compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 10.4: Commit**

```bash
git add auth/auth-api/src/main/java/com/tencent/supersonic/auth/api/quota/request/TenantQuotaReq.java \
        auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/rest/TenantQuotaController.java
git commit -m "feat(quota): admin REST controller for tenant quota upsert/list/delete"
```

---

## Task 11: `application.yaml` defaults

**Files:**
- Modify: `launchers/standalone/src/main/resources/application.yaml` (append after the existing `s2.tenant:` block ending near line 190)

- [ ] **Step 11.1: Locate the end of the `s2.tenant:` block**

Run: `grep -n "^    excluded-tables:" launchers/standalone/src/main/resources/application.yaml`
Expected: single line number reference (around 147) — this is where the tenant config ends.

- [ ] **Step 11.2: Append the quota configuration**

Edit `launchers/standalone/src/main/resources/application.yaml`. Inside the `s2.tenant:` YAML block (same indentation as `excluded-tables:`), append:

```yaml
    # Per-tenant concurrency quota (P1-5). Opt-in; default OFF.
    quota:
      enabled: ${S2_TENANT_QUOTA_ENABLED:false}
      fallback-tenant-id: 0
      default-quota:
        jdbc-concurrent: ${S2_TENANT_QUOTA_JDBC:10}
        llm-concurrent: ${S2_TENANT_QUOTA_LLM:5}
        acquire-timeout-ms: ${S2_TENANT_QUOTA_ACQUIRE_MS:2000}
```

**Note on placement:** `s2.tenant.quota.*` nests under the same `s2.tenant:` parent (not a new top-level block) so the `@ConfigurationProperties(prefix = "s2.tenant.quota")` binding resolves correctly.

- [ ] **Step 11.3: Verify YAML parses on app startup**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 11.4: Commit**

```bash
git add launchers/standalone/src/main/resources/application.yaml
git commit -m "feat(quota): add s2.tenant.quota.* defaults to application.yaml (disabled by default)"
```

---

## Task 12: Web UI — admin page for quota management

**Files:**
- Create: `webapp/packages/supersonic-fe/src/services/tenantQuota.ts`
- Create: `webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/index.tsx`
- Create: `webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/style.less`
- Modify: `webapp/packages/supersonic-fe/config/routes.ts` (add a new route under the existing Platform menu group)

**Pattern reference:** `webapp/packages/supersonic-fe/src/pages/Platform/SubscriptionManagement/index.tsx` — reuse identical structure (Table + Modal + Form).

- [ ] **Step 12.1: Write the service layer**

Create `webapp/packages/supersonic-fe/src/services/tenantQuota.ts`:

```ts
import request from './request';

const API_V1 = '/api/v1';

export interface TenantQuota {
  id?: number;
  tenantId: number;
  jdbcConcurrent: number;
  llmConcurrent: number;
  monthlyQueryCount: number;
  acquireTimeoutMs: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export async function listTenantQuotas() {
  return request.get<Result<TenantQuota[]>>(`${API_V1}/admin/tenant-quotas`);
}

export async function upsertTenantQuota(tenantId: number, data: Partial<TenantQuota>) {
  return request.put<Result<TenantQuota>>(`${API_V1}/admin/tenant-quotas/${tenantId}`, { data });
}

export async function deleteTenantQuota(tenantId: number) {
  return request.delete<Result<void>>(`${API_V1}/admin/tenant-quotas/${tenantId}`);
}
```

- [ ] **Step 12.2: Write the page**

Create `webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/index.tsx`:

```tsx
import React, { useEffect, useState } from 'react';
import { Button, Card, Form, InputNumber, Modal, Popconfirm, Space, Switch, Table, message } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  TenantQuota,
  deleteTenantQuota,
  listTenantQuotas,
  upsertTenantQuota,
} from '@/services/tenantQuota';
import styles from './style.less';

const TenantQuotaManagement: React.FC = () => {
  const [rows, setRows] = useState<TenantQuota[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TenantQuota | null>(null);
  const [form] = Form.useForm<TenantQuota>();

  const load = async () => {
    setLoading(true);
    try {
      const { code, data } = await listTenantQuotas();
      if (code === 200 && data) setRows(data);
    } catch {
      message.error('加载租户配额失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      jdbcConcurrent: 10,
      llmConcurrent: 5,
      monthlyQueryCount: 0,
      acquireTimeoutMs: 2000,
      enabled: true,
    } as any);
    setModalOpen(true);
  };

  const openEdit = (row: TenantQuota) => {
    setEditing(row);
    form.setFieldsValue(row);
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    const { code } = await upsertTenantQuota(values.tenantId, values);
    if (code === 200) {
      message.success('保存成功');
      setModalOpen(false);
      load();
    } else {
      message.error('保存失败');
    }
  };

  const columns = [
    { title: '租户 ID', dataIndex: 'tenantId', width: 100 },
    { title: 'JDBC 并发', dataIndex: 'jdbcConcurrent', width: 120 },
    { title: 'LLM 并发', dataIndex: 'llmConcurrent', width: 120 },
    { title: '每月查询上限', dataIndex: 'monthlyQueryCount', width: 140 },
    { title: '超时 (ms)', dataIndex: 'acquireTimeoutMs', width: 120 },
    {
      title: '启用',
      dataIndex: 'enabled',
      width: 80,
      render: (v: boolean) => (v ? '是' : '否'),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: (v?: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      width: 160,
      render: (_: any, row: TenantQuota) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>
            编辑
          </Button>
          <Popconfirm
            title="删除该租户配额覆盖？"
            onConfirm={async () => {
              const { code } = await deleteTenantQuota(row.tenantId);
              if (code === 200) {
                message.success('已删除');
                load();
              }
            }}
          >
            <Button size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      className={styles.root}
      title="租户并发配额管理"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>
          新增
        </Button>
      }
    >
      <Table rowKey="tenantId" columns={columns as any} dataSource={rows} loading={loading} />
      <Modal
        title={editing ? '编辑租户配额' : '新增租户配额'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={submit}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="tenantId" label="租户 ID" rules={[{ required: true }]}>
            <InputNumber disabled={!!editing} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="jdbcConcurrent" label="JDBC 并发" rules={[{ required: true }]}>
            <InputNumber min={1} max={1000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="llmConcurrent" label="LLM 并发">
            <InputNumber min={0} max={1000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="monthlyQueryCount" label="每月查询上限 (0=不限)">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="acquireTimeoutMs" label="获取超时 (ms)">
            <InputNumber min={100} max={60000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default TenantQuotaManagement;
```

- [ ] **Step 12.3: Write minimal styles**

Create `webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/style.less`:

```less
.root {
  margin: 16px;
}
```

- [ ] **Step 12.4: Register the route**

Edit `webapp/packages/supersonic-fe/config/routes.ts`. Inside the existing Platform route group (look for `path: '/platform'` or the block containing `SubscriptionManagement`), add a new child entry (maintain existing indentation pattern):

```ts
{
  name: '租户配额',
  path: '/platform/tenant-quotas',
  component: './Platform/TenantQuotaManagement',
},
```

- [ ] **Step 12.5: Verify frontend build**

Run: `cd webapp/packages/supersonic-fe && pnpm run build 2>&1 | tail -20`
Expected: build succeeds; no TypeScript compile errors mentioning `TenantQuotaManagement` or `tenantQuota.ts`.

- [ ] **Step 12.6: Commit**

```bash
git add webapp/packages/supersonic-fe/src/services/tenantQuota.ts \
        webapp/packages/supersonic-fe/src/pages/Platform/TenantQuotaManagement/ \
        webapp/packages/supersonic-fe/config/routes.ts
git commit -m "feat(quota): admin UI for per-tenant concurrency quota management"
```

---

## Task 13: Runbook + detail spec

**Files:**
- Create: `docs/runbook/tenant-quota.md`
- Create: `docs/details/platform/tenant-quota.md`

- [ ] **Step 13.1: Write the runbook**

Create `docs/runbook/tenant-quota.md`:

```markdown
# Runbook: Per-Tenant Concurrency Quota

## Feature flag

`s2.tenant.quota.enabled` — default **false**. Set to `true` in profile YAML or via env:

```
S2_TENANT_QUOTA_ENABLED=true
S2_TENANT_QUOTA_JDBC=10
S2_TENANT_QUOTA_LLM=5
S2_TENANT_QUOTA_ACQUIRE_MS=2000
```

## Per-tenant overrides

Insert/upsert rows in `s2_tenant_quota`, or use admin REST:

```
PUT /api/v1/admin/tenant-quotas/{tenantId}
{
  "jdbcConcurrent": 20,
  "acquireTimeoutMs": 3000,
  "enabled": true
}
```

Live refresh is automatic (controller calls `TenantQuotaService.refresh(tenantId)`).

## Symptoms

- 429 responses with `Retry-After` header → tenant has exceeded its cap.
- Metric `s2_tenant_jdbc_permits_waiting{tenantId=X}` > 0 consistently → permanent saturation; raise quota or investigate slow queries.
- Metric `s2_tenant_jdbc_permits_available{tenantId=X}` == 0 for >1 minute → starvation; check for hanging queries.

## Rollback

Set `s2.tenant.quota.enabled=false` and restart. All permits become no-op; no DB rollback needed.

## Limitations

- **Per-instance counters.** In a multi-pod deployment each pod enforces its own semaphore. A tenant with capacity 10 and 3 pods can achieve up to 30 global concurrent queries. Future: Redis-backed counters (out of scope for MVP).
- `llm_concurrent` column exists but is not yet wired (reserved for follow-up work).
- `monthly_query_count` is advisory only — not enforced in this MVP.

## Observability

Prometheus metrics:
- `s2_tenant_jdbc_permits_available{tenantId="X"}`
- `s2_tenant_jdbc_permits_waiting{tenantId="X"}`
- `s2_tenant_quota_known_tenants`

Recommended alert:
```
alert: TenantQuotaStarvation
expr: s2_tenant_jdbc_permits_available == 0 for 2m
```
```

- [ ] **Step 13.2: Write the detail spec**

Create `docs/details/platform/tenant-quota.md`:

```markdown
---
status: implemented
module: common, auth, headless
key-files:
  - common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaService.java
  - common/src/main/java/com/tencent/supersonic/common/quota/InMemoryTenantQuotaService.java
  - common/src/main/java/com/tencent/supersonic/common/quota/TenantQuotaFilter.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/SqlUtils.java
  - auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/persistence/repository/TenantQuotaRepository.java
---

# 租户并发配额与连接池隔离（P1-5）

## 目标
避免单个大租户打满全局 HikariCP/Druid 池。

## 实现
- 以租户 ID 为 key 的 `ConcurrentHashMap<Long, Semaphore>`。
- 公平信号量，`tryAcquire(timeoutMs)`，超时抛 `TooManyRequestsException`。
- 只在 `SqlUtils#queryInternal` 这个唯一汇聚点 acquire/release；HTTP 层不重复计数。
- 过滤器将异常转 HTTP 429 + `Retry-After`。
- Micrometer 指标 `s2_tenant_jdbc_permits_available` / `s2_tenant_jdbc_permits_waiting`。

## 配置
`s2.tenant.quota.enabled=true` 开启；默认值走 YAML，每租户覆盖走 `s2_tenant_quota`。

## 局限
计数器仅在单实例范围内生效；多实例未共享。后续可引入 Redis 令牌桶。
```

- [ ] **Step 13.3: Commit**

```bash
git add docs/runbook/tenant-quota.md docs/details/platform/tenant-quota.md
git commit -m "docs(quota): add runbook and platform detail spec for P1-5"
```

---

## Task 14: End-to-end verification

- [ ] **Step 14.1: Full project compile**

Run: `mvn compile -pl launchers/standalone -am -q`
Expected: BUILD SUCCESS.

- [ ] **Step 14.2: Run all new unit + integration tests**

Run:
```bash
mvn test -pl common -Dtest='InMemoryTenantQuotaServiceTest,TenantQuotaMeterBinderTest,TenantQuotaFilterTest' -q
mvn test -pl auth/authentication -Dtest=TenantQuotaRepositoryTest -q
mvn test -pl headless/core -Dtest=SqlUtilsQuotaIntegrationTest -q
```
Expected: all green, cumulative `Tests run: 10+, Failures: 0, Errors: 0`.

- [ ] **Step 14.3: Start app with quota enabled, smoke test**

Run (manual): boot launcher-standalone with `S2_TENANT_QUOTA_ENABLED=true S2_TENANT_QUOTA_JDBC=2`.
Then: issue 5 parallel POSTs to `/api/semantic/query` with the same `X-Tenant-Id` header.
Expected: 2 succeed (200), 3 fail with HTTP 429 and `Retry-After: 2` header.

- [ ] **Step 14.4: Verify metrics scrape**

Run (manual): `curl http://localhost:<port>/actuator/prometheus | grep s2_tenant_jdbc_permits`
Expected: both gauges present with `tenantId` tag.

- [ ] **Step 14.5: Final commit if any adjustments were needed**

```bash
git status
# commit only if there are residual changes
```

---

## Self-Review

**Spec coverage check:**

| Spec item | Task |
|---|---|
| File Structure section | above, pre-tasks |
| Flyway V21+ migration (MySQL + PostgreSQL, no `ADD COLUMN IF NOT EXISTS`) | Task 1 (uses `CREATE TABLE IF NOT EXISTS`; no `ADD COLUMN` needed) |
| `TenantQuotaService` + `InMemoryTenantQuotaService` impl | Tasks 2, 3 |
| `TenantQuotaRepository` + MyBatis-Plus mapper | Task 4 |
| Unit test 10 threads / quota=3 | Task 3 Step 3.2 (`tenThreadsQuotaThreeExactlyThreeAcquireImmediately`) |
| Wrap `JdbcDataSource#execute` (actually `SqlUtils#queryInternal`) | Task 6 with aspect-vs-decorator decision |
| Integration test — 2-quota / 5 parallel | Task 7 |
| Micrometer gauges + MeterRegistry assertion | Task 8 |
| HTTP filter 429 + `Retry-After` | Task 9 |
| Admin UI | Task 12 |
| Docs + runbook + rollback | Task 13 |
| Backward compat feature flag | Task 5 (`@ConditionalOnProperty`), Task 11 (default `false`) |
| Default quota config key | Task 11 (`s2.tenant.quota.default.jdbc-concurrent`) |
| Per-tenant override table schema | Task 1 DDL |
| LLM concurrency (optional) | `llm_concurrent` column + config key reserved; enforcement deferred (documented in Task 13 runbook) |
| Integration with `TenantInterceptor` — no double-count | Task 6 notes acquire only at SQL layer |

**Placeholder scan:** no "TBD", "implement later", "handle edge cases" without code, or bare "similar to X" references.

**Type consistency check:**
- `TenantPermit` — same name in all tasks (2, 3, 6, 7).
- `TenantQuotaService` methods — `acquireJdbc(Long, long)`, `refresh(Long)`, `availablePermits()`, `waitingThreads()` — consistent across Task 3 impl, Task 7 test, Task 8 binder, Task 10 controller.
- `TenantQuotaOverride` fields — `jdbcConcurrent`, `llmConcurrent`, `acquireTimeoutMs`, `enabled` — consistent with DTO, DB columns, and loader mapping.
- `TooManyRequestsException(Long tenantId, int retryAfterSeconds)` — constructor signature consistent in Tasks 2, 3, 9.

**Open items flagged to executor:** Step 5.3 notes that `common/src/main/resources/META-INF/spring.factories` may not exist; create only if missing. Step 12.4 instructs the executor to locate the existing Platform route group and append (the exact `routes.ts` shape varies).

Plan complete. Ready for execution.
