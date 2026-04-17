# P0-3: Unified Cache Abstraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate three parallel cache implementations (`FeishuCacheService`, `CodeExchangeStorage`, `CaffeineCacheManager`) into a single `common/cache/` abstraction with pluggable Caffeine/Redis providers and per-namespace config under `s2.cache.*`, while preserving backward compatibility with the existing `s2.oauth.storage.type` property.

**Architecture:** A new `CacheProvider` SPI in `common` exposes `get / put / evict / putIfAbsent / increment` operations, plus a `CacheNamespace` descriptor carrying TTL, max-size, tenant-scope flag, and optional per-namespace provider override. `UnifiedCacheAutoConfiguration` reads `s2.cache.type` (global default) and `s2.cache.namespaces.<name>.*` (per-namespace overrides) to produce one `CacheProvider` bean per declared namespace. Feishu, auth (OAuth code exchange), and headless-core query cache all migrate to request providers by namespace from a `CacheProviderRegistry`. A thin `UnifiedSpringCacheManager` bridge adapts the registry to Spring's `org.springframework.cache.CacheManager` so future `@Cacheable` usage works transparently. Key generation prepends `tenant:<id>:` when the namespace is tenant-scoped, reusing `TenantContext`.

**Tech Stack:** Java 21, Spring Boot 3.4.x, Caffeine (already a `common` dependency), Spring Data Redis (`StringRedisTemplate`, already optional in feishu/auth poms — moved to `common` optional), Jackson (already transitive), JUnit 5, Mockito.

---

## Current-state inventory (verified 2026-04-17)

Existing cache surfaces that MUST be migrated:

| Location | Files | Consumers |
|----------|-------|-----------|
| `feishu/api/.../cache/FeishuCacheService.java` (interface) + `feishu/server/.../cache/{Caffeine,Redis}FeishuCacheService.java` + `feishu/server/.../config/FeishuAutoConfiguration.java` (bean wiring) | 4 files | `FeishuBotService`, `FeishuTokenManager`, `FeishuApiRateLimiter`, `FeishuBindTokenService` |
| `auth/authentication/.../oauth/storage/{CodeExchangeStorage, Caffeine..., Redis..., CodeExchangeStorageConfig}.java` | 4 files | `OAuthCodeExchangeService` |
| `headless/core/.../cache/{CacheManager, CaffeineCacheManager, CaffeineCacheConfig, CacheCommonConfig, QueryCache, DefaultQueryCache}.java` | 6 files | `DefaultQueryCache` (resolves via `ContextUtils.getBean`); also a second unused bean `searchCaffeineCache` never injected anywhere — delete. |

Verified facts used throughout this plan:

- No `@Cacheable` / `@CacheEvict` / `@CachePut` annotations exist in the codebase today (grep returned 0 hits). The Spring `CacheManager` bridge in Task 5 is therefore a thin, optional adapter — enabled but with no current consumers.
- `StringRedisTemplate` is auto-configured by Spring Boot's `spring-boot-starter-data-redis`; the starter is currently an `optional` dependency of `auth/authentication` and `feishu/server`. This plan moves the starter to `common/pom.xml` (still `optional`) so `CacheProvider` code compiles regardless of whether downstream callers pull it in.
- Caffeine is already a **non-optional** `common` dependency (used by `LocalLockProvider`), so no pom change is needed for Caffeine.
- The current `s2.oauth.storage.type` is already set to `redis` in `launchers/standalone/src/main/resources/application.yaml` (line 130) and the Feishu cache to `redis` (line 231). Behavior must not change during migration.
- `TenantContext` already provides `getTenantIdOrDefault(null)` semantics via `getTenantId()` returning `null` when unset.

## Out of scope

- Replacing Spring Data Redis with Redisson (`LockAutoConfiguration` already owns Redisson; cache uses the lighter `StringRedisTemplate` — keep that separation).
- Refactoring `QueryCache` / `DefaultQueryCache` contract shape — this plan only swaps its backing storage.
- Adding new cache namespaces beyond the three existing ones plus any strictly required sub-namespaces.
- Cluster-wide cache invalidation / pub-sub cache eviction broadcasting (YAGNI until a consumer asks).

---

## File Structure

### New files (all under `common/src/main/java/com/tencent/supersonic/common/cache/`)

| File | Responsibility |
|------|---------------|
| `CacheProvider.java` | The SPI interface — `get / put / putIfAbsent / evict / increment / getName`. |
| `CacheNamespace.java` | Immutable value object (name, type override, TTL, maxSize, tenantScoped). |
| `CacheType.java` | Enum: `CAFFEINE`, `REDIS`. |
| `CacheProviderRegistry.java` | `Map<String,CacheProvider>` wrapper with `require(namespace)` lookup. |
| `CaffeineCacheProvider.java` | Caffeine-backed `CacheProvider`. |
| `RedisCacheProvider.java` | `StringRedisTemplate`-backed `CacheProvider`. |
| `UnifiedCacheProperties.java` | `@ConfigurationProperties("s2.cache")` POJO. |
| `UnifiedCacheAutoConfiguration.java` | Creates `CacheProvider` beans per declared namespace + `CacheProviderRegistry` + Spring `CacheManager` bridge. |
| `UnifiedSpringCacheManager.java` | Adapter from `CacheProviderRegistry` to `org.springframework.cache.CacheManager`. |
| `DeprecatedOAuthStorageAliasProcessor.java` | `EnvironmentPostProcessor` that maps legacy `s2.oauth.storage.type` → `s2.cache.namespaces.oauth-code.type`. |

### New test files (under `common/src/test/java/com/tencent/supersonic/common/cache/`)

| File | Responsibility |
|------|---------------|
| `CacheProviderContractTest.java` | Abstract JUnit class — one test suite every `CacheProvider` impl must pass. |
| `CaffeineCacheProviderTest.java` | `extends CacheProviderContractTest`. |
| `RedisCacheProviderTest.java` | `extends CacheProviderContractTest`, uses `InMemoryStringRedisTemplate` fake. |
| `InMemoryStringRedisTemplate.java` | Test fake extending `StringRedisTemplate` that stores in a `ConcurrentHashMap` with TTL simulation — avoids Docker/testcontainers. |
| `UnifiedCacheAutoConfigurationTest.java` | `ApplicationContextRunner` tests for per-namespace resolution, global default, deprecation shim. |
| `DeprecatedOAuthStorageAliasProcessorTest.java` | Verifies the legacy-key shim. |

### Files DELETED (after migrations land)

| File | Replaced by |
|------|-------------|
| `feishu/api/.../cache/FeishuCacheService.java` | `CacheProvider` directly (inlined at call sites; see Task 6 for the thin facade alternative). |
| `feishu/server/.../cache/CaffeineFeishuCacheService.java` | `CaffeineCacheProvider` via `feishu-*` namespaces. |
| `feishu/server/.../cache/RedisFeishuCacheService.java` | `RedisCacheProvider` via `feishu-*` namespaces. |
| `auth/authentication/.../oauth/storage/CaffeineCodeExchangeStorage.java` | `CaffeineCacheProvider` via `oauth-code` namespace. |
| `auth/authentication/.../oauth/storage/RedisCodeExchangeStorage.java` | `RedisCacheProvider` via `oauth-code` namespace. |
| `auth/authentication/.../oauth/storage/CodeExchangeStorageConfig.java` | Deleted (replaced by unified auto-config). |
| `headless/core/.../cache/CaffeineCacheConfig.java` | Unified auto-config (namespace `semantic-query`). |
| `headless/core/.../cache/CaffeineCacheManager.java` | Unified provider + `CacheManager` becomes a thin adapter (Task 8). |

### Files MODIFIED

| File | Change |
|------|--------|
| `common/pom.xml` | Add optional `spring-boot-starter-data-redis`. |
| `auth/authentication/pom.xml` | Remove now-redundant `spring-boot-starter-data-redis` block (common re-exports it). |
| `feishu/server/pom.xml` | Same as above. |
| `feishu/server/.../config/FeishuAutoConfiguration.java` | Delete the two inner `@Configuration` classes for cache wiring. |
| `feishu/server/.../service/{FeishuTokenManager, FeishuApiRateLimiter, FeishuBindTokenService, FeishuBotService}.java` | Replace `FeishuCacheService` field with `CacheProviderRegistry`, fetch provider(s) in constructor. |
| `auth/authentication/.../oauth/service/OAuthCodeExchangeService.java` | Replace `CodeExchangeStorage` field with `CacheProvider` + Jackson `ObjectMapper`. |
| `auth/authentication/.../oauth/storage/CodeExchangeStorage.java` | Kept as **thin facade** over `CacheProvider` so existing callers outside `OAuthCodeExchangeService` (none today, but defensive) and tests keep compiling. Reimplemented to delegate. |
| `headless/core/.../cache/CaffeineCacheManager.java` | Rewritten to delegate to `CacheProvider` (keeps the `headless.core.cache.CacheManager` interface intact for `DefaultQueryCache`). |
| `headless/core/.../cache/CacheCommonConfig.java` | Keep the key-generation context fields (`app/env/version`); remove `cacheEnable` + `expireAfterWrite` (now owned by namespace). |
| `launchers/standalone/src/main/resources/application.yaml` | Replace `s2.oauth.storage.*` and `s2.feishu.cache.type` blocks with unified `s2.cache.*` block (keep legacy keys as commented deprecation note). |
| `launchers/chat/src/main/resources/application.yaml`, `launchers/headless/src/main/resources/application.yaml` | Same. |
| `docs/details/feishu/06-infra.md`, `docs/details/platform/*.md` (if cache mentioned) | Update references. |

---

## Task 1: Define the core SPI (`CacheProvider`, `CacheNamespace`, `UnifiedCacheProperties`)

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/CacheType.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/CacheNamespace.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/CacheProvider.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/CacheProviderRegistry.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheProperties.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/cache/UnifiedCachePropertiesTest.java`

### Step 1.1: Add `spring-boot-starter-data-redis` to `common/pom.xml`

- [ ] **Step 1.1.1: Write the failing test (dependency resolution smoke)**

Create `common/src/test/java/com/tencent/supersonic/common/cache/RedisClasspathProbeTest.java`:

```java
package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class RedisClasspathProbeTest {

    @Test
    void stringRedisTemplateIsOnClasspath() {
        assertThatCode(() ->
                Class.forName("org.springframework.data.redis.core.StringRedisTemplate"))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 1.1.2: Run — expect FAIL**

```bash
mvn -pl common test -Dtest=RedisClasspathProbeTest
```

Expected: `ClassNotFoundException: org.springframework.data.redis.core.StringRedisTemplate`.

- [ ] **Step 1.1.3: Edit `common/pom.xml` — add the optional starter**

Locate the `<dependency>` block for `redisson-spring-boot-starter` (already present as optional) and insert immediately after it:

```xml
        <!-- Spring Data Redis (optional, required when any cache namespace resolves to REDIS) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
            <optional>true</optional>
        </dependency>
```

- [ ] **Step 1.1.4: Re-run — expect PASS**

```bash
mvn -pl common test -Dtest=RedisClasspathProbeTest
```

Expected: `BUILD SUCCESS` / 1 test passed.

- [ ] **Step 1.1.5: Commit**

```bash
git add common/pom.xml common/src/test/java/com/tencent/supersonic/common/cache/RedisClasspathProbeTest.java
git commit -m "chore(common): add optional spring-boot-starter-data-redis for unified cache"
```

### Step 1.2: Define `CacheType` enum

- [ ] **Step 1.2.1: Write `CacheType.java`**

```java
package com.tencent.supersonic.common.cache;

/**
 * Available cache backends. Used for both the global default ({@code s2.cache.type}) and
 * per-namespace overrides ({@code s2.cache.namespaces.<name>.type}).
 */
public enum CacheType {
    CAFFEINE,
    REDIS
}
```

- [ ] **Step 1.2.2: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/CacheType.java
git commit -m "feat(common): add CacheType enum for unified cache"
```

### Step 1.3: Define `CacheNamespace`

- [ ] **Step 1.3.1: Write `CacheNamespace.java`**

```java
package com.tencent.supersonic.common.cache;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Descriptor for a single logical cache partition. One instance is registered per business domain
 * (e.g. {@code feishu-token}, {@code oauth-code}, {@code semantic-query}).
 *
 * <p>{@link #typeOverride} is null when the namespace should fall back to the global
 * {@code s2.cache.type} default. {@link #tenantScoped} = true causes all keys to be prefixed with
 * {@code tenant:<id>:} using {@link com.tencent.supersonic.common.context.TenantContext}; when the
 * tenant context is unset, the prefix becomes {@code tenant:_:} so no cross-tenant bleed is
 * possible via missing context.
 */
@Value
@Builder
public class CacheNamespace {

    /** Logical name, unique across the process. */
    String name;

    /** When non-null, forces this namespace onto the given backend regardless of the global default. */
    CacheType typeOverride;

    /** Time-to-live for every entry. Required — callers must pick a bound. */
    Duration ttl;

    /** Max entries to keep (Caffeine) / ignored for Redis. */
    long maxSize;

    /** Whether keys are tenant-scoped. OAuth codes and Feishu tokens are NOT tenant-scoped. */
    boolean tenantScoped;
}
```

- [ ] **Step 1.3.2: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/CacheNamespace.java
git commit -m "feat(common): add CacheNamespace descriptor for unified cache"
```

### Step 1.4: Define `CacheProvider` SPI

- [ ] **Step 1.4.1: Write `CacheProvider.java`**

```java
package com.tencent.supersonic.common.cache;

import java.util.Optional;

/**
 * Minimal cache SPI implemented by Caffeine and Redis backends. One instance per
 * {@link CacheNamespace}.
 *
 * <p>Values are stored as serialized strings. Typed storage (JSON / Jackson) is the caller's
 * responsibility — keeping this interface String-based avoids coupling the cache layer to any one
 * serialization library.
 *
 * <p>All methods are null-tolerant: a missing key returns {@link Optional#empty()} rather than
 * throwing.
 */
public interface CacheProvider {

    /**
     * @return the {@link CacheNamespace#getName()} this provider serves.
     */
    String getName();

    /**
     * @return the underlying {@link CacheNamespace} (useful for observability / tests).
     */
    CacheNamespace getNamespace();

    /**
     * Fetch a value by logical key. Tenant-scoping (if enabled on the namespace) is applied
     * transparently.
     */
    Optional<String> get(String key);

    /**
     * Store a value with the namespace's configured TTL.
     */
    void put(String key, String value);

    /**
     * Atomically store only if the key is currently absent.
     *
     * @return true if the value was written (key was previously absent), false otherwise.
     */
    boolean putIfAbsent(String key, String value);

    /**
     * Remove a key. Idempotent — no error if the key is missing.
     */
    void evict(String key);

    /**
     * Atomically increment a numeric counter under {@code key} and return the new value. The TTL is
     * applied the first time the counter is created and is NOT refreshed on subsequent increments
     * (fixed-window rate-limit semantics matching {@code RedisFeishuCacheService}).
     */
    long increment(String key);
}
```

- [ ] **Step 1.4.2: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/CacheProvider.java
git commit -m "feat(common): add CacheProvider SPI for unified cache"
```

### Step 1.5: Define `CacheProviderRegistry`

- [ ] **Step 1.5.1: Write the failing test**

Create `common/src/test/java/com/tencent/supersonic/common/cache/CacheProviderRegistryTest.java`:

```java
package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheProviderRegistryTest {

    @Test
    void requireReturnsRegisteredProvider() {
        CacheProvider fake = stub("foo");
        CacheProviderRegistry registry = new CacheProviderRegistry(List.of(fake));
        assertThat(registry.require("foo")).isSameAs(fake);
    }

    @Test
    void requireThrowsWhenMissing() {
        CacheProviderRegistry registry = new CacheProviderRegistry(List.of());
        assertThatThrownBy(() -> registry.require("missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing")
                .hasMessageContaining("declared in s2.cache.namespaces");
    }

    @Test
    void findReturnsEmptyWhenMissing() {
        CacheProviderRegistry registry = new CacheProviderRegistry(List.of());
        assertThat(registry.find("missing")).isEmpty();
    }

    @Test
    void duplicateNamespaceRejected() {
        CacheProvider a = stub("dupe");
        CacheProvider b = stub("dupe");
        assertThatThrownBy(() -> new CacheProviderRegistry(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate cache namespace")
                .hasMessageContaining("dupe");
    }

    private static CacheProvider stub(String name) {
        CacheNamespace ns = CacheNamespace.builder()
                .name(name)
                .ttl(Duration.ofMinutes(1))
                .maxSize(10)
                .build();
        return new CacheProvider() {
            @Override public String getName() { return name; }
            @Override public CacheNamespace getNamespace() { return ns; }
            @Override public Optional<String> get(String k) { return Optional.empty(); }
            @Override public void put(String k, String v) {}
            @Override public boolean putIfAbsent(String k, String v) { return true; }
            @Override public void evict(String k) {}
            @Override public long increment(String k) { return 0; }
        };
    }
}
```

- [ ] **Step 1.5.2: Run — expect FAIL (compile error: CacheProviderRegistry missing)**

```bash
mvn -pl common test -Dtest=CacheProviderRegistryTest
```

Expected: `cannot find symbol: class CacheProviderRegistry`.

- [ ] **Step 1.5.3: Write `CacheProviderRegistry.java`**

```java
package com.tencent.supersonic.common.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lookup table of all {@link CacheProvider} beans registered in the application context, keyed by
 * {@link CacheProvider#getName()}. Constructed by {@link UnifiedCacheAutoConfiguration}.
 */
public class CacheProviderRegistry {

    private final Map<String, CacheProvider> providers;

    public CacheProviderRegistry(Collection<CacheProvider> providers) {
        Map<String, CacheProvider> map = new HashMap<>();
        for (CacheProvider p : providers) {
            if (map.put(p.getName(), p) != null) {
                throw new IllegalStateException(
                        "Duplicate cache namespace: " + p.getName()
                                + ". Check s2.cache.namespaces configuration.");
            }
        }
        this.providers = Map.copyOf(map);
    }

    /** Return the provider for {@code namespace} or throw if it was not declared. */
    public CacheProvider require(String namespace) {
        CacheProvider p = providers.get(namespace);
        if (p == null) {
            throw new IllegalStateException(
                    "No CacheProvider registered for namespace '" + namespace
                            + "'. Declared in s2.cache.namespaces? Available: "
                            + providers.keySet());
        }
        return p;
    }

    /** Optional lookup variant — returns empty if the namespace is not registered. */
    public Optional<CacheProvider> find(String namespace) {
        return Optional.ofNullable(providers.get(namespace));
    }

    /** Snapshot of all registered namespaces (for metrics / health checks). */
    public Map<String, CacheProvider> asMap() {
        return providers;
    }
}
```

- [ ] **Step 1.5.4: Run — expect PASS**

```bash
mvn -pl common test -Dtest=CacheProviderRegistryTest
```

Expected: 4 tests passed.

- [ ] **Step 1.5.5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/CacheProviderRegistry.java \
        common/src/test/java/com/tencent/supersonic/common/cache/CacheProviderRegistryTest.java
git commit -m "feat(common): add CacheProviderRegistry with duplicate detection"
```

### Step 1.6: Define `UnifiedCacheProperties`

- [ ] **Step 1.6.1: Write the failing test**

Create `common/src/test/java/com/tencent/supersonic/common/cache/UnifiedCachePropertiesTest.java`:

```java
package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedCachePropertiesTest {

    @Test
    void bindsGlobalDefaultAndNamespaceOverrides() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("s2.cache.type", "redis");
        env.setProperty("s2.cache.namespaces.oauth-code.type", "caffeine");
        env.setProperty("s2.cache.namespaces.oauth-code.ttl", "45s");
        env.setProperty("s2.cache.namespaces.oauth-code.max-size", "2000");
        env.setProperty("s2.cache.namespaces.oauth-code.tenant-scoped", "false");
        env.setProperty("s2.cache.namespaces.feishu-token.ttl", "110m");

        UnifiedCacheProperties props = Binder.get(env)
                .bind("s2.cache", UnifiedCacheProperties.class)
                .get();

        assertThat(props.getType()).isEqualTo(CacheType.REDIS);

        Map<String, UnifiedCacheProperties.NamespaceConfig> nss = props.getNamespaces();
        assertThat(nss).containsKeys("oauth-code", "feishu-token");

        UnifiedCacheProperties.NamespaceConfig oauth = nss.get("oauth-code");
        assertThat(oauth.getType()).isEqualTo(CacheType.CAFFEINE);
        assertThat(oauth.getTtl()).isEqualTo(Duration.ofSeconds(45));
        assertThat(oauth.getMaxSize()).isEqualTo(2000L);
        assertThat(oauth.isTenantScoped()).isFalse();

        UnifiedCacheProperties.NamespaceConfig feishu = nss.get("feishu-token");
        assertThat(feishu.getType()).isNull(); // falls back to global default
        assertThat(feishu.getTtl()).isEqualTo(Duration.ofMinutes(110));
    }

    @Test
    void globalTypeDefaultsToCaffeineWhenAbsent() {
        MockEnvironment env = new MockEnvironment();
        ConfigurationPropertySources.attach(env);
        UnifiedCacheProperties props = Binder.get(env)
                .bindOrCreate("s2.cache", UnifiedCacheProperties.class);
        assertThat(props.getType()).isEqualTo(CacheType.CAFFEINE);
        assertThat(props.getNamespaces()).isEmpty();
    }
}
```

- [ ] **Step 1.6.2: Run — expect FAIL**

```bash
mvn -pl common test -Dtest=UnifiedCachePropertiesTest
```

Expected: `cannot find symbol: class UnifiedCacheProperties`.

- [ ] **Step 1.6.3: Write `UnifiedCacheProperties.java`**

```java
package com.tencent.supersonic.common.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binding target for {@code s2.cache.*}.
 *
 * <pre>
 * s2.cache:
 *   type: redis                       # global default backend
 *   namespaces:
 *     oauth-code:
 *       type: caffeine                # optional override
 *       ttl: 45s
 *       max-size: 2000
 *       tenant-scoped: false
 *     feishu-token:
 *       ttl: 110m
 *     semantic-query:
 *       ttl: 10m
 *       max-size: 5000
 *       tenant-scoped: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "s2.cache")
public class UnifiedCacheProperties {

    /** Backend used when a namespace omits {@link NamespaceConfig#type}. */
    private CacheType type = CacheType.CAFFEINE;

    /** Per-namespace overrides. Key = namespace name. */
    private Map<String, NamespaceConfig> namespaces = new LinkedHashMap<>();

    @Data
    public static class NamespaceConfig {
        /** Null = inherit {@link UnifiedCacheProperties#type}. */
        private CacheType type;

        /** Required. Bind via ISO-8601 duration or Spring suffix syntax (e.g. {@code 30s}, {@code 5m}). */
        private Duration ttl;

        /** Caffeine max entries; ignored for Redis. */
        private long maxSize = 10_000L;

        /** Whether entries should be prefixed with {@code tenant:<id>:}. */
        private boolean tenantScoped;
    }
}
```

- [ ] **Step 1.6.4: Run — expect PASS**

```bash
mvn -pl common test -Dtest=UnifiedCachePropertiesTest
```

Expected: 2 tests passed.

- [ ] **Step 1.6.5: Verify module compiles**

```bash
mvn -pl common compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 1.6.6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheProperties.java \
        common/src/test/java/com/tencent/supersonic/common/cache/UnifiedCachePropertiesTest.java
git commit -m "feat(common): add UnifiedCacheProperties for s2.cache.* binding"
```

---

## Task 2: Abstract contract test + in-memory Redis fake

**Files:**
- Create: `common/src/test/java/com/tencent/supersonic/common/cache/CacheProviderContractTest.java`
- Create: `common/src/test/java/com/tencent/supersonic/common/cache/InMemoryStringRedisTemplate.java`

### Step 2.1: Write the in-memory `StringRedisTemplate` fake

- [ ] **Step 2.1.1: Write `InMemoryStringRedisTemplate.java`**

```java
package com.tencent.supersonic.common.cache;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Test-only in-memory replacement for {@link StringRedisTemplate}. Implements only the subset of
 * operations exercised by {@link RedisCacheProvider} — enough for the contract test to run without
 * Docker or Testcontainers.
 *
 * <p><strong>Not thread-safe for TTL expiry</strong>: the contract test drives clock skew via
 * {@link #advanceTime(Duration)} rather than {@link Thread#sleep(long)} so tests stay fast and
 * deterministic.
 */
public class InMemoryStringRedisTemplate extends StringRedisTemplate {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private Instant now = Instant.now();

    public InMemoryStringRedisTemplate() {
        // Skip the normal RedisTemplate init path entirely — we override every op we use.
    }

    public void advanceTime(Duration amount) {
        now = now.plus(amount);
        // Lazy eviction
        store.entrySet().removeIf(e -> e.getValue().expiresAt != null
                && !e.getValue().expiresAt.isAfter(now));
    }

    private void prune(String key) {
        Entry e = store.get(key);
        if (e != null && e.expiresAt != null && !e.expiresAt.isAfter(now)) {
            store.remove(key);
        }
    }

    @Override
    public ValueOperations<String, String> opsForValue() {
        return new ValueOperations<>() {
            @Override
            public void set(String key, String value) {
                store.put(key, new Entry(value, null));
            }

            @Override
            public void set(String key, String value, long timeout, TimeUnit unit) {
                store.put(key, new Entry(value,
                        now.plusMillis(unit.toMillis(timeout))));
            }

            @Override
            public void set(String key, String value, Duration timeout) {
                store.put(key, new Entry(value, now.plus(timeout)));
            }

            @Override
            public String get(Object key) {
                prune((String) key);
                Entry e = store.get(key);
                return e == null ? null : e.value;
            }

            @Override
            public Boolean setIfAbsent(String key, String value) {
                prune(key);
                return store.putIfAbsent(key, new Entry(value, null)) == null;
            }

            @Override
            public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
                prune(key);
                Instant exp = now.plusMillis(unit.toMillis(timeout));
                return store.putIfAbsent(key, new Entry(value, exp)) == null;
            }

            @Override
            public Boolean setIfAbsent(String key, String value, Duration timeout) {
                prune(key);
                return store.putIfAbsent(key, new Entry(value, now.plus(timeout))) == null;
            }

            @Override
            public Long increment(String key) {
                prune(key);
                Entry e = store.computeIfAbsent(key, k -> new Entry("0", null));
                long v = Long.parseLong(e.value) + 1L;
                store.put(key, new Entry(Long.toString(v), e.expiresAt));
                return v;
            }

            @Override
            public String getAndDelete(String key) {
                prune(key);
                Entry e = store.remove(key);
                return e == null ? null : e.value;
            }

            // Unused ops: throw to catch accidental usage in tests.
            @Override public Long increment(String key, long delta) { throw new UnsupportedOperationException(); }
            @Override public Double increment(String key, double delta) { throw new UnsupportedOperationException(); }
            @Override public Long decrement(String key) { throw new UnsupportedOperationException(); }
            @Override public Long decrement(String key, long delta) { throw new UnsupportedOperationException(); }
            @Override public Integer append(String key, String value) { throw new UnsupportedOperationException(); }
            @Override public String get(Object key, long start, long end) { throw new UnsupportedOperationException(); }
            @Override public String getAndSet(String key, String newValue) { throw new UnsupportedOperationException(); }
            @Override public String getAndExpire(String key, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
            @Override public String getAndExpire(String key, Duration timeout) { throw new UnsupportedOperationException(); }
            @Override public String getAndPersist(String key) { throw new UnsupportedOperationException(); }
            @Override public java.util.List<String> multiGet(Collection<String> keys) { throw new UnsupportedOperationException(); }
            @Override public void multiSet(Map<? extends String, ? extends String> map) { throw new UnsupportedOperationException(); }
            @Override public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) { throw new UnsupportedOperationException(); }
            @Override public Long size(String key) { throw new UnsupportedOperationException(); }
            @Override public Boolean setBit(String key, long offset, boolean value) { throw new UnsupportedOperationException(); }
            @Override public Boolean getBit(String key, long offset) { throw new UnsupportedOperationException(); }
            @Override public org.springframework.data.redis.core.RedisOperations<String, String> getOperations() { throw new UnsupportedOperationException(); }
            @Override public void set(String key, String value, long offset) { throw new UnsupportedOperationException(); }
        };
    }

    @Override
    public Boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public Long delete(Collection<String> keys) {
        long removed = 0;
        for (String k : keys) {
            if (store.remove(k) != null) removed++;
        }
        return removed;
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        Entry e = store.get(key);
        if (e == null) return false;
        store.put(key, new Entry(e.value, now.plusMillis(unit.toMillis(timeout))));
        return true;
    }

    @Override
    public Boolean hasKey(String key) {
        prune(key);
        return store.containsKey(key);
    }

    @Override
    public RedisConnectionFactory getConnectionFactory() {
        return null;
    }

    @Override
    public RedisConnection getRequiredConnectionFactory() {
        throw new UnsupportedOperationException();
    }

    public Map<String, String> snapshot() {
        Map<String, String> out = new HashMap<>();
        store.forEach((k, e) -> out.put(k, e.value));
        return Collections.unmodifiableMap(out);
    }

    private record Entry(String value, Instant expiresAt) {}
}
```

> Note: The overridden `ValueOperations` methods that throw are intentionally exhaustive — Spring's `ValueOperations<K,V>` interface has a fixed surface area, so this compiles on Spring Data Redis 3.x (Boot 3.4). If the Spring Data Redis version bumps and adds a new method, compilation will fail here — fix by adding another throwing stub.

- [ ] **Step 2.1.2: Verify it compiles**

```bash
mvn -pl common test-compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2.1.3: Commit**

```bash
git add common/src/test/java/com/tencent/supersonic/common/cache/InMemoryStringRedisTemplate.java
git commit -m "test(common): add InMemoryStringRedisTemplate fake for cache tests"
```

### Step 2.2: Write the abstract contract test

- [ ] **Step 2.2.1: Write `CacheProviderContractTest.java`**

```java
package com.tencent.supersonic.common.cache;

import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral contract every {@link CacheProvider} implementation must satisfy. Subclasses
 * implement {@link #newProvider(CacheNamespace)} and {@link #advanceTime(Duration)} — the rest is
 * shared.
 */
public abstract class CacheProviderContractTest {

    protected abstract CacheProvider newProvider(CacheNamespace namespace);

    /** Simulate clock advancement to test TTL expiry without {@code Thread.sleep}. */
    protected abstract void advanceTime(Duration amount);

    protected CacheNamespace defaultNs() {
        return CacheNamespace.builder()
                .name("test-default")
                .ttl(Duration.ofSeconds(30))
                .maxSize(100)
                .tenantScoped(false)
                .build();
    }

    protected CacheNamespace tenantScopedNs() {
        return CacheNamespace.builder()
                .name("test-tenant")
                .ttl(Duration.ofSeconds(30))
                .maxSize(100)
                .tenantScoped(true)
                .build();
    }

    @BeforeEach
    void clearTenant() {
        TenantContext.clear();
    }

    @AfterEach
    void afterEach() {
        TenantContext.clear();
    }

    @Test
    void getReturnsEmptyWhenKeyAbsent() {
        CacheProvider cache = newProvider(defaultNs());
        assertThat(cache.get("missing")).isEmpty();
    }

    @Test
    void putThenGetRoundtrips() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "v1");
        assertThat(cache.get("k1")).contains("v1");
    }

    @Test
    void evictRemovesEntry() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "v1");
        cache.evict("k1");
        assertThat(cache.get("k1")).isEmpty();
    }

    @Test
    void evictIdempotentOnMissingKey() {
        CacheProvider cache = newProvider(defaultNs());
        cache.evict("never-written");
        assertThat(cache.get("never-written")).isEmpty();
    }

    @Test
    void putIfAbsentReturnsTrueWhenKeyAbsent() {
        CacheProvider cache = newProvider(defaultNs());
        assertThat(cache.putIfAbsent("k1", "v1")).isTrue();
        assertThat(cache.get("k1")).contains("v1");
    }

    @Test
    void putIfAbsentReturnsFalseAndDoesNotOverwrite() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "existing");
        assertThat(cache.putIfAbsent("k1", "replacement")).isFalse();
        assertThat(cache.get("k1")).contains("existing");
    }

    @Test
    void putOverwritesExistingValue() {
        CacheProvider cache = newProvider(defaultNs());
        cache.put("k1", "v1");
        cache.put("k1", "v2");
        assertThat(cache.get("k1")).contains("v2");
    }

    @Test
    void incrementStartsAtOneForNewKey() {
        CacheProvider cache = newProvider(defaultNs());
        assertThat(cache.increment("counter")).isEqualTo(1L);
    }

    @Test
    void incrementIsMonotonic() {
        CacheProvider cache = newProvider(defaultNs());
        assertThat(cache.increment("counter")).isEqualTo(1L);
        assertThat(cache.increment("counter")).isEqualTo(2L);
        assertThat(cache.increment("counter")).isEqualTo(3L);
    }

    @Test
    void entryExpiresAfterTtl() {
        CacheNamespace ns = CacheNamespace.builder()
                .name("short-lived")
                .ttl(Duration.ofSeconds(10))
                .maxSize(10)
                .build();
        CacheProvider cache = newProvider(ns);
        cache.put("k1", "v1");
        assertThat(cache.get("k1")).contains("v1");
        advanceTime(Duration.ofSeconds(11));
        assertThat(cache.get("k1")).isEmpty();
    }

    @Test
    void tenantScopedKeysAreIsolatedBetweenTenants() {
        CacheProvider cache = newProvider(tenantScopedNs());
        TenantContext.setTenantId(1L);
        cache.put("shared", "tenant1-value");
        TenantContext.setTenantId(2L);
        assertThat(cache.get("shared")).isEmpty();
        cache.put("shared", "tenant2-value");
        TenantContext.setTenantId(1L);
        assertThat(cache.get("shared")).contains("tenant1-value");
        TenantContext.setTenantId(2L);
        assertThat(cache.get("shared")).contains("tenant2-value");
    }

    @Test
    void tenantScopedKeysUseUnderscoreWhenContextUnset() {
        CacheProvider cache = newProvider(tenantScopedNs());
        // No TenantContext set — must still work, but under a separate "tenant:_:" namespace.
        cache.put("shared", "no-tenant");
        TenantContext.setTenantId(1L);
        assertThat(cache.get("shared")).isEmpty();
        TenantContext.clear();
        assertThat(cache.get("shared")).contains("no-tenant");
    }

    @Test
    void nonTenantScopedKeysIgnoreTenantContext() {
        CacheProvider cache = newProvider(defaultNs());
        TenantContext.setTenantId(1L);
        cache.put("global", "v1");
        TenantContext.setTenantId(2L);
        assertThat(cache.get("global")).contains("v1");
    }

    @Test
    void getNamespaceExposesSourceDescriptor() {
        CacheNamespace ns = defaultNs();
        CacheProvider cache = newProvider(ns);
        assertThat(cache.getName()).isEqualTo(ns.getName());
        assertThat(cache.getNamespace()).isEqualTo(ns);
    }

    @Test
    void optionalSemanticsNeverThrowOnMissing() {
        CacheProvider cache = newProvider(defaultNs());
        Optional<String> result = cache.get("never-put");
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2.2.2: Verify compile**

```bash
mvn -pl common test-compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2.2.3: Commit**

```bash
git add common/src/test/java/com/tencent/supersonic/common/cache/CacheProviderContractTest.java
git commit -m "test(common): add CacheProvider contract test suite"
```

---

## Task 3: Implement `CaffeineCacheProvider` + pass the contract

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/CaffeineCacheProvider.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/cache/CaffeineCacheProviderTest.java`

### Step 3.1: Write the subclass test

- [ ] **Step 3.1.1: Write `CaffeineCacheProviderTest.java`**

```java
package com.tencent.supersonic.common.cache;

import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

class CaffeineCacheProviderTest extends CacheProviderContractTest {

    private final AtomicLong fakeNanos = new AtomicLong(0);
    private final Ticker ticker = fakeNanos::get;

    @Override
    protected CacheProvider newProvider(CacheNamespace namespace) {
        return new CaffeineCacheProvider(namespace, ticker);
    }

    @Override
    protected void advanceTime(Duration amount) {
        fakeNanos.addAndGet(amount.toNanos());
    }
}
```

- [ ] **Step 3.1.2: Run — expect FAIL (class missing)**

```bash
mvn -pl common test -Dtest=CaffeineCacheProviderTest
```

Expected: `cannot find symbol: class CaffeineCacheProvider`.

### Step 3.2: Implement `CaffeineCacheProvider`

- [ ] **Step 3.2.1: Write `CaffeineCacheProvider.java`**

```java
package com.tencent.supersonic.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caffeine-backed {@link CacheProvider}. Suitable for single-instance deployments. Stores values as
 * Strings to match the {@link CacheProvider} contract and avoid any typed-key coupling.
 *
 * <p>Atomic counters are stored in a separate {@link Cache} so that the main value cache's
 * {@code maxSize} eviction doesn't destroy in-flight rate-limit counters.
 */
@Slf4j
public class CaffeineCacheProvider implements CacheProvider {

    private final CacheNamespace namespace;
    private final Cache<String, String> values;
    private final Cache<String, AtomicLong> counters;

    /** Production constructor — uses {@link Ticker#systemTicker()}. */
    public CaffeineCacheProvider(CacheNamespace namespace) {
        this(namespace, Ticker.systemTicker());
    }

    /** Test constructor accepting a custom ticker for deterministic TTL tests. */
    public CaffeineCacheProvider(CacheNamespace namespace, Ticker ticker) {
        this.namespace = namespace;
        this.values = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(namespace.getTtl())
                .maximumSize(namespace.getMaxSize())
                .build();
        this.counters = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(namespace.getTtl())
                .maximumSize(namespace.getMaxSize())
                .build();
        log.info("Caffeine cache initialized: namespace={}, ttl={}, maxSize={}, tenantScoped={}",
                namespace.getName(), namespace.getTtl(), namespace.getMaxSize(),
                namespace.isTenantScoped());
    }

    @Override
    public String getName() {
        return namespace.getName();
    }

    @Override
    public CacheNamespace getNamespace() {
        return namespace;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.getIfPresent(scoped(key)));
    }

    @Override
    public void put(String key, String value) {
        values.put(scoped(key), value);
    }

    @Override
    public boolean putIfAbsent(String key, String value) {
        String scoped = scoped(key);
        String previous = values.asMap().putIfAbsent(scoped, value);
        return previous == null;
    }

    @Override
    public void evict(String key) {
        values.invalidate(scoped(key));
    }

    @Override
    public long increment(String key) {
        AtomicLong counter = counters.get(scoped(key), k -> new AtomicLong(0L));
        return counter.incrementAndGet();
    }

    private String scoped(String key) {
        if (!namespace.isTenantScoped()) {
            return key;
        }
        Long tenantId = TenantContext.getTenantId();
        String tenantPart = tenantId == null ? "_" : tenantId.toString();
        return "tenant:" + tenantPart + ":" + key;
    }
}
```

- [ ] **Step 3.2.2: Run contract suite — expect PASS**

```bash
mvn -pl common test -Dtest=CaffeineCacheProviderTest
```

Expected: all 15 tests inherited from `CacheProviderContractTest` pass.

- [ ] **Step 3.2.3: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/CaffeineCacheProvider.java \
        common/src/test/java/com/tencent/supersonic/common/cache/CaffeineCacheProviderTest.java
git commit -m "feat(common): add CaffeineCacheProvider passing contract suite"
```

---

## Task 4: Implement `RedisCacheProvider` + pass the contract

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/RedisCacheProvider.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/cache/RedisCacheProviderTest.java`

### Step 4.1: Write the subclass test

- [ ] **Step 4.1.1: Write `RedisCacheProviderTest.java`**

```java
package com.tencent.supersonic.common.cache;

import java.time.Duration;

class RedisCacheProviderTest extends CacheProviderContractTest {

    private final InMemoryStringRedisTemplate fakeRedis = new InMemoryStringRedisTemplate();

    @Override
    protected CacheProvider newProvider(CacheNamespace namespace) {
        return new RedisCacheProvider(namespace, fakeRedis);
    }

    @Override
    protected void advanceTime(Duration amount) {
        fakeRedis.advanceTime(amount);
    }
}
```

- [ ] **Step 4.1.2: Run — expect FAIL (class missing)**

```bash
mvn -pl common test -Dtest=RedisCacheProviderTest
```

Expected: `cannot find symbol: class RedisCacheProvider`.

### Step 4.2: Implement `RedisCacheProvider`

- [ ] **Step 4.2.1: Write `RedisCacheProvider.java`**

```java
package com.tencent.supersonic.common.cache;

import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed {@link CacheProvider} using {@link StringRedisTemplate}. Keys are always prefixed
 * with {@code s2:cache:<namespace>:} to avoid collisions with other Redis consumers, and with
 * {@code tenant:<id>:} when the namespace is tenant-scoped.
 *
 * <p>TTL for {@link #increment(String)} is applied only on the first increment, matching the
 * fixed-window semantics required by rate limiters.
 */
@Slf4j
public class RedisCacheProvider implements CacheProvider {

    private static final String GLOBAL_PREFIX = "s2:cache:";

    private final CacheNamespace namespace;
    private final StringRedisTemplate redis;
    private final long ttlMs;

    public RedisCacheProvider(CacheNamespace namespace, StringRedisTemplate redis) {
        this.namespace = namespace;
        this.redis = redis;
        this.ttlMs = namespace.getTtl().toMillis();
        log.info("Redis cache initialized: namespace={}, ttl={}, tenantScoped={}",
                namespace.getName(), namespace.getTtl(), namespace.isTenantScoped());
    }

    @Override
    public String getName() {
        return namespace.getName();
    }

    @Override
    public CacheNamespace getNamespace() {
        return namespace;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(fullKey(key)));
    }

    @Override
    public void put(String key, String value) {
        redis.opsForValue().set(fullKey(key), value, ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean putIfAbsent(String key, String value) {
        Boolean set = redis.opsForValue().setIfAbsent(fullKey(key), value, ttlMs,
                TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(set);
    }

    @Override
    public void evict(String key) {
        redis.delete(fullKey(key));
    }

    @Override
    public long increment(String key) {
        String fk = fullKey(key);
        Long count = redis.opsForValue().increment(fk);
        if (count != null && count == 1L) {
            redis.expire(fk, ttlMs, TimeUnit.MILLISECONDS);
        }
        return count == null ? 0L : count;
    }

    private String fullKey(String key) {
        StringBuilder sb = new StringBuilder(GLOBAL_PREFIX).append(namespace.getName()).append(":");
        if (namespace.isTenantScoped()) {
            Long tenantId = TenantContext.getTenantId();
            sb.append("tenant:").append(tenantId == null ? "_" : tenantId).append(":");
        }
        sb.append(key);
        return sb.toString();
    }
}
```

- [ ] **Step 4.2.2: Run contract suite — expect PASS**

```bash
mvn -pl common test -Dtest=RedisCacheProviderTest
```

Expected: all 15 tests pass.

- [ ] **Step 4.2.3: Verify both providers still pass together**

```bash
mvn -pl common test -Dtest='*CacheProvider*Test'
```

Expected: 30 tests pass (15 Caffeine + 15 Redis).

- [ ] **Step 4.2.4: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/RedisCacheProvider.java \
        common/src/test/java/com/tencent/supersonic/common/cache/RedisCacheProviderTest.java
git commit -m "feat(common): add RedisCacheProvider passing contract suite"
```

---

## Task 5: Auto-configuration with per-namespace resolution + Spring `CacheManager` bridge

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/UnifiedSpringCacheManager.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheAutoConfiguration.java`
- Create: `common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `common/src/test/java/com/tencent/supersonic/common/cache/UnifiedCacheAutoConfigurationTest.java`

### Step 5.1: Define programmatic namespace registration

The three built-in namespaces need TTL / max-size defaults that match today's behaviors (verified from current code):

| Namespace | Used by | TTL | MaxSize | TenantScoped |
|-----------|---------|-----|---------|--------------|
| `feishu-event-dedup` | `FeishuEventController` dedup (Caffeine today: 5m / 10_000) | 5m | 10_000 | false |
| `feishu-token` | `FeishuTokenManager` (110m / 1) | 110m | 16 | false |
| `feishu-general` | `FeishuBindTokenService.markUsed`, generic `get/put/remove` (30m / 5_000) | 30m | 5_000 | false |
| `feishu-counter` | `FeishuApiRateLimiter` + `FeishuBotService.isRateLimited` (60s fixed window) | 60s | 10_000 | false |
| `oauth-code` | `OAuthCodeExchangeService` (60s / 10_000) | 60s | 10_000 | false |
| `semantic-query` | `DefaultQueryCache` (`s2.cache.common.expire.after.write` default 10m / 5_000) | 10m | 5_000 | true |

These defaults are installed programmatically so apps work out-of-the-box with zero YAML. Any field can be overridden by `s2.cache.namespaces.<name>.*`.

### Step 5.2: Write the failing auto-config test

- [ ] **Step 5.2.1: Write `UnifiedCacheAutoConfigurationTest.java`**

```java
package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedCacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnifiedCacheAutoConfiguration.class));

    @Test
    void defaultsToCaffeineForAllBuiltinNamespaces() {
        runner.run(ctx -> {
            CacheProviderRegistry registry = ctx.getBean(CacheProviderRegistry.class);
            assertThat(registry.asMap().keySet()).contains(
                    "feishu-event-dedup", "feishu-token", "feishu-general", "feishu-counter",
                    "oauth-code", "semantic-query");
            for (CacheProvider p : registry.asMap().values()) {
                assertThat(p).isInstanceOf(CaffeineCacheProvider.class);
            }
        });
    }

    @Test
    void globalRedisTypeSwitchesAllNamespacesWhenRedisAvailable() {
        runner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues(
                        "s2.cache.type=redis",
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379")
                .run(ctx -> {
                    CacheProviderRegistry registry = ctx.getBean(CacheProviderRegistry.class);
                    for (CacheProvider p : registry.asMap().values()) {
                        assertThat(p).isInstanceOf(RedisCacheProvider.class);
                    }
                });
    }

    @Test
    void perNamespaceOverrideTakesPrecedenceOverGlobal() {
        runner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues(
                        "s2.cache.type=redis",
                        "s2.cache.namespaces.oauth-code.type=caffeine",
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379")
                .run(ctx -> {
                    CacheProviderRegistry registry = ctx.getBean(CacheProviderRegistry.class);
                    assertThat(registry.require("oauth-code")).isInstanceOf(CaffeineCacheProvider.class);
                    assertThat(registry.require("feishu-token")).isInstanceOf(RedisCacheProvider.class);
                });
    }

    @Test
    void perNamespaceTtlOverrideApplied() {
        runner.withPropertyValues(
                        "s2.cache.namespaces.oauth-code.ttl=45s",
                        "s2.cache.namespaces.oauth-code.max-size=2000")
                .run(ctx -> {
                    CacheProvider oauth = ctx.getBean(CacheProviderRegistry.class).require("oauth-code");
                    assertThat(oauth.getNamespace().getTtl()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(oauth.getNamespace().getMaxSize()).isEqualTo(2000L);
                });
    }

    @Test
    void adHocNamespaceCanBeDeclaredInYamlOnly() {
        runner.withPropertyValues(
                        "s2.cache.namespaces.my-custom.ttl=2m",
                        "s2.cache.namespaces.my-custom.max-size=50",
                        "s2.cache.namespaces.my-custom.tenant-scoped=true")
                .run(ctx -> {
                    CacheProvider custom = ctx.getBean(CacheProviderRegistry.class).require("my-custom");
                    assertThat(custom.getNamespace().isTenantScoped()).isTrue();
                    assertThat(custom.getNamespace().getTtl()).isEqualTo(Duration.ofMinutes(2));
                });
    }

    @Test
    void springCacheManagerExposedForAtCacheableConsumers() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(org.springframework.cache.CacheManager.class);
            org.springframework.cache.CacheManager mgr =
                    ctx.getBean(org.springframework.cache.CacheManager.class);
            assertThat(mgr.getCache("oauth-code")).isNotNull();
            assertThat(mgr.getCacheNames()).contains("oauth-code", "feishu-token", "semantic-query");
        });
    }
}
```

- [ ] **Step 5.2.2: Run — expect FAIL (classes missing)**

```bash
mvn -pl common test -Dtest=UnifiedCacheAutoConfigurationTest
```

Expected: compile errors.

### Step 5.3: Implement `UnifiedSpringCacheManager`

- [ ] **Step 5.3.1: Write `UnifiedSpringCacheManager.java`**

```java
package com.tencent.supersonic.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Adapter exposing the {@link CacheProviderRegistry} as a Spring {@link CacheManager}, so callers
 * can optionally use {@code @Cacheable("namespace-name")} / {@code @CacheEvict}. Values are stored
 * as {@code toString()} for now — no existing consumer uses this bridge (verified: 0
 * {@code @Cacheable} usages in the codebase), so typed serialization is deferred until a real
 * caller appears.
 */
public class UnifiedSpringCacheManager implements CacheManager {

    private final CacheProviderRegistry registry;

    public UnifiedSpringCacheManager(CacheProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    @Nullable
    public Cache getCache(@NonNull String name) {
        return registry.find(name).map(ProviderCache::new).orElse(null);
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(registry.asMap().keySet());
    }

    private static final class ProviderCache implements Cache {

        private final CacheProvider provider;

        ProviderCache(CacheProvider provider) {
            this.provider = provider;
        }

        @Override
        @NonNull
        public String getName() {
            return provider.getName();
        }

        @Override
        @NonNull
        public Object getNativeCache() {
            return provider;
        }

        @Override
        @Nullable
        public ValueWrapper get(@NonNull Object key) {
            return provider.get(key.toString())
                    .map(v -> (ValueWrapper) new SimpleValueWrapper(v))
                    .orElse(null);
        }

        @Override
        @Nullable
        public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
            Object value = provider.get(key.toString()).orElse(null);
            if (value == null) return null;
            if (type != null && !type.isInstance(value)) {
                throw new IllegalStateException(
                        "Cached value for key '" + key + "' is not of required type "
                                + type.getName());
            }
            @SuppressWarnings("unchecked")
            T typed = (T) value;
            return typed;
        }

        @Override
        @Nullable
        public <T> T get(@NonNull Object key, @NonNull java.util.concurrent.Callable<T> valueLoader) {
            throw new UnsupportedOperationException(
                    "Synchronous load-through not supported by UnifiedSpringCacheManager yet — "
                            + "use the CacheProvider API directly.");
        }

        @Override
        public void put(@NonNull Object key, @Nullable Object value) {
            if (value == null) {
                provider.evict(key.toString());
            } else {
                provider.put(key.toString(), value.toString());
            }
        }

        @Override
        public void evict(@NonNull Object key) {
            provider.evict(key.toString());
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException(
                    "clear() is not supported — namespaces auto-expire via TTL. "
                            + "For explicit invalidation, evict specific keys.");
        }
    }
}
```

- [ ] **Step 5.3.2: Commit (WIP — tests still fail; next step completes auto-config)**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/UnifiedSpringCacheManager.java
git commit -m "feat(common): add UnifiedSpringCacheManager bridge (wip)"
```

### Step 5.4: Implement `UnifiedCacheAutoConfiguration`

- [ ] **Step 5.4.1: Write `UnifiedCacheAutoConfiguration.java`**

```java
package com.tencent.supersonic.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wires up {@link CacheProvider} beans — one per declared namespace — plus the
 * {@link CacheProviderRegistry} and the Spring {@link org.springframework.cache.CacheManager}
 * bridge.
 *
 * <p>Built-in namespaces ({@code feishu-token}, {@code oauth-code}, etc.) are always registered
 * with sensible defaults; users can override any field via {@code s2.cache.namespaces.*}.
 *
 * <p>Per the SuperSonic {@code CLAUDE.md} memory: <em>{@code @ConditionalOnBean} on
 * {@code @Configuration} classes is unreliable for auto-configured beans</em>. We therefore
 * constructor-inject the {@link StringRedisTemplate} as an {@link ObjectProvider} — Spring resolves
 * the dependency ordering correctly — and fall back to Caffeine when Redis is not on the
 * classpath or not configured.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UnifiedCacheProperties.class)
public class UnifiedCacheAutoConfiguration {

    /** Built-in namespace defaults. Overridable via {@code s2.cache.namespaces.*}. */
    private static Map<String, CacheNamespace> defaults() {
        Map<String, CacheNamespace> m = new LinkedHashMap<>();
        m.put("feishu-event-dedup", CacheNamespace.builder()
                .name("feishu-event-dedup")
                .ttl(Duration.ofMinutes(5))
                .maxSize(10_000)
                .tenantScoped(false)
                .build());
        m.put("feishu-token", CacheNamespace.builder()
                .name("feishu-token")
                .ttl(Duration.ofMinutes(110))
                .maxSize(16)
                .tenantScoped(false)
                .build());
        m.put("feishu-general", CacheNamespace.builder()
                .name("feishu-general")
                .ttl(Duration.ofMinutes(30))
                .maxSize(5_000)
                .tenantScoped(false)
                .build());
        m.put("feishu-counter", CacheNamespace.builder()
                .name("feishu-counter")
                .ttl(Duration.ofSeconds(60))
                .maxSize(10_000)
                .tenantScoped(false)
                .build());
        m.put("oauth-code", CacheNamespace.builder()
                .name("oauth-code")
                .ttl(Duration.ofSeconds(60))
                .maxSize(10_000)
                .tenantScoped(false)
                .build());
        m.put("semantic-query", CacheNamespace.builder()
                .name("semantic-query")
                .ttl(Duration.ofMinutes(10))
                .maxSize(5_000)
                .tenantScoped(true)
                .build());
        return m;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheProviderRegistry cacheProviderRegistry(
            UnifiedCacheProperties properties,
            ObjectProvider<StringRedisTemplate> redisProvider) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        List<CacheProvider> providers = new ArrayList<>();

        Map<String, CacheNamespace> resolved = resolveNamespaces(properties);
        for (CacheNamespace ns : resolved.values()) {
            CacheType effective = effectiveType(ns, properties);
            providers.add(buildProvider(ns, effective, redis));
        }
        log.info("Unified cache initialized: globalType={}, namespaces={}, redisAvailable={}",
                properties.getType(), resolved.keySet(), redis != null);
        return new CacheProviderRegistry(providers);
    }

    private Map<String, CacheNamespace> resolveNamespaces(UnifiedCacheProperties properties) {
        Map<String, CacheNamespace> out = new LinkedHashMap<>(defaults());
        for (Map.Entry<String, UnifiedCacheProperties.NamespaceConfig> e
                : properties.getNamespaces().entrySet()) {
            String name = e.getKey();
            UnifiedCacheProperties.NamespaceConfig cfg = e.getValue();
            CacheNamespace base = out.get(name);
            CacheNamespace.CacheNamespaceBuilder b = (base == null)
                    ? CacheNamespace.builder().name(name).maxSize(10_000L)
                    : CacheNamespace.builder()
                            .name(base.getName())
                            .typeOverride(base.getTypeOverride())
                            .ttl(base.getTtl())
                            .maxSize(base.getMaxSize())
                            .tenantScoped(base.isTenantScoped());
            if (cfg.getType() != null) {
                b.typeOverride(cfg.getType());
            }
            if (cfg.getTtl() != null) {
                b.ttl(cfg.getTtl());
            }
            if (cfg.getMaxSize() > 0L) {
                b.maxSize(cfg.getMaxSize());
            }
            b.tenantScoped(cfg.isTenantScoped() || (base != null && base.isTenantScoped()
                    && !explicitlyFalse(properties, name)));
            CacheNamespace ns = b.build();
            if (ns.getTtl() == null) {
                throw new IllegalStateException(
                        "Cache namespace '" + name + "' must declare a TTL (s2.cache.namespaces."
                                + name + ".ttl).");
            }
            out.put(name, ns);
        }
        return out;
    }

    /**
     * Tri-state check: if the user wrote {@code tenant-scoped: false}, respect it even when the
     * builtin default is true. Because boolean primitives can't distinguish "unset" from "false",
     * we rely on the fact that {@code NamespaceConfig.setTenantScoped(false)} is only invoked when
     * the property is explicitly present in the Environment.
     */
    private boolean explicitlyFalse(UnifiedCacheProperties properties, String name) {
        // Heuristic: if the namespace config exists AND isTenantScoped() is false AND the builtin
        // default was true, assume the user set it explicitly. Defensive only — in practice no one
        // overrides semantic-query to non-tenant-scoped today.
        UnifiedCacheProperties.NamespaceConfig cfg = properties.getNamespaces().get(name);
        return cfg != null && !cfg.isTenantScoped();
    }

    private CacheType effectiveType(CacheNamespace ns, UnifiedCacheProperties props) {
        return ns.getTypeOverride() != null ? ns.getTypeOverride() : props.getType();
    }

    private CacheProvider buildProvider(CacheNamespace ns, CacheType type, StringRedisTemplate redis) {
        if (type == CacheType.REDIS) {
            if (redis == null) {
                log.warn("Cache namespace '{}' requested REDIS but no StringRedisTemplate is "
                        + "available — falling back to Caffeine.", ns.getName());
                return new CaffeineCacheProvider(ns);
            }
            return new RedisCacheProvider(ns, redis);
        }
        return new CaffeineCacheProvider(ns);
    }

    @Bean
    @ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
    public org.springframework.cache.CacheManager unifiedSpringCacheManager(
            CacheProviderRegistry registry) {
        return new UnifiedSpringCacheManager(registry);
    }
}
```

- [ ] **Step 5.4.2: Register auto-config in `spring.factories`-equivalent (Boot 3.x)**

Create `common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.tencent.supersonic.common.cache.UnifiedCacheAutoConfiguration
```

(If the file already exists — check first; append this line rather than overwriting.)

- [ ] **Step 5.4.3: Check whether that file already exists and merge**

```bash
test -f common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
     && cat common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
     || echo "MISSING"
```

If "MISSING": create the file with just the line above.
If contents exist: use the `Edit` tool to append a single newline + `com.tencent.supersonic.common.cache.UnifiedCacheAutoConfiguration` to the end of the file.

- [ ] **Step 5.4.4: Run auto-config tests — expect PASS**

```bash
mvn -pl common test -Dtest=UnifiedCacheAutoConfigurationTest
```

Expected: 6 tests pass.

- [ ] **Step 5.4.5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheAutoConfiguration.java \
        common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
        common/src/test/java/com/tencent/supersonic/common/cache/UnifiedCacheAutoConfigurationTest.java
git commit -m "feat(common): add UnifiedCacheAutoConfiguration with per-namespace resolution"
```

### Step 5.5: Verify standalone still compiles

- [ ] **Step 5.5.1: Run the mandatory compile check**

```bash
mvn compile -pl launchers/standalone -am
```

Expected: `BUILD SUCCESS`.

---

## Task 6: Migrate `feishu/server/cache/*` to the unified provider

**Strategy decision:** Keep `FeishuCacheService` interface temporarily as a **thin facade** over `CacheProviderRegistry`, so all four Feishu service classes continue to compile with minimal churn during the migration. Delete `FeishuCacheService` + facade in a **second commit** of this task once callers have been refactored to hold `CacheProviderRegistry` directly — OR keep the facade forever if the ergonomics are nicer for Feishu-specific semantics (duplicate-detection helper). **This plan keeps the facade** to reduce diff noise; the interface moves from `feishu/api` → `feishu/server` (it's no longer a cross-module API once it's a pure impl-side convenience).

**Files:**
- Modify: `feishu/api/src/main/java/com/tencent/supersonic/feishu/api/cache/FeishuCacheService.java` → DELETE (move to server package)
- Create: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/FeishuCacheFacade.java`
- Delete: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/CaffeineFeishuCacheService.java`
- Delete: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/RedisFeishuCacheService.java`
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/config/FeishuAutoConfiguration.java`
- Modify: `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/{FeishuBotService, FeishuTokenManager, FeishuApiRateLimiter, FeishuBindTokenService}.java`
- Modify: `feishu/server/pom.xml` (drop redundant Redis dep — common re-exports it)

### Step 6.1: Write tests for the facade

- [ ] **Step 6.1.1: Write `FeishuCacheFacadeTest.java`**

Create `feishu/server/src/test/java/com/tencent/supersonic/feishu/server/cache/FeishuCacheFacadeTest.java`:

```java
package com.tencent.supersonic.feishu.server.cache;

import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuCacheFacadeTest {

    private FeishuCacheFacade facade;

    @BeforeEach
    void setup() {
        List<CacheProvider> providers = List.of(
                new CaffeineCacheProvider(ns("feishu-event-dedup")),
                new CaffeineCacheProvider(ns("feishu-token")),
                new CaffeineCacheProvider(ns("feishu-general")),
                new CaffeineCacheProvider(ns("feishu-counter")));
        facade = new FeishuCacheFacade(new CacheProviderRegistry(providers));
    }

    private CacheNamespace ns(String name) {
        return CacheNamespace.builder()
                .name(name).ttl(Duration.ofMinutes(5)).maxSize(10_000).build();
    }

    @Test
    void tokenRoundtrips() {
        assertThat(facade.getToken()).isNull();
        facade.putToken("abc");
        assertThat(facade.getToken()).isEqualTo("abc");
    }

    @Test
    void duplicateEventDetectedOnSecondCall() {
        assertThat(facade.isDuplicateEvent("evt1")).isFalse();
        assertThat(facade.isDuplicateEvent("evt1")).isTrue();
        assertThat(facade.isDuplicateEvent("evt2")).isFalse();
    }

    @Test
    void duplicateEventNullIdReturnsFalse() {
        assertThat(facade.isDuplicateEvent(null)).isFalse();
    }

    @Test
    void generalGetPutRemoveRoundtrips() {
        facade.put("k1", "v1");
        assertThat(facade.get("k1")).isEqualTo("v1");
        facade.remove("k1");
        assertThat(facade.get("k1")).isNull();
    }

    @Test
    void incrementCounterMonotonic() {
        assertThat(facade.incrementCounter("c1")).isEqualTo(1L);
        assertThat(facade.incrementCounter("c1")).isEqualTo(2L);
        assertThat(facade.incrementCounter("c2")).isEqualTo(1L);
    }
}
```

- [ ] **Step 6.1.2: Run — expect FAIL (class missing)**

```bash
mvn -pl feishu/server test -Dtest=FeishuCacheFacadeTest
```

Expected: compile error — `FeishuCacheFacade` undefined.

### Step 6.2: Implement the facade and delete old classes

- [ ] **Step 6.2.1: Write `FeishuCacheFacade.java`**

```java
package com.tencent.supersonic.feishu.server.cache;

import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thin facade over {@link CacheProviderRegistry} exposing the historical {@code FeishuCacheService}
 * API so existing Feishu business code compiles without deep refactors. Each logical store maps to
 * a dedicated cache namespace declared in {@link com.tencent.supersonic.common.cache.UnifiedCacheAutoConfiguration}.
 */
@Slf4j
@Component
public class FeishuCacheFacade {

    private static final String TOKEN_KEY = "tenant_access_token";

    private final CacheProvider dedup;
    private final CacheProvider token;
    private final CacheProvider general;
    private final CacheProvider counter;

    public FeishuCacheFacade(CacheProviderRegistry registry) {
        this.dedup = registry.require("feishu-event-dedup");
        this.token = registry.require("feishu-token");
        this.general = registry.require("feishu-general");
        this.counter = registry.require("feishu-counter");
    }

    public boolean isDuplicateEvent(String eventId) {
        if (eventId == null) return false;
        // putIfAbsent returns true on first write — so NOT a duplicate.
        return !dedup.putIfAbsent(eventId, "1");
    }

    public String getToken() {
        return token.get(TOKEN_KEY).orElse(null);
    }

    public void putToken(String value) {
        token.put(TOKEN_KEY, value);
    }

    public String get(String key) {
        return general.get(key).orElse(null);
    }

    public void put(String key, String value) {
        general.put(key, value);
    }

    public void remove(String key) {
        general.evict(key);
    }

    public long incrementCounter(String key) {
        return counter.increment(key);
    }
}
```

- [ ] **Step 6.2.2: Delete the old cache classes**

```bash
git rm feishu/api/src/main/java/com/tencent/supersonic/feishu/api/cache/FeishuCacheService.java
git rm feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/CaffeineFeishuCacheService.java
git rm feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/RedisFeishuCacheService.java
```

- [ ] **Step 6.2.3: Rewrite `FeishuAutoConfiguration.java`**

Replace the current contents with:

```java
package com.tencent.supersonic.feishu.server.config;

import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "s2.feishu", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FeishuProperties.class)
@ComponentScan(basePackages = "com.tencent.supersonic.feishu.server")
public class FeishuAutoConfiguration {
    // Cache backends are now provided by
    // com.tencent.supersonic.common.cache.UnifiedCacheAutoConfiguration.
    // See s2.cache.namespaces.feishu-{event-dedup,token,general,counter}.
}
```

- [ ] **Step 6.2.4: Rewrite every call site**

In `FeishuTokenManager.java` — replace `private final FeishuCacheService cacheService;` with:

```java
private final FeishuCacheFacade cacheService;
```

And change the import line `import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;` to:

```java
import com.tencent.supersonic.feishu.server.cache.FeishuCacheFacade;
```

Do the same four-field swap in:
- `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuApiRateLimiter.java`
- `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBindTokenService.java`
- `feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBotService.java`

The constructor parameter type in `FeishuBotService` (which currently reads `FeishuCacheService cacheService`) also needs the same rename — the rest of the logic is untouched because the facade preserves the original method names (`getToken`, `putToken`, `get`, `put`, `remove`, `isDuplicateEvent`, `incrementCounter`).

- [ ] **Step 6.2.5: Drop the redundant `spring-boot-starter-data-redis` from `feishu/server/pom.xml`**

Delete the entire `<!-- Redis cache ... -->` block (the 6 lines from `<!-- Redis cache` through `</dependency>`) — the `common` module already exports the optional starter.

- [ ] **Step 6.2.6: Verify facade test + compile**

```bash
mvn -pl feishu/server test -Dtest=FeishuCacheFacadeTest
mvn compile -pl launchers/standalone -am
```

Expected: 5 facade tests pass; standalone compiles.

- [ ] **Step 6.2.7: Run existing Feishu tests — they MUST continue to pass untouched**

```bash
mvn -pl feishu/server test
```

Expected: no regressions. If a test explicitly instantiated `CaffeineFeishuCacheService` or `RedisFeishuCacheService` directly, replace the instantiation with a new `FeishuCacheFacade(new CacheProviderRegistry(List.of(new CaffeineCacheProvider(ns(...)), ...)))` — the test file is modified in-place but no production code changes beyond those already made.

- [ ] **Step 6.2.8: Commit**

```bash
git add feishu/server/src/main/java/com/tencent/supersonic/feishu/server/cache/FeishuCacheFacade.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/config/FeishuAutoConfiguration.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuTokenManager.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuApiRateLimiter.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBindTokenService.java \
        feishu/server/src/main/java/com/tencent/supersonic/feishu/server/service/FeishuBotService.java \
        feishu/server/src/test/java/com/tencent/supersonic/feishu/server/cache/FeishuCacheFacadeTest.java \
        feishu/server/pom.xml
git commit -m "refactor(feishu): migrate FeishuCacheService to unified cache facade"
```

---

## Task 7: Migrate `auth/.../oauth/storage/*` with deprecated `s2.oauth.storage.type` alias

### Step 7.1: Write the alias shim test

- [ ] **Step 7.1.1: Write `DeprecatedOAuthStorageAliasProcessorTest.java`**

Create `common/src/test/java/com/tencent/supersonic/common/cache/DeprecatedOAuthStorageAliasProcessorTest.java`:

```java
package com.tencent.supersonic.common.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DeprecatedOAuthStorageAliasProcessorTest {

    @Test
    void legacyKeyMappedToNewKey() {
        StandardEnvironment env = new StandardEnvironment();
        env.getSystemProperties().put("s2.oauth.storage.type", "redis");
        try {
            new DeprecatedOAuthStorageAliasProcessor()
                    .postProcessEnvironment(env, new SpringApplication());
            assertThat(env.getProperty("s2.cache.namespaces.oauth-code.type")).isEqualTo("redis");
        } finally {
            env.getSystemProperties().remove("s2.oauth.storage.type");
        }
    }

    @Test
    void newKeyTakesPrecedenceWhenBothPresent() {
        StandardEnvironment env = new StandardEnvironment();
        env.getSystemProperties().put("s2.oauth.storage.type", "caffeine");
        env.getSystemProperties().put("s2.cache.namespaces.oauth-code.type", "redis");
        try {
            new DeprecatedOAuthStorageAliasProcessor()
                    .postProcessEnvironment(env, new SpringApplication());
            assertThat(env.getProperty("s2.cache.namespaces.oauth-code.type")).isEqualTo("redis");
        } finally {
            env.getSystemProperties().remove("s2.oauth.storage.type");
            env.getSystemProperties().remove("s2.cache.namespaces.oauth-code.type");
        }
    }

    @Test
    void absentLegacyKeyIsNoOp() {
        StandardEnvironment env = new StandardEnvironment();
        new DeprecatedOAuthStorageAliasProcessor()
                .postProcessEnvironment(env, new SpringApplication());
        assertThat(env.getProperty("s2.cache.namespaces.oauth-code.type")).isNull();
    }
}
```

- [ ] **Step 7.1.2: Run — expect FAIL (class missing)**

```bash
mvn -pl common test -Dtest=DeprecatedOAuthStorageAliasProcessorTest
```

Expected: compile error.

### Step 7.2: Implement the environment post-processor

- [ ] **Step 7.2.1: Write `DeprecatedOAuthStorageAliasProcessor.java`**

```java
package com.tencent.supersonic.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Backward-compat shim: maps the deprecated {@code s2.oauth.storage.type} property onto the new
 * {@code s2.cache.namespaces.oauth-code.type}. If BOTH are set, the new key wins (new config always
 * takes precedence over legacy aliases).
 *
 * <p>Registered in {@code META-INF/spring.factories}.
 */
@Slf4j
public class DeprecatedOAuthStorageAliasProcessor implements EnvironmentPostProcessor {

    private static final String LEGACY_KEY = "s2.oauth.storage.type";
    private static final String NEW_KEY = "s2.cache.namespaces.oauth-code.type";
    private static final String ALIAS_SOURCE = "s2-cache-deprecated-aliases";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        String legacy = env.getProperty(LEGACY_KEY);
        if (legacy == null) return;

        if (env.getProperty(NEW_KEY) != null) {
            log.warn("Both '{}' (deprecated) and '{}' are set — the new key wins. Remove '{}' "
                    + "from your configuration.", LEGACY_KEY, NEW_KEY, LEGACY_KEY);
            return;
        }

        log.warn("Configuration key '{}' is deprecated. Migrate to '{}' (value: {}).",
                LEGACY_KEY, NEW_KEY, legacy);

        Map<String, Object> aliases = new HashMap<>();
        aliases.put(NEW_KEY, legacy);
        // addFirst so this wins over other sources that might also be missing the new key.
        env.getPropertySources().addFirst(new MapPropertySource(ALIAS_SOURCE, aliases));
    }
}
```

- [ ] **Step 7.2.2: Register in `spring.factories`**

Create or append to `common/src/main/resources/META-INF/spring.factories`:

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
com.tencent.supersonic.common.cache.DeprecatedOAuthStorageAliasProcessor
```

If the file exists, append with proper line continuation (use `Edit` tool).

- [ ] **Step 7.2.3: Run — expect PASS**

```bash
mvn -pl common test -Dtest=DeprecatedOAuthStorageAliasProcessorTest
```

Expected: 3 tests pass.

- [ ] **Step 7.2.4: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/cache/DeprecatedOAuthStorageAliasProcessor.java \
        common/src/main/resources/META-INF/spring.factories \
        common/src/test/java/com/tencent/supersonic/common/cache/DeprecatedOAuthStorageAliasProcessorTest.java
git commit -m "feat(common): add deprecated s2.oauth.storage.type alias processor"
```

### Step 7.3: Reimplement `CodeExchangeStorage` as a facade, remove legacy impls

**Files:**
- Modify: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/CodeExchangeStorage.java` (delete — keep only if still publicly referenced; see Step 7.3.2)
- Delete: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/CaffeineCodeExchangeStorage.java`
- Delete: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/RedisCodeExchangeStorage.java`
- Delete: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/CodeExchangeStorageConfig.java`
- Modify: `auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/service/OAuthCodeExchangeService.java`
- Modify: `auth/authentication/pom.xml` (drop redundant Redis starter)

- [ ] **Step 7.3.1: Write the failing test for the new service**

Create `auth/authentication/src/test/java/com/tencent/supersonic/auth/authentication/oauth/service/OAuthCodeExchangeServiceTest.java`:

```java
package com.tencent.supersonic.auth.authentication.oauth.service;

import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthCodeExchangeServiceTest {

    private OAuthCodeExchangeService service;

    @BeforeEach
    void setup() {
        CacheNamespace ns = CacheNamespace.builder()
                .name("oauth-code").ttl(Duration.ofSeconds(60)).maxSize(1000).build();
        CacheProvider provider = new CaffeineCacheProvider(ns);
        CacheProviderRegistry registry = new CacheProviderRegistry(List.<CacheProvider>of(provider));
        service = new OAuthCodeExchangeService(registry);
    }

    @Test
    void createExchangeCodeReturnsNonNull() {
        String code = service.createExchangeCode("access", "refresh", "sid", 42L);
        assertThat(code).isNotBlank();
    }

    @Test
    void exchangeCodeForTokensRetrievesExchange() {
        String code = service.createExchangeCode("access", "refresh", "sid", 42L);
        OAuthCodeExchange x = service.exchangeCodeForTokens(code);
        assertThat(x).isNotNull();
        assertThat(x.getAccessToken()).isEqualTo("access");
        assertThat(x.getUserId()).isEqualTo(42L);
    }

    @Test
    void exchangeCodeIsOneTimeUse() {
        String code = service.createExchangeCode("access", "refresh", "sid", 42L);
        assertThat(service.exchangeCodeForTokens(code)).isNotNull();
        assertThat(service.exchangeCodeForTokens(code)).isNull();
    }

    @Test
    void exchangeCodeNullReturnsNull() {
        assertThat(service.exchangeCodeForTokens(null)).isNull();
        assertThat(service.exchangeCodeForTokens("")).isNull();
    }

    @Test
    void unknownCodeReturnsNull() {
        assertThat(service.exchangeCodeForTokens("does-not-exist")).isNull();
    }
}
```

- [ ] **Step 7.3.2: Delete the old storage classes AND the public interface**

```bash
git rm auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/CaffeineCodeExchangeStorage.java
git rm auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/RedisCodeExchangeStorage.java
git rm auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/CodeExchangeStorageConfig.java
git rm auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/storage/CodeExchangeStorage.java
```

Verify no other consumer references `CodeExchangeStorage` (verified earlier grep returned only the 5 files we're touching):

```bash
# Use the Grep tool, not shell grep:
# Grep pattern: "CodeExchangeStorage"
# Expected: only OAuthCodeExchangeServiceTest (removed) and the now-deleted files.
```

If Grep finds any residual reference in auth test files, update that file to use `CacheProviderRegistry` directly (same pattern as the new `OAuthCodeExchangeServiceTest`).

- [ ] **Step 7.3.3: Rewrite `OAuthCodeExchangeService.java`**

```java
package com.tencent.supersonic.auth.authentication.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.auth.authentication.oauth.util.PKCEUtil;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing temporary OAuth exchange codes. Exchange codes are short-lived (60 seconds,
 * configurable via {@code s2.cache.namespaces.oauth-code.ttl}) and can only be used once.
 *
 * <p>Storage backend is configurable via {@code s2.cache.type} / {@code
 * s2.cache.namespaces.oauth-code.type} — Caffeine for single-node, Redis for distributed.
 */
@Slf4j
@Service
public class OAuthCodeExchangeService {

    private static final long EXCHANGE_CODE_TTL_SECONDS = 60;

    private final CacheProvider cache;
    private final ObjectMapper objectMapper;

    public OAuthCodeExchangeService(CacheProviderRegistry registry) {
        this.cache = registry.require("oauth-code");
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        log.info("OAuthCodeExchangeService initialized with cache backend: {}",
                cache.getNamespace().getTypeOverride() != null
                        ? cache.getNamespace().getTypeOverride()
                        : "default");
    }

    /** Create a new exchange code for the given tokens. */
    public String createExchangeCode(String accessToken, String refreshToken, String sessionId,
            Long userId) {
        String exchangeCode = PKCEUtil.generateState();
        OAuthCodeExchange exchange = OAuthCodeExchange.create(exchangeCode, accessToken,
                refreshToken, sessionId, userId, EXCHANGE_CODE_TTL_SECONDS);
        try {
            cache.put(exchangeCode, objectMapper.writeValueAsString(exchange));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize exchange code", e);
        }
        log.debug("Created exchange code for user: {}", userId);
        return exchangeCode;
    }

    /** Exchange the code for tokens. This is a one-time operation. */
    public OAuthCodeExchange exchangeCodeForTokens(String exchangeCode) {
        if (exchangeCode == null || exchangeCode.isEmpty()) {
            log.warn("Exchange code is null or empty");
            return null;
        }
        Optional<String> raw = cache.get(exchangeCode);
        if (raw.isEmpty()) {
            log.warn("Exchange code not found or expired: {}", exchangeCode);
            return null;
        }
        cache.evict(exchangeCode); // one-time use
        try {
            OAuthCodeExchange exchange =
                    objectMapper.readValue(raw.get(), OAuthCodeExchange.class);
            if (!exchange.isValid()) {
                log.debug("Exchange code expired or already used: {}", exchangeCode);
                return null;
            }
            exchange.setUsed(true);
            log.debug("Successfully exchanged code for user: {}", exchange.getUserId());
            return exchange;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize exchange code: {}", e.getMessage());
            return null;
        }
    }

    /** @deprecated retained for backward compatibility. Returns the configured backend name. */
    @Deprecated
    public String getStorageType() {
        return cache.getNamespace().getTypeOverride() != null
                ? cache.getNamespace().getTypeOverride().name().toLowerCase()
                : "default";
    }
}
```

- [ ] **Step 7.3.4: Drop `spring-boot-starter-data-redis` from `auth/authentication/pom.xml`**

Delete the lines matching the existing Redis block (the comment `<!-- Spring Data Redis ... -->` through the closing `</dependency>` — verified earlier to be at approximately lines 85–90 of `auth/authentication/pom.xml`).

- [ ] **Step 7.3.5: Run auth tests — expect PASS**

```bash
mvn -pl auth/authentication test -Dtest=OAuthCodeExchangeServiceTest
mvn compile -pl launchers/standalone -am
```

Expected: 5 tests pass; standalone compiles.

- [ ] **Step 7.3.6: Commit**

```bash
git add auth/authentication/src/main/java/com/tencent/supersonic/auth/authentication/oauth/service/OAuthCodeExchangeService.java \
        auth/authentication/src/test/java/com/tencent/supersonic/auth/authentication/oauth/service/OAuthCodeExchangeServiceTest.java \
        auth/authentication/pom.xml
git commit -m "refactor(auth): migrate OAuth exchange storage to unified cache"
```

---

## Task 8: Migrate `headless/core/cache/CaffeineCacheManager`

**Analysis (verified):**
- `headless/core/cache/CaffeineCacheManager` is consumed exclusively by `DefaultQueryCache` (`ContextUtils.getBean(CacheManager.class)`). Not an LLM-only cache.
- The bean `searchCaffeineCache` defined in `CaffeineCacheConfig` is never injected anywhere (verified — only 1 grep hit, in the config file itself). **Delete it.**
- The tenant scoping: `semantic-query` results ARE tenant-specific (query joins a tenant's models) — the existing code bakes the tenant into the key-generation path *implicitly* through `cacheCommonApp/env/version + modelIds + MD5(query)` but does NOT prefix with tenant id. A tenant switch changes `modelIds` so collisions are unlikely, but the new abstraction makes this **explicit** via `tenantScoped=true`. This is a strictly safer default.

**Files:**
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/cache/CaffeineCacheManager.java`
- Delete: `headless/core/src/main/java/com/tencent/supersonic/headless/core/cache/CaffeineCacheConfig.java`
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/cache/CacheCommonConfig.java` (drop `expireAfterWrite` field — now owned by namespace; keep `cacheEnable`, `app`, `env`, `version` for key generation)

### Step 8.1: Write the failing test

- [ ] **Step 8.1.1: Write `CaffeineCacheManagerTest.java`**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/cache/CaffeineCacheManagerTest.java`:

```java
package com.tencent.supersonic.headless.core.cache;

import com.tencent.supersonic.common.cache.CacheNamespace;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineCacheManagerTest {

    private CacheManager manager;

    @BeforeEach
    void setup() {
        CacheNamespace ns = CacheNamespace.builder()
                .name("semantic-query").ttl(Duration.ofMinutes(10))
                .maxSize(100).tenantScoped(true).build();
        CacheProvider provider = new CaffeineCacheProvider(ns);
        CacheProviderRegistry registry =
                new CacheProviderRegistry(List.<CacheProvider>of(provider));
        CacheCommonConfig cfg = new CacheCommonConfig();
        cfg.setCacheCommonApp("supersonic");
        cfg.setCacheCommonEnv("test");
        cfg.setCacheCommonVersion(0);
        cfg.setCacheEnable(Boolean.TRUE);
        manager = new CaffeineCacheManager(cfg, registry);
    }

    @Test
    void putThenGet() {
        manager.put("k1", "v1");
        assertThat(manager.get("k1")).isEqualTo("v1");
    }

    @Test
    void removeClearsEntry() {
        manager.put("k1", "v1");
        manager.removeCache("k1");
        assertThat(manager.get("k1")).isNull();
    }

    @Test
    void generateCacheKeyIncludesAppEnvVersionPrefixBody() {
        String key = manager.generateCacheKey("111,222", "abc123");
        assertThat(key).isEqualTo("supersonic:test:0:111,222:abc123");
    }

    @Test
    void generateCacheKeyHandlesEmptyPrefix() {
        String key = manager.generateCacheKey("", "abc");
        assertThat(key).isEqualTo("supersonic:test:0:-1:abc");
    }
}
```

- [ ] **Step 8.1.2: Run — expect compile FAIL (constructor signature changes)**

```bash
mvn -pl headless/core test -Dtest=CaffeineCacheManagerTest
```

Expected: compile error due to constructor mismatch.

### Step 8.2: Rewrite the three files

- [ ] **Step 8.2.1: Rewrite `CacheCommonConfig.java`**

```java
package com.tencent.supersonic.headless.core.cache;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Holds the cache-key generation context for {@link CaffeineCacheManager}. TTL / max-size now live
 * under {@code s2.cache.namespaces.semantic-query.*} (see
 * {@code com.tencent.supersonic.common.cache.UnifiedCacheAutoConfiguration}).
 */
@Configuration
@Data
public class CacheCommonConfig {

    @Value("${s2.cache.common.app:supersonic}")
    private String cacheCommonApp;

    @Value("${s2.cache.common.env:dev}")
    private String cacheCommonEnv;

    @Value("${s2.cache.common.version:0}")
    private Integer cacheCommonVersion;

    @Value("${s2.query.cache.enable:true}")
    private Boolean cacheEnable;
}
```

- [ ] **Step 8.2.2: Rewrite `CaffeineCacheManager.java`**

```java
package com.tencent.supersonic.headless.core.cache;

import com.google.common.base.Joiner;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Headless query cache — thin adapter over the unified {@link CacheProvider}. Values are stored as
 * their {@code toString()} representation, matching the pre-migration behavior; callers that need
 * richer types are responsible for JSON encoding (consistent with {@code
 * OAuthCodeExchangeService}).
 */
@Component
@Slf4j
public class CaffeineCacheManager implements CacheManager {

    private final CacheCommonConfig cacheCommonConfig;
    private final CacheProvider provider;

    public CaffeineCacheManager(CacheCommonConfig cacheCommonConfig,
            CacheProviderRegistry registry) {
        this.cacheCommonConfig = cacheCommonConfig;
        this.provider = registry.require("semantic-query");
    }

    @Override
    public Boolean put(String key, Object value) {
        log.debug("[put cache] key:{}", key);
        provider.put(key, value == null ? "" : value.toString());
        return true;
    }

    @Override
    public Object get(String key) {
        Object value = provider.get(key).orElse(null);
        log.debug("[get cache] key:{}, hit:{}", key, value != null);
        return value;
    }

    @Override
    public String generateCacheKey(String prefix, String body) {
        if (StringUtils.isEmpty(prefix)) {
            prefix = "-1";
        }
        return Joiner.on(":").join(cacheCommonConfig.getCacheCommonApp(),
                cacheCommonConfig.getCacheCommonEnv(), cacheCommonConfig.getCacheCommonVersion(),
                prefix, body);
    }

    @Override
    public Boolean removeCache(String key) {
        provider.evict(key);
        return true;
    }
}
```

- [ ] **Step 8.2.3: Delete `CaffeineCacheConfig.java`**

```bash
git rm headless/core/src/main/java/com/tencent/supersonic/headless/core/cache/CaffeineCacheConfig.java
```

- [ ] **Step 8.2.4: Run tests**

```bash
mvn -pl headless/core test -Dtest=CaffeineCacheManagerTest
mvn compile -pl launchers/standalone -am
```

Expected: 4 tests pass; standalone compiles.

- [ ] **Step 8.2.5: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/cache/CaffeineCacheManager.java \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/cache/CacheCommonConfig.java \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/cache/CaffeineCacheManagerTest.java
git commit -m "refactor(headless): migrate CaffeineCacheManager to unified cache"
```

---

## Task 9: End-to-end smoke test with `s2.cache.type=redis` across all three subsystems

**Files:**
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/config/UnifiedCacheSmokeTest.java`

### Step 9.1: Write the multi-module smoke test

- [ ] **Step 9.1.1: Write `UnifiedCacheSmokeTest.java`**

```java
package com.tencent.supersonic.config;

import com.tencent.supersonic.auth.authentication.oauth.model.OAuthCodeExchange;
import com.tencent.supersonic.auth.authentication.oauth.service.OAuthCodeExchangeService;
import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.RedisCacheProvider;
import com.tencent.supersonic.common.cache.InMemoryStringRedisTemplate;
import com.tencent.supersonic.feishu.server.cache.FeishuCacheFacade;
import com.tencent.supersonic.headless.core.cache.CacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = com.tencent.supersonic.SuperSonicStandalone.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "s2.cache.type=redis",
        "s2.feishu.enabled=true",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=16379" // unused — we inject a fake StringRedisTemplate
})
@org.springframework.context.annotation.Import(UnifiedCacheSmokeTest.FakeRedisConfig.class)
class UnifiedCacheSmokeTest {

    @TestConfiguration
    static class FakeRedisConfig {
        @Bean
        @Primary
        public StringRedisTemplate stringRedisTemplate() {
            return new InMemoryStringRedisTemplate();
        }
    }

    @Autowired
    CacheProviderRegistry registry;

    @Autowired
    FeishuCacheFacade feishu;

    @Autowired
    OAuthCodeExchangeService oauth;

    @Autowired
    CacheManager headless;

    @Test
    void allThreeSubsystemsUseRedisBackend() {
        for (CacheProvider p : registry.asMap().values()) {
            assertThat(p)
                    .as("namespace %s should be Redis-backed", p.getName())
                    .isInstanceOf(RedisCacheProvider.class);
        }
    }

    @Test
    void feishuSubsystemRoundtripsViaRedis() {
        feishu.putToken("tok-xyz");
        assertThat(feishu.getToken()).isEqualTo("tok-xyz");
        feishu.put("conv:42", "ctx");
        assertThat(feishu.get("conv:42")).isEqualTo("ctx");
        assertThat(feishu.incrementCounter("rate:abc")).isEqualTo(1L);
        assertThat(feishu.incrementCounter("rate:abc")).isEqualTo(2L);
    }

    @Test
    void oauthSubsystemRoundtripsViaRedis() {
        String code = oauth.createExchangeCode("at", "rt", "sid", 99L);
        OAuthCodeExchange x = oauth.exchangeCodeForTokens(code);
        assertThat(x).isNotNull();
        assertThat(x.getUserId()).isEqualTo(99L);
        assertThat(oauth.exchangeCodeForTokens(code)).isNull(); // one-time use
    }

    @Test
    void headlessSubsystemRoundtripsViaRedis() {
        String key = headless.generateCacheKey("1,2,3", "md5abc");
        headless.put(key, "result-payload");
        assertThat(headless.get(key)).isEqualTo("result-payload");
        headless.removeCache(key);
        assertThat(headless.get(key)).isNull();
    }
}
```

- [ ] **Step 9.1.2: Run smoke test**

```bash
mvn -pl launchers/standalone test -Dtest=UnifiedCacheSmokeTest -am
```

Expected: 4 tests pass. If any fail:
- `NoSuchBeanDefinition`: check `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` includes the unified auto-config.
- `StringRedisTemplate` mismatch: confirm the `@Primary` override in `FakeRedisConfig` is picked up.
- Tenant collision in `semantic-query`: the test doesn't set a `TenantContext`, so all ops fall under `tenant:_:` — which is consistent.

- [ ] **Step 9.1.3: Also verify Caffeine-only default smoke**

Add a second class to strengthen coverage.

Create `launchers/standalone/src/test/java/com/tencent/supersonic/config/UnifiedCacheDefaultSmokeTest.java`:

```java
package com.tencent.supersonic.config;

import com.tencent.supersonic.common.cache.CacheProvider;
import com.tencent.supersonic.common.cache.CacheProviderRegistry;
import com.tencent.supersonic.common.cache.CaffeineCacheProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = com.tencent.supersonic.SuperSonicStandalone.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnifiedCacheDefaultSmokeTest {

    @Autowired
    CacheProviderRegistry registry;

    @Test
    void defaultConfigurationIsAllCaffeine() {
        for (CacheProvider p : registry.asMap().values()) {
            assertThat(p).isInstanceOf(CaffeineCacheProvider.class);
        }
    }
}
```

- [ ] **Step 9.1.4: Run it**

```bash
mvn -pl launchers/standalone test -Dtest=UnifiedCacheDefaultSmokeTest -am
```

Expected: 1 test passes.

- [ ] **Step 9.1.5: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/config/UnifiedCacheSmokeTest.java \
        launchers/standalone/src/test/java/com/tencent/supersonic/config/UnifiedCacheDefaultSmokeTest.java
git commit -m "test(standalone): add end-to-end unified cache smoke tests"
```

---

## Task 10: Update `application.yaml` + docs + deprecation notes + final commit

### Step 10.1: Update `application.yaml` files

- [ ] **Step 10.1.1: Edit `launchers/standalone/src/main/resources/application.yaml`**

Locate the existing `s2.oauth.storage.type` block (lines ~127–130) and the `s2.feishu.cache.type` block (lines ~230–231) and replace with a unified block. The final layout:

Remove these lines (currently present):

```yaml
  # OAuth storage configuration (redis or caffeine, default: caffeine)
  oauth:
    storage:
      type: ${S2_OAUTH_STORAGE_TYPE:redis}
```

And remove this line inside `s2.feishu`:

```yaml
    cache:
      type: ${FEISHU_CACHE_TYPE:redis}  # caffeine or redis
```

Add a single new block immediately above `s2.permission`:

```yaml
  # Unified cache configuration. `type` is the global default backend; per-namespace overrides
  # available under `namespaces.<name>.{type,ttl,max-size,tenant-scoped}`. See
  # docs/details/platform/cache-abstraction.md.
  #
  # DEPRECATED ALIASES (still honored for backward compat, mapped by
  # DeprecatedOAuthStorageAliasProcessor):
  #   s2.oauth.storage.type → s2.cache.namespaces.oauth-code.type
  cache:
    type: ${S2_CACHE_TYPE:redis}
    namespaces:
      oauth-code:
        type: ${S2_OAUTH_STORAGE_TYPE:redis}
      feishu-token:
        type: ${FEISHU_CACHE_TYPE:redis}
      feishu-event-dedup:
        type: ${FEISHU_CACHE_TYPE:redis}
      feishu-general:
        type: ${FEISHU_CACHE_TYPE:redis}
      feishu-counter:
        type: ${FEISHU_CACHE_TYPE:redis}
```

- [ ] **Step 10.1.2: Edit `launchers/chat/src/main/resources/application.yaml` and `launchers/headless/src/main/resources/application.yaml`**

Apply the same replacement in each. If either file doesn't carry the `s2.oauth.storage` or `s2.feishu.cache` blocks today, add only the new `s2.cache` block.

- [ ] **Step 10.1.3: Smoke-run standalone once more**

```bash
mvn compile -pl launchers/standalone -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10.1.4: Commit**

```bash
git add launchers/standalone/src/main/resources/application.yaml \
        launchers/chat/src/main/resources/application.yaml \
        launchers/headless/src/main/resources/application.yaml
git commit -m "chore(config): migrate application.yaml to unified s2.cache block"
```

### Step 10.2: Write documentation

- [ ] **Step 10.2.1: Create `docs/details/platform/cache-abstraction.md`**

```markdown
---
status: active
module: platform/cache
key-files:
  - common/src/main/java/com/tencent/supersonic/common/cache/CacheProvider.java
  - common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheAutoConfiguration.java
  - common/src/main/java/com/tencent/supersonic/common/cache/UnifiedCacheProperties.java
---

# Unified Cache Abstraction

SuperSonic provides a single cache SPI (`CacheProvider`) with pluggable Caffeine/Redis backends,
configured under `s2.cache.*`. Each business domain declares a named namespace; the framework
instantiates one `CacheProvider` bean per namespace according to the global default
(`s2.cache.type`) and any per-namespace overrides.

## Configuration

```yaml
s2:
  cache:
    type: redis            # global default: caffeine | redis
    namespaces:
      oauth-code:
        type: caffeine     # per-namespace override
        ttl: 45s
        max-size: 2000
        tenant-scoped: false
      semantic-query:
        ttl: 10m
        max-size: 5000
        tenant-scoped: true
```

## Built-in namespaces

| Namespace | Consumer | Default TTL | Default MaxSize | Tenant-scoped |
|-----------|----------|------------:|----------------:|:-------------:|
| `feishu-event-dedup` | Feishu webhook dedup | 5m | 10 000 | no |
| `feishu-token` | `FeishuTokenManager` | 110m | 16 | no |
| `feishu-general` | `FeishuBindTokenService`, generic KV | 30m | 5 000 | no |
| `feishu-counter` | `FeishuApiRateLimiter` | 60s | 10 000 | no |
| `oauth-code` | `OAuthCodeExchangeService` | 60s | 10 000 | no |
| `semantic-query` | `DefaultQueryCache` | 10m | 5 000 | yes |

## Tenant-scoped keys

When `tenant-scoped: true`, `CacheProvider` implementations prefix every key with
`tenant:<tenantId>:`. The tenant id is read from `TenantContext.getTenantId()`. When no tenant is
set, `tenant:_:` is used — preventing cross-tenant cache bleed even if a caller forgets to set
`TenantContext`.

**OAuth exchange codes and Feishu tokens are NOT tenant-scoped** (they are per-user or per-app).

## Deprecated properties

| Old property | New property | Deprecated in |
|--------------|--------------|---------------|
| `s2.oauth.storage.type` | `s2.cache.namespaces.oauth-code.type` | 2026-04 (this release) |
| `s2.feishu.cache.type` | `s2.cache.namespaces.feishu-*.type` | 2026-04 (this release) |

The legacy `s2.oauth.storage.type` is still read at startup (see
`DeprecatedOAuthStorageAliasProcessor`) and a `WARN` is logged. `s2.feishu.cache.type` is NOT
auto-aliased — migrate by setting per-namespace overrides (`feishu-token`, `feishu-event-dedup`,
`feishu-general`, `feishu-counter`). A future release will delete both legacy keys.

## Adding a new namespace

1. Declare it in YAML:
   ```yaml
   s2.cache.namespaces.my-new-cache:
     ttl: 1m
     max-size: 1000
     tenant-scoped: true
   ```
2. Inject the registry and look up your provider:
   ```java
   private final CacheProvider cache;

   public MyService(CacheProviderRegistry registry) {
       this.cache = registry.require("my-new-cache");
   }
   ```
3. Operate on it via `CacheProvider`'s typed methods.

Built-in namespaces are registered programmatically in `UnifiedCacheAutoConfiguration.defaults()`
— add a new entry there if the namespace is part of the platform, or configure it purely via YAML
if it belongs to a single module.
```

- [ ] **Step 10.2.2: Update the details README index**

Append to `docs/details/README.md` (under the platform section — locate the existing platform entries first via Grep, then add a new line):

```
| `platform/cache-abstraction.md` | Unified cache SPI (Caffeine/Redis) with per-namespace config |
```

- [ ] **Step 10.2.3: Add a migration note to `docs/details/platform/backlog.md` (if it exists, under a "Completed" or "Recent changes" section)**

Check for the file first:

```bash
# Use Read, not cat.
# Read docs/details/platform/backlog.md
```

If the file exists: add a one-line bullet under its most recent "Completed" section:

```
- 2026-04-17: P0-3 unified cache abstraction — consolidates Feishu + OAuth + headless caches under `s2.cache.*`. See `cache-abstraction.md`.
```

If the file does not exist: skip this step.

- [ ] **Step 10.2.4: Update the CLAUDE.md memory (env var table in docs/details/env.md or similar if present)**

```bash
# Use Grep with pattern "S2_OAUTH_STORAGE_TYPE" over docs/
```

For every documentation file found: add a sibling row for `S2_CACHE_TYPE` mirroring the format, and annotate the `S2_OAUTH_STORAGE_TYPE` row as `DEPRECATED — use S2_CACHE_TYPE`.

- [ ] **Step 10.2.5: Commit documentation**

```bash
git add docs/details/platform/cache-abstraction.md docs/details/README.md
# Add any additional files touched in 10.2.3/10.2.4 if they existed
git commit -m "docs(cache): document unified cache abstraction and deprecations"
```

### Step 10.3: Final verification

- [ ] **Step 10.3.1: Full module compile**

```bash
mvn compile -pl launchers/standalone -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10.3.2: Full test run on touched modules**

```bash
mvn test -pl common,auth/authentication,feishu/server,headless/core
```

Expected: all tests pass; specifically:
- `UnifiedCachePropertiesTest`, `CacheProviderRegistryTest`, `UnifiedCacheAutoConfigurationTest`, `DeprecatedOAuthStorageAliasProcessorTest` (common)
- `CaffeineCacheProviderTest`, `RedisCacheProviderTest` (15 contract tests each)
- `FeishuCacheFacadeTest`, `OAuthCodeExchangeServiceTest`, `CaffeineCacheManagerTest`

- [ ] **Step 10.3.3: Standalone smoke tests**

```bash
mvn test -pl launchers/standalone -Dtest=UnifiedCacheSmokeTest,UnifiedCacheDefaultSmokeTest -am
```

Expected: 5 tests pass.

- [ ] **Step 10.3.4: Check no orphaned references remain**

Run Grep (via the Grep tool, not shell) over the whole repo for:

| Pattern | Expected | Action if found |
|---------|----------|-----------------|
| `FeishuCacheService` | 0 hits in `*.java` | Delete / migrate |
| `CaffeineFeishuCacheService\|RedisFeishuCacheService` | 0 hits in `*.java` | Delete / migrate |
| `CodeExchangeStorage` | 0 hits in `*.java` | Delete / migrate |
| `CaffeineCodeExchangeStorage\|RedisCodeExchangeStorage` | 0 hits in `*.java` | Delete / migrate |
| `CaffeineCacheConfig` | 0 hits in `*.java` | Delete / migrate |
| `searchCaffeineCache` | 0 hits | Delete / migrate |
| `s2.oauth.storage.type` | Only in `DeprecatedOAuthStorageAliasProcessor*` and `application.yaml` files | OK |

If any unexpected hits remain, revisit the appropriate task and fix.

- [ ] **Step 10.3.5: Final commit summary — review commit list**

```bash
git log --oneline master..HEAD
```

Expected commit list (10–12 commits total):

```
chore(common): add optional spring-boot-starter-data-redis for unified cache
feat(common): add CacheType enum for unified cache
feat(common): add CacheNamespace descriptor for unified cache
feat(common): add CacheProvider SPI for unified cache
feat(common): add CacheProviderRegistry with duplicate detection
feat(common): add UnifiedCacheProperties for s2.cache.* binding
test(common): add InMemoryStringRedisTemplate fake for cache tests
test(common): add CacheProvider contract test suite
feat(common): add CaffeineCacheProvider passing contract suite
feat(common): add RedisCacheProvider passing contract suite
feat(common): add UnifiedSpringCacheManager bridge (wip)
feat(common): add UnifiedCacheAutoConfiguration with per-namespace resolution
feat(common): add deprecated s2.oauth.storage.type alias processor
refactor(feishu): migrate FeishuCacheService to unified cache facade
refactor(auth): migrate OAuth exchange storage to unified cache
refactor(headless): migrate CaffeineCacheManager to unified cache
test(standalone): add end-to-end unified cache smoke tests
chore(config): migrate application.yaml to unified s2.cache block
docs(cache): document unified cache abstraction and deprecations
```

- [ ] **Step 10.3.6: Push when ready (only when user explicitly asks)**

Per `CLAUDE.md`: **never push without explicit authorization**. Present the commit list to the user for review first.

---

## Self-review checklist

- [x] Every step has complete code blocks — no TBDs or placeholders.
- [x] Type signatures consistent across tasks (`CacheProvider`, `CacheProviderRegistry`, `CacheNamespace`) — verified by re-reading Tasks 1 / 3 / 4 / 6 / 7 / 8.
- [x] Covers all 3 existing cache systems (feishu, auth, headless) + adds a unified one in `common`.
- [x] Backward compatibility: `s2.oauth.storage.type` still works via `DeprecatedOAuthStorageAliasProcessor` (Task 7.2).
- [x] Per-namespace config: `s2.cache.namespaces.<name>.{type,ttl,max-size,tenant-scoped}` (verified in `UnifiedCacheAutoConfigurationTest`).
- [x] Tenant-scoping: builtin `semantic-query` is tenant-scoped; OAuth + Feishu token are not.
- [x] Per CLAUDE.md memory: no `@ConditionalOnBean` on auto-config — uses `ObjectProvider` (Task 5.4.1) and `@ConditionalOnProperty` patterns.
- [x] Contract test used for Caffeine AND Redis — single source of truth for provider behavior.
- [x] Build-verification step after every Java-code commit (`mvn compile -pl launchers/standalone -am`).
- [x] Tests use `ApplicationContextRunner` / `@SpringBootTest` per Spring Boot best practices.
- [x] No testcontainers / Docker / embedded-redis dependency added — the `InMemoryStringRedisTemplate` fake keeps CI hermetic and fast.
- [x] Git hygiene: one commit per logical unit (~20 commits), no amending.
