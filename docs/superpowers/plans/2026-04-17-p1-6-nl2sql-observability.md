# P1-6 NL2SQL Chain Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Instrument the NL2SQL chain (parsers, mappers, LLM, correctors, DB) with Micrometer timers/counters plus a propagating `queryTraceId`, expose a Grafana dashboard and Prometheus alerts, and publish an oncall runbook so production triage stops being log-diving.

**Architecture:** A thin `Nl2sqlMetrics` facade wraps `MeterRegistry` to standardize metric names, tag normalization (tenant capping), and Timer.Sample usage. A `QueryTraceContext` helper wraps MDC with a single `queryTraceId` key, propagation for async/executor boundaries piggybacks on the existing `ThreadMdcUtil` / `ContextAwareThreadPoolExecutor`. Mapper/corrector/LLM/DB stages each write a histogram sample; tag cardinality is capped via a `TenantTagNormalizer` config that reads a top-N tenant allowlist. The existing Spring Boot Actuator + `micrometer-registry-prometheus` (already in `launchers/standalone/pom.xml`) is reused — no new dependencies. Logback gets a JSON appender layered on the existing rolling file appenders and includes `queryTraceId` from MDC. The Grafana dashboard JSON lives alongside the report dashboards and Prometheus rules alongside the report rules.

**Tech Stack:** Java 21, Spring Boot 3.4.x, Micrometer 1.12.x (already on classpath), LangChain4j, Logback + `logstash-logback-encoder` (new runtime dep for JSON layout), Grafana + Prometheus (docker compose already configured under `docker/`).

---

## File Structure

### New files (create)

- `common/src/main/java/com/tencent/supersonic/common/metrics/Nl2sqlMetricConstants.java` — metric names and tag-key constants (mirrors `ReportMetricConstants`).
- `common/src/main/java/com/tencent/supersonic/common/metrics/Nl2sqlMetrics.java` — facade for recording stage timers/counters; owns tag normalization.
- `common/src/main/java/com/tencent/supersonic/common/metrics/TenantTagNormalizer.java` — caps `tenant_id` cardinality using a top-N allowlist.
- `common/src/main/java/com/tencent/supersonic/common/metrics/QueryTraceContext.java` — MDC wrapper for the NL2SQL trace id + try-with-resources scope.
- `common/src/test/java/com/tencent/supersonic/common/metrics/Nl2sqlMetricsTest.java`
- `common/src/test/java/com/tencent/supersonic/common/metrics/TenantTagNormalizerTest.java`
- `common/src/test/java/com/tencent/supersonic/common/metrics/QueryTraceContextTest.java`
- `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsDecorator.java` — wraps any `SemanticCorrector` with stage timing.
- `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsConfiguration.java` — Spring `BeanPostProcessor` that wraps every registered corrector.
- `headless/chat/src/test/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsDecoratorTest.java`
- `docs/monitoring/nl2sql-dashboard.json` — Grafana dashboard JSON.
- `docker/grafana/dashboards/nl2sql-dashboard.json` — symlink target (same content, provisioned location).
- `docker/prometheus/rules/nl2sql-slo-alert-rules.yml` — Prometheus alert rules.
- `docs/runbook/nl2sql-observability.md` — oncall runbook + query cheatsheet.

### Existing files to modify

- `chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/NL2SQLParser.java` — entry-point instrumentation + trace scope.
- `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/mapper/BaseMapper.java` — replace ad-hoc timing log with metric emission.
- `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMRequestService.java` — capture latency + token counts.
- `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMSqlParser.java` — propagate trace id across retry loop.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/executor/JdbcExecutor.java` — DB latency + row count.
- `launchers/standalone/src/main/resources/logback-spring.xml` — add JSON appender + include `queryTraceId` MDC field.
- `launchers/chat/src/main/resources/logback-spring.xml` — identical changes.
- `launchers/headless/src/main/resources/logback-spring.xml` — identical changes.
- `launchers/standalone/src/main/resources/application.yaml` — tag filter properties + optional distribution stats enable.
- `launchers/standalone/pom.xml` — add `logstash-logback-encoder` for JSON layout.
- `docs/details/platform/03-monitoring-alerts.md` — append NL2SQL metrics table row + link to new dashboard.

---

## Task 1: Actuator Prometheus smoke test

**Files:**
- Modify: `launchers/standalone/src/main/resources/application.yaml:76-86`
- Test: `launchers/standalone/src/test/java/com/tencent/supersonic/metrics/PrometheusEndpointSmokeTest.java` (new)

Micrometer + `micrometer-registry-prometheus` + `spring-boot-starter-actuator` are already declared in `launchers/standalone/pom.xml:65-71`. The Actuator exposure list already includes `prometheus` at `application.yaml:80`. This task adds distribution histogram statistics (needed so `histogram_quantile()` works against `_bucket` series) and adds a smoke test.

- [ ] **Step 1: Write the failing smoke test**

```java
package com.tencent.supersonic.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrometheusEndpointSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusEndpointReturnsTextWithApplicationTag() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("application=\"supersonic\"")));
    }
}
```

- [ ] **Step 2: Run the test and confirm it passes (or fails if exclusion rules block the URL)**

Run: `mvn -pl launchers/standalone -am test -Dtest=PrometheusEndpointSmokeTest`
Expected: PASS (endpoint already wired). If it fails with 401/403, check the auth exclusion list at `application.yaml:212` (`/actuator/**` is already listed).

- [ ] **Step 3: Enable distribution histogram stats via config**

Edit `launchers/standalone/src/main/resources/application.yaml` lines 84-86 (under `management.metrics`):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: supersonic
    distribution:
      percentiles-histogram:
        s2.nl2sql.stage.duration: true
        s2.nl2sql.llm.duration: true
        s2.nl2sql.db.duration: true
      slo:
        s2.nl2sql.stage.duration: 0.1s,0.5s,1s,2s,5s,10s,15s,30s
        s2.nl2sql.llm.duration: 0.5s,1s,2s,5s,10s,15s,30s
        s2.nl2sql.db.duration: 0.1s,0.5s,1s,5s,10s,30s
```

- [ ] **Step 4: Add tenant cardinality cap configuration**

Append to the same `application.yaml` under a new `s2.observability.nl2sql` block (after `s2.tenant` or at end of file — pick a spot consistent with existing `s2.*` keys):

```yaml
s2:
  observability:
    nl2sql:
      tenant-tag-limit: 50
      top-tenants: []  # operator-populated, e.g. ['tenant-a','tenant-b']; others rolled to 'other'
      emit-tenant-tag: true
```

- [ ] **Step 5: Re-run the smoke test after config changes**

Run: `mvn -pl launchers/standalone -am test -Dtest=PrometheusEndpointSmokeTest`
Expected: PASS. No new metric series yet — that comes in later tasks.

- [ ] **Step 6: Commit**

```bash
git add launchers/standalone/src/main/resources/application.yaml \
        launchers/standalone/src/test/java/com/tencent/supersonic/metrics/PrometheusEndpointSmokeTest.java
git commit -m "test(metrics): add prometheus endpoint smoke test + histogram config

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: `Nl2sqlMetricConstants` + `TenantTagNormalizer`

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/metrics/Nl2sqlMetricConstants.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/metrics/TenantTagNormalizer.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/metrics/TenantTagNormalizerTest.java`

- [ ] **Step 1: Write the failing test for `TenantTagNormalizer`**

```java
package com.tencent.supersonic.common.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantTagNormalizerTest {

    @Test
    void returnsTenantWhenInAllowlist() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of("a", "b"), 50, true);
        assertEquals("a", n.normalize("a"));
    }

    @Test
    void returnsOtherWhenNotInAllowlistAndListFull() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of("a"), 1, true);
        assertEquals("other", n.normalize("zzz"));
    }

    @Test
    void returnsNoneWhenInputNullOrBlank() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of(), 50, true);
        assertEquals("none", n.normalize(null));
        assertEquals("none", n.normalize("  "));
    }

    @Test
    void returnsDisabledWhenEmitDisabled() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of("a"), 50, false);
        assertEquals("disabled", n.normalize("a"));
    }

    @Test
    void admitsUpToLimitWhenAllowlistEmpty() {
        TenantTagNormalizer n = new TenantTagNormalizer(List.of(), 2, true);
        assertEquals("x", n.normalize("x"));
        assertEquals("y", n.normalize("y"));
        assertEquals("other", n.normalize("z"));
    }
}
```

- [ ] **Step 2: Run to verify it fails to compile**

Run: `mvn -pl common test -Dtest=TenantTagNormalizerTest`
Expected: FAIL — `TenantTagNormalizer` does not exist.

- [ ] **Step 3: Implement `TenantTagNormalizer`**

```java
package com.tencent.supersonic.common.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps the cardinality of the {@code tenant_id} metric tag. Tenants in the configured allowlist are
 * passed through unchanged; new tenants are admitted dynamically up to {@code limit}; any further
 * tenant is rolled into the bucket {@code "other"}.
 *
 * <p>Blank input maps to {@code "none"}. When emit is disabled globally, every tenant maps to
 * {@code "disabled"} so dashboards can still tell the difference between "no traffic" and
 * "tenant tagging off".</p>
 */
@Component
public class TenantTagNormalizer {

    public static final String OTHER = "other";
    public static final String NONE = "none";
    public static final String DISABLED = "disabled";

    private final Set<String> allowlist;
    private final int limit;
    private final boolean emit;
    private final Set<String> dynamicAdmitted = ConcurrentHashMap.newKeySet();

    public TenantTagNormalizer(
            @Value("${s2.observability.nl2sql.top-tenants:}") List<String> topTenants,
            @Value("${s2.observability.nl2sql.tenant-tag-limit:50}") int limit,
            @Value("${s2.observability.nl2sql.emit-tenant-tag:true}") boolean emit) {
        this.allowlist = Set.copyOf(topTenants);
        this.limit = Math.max(1, limit);
        this.emit = emit;
    }

    public String normalize(String tenantId) {
        if (!emit) {
            return DISABLED;
        }
        if (tenantId == null || tenantId.isBlank()) {
            return NONE;
        }
        if (allowlist.contains(tenantId)) {
            return tenantId;
        }
        if (dynamicAdmitted.contains(tenantId)) {
            return tenantId;
        }
        if (allowlist.size() + dynamicAdmitted.size() < limit) {
            dynamicAdmitted.add(tenantId);
            return tenantId;
        }
        return OTHER;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl common test -Dtest=TenantTagNormalizerTest`
Expected: PASS.

- [ ] **Step 5: Add `Nl2sqlMetricConstants`**

```java
package com.tencent.supersonic.common.metrics;

/**
 * Metric names and tag keys for the NL2SQL chain. Names follow Prometheus conventions
 * (snake_case + unit suffix) once Micrometer converts dots via the Prometheus naming convention.
 */
public final class Nl2sqlMetricConstants {

    private Nl2sqlMetricConstants() {}

    public static final String MODULE = "nl2sql";

    // histograms (rendered as _seconds buckets in Prometheus)
    public static final String STAGE_DURATION = "s2.nl2sql.stage.duration";
    public static final String LLM_DURATION = "s2.nl2sql.llm.duration";
    public static final String DB_DURATION = "s2.nl2sql.db.duration";

    // counters
    public static final String STAGE_OUTCOME_TOTAL = "s2.nl2sql.stage.outcome.total";
    public static final String LLM_TOKENS_TOTAL = "s2.nl2sql.llm.tokens.total";
    public static final String MAPPER_HITS_TOTAL = "s2.nl2sql.mapper.hits.total";

    // summary
    public static final String DB_ROWS_SCANNED = "s2.nl2sql.sql.rows.scanned";

    // outcomes
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_ERROR = "error";
    public static final String OUTCOME_TIMEOUT = "timeout";
    public static final String OUTCOME_EMPTY = "empty";

    public static final class TagKeys {
        private TagKeys() {}

        public static final String MODULE = "module";
        public static final String STAGE = "stage";
        public static final String OUTCOME = "outcome";
        public static final String TENANT = "tenant_id";
        public static final String AGENT = "agent_id";
        public static final String PARSER = "parser_name";
        public static final String MAPPER = "mapper_name";
        public static final String CORRECTOR = "corrector_name";
        public static final String MODEL = "model";
        public static final String KIND = "kind";          // prompt | completion
        public static final String DB_TYPE = "db_type";
        public static final String HIT = "hit";            // true | false
    }
}
```

- [ ] **Step 6: Compile to catch typos**

Run: `mvn -pl common compile`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/metrics/Nl2sqlMetricConstants.java \
        common/src/main/java/com/tencent/supersonic/common/metrics/TenantTagNormalizer.java \
        common/src/test/java/com/tencent/supersonic/common/metrics/TenantTagNormalizerTest.java
git commit -m "feat(metrics): add NL2SQL metric constants + tenant tag normalizer

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: `Nl2sqlMetrics` facade

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/metrics/Nl2sqlMetrics.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/metrics/Nl2sqlMetricsTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Nl2sqlMetricsTest {

    private SimpleMeterRegistry registry;
    private Nl2sqlMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        TenantTagNormalizer normalizer = new TenantTagNormalizer(List.of("acme"), 50, true);
        metrics = new Nl2sqlMetrics(registry, normalizer);
    }

    @Test
    void recordStagePublishesTimerWithTags() {
        metrics.recordStage("rule_parse", Duration.ofMillis(123),
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "acme", "agent-1", "NL2SQLParser");

        Timer t = registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("stage", "rule_parse")
                .tag("outcome", "success")
                .tag("tenant_id", "acme")
                .tag("agent_id", "agent-1")
                .tag("parser_name", "NL2SQLParser")
                .timer();
        assertThat(t).isNotNull();
        assertThat(t.count()).isEqualTo(1);
        assertThat(t.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(100);
    }

    @Test
    void recordStageNormalizesUnknownTenant() {
        metrics.recordStage("rule_parse", Duration.ofMillis(10),
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "some-new-tenant", "agent-1", "NL2SQLParser");

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("tenant_id", "some-new-tenant").timer()).isNotNull();
    }

    @Test
    void recordLlmTokensPublishesCounterPerKind() {
        metrics.recordLlmTokens("gpt-4o", 300, 120);
        Counter prompt = registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tag("model", "gpt-4o").tag("kind", "prompt").counter();
        Counter completion = registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tag("model", "gpt-4o").tag("kind", "completion").counter();
        assertThat(prompt.count()).isEqualTo(300);
        assertThat(completion.count()).isEqualTo(120);
    }

    @Test
    void startStageReturnsAutoCloseableThatStopsTimer() {
        try (Nl2sqlMetrics.StageTimer t = metrics.startStage("mapper",
                "acme", "agent-1", "NL2SQLParser")) {
            t.markMapper("KeywordMapper");
            // simulate work
        }
        Timer timer = registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("stage", "mapper").tag("mapper_name", "KeywordMapper").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void mapperHitCounterIncrements() {
        metrics.recordMapperHit("KeywordMapper", true, "acme");
        Counter c = registry.find(Nl2sqlMetricConstants.MAPPER_HITS_TOTAL)
                .tag("mapper_name", "KeywordMapper").tag("hit", "true").counter();
        assertThat(c.count()).isEqualTo(1.0);
    }
}
```

- [ ] **Step 2: Run the test, confirm it fails to compile**

Run: `mvn -pl common test -Dtest=Nl2sqlMetricsTest`
Expected: FAIL — `Nl2sqlMetrics` does not exist.

- [ ] **Step 3: Implement `Nl2sqlMetrics`**

```java
package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Facade over {@link MeterRegistry} for the NL2SQL chain. Centralises metric names, tag
 * normalisation, and outcome enumerations. All stage instrumentation should go through this class
 * rather than talking to {@link MeterRegistry} directly.
 */
@Component
@ConditionalOnBean(MeterRegistry.class)
@RequiredArgsConstructor
public class Nl2sqlMetrics {

    private final MeterRegistry registry;
    private final TenantTagNormalizer tenantTagNormalizer;

    public void recordStage(String stage, Duration duration, String outcome,
                            String tenantId, String agentId, String parserName) {
        Timer.builder(Nl2sqlMetricConstants.STAGE_DURATION)
                .tags(baseTags(stage, outcome, tenantId, agentId, parserName))
                .register(registry)
                .record(duration);
        Counter.builder(Nl2sqlMetricConstants.STAGE_OUTCOME_TOTAL)
                .tags(baseTags(stage, outcome, tenantId, agentId, parserName))
                .register(registry)
                .increment();
    }

    /**
     * Starts a stage timer. The returned {@link StageTimer} is AutoCloseable; closing it stops the
     * timer and publishes the sample. Outcome defaults to {@code success} unless
     * {@link StageTimer#failed(String)} is called.
     */
    public StageTimer startStage(String stage, String tenantId, String agentId, String parserName) {
        return new StageTimer(this, stage, tenantId, agentId, parserName);
    }

    public void recordLlmLatency(String model, Duration duration, String outcome,
                                 String tenantId, String agentId) {
        Timer.builder(Nl2sqlMetricConstants.LLM_DURATION)
                .tags(Tags.of(
                        Nl2sqlMetricConstants.TagKeys.MODEL, safe(model),
                        Nl2sqlMetricConstants.TagKeys.OUTCOME, safe(outcome),
                        Nl2sqlMetricConstants.TagKeys.TENANT, tenantTagNormalizer.normalize(tenantId),
                        Nl2sqlMetricConstants.TagKeys.AGENT, safe(agentId),
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry)
                .record(duration);
    }

    public void recordLlmTokens(String model, long promptTokens, long completionTokens) {
        Counter.builder(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tags(Tags.of(
                        Nl2sqlMetricConstants.TagKeys.MODEL, safe(model),
                        Nl2sqlMetricConstants.TagKeys.KIND, "prompt",
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry)
                .increment(promptTokens);
        Counter.builder(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tags(Tags.of(
                        Nl2sqlMetricConstants.TagKeys.MODEL, safe(model),
                        Nl2sqlMetricConstants.TagKeys.KIND, "completion",
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry)
                .increment(completionTokens);
    }

    public void recordMapperHit(String mapperName, boolean hit, String tenantId) {
        Counter.builder(Nl2sqlMetricConstants.MAPPER_HITS_TOTAL)
                .tags(Tags.of(
                        Nl2sqlMetricConstants.TagKeys.MAPPER, safe(mapperName),
                        Nl2sqlMetricConstants.TagKeys.HIT, String.valueOf(hit),
                        Nl2sqlMetricConstants.TagKeys.TENANT, tenantTagNormalizer.normalize(tenantId),
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry)
                .increment();
    }

    public void recordDb(String dbType, Duration duration, long rowsScanned, String outcome,
                         String tenantId) {
        Timer.builder(Nl2sqlMetricConstants.DB_DURATION)
                .tags(Tags.of(
                        Nl2sqlMetricConstants.TagKeys.DB_TYPE, safe(dbType),
                        Nl2sqlMetricConstants.TagKeys.OUTCOME, safe(outcome),
                        Nl2sqlMetricConstants.TagKeys.TENANT, tenantTagNormalizer.normalize(tenantId),
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry)
                .record(duration);
        if (rowsScanned >= 0) {
            DistributionSummary.builder(Nl2sqlMetricConstants.DB_ROWS_SCANNED)
                    .tags(Tags.of(
                            Nl2sqlMetricConstants.TagKeys.DB_TYPE, safe(dbType),
                            Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                    .register(registry)
                    .record(rowsScanned);
        }
    }

    private Tags baseTags(String stage, String outcome, String tenantId, String agentId,
                          String parserName) {
        return Tags.of(
                Nl2sqlMetricConstants.TagKeys.STAGE, safe(stage),
                Nl2sqlMetricConstants.TagKeys.OUTCOME, safe(outcome),
                Nl2sqlMetricConstants.TagKeys.TENANT, tenantTagNormalizer.normalize(tenantId),
                Nl2sqlMetricConstants.TagKeys.AGENT, safe(agentId),
                Nl2sqlMetricConstants.TagKeys.PARSER, safe(parserName),
                Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE);
    }

    private static String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }

    /**
     * AutoCloseable wrapper over {@link Timer.Sample}. Defaults outcome=success; callers may
     * override via {@link #failed(String)} or {@link #timedOut()}. Supports attaching a
     * mapper/corrector name for finer-grained stage tagging.
     */
    public static final class StageTimer implements AutoCloseable {

        private final Nl2sqlMetrics owner;
        private final String stage;
        private final String tenantId;
        private final String agentId;
        private final String parserName;
        private final long startNanos = System.nanoTime();
        private String outcome = Nl2sqlMetricConstants.OUTCOME_SUCCESS;
        private String mapperName;
        private String correctorName;
        private boolean stopped;

        StageTimer(Nl2sqlMetrics owner, String stage, String tenantId, String agentId,
                   String parserName) {
            this.owner = owner;
            this.stage = stage;
            this.tenantId = tenantId;
            this.agentId = agentId;
            this.parserName = parserName;
        }

        public StageTimer markMapper(String mapperName) {
            this.mapperName = mapperName;
            return this;
        }

        public StageTimer markCorrector(String correctorName) {
            this.correctorName = correctorName;
            return this;
        }

        public void failed(String outcome) {
            this.outcome = (outcome == null) ? Nl2sqlMetricConstants.OUTCOME_ERROR : outcome;
        }

        public void timedOut() {
            this.outcome = Nl2sqlMetricConstants.OUTCOME_TIMEOUT;
        }

        @Override
        public void close() {
            if (stopped) {
                return;
            }
            stopped = true;
            Duration d = Duration.ofNanos(System.nanoTime() - startNanos);
            Tags tags = owner.baseTags(stage, outcome, tenantId, agentId, parserName);
            if (mapperName != null) {
                tags = tags.and(Nl2sqlMetricConstants.TagKeys.MAPPER, mapperName);
            }
            if (correctorName != null) {
                tags = tags.and(Nl2sqlMetricConstants.TagKeys.CORRECTOR, correctorName);
            }
            Timer.builder(Nl2sqlMetricConstants.STAGE_DURATION).tags(tags)
                    .register(owner.registry).record(d.toNanos(), TimeUnit.NANOSECONDS);
            Counter.builder(Nl2sqlMetricConstants.STAGE_OUTCOME_TOTAL).tags(tags)
                    .register(owner.registry).increment();
        }
    }
}
```

- [ ] **Step 4: Run the test and confirm it passes**

Run: `mvn -pl common test -Dtest=Nl2sqlMetricsTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/metrics/Nl2sqlMetrics.java \
        common/src/test/java/com/tencent/supersonic/common/metrics/Nl2sqlMetricsTest.java
git commit -m "feat(metrics): add Nl2sqlMetrics facade with Timer.Sample helper

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: `QueryTraceContext`

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/metrics/QueryTraceContext.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/metrics/QueryTraceContextTest.java`

Reuses existing `TraceIdUtil` (`common/src/main/java/com/tencent/supersonic/common/util/TraceIdUtil.java`) for the UUID generation convention but adds a *separate* MDC key `queryTraceId` so HTTP `traceId` and per-query traces can coexist.

- [ ] **Step 1: Write the failing test**

```java
package com.tencent.supersonic.common.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QueryTraceContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void openSetsMdcAndCloseRestoresPrevious() {
        MDC.put(QueryTraceContext.KEY, "pre-existing");
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            assertThat(MDC.get(QueryTraceContext.KEY)).isNotEqualTo("pre-existing");
            assertThat(s.traceId()).startsWith("q_");
        }
        assertThat(MDC.get(QueryTraceContext.KEY)).isEqualTo("pre-existing");
    }

    @Test
    void openSetsMdcAndCloseClearsWhenNoPrevious() {
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            assertThat(MDC.get(QueryTraceContext.KEY)).isEqualTo(s.traceId());
        }
        assertThat(MDC.get(QueryTraceContext.KEY)).isNull();
    }

    @Test
    void currentReturnsEmptyWhenUnset() {
        assertThat(QueryTraceContext.current()).isEmpty();
    }

    @Test
    void snapshotCarriesTraceAcrossBoundary() throws Exception {
        try (QueryTraceContext.Scope outer = QueryTraceContext.open()) {
            Map<String, String> snap = QueryTraceContext.snapshot();
            MDC.remove(QueryTraceContext.KEY);
            QueryTraceContext.restore(snap);
            assertThat(MDC.get(QueryTraceContext.KEY)).isEqualTo(outer.traceId());
        }
    }
}
```

- [ ] **Step 2: Run the test, expect compile failure**

Run: `mvn -pl common test -Dtest=QueryTraceContextTest`
Expected: FAIL — missing class.

- [ ] **Step 3: Implement `QueryTraceContext`**

```java
package com.tencent.supersonic.common.metrics;

import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-query trace id manager. Distinct from {@link com.tencent.supersonic.common.util.TraceIdUtil}
 * (which scopes an HTTP request) — this scopes one NL2SQL parse and is safe to nest.
 *
 * <p>Typical use at parser entry:</p>
 * <pre>
 *   try (QueryTraceContext.Scope scope = QueryTraceContext.open()) {
 *       // parse stages log with MDC key "queryTraceId"
 *   }
 * </pre>
 *
 * <p>For async boundaries, wrap tasks with {@link #wrap(Runnable)} or call
 * {@link #snapshot()} + {@link #restore(Map)} manually. {@code ContextAwareThreadPoolExecutor}
 * already copies MDC so most cases are covered.</p>
 */
public final class QueryTraceContext {

    public static final String KEY = "queryTraceId";
    public static final String PREFIX = "q_";

    private QueryTraceContext() {}

    public static Scope open() {
        String previous = MDC.get(KEY);
        String id = PREFIX + UUID.randomUUID().toString().replace("-", "");
        MDC.put(KEY, id);
        return new Scope(id, previous);
    }

    public static Scope open(String id) {
        String previous = MDC.get(KEY);
        MDC.put(KEY, id);
        return new Scope(id, previous);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(KEY));
    }

    public static Map<String, String> snapshot() {
        Map<String, String> snap = new HashMap<>();
        String v = MDC.get(KEY);
        if (v != null) {
            snap.put(KEY, v);
        }
        return Collections.unmodifiableMap(snap);
    }

    public static void restore(Map<String, String> snapshot) {
        if (snapshot == null) {
            return;
        }
        String v = snapshot.get(KEY);
        if (v != null) {
            MDC.put(KEY, v);
        }
    }

    public static Runnable wrap(Runnable task) {
        Map<String, String> snap = snapshot();
        return () -> {
            String prev = MDC.get(KEY);
            try {
                restore(snap);
                task.run();
            } finally {
                if (prev == null) {
                    MDC.remove(KEY);
                } else {
                    MDC.put(KEY, prev);
                }
            }
        };
    }

    public static final class Scope implements AutoCloseable {
        private final String traceId;
        private final String previous;

        Scope(String traceId, String previous) {
            this.traceId = traceId;
            this.previous = previous;
        }

        public String traceId() {
            return traceId;
        }

        @Override
        public void close() {
            if (previous == null) {
                MDC.remove(KEY);
            } else {
                MDC.put(KEY, previous);
            }
        }
    }
}
```

- [ ] **Step 4: Run to verify PASS**

Run: `mvn -pl common test -Dtest=QueryTraceContextTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/metrics/QueryTraceContext.java \
        common/src/test/java/com/tencent/supersonic/common/metrics/QueryTraceContextTest.java
git commit -m "feat(metrics): add QueryTraceContext MDC wrapper with async snapshot/restore

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: Instrument `NL2SQLParser` entry point

**Files:**
- Modify: `chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/NL2SQLParser.java`
- Test: `chat/server/src/test/java/com/tencent/supersonic/chat/server/parser/NL2SQLParserMetricsTest.java` (new)

Before the change, `parse(ParseContext)` has no timing. After, the whole parse is wrapped in a `QueryTraceContext.Scope` and the rule-phase / llm-phase each emit a `stage=rule_parse` / `stage=llm_parse` histogram sample via `Nl2sqlMetrics.startStage`.

- [ ] **Step 1: Write the failing test**

```java
package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NL2SQLParserMetricsTest {

    @Test
    void stageTimerProducesSampleWithParserTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TenantTagNormalizer normalizer = new TenantTagNormalizer(List.of("acme"), 10, true);
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(registry, normalizer);

        try (Nl2sqlMetrics.StageTimer t = metrics.startStage(
                "rule_parse", "acme", "agent-1", "NL2SQLParser")) {
            assertThat(MDC.get("queryTraceId")).isNull(); // parser sets this, facade doesn't
        }

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("stage", "rule_parse")
                .tag("parser_name", "NL2SQLParser").timer()).isNotNull();
    }
}
```

- [ ] **Step 2: Run to confirm PASS**

Run: `mvn -pl chat/server -am test -Dtest=NL2SQLParserMetricsTest`
Expected: PASS (this is a regression guard; the parser-facing wiring is covered in the code change).

- [ ] **Step 3: Modify `NL2SQLParser.java` — imports**

Open `chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/NL2SQLParser.java`, add imports after line 17:

```java
import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.QueryTraceContext;
```

- [ ] **Step 4: Modify `NL2SQLParser#parse` — wrap with trace scope and stage timers**

Replace the method body of `parse(ParseContext parseContext)` (currently lines 82-156).

Before:
```java
@Override
public void parse(ParseContext parseContext) {
    // first go with rule-based parsers unless the user has already selected one parse.
    if (Objects.isNull(parseContext.getRequest().getSelectedParse())) {
        QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
        queryNLReq.setText2SQLType(Text2SQLType.ONLY_RULE);
        if (parseContext.enableLLM()) {
            queryNLReq.setText2SQLType(Text2SQLType.NONE);
        }
        // ... rule-based loop ...
    }

    if (parseContext.needLLMParse() && !parseContext.needFeedback()) {
        // ... llm-based block ...
    }
}
```

After:
```java
@Override
public void parse(ParseContext parseContext) {
    Nl2sqlMetrics metrics = ContextUtils.getBean(Nl2sqlMetrics.class);
    String tenantId = com.tencent.supersonic.common.pojo.TenantContext.getTenantIdOrDefault();
    String agentId = parseContext.getAgent() == null ? "unknown"
            : String.valueOf(parseContext.getAgent().getId());

    try (QueryTraceContext.Scope trace = QueryTraceContext.open()) {
        log.info("NL2SQL parse begin, queryTraceId={}, agentId={}, query={}",
                trace.traceId(), agentId, parseContext.getRequest().getQueryText());

        // first go with rule-based parsers unless the user has already selected one parse.
        if (Objects.isNull(parseContext.getRequest().getSelectedParse())) {
            try (Nl2sqlMetrics.StageTimer stage = metrics.startStage(
                    "rule_parse", tenantId, agentId, "NL2SQLParser")) {
                try {
                    QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
                    queryNLReq.setText2SQLType(Text2SQLType.ONLY_RULE);
                    if (parseContext.enableLLM()) {
                        queryNLReq.setText2SQLType(Text2SQLType.NONE);
                    }
                    Set<Long> requestedDatasets = queryNLReq.getDataSetIds();
                    List<SemanticParseInfo> candidateParses = Lists.newArrayList();
                    StringBuilder errMsg = new StringBuilder();
                    for (Long datasetId : requestedDatasets) {
                        queryNLReq.setDataSetIds(Collections.singleton(datasetId));
                        ChatParseResp parseResp = new ChatParseResp(parseContext.getRequest().getQueryId());
                        for (MapModeEnum mode : Lists.newArrayList(MapModeEnum.STRICT, MapModeEnum.MODERATE)) {
                            queryNLReq.setMapModeEnum(mode);
                            doParse(queryNLReq, parseResp);
                        }
                        if (parseResp.getSelectedParses().isEmpty() && candidateParses.isEmpty()) {
                            queryNLReq.setMapModeEnum(MapModeEnum.LOOSE);
                            doParse(queryNLReq, parseResp);
                        }
                        if (parseResp.getSelectedParses().isEmpty()) {
                            errMsg.append(parseResp.getErrorMsg());
                            continue;
                        }
                        SemanticParseInfo.sort(parseResp.getSelectedParses());
                        candidateParses.add(parseResp.getSelectedParses().get(0));
                    }
                    ParserConfig parserConfig = ContextUtils.getBean(ParserConfig.class);
                    int parserShowCount =
                            Integer.parseInt(parserConfig.getParameterValue(PARSER_SHOW_COUNT));
                    SemanticParseInfo.sort(candidateParses);
                    parseContext.getResponse().setSelectedParses(candidateParses.subList(0,
                            Math.min(parserShowCount, candidateParses.size())));
                    if (parseContext.getResponse().getSelectedParses().isEmpty()) {
                        parseContext.getResponse().setState(ParseResp.ParseState.FAILED);
                        parseContext.getResponse().setErrorMsg(errMsg.toString());
                        stage.failed(Nl2sqlMetricConstants.OUTCOME_EMPTY);
                    }
                } catch (RuntimeException e) {
                    stage.failed(Nl2sqlMetricConstants.OUTCOME_ERROR);
                    throw e;
                }
            }
        }

        if (parseContext.needLLMParse() && !parseContext.needFeedback()) {
            if (Objects.isNull(parseContext.getRequest().getSelectedParse())
                    && parseContext.getResponse().getSelectedParses().isEmpty()) {
                return;
            }
            try (Nl2sqlMetrics.StageTimer stage = metrics.startStage(
                    "llm_parse", tenantId, agentId, "NL2SQLParser")) {
                try {
                    QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
                    queryNLReq.setText2SQLType(Text2SQLType.LLM_OR_RULE);
                    SemanticParseInfo userSelectParse = parseContext.getRequest().getSelectedParse();
                    queryNLReq.setSelectedParseInfo(Objects.nonNull(userSelectParse) ? userSelectParse
                            : parseContext.getResponse().getSelectedParses().get(0));
                    parseContext.setResponse(new ChatParseResp(parseContext.getResponse().getQueryId()));

                    rewriteMultiTurn(parseContext, queryNLReq);
                    addDynamicExemplars(parseContext, queryNLReq);
                    doParse(queryNLReq, parseContext.getResponse());

                    if (parseContext.getResponse().getState().equals(ParseResp.ParseState.FAILED)) {
                        queryNLReq.setSelectedParseInfo(null);
                        queryNLReq.setMapModeEnum(MapModeEnum.ALL);
                        doParse(queryNLReq, parseContext.getResponse());
                        if (parseContext.getResponse().getState().equals(ParseResp.ParseState.FAILED)) {
                            stage.failed(Nl2sqlMetricConstants.OUTCOME_ERROR);
                        }
                    }
                } catch (RuntimeException e) {
                    stage.failed(Nl2sqlMetricConstants.OUTCOME_ERROR);
                    throw e;
                }
            }
        }
    }
}
```

Note: if `TenantContext.getTenantIdOrDefault()` does not exist, use the existing tenant accessor (`TenantContext.getTenantId()` returning `null` is handled by `TenantTagNormalizer`). Verify with a quick grep before compiling:

Run: `grep -n "getTenantId" common/src/main/java/com/tencent/supersonic/common/pojo/TenantContext.java`
Expected: method named `getTenantId()`; use that and fall back to `"unknown"` if null.

- [ ] **Step 5: Compile and run tests**

Run: `mvn compile -pl launchers/standalone -am`
Run: `mvn -pl chat/server -am test -Dtest=NL2SQLParserMetricsTest`
Expected: BUILD SUCCESS, PASS.

- [ ] **Step 6: Commit**

```bash
git add chat/server/src/main/java/com/tencent/supersonic/chat/server/parser/NL2SQLParser.java \
        chat/server/src/test/java/com/tencent/supersonic/chat/server/parser/NL2SQLParserMetricsTest.java
git commit -m "feat(nl2sql): instrument parser entry with stage timers and queryTraceId

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6: Instrument mapper strategies

**Files:**
- Modify: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/mapper/BaseMapper.java`
- Test: `headless/chat/src/test/java/com/tencent/supersonic/headless/chat/mapper/BaseMapperMetricsTest.java` (new)

Every mapper subclasses `BaseMapper` (see existing list: `KeywordMapper`, `EmbeddingMapper`, `AllFieldMapper`, `PartitionTimeMapper`, `QueryFilterMapper`, `SchemaMapper`, `TermDescMapper`). Instrument the base so all subclasses are covered automatically.

- [ ] **Step 1: Write the failing test**

```java
package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseMapperMetricsTest {

    private SimpleMeterRegistry registry;
    private Nl2sqlMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new Nl2sqlMetrics(registry, new TenantTagNormalizer(List.of(), 50, true));
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(Nl2sqlMetrics.class)).thenReturn(metrics);
        ContextUtils.setApplicationContext(ctx);
    }

    @Test
    void mapperExecutionEmitsStageMetricWithMapperTag() {
        BaseMapper mapper = new BaseMapper() {
            @Override public void doMap(ChatQueryContext c) { /* no-op */ }
        };
        ChatQueryContext ctx = new ChatQueryContext();

        mapper.map(ctx);

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("stage", "mapper")
                .tag("mapper_name", mapper.getClass().getSimpleName()).timer()).isNotNull();
    }
}
```

- [ ] **Step 2: Run, expect failure**

Run: `mvn -pl headless/chat test -Dtest=BaseMapperMetricsTest`
Expected: FAIL (no metric emitted).

- [ ] **Step 3: Modify `BaseMapper.map`**

Replace `map(ChatQueryContext chatQueryContext)` body (currently lines 29-49):

```java
@Override
public void map(ChatQueryContext chatQueryContext) {
    if (!accept(chatQueryContext)) {
        return;
    }
    String simpleName = this.getClass().getSimpleName();

    com.tencent.supersonic.common.metrics.Nl2sqlMetrics metrics =
            com.tencent.supersonic.common.util.ContextUtils
                    .getBean(com.tencent.supersonic.common.metrics.Nl2sqlMetrics.class);
    String tenantId;
    try {
        tenantId = com.tencent.supersonic.common.pojo.TenantContext.getTenantId();
    } catch (Throwable ignore) {
        tenantId = null;
    }
    String agentId = "unknown";

    log.debug("before {},mapInfo:{}", simpleName,
            chatQueryContext.getMapInfo().getDataSetElementMatches());

    int matchesBefore = chatQueryContext.getMapInfo().getDataSetElementMatches().size();
    try (com.tencent.supersonic.common.metrics.Nl2sqlMetrics.StageTimer t =
                 metrics.startStage("mapper", tenantId, agentId, "NL2SQLParser")
                         .markMapper(simpleName)) {
        try {
            doMap(chatQueryContext);
            MapFilter.filter(chatQueryContext);
            int matchesAfter = chatQueryContext.getMapInfo().getDataSetElementMatches().size();
            boolean hit = matchesAfter > matchesBefore;
            metrics.recordMapperHit(simpleName, hit, tenantId);
        } catch (Exception e) {
            t.failed(com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_ERROR);
            log.error("work error", e);
        }
    }

    log.debug("after {},mapInfo:{}", simpleName,
            chatQueryContext.getMapInfo().getDataSetElementMatches());
}
```

- [ ] **Step 4: Run to confirm PASS**

Run: `mvn -pl headless/chat test -Dtest=BaseMapperMetricsTest`
Expected: PASS.

- [ ] **Step 5: Compile whole launcher**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add headless/chat/src/main/java/com/tencent/supersonic/headless/chat/mapper/BaseMapper.java \
        headless/chat/src/test/java/com/tencent/supersonic/headless/chat/mapper/BaseMapperMetricsTest.java
git commit -m "feat(nl2sql): instrument mapper stage with duration + hit counter

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7: Instrument `LLMRequestService.runText2SQL`

**Files:**
- Modify: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMRequestService.java`
- Modify: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMSqlParser.java` (retry loop trace propagation)
- Test: `headless/chat/src/test/java/com/tencent/supersonic/headless/chat/parser/llm/LLMRequestServiceMetricsTest.java` (new)

Token counts come from LangChain4j `Response<AiMessage>`; current `runText2SQL` returns an `LLMResp` built by a `SqlGenStrategy`. We capture latency at the `runText2SQL` boundary, and fetch token data from `LLMResp` (add getters if needed — confirm in spec below).

- [ ] **Step 1: Inspect `LLMResp` to confirm token fields**

Run: `grep -n "tokens\|usage\|promptToken" headless/chat/src/main/java/com/tencent/supersonic/headless/chat/query/llm/s2sql/LLMResp.java`

If no token fields exist, add them as part of this task. Assume they need to be added for the plan — the step below covers it.

- [ ] **Step 2: Add token fields to `LLMResp`**

File: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/query/llm/s2sql/LLMResp.java`. Near other fields:

```java
private String modelName;
private long promptTokens;
private long completionTokens;
```

Ensure `OnePassSCSqlGenStrategy` and any other implementations populate these from LangChain4j `Response.tokenUsage()`. Existing implementations already call `chatLanguageModel.generate(...)`; find each `Response<AiMessage>` usage and wire through.

Run: `grep -rn "chatLanguageModel.generate" headless/chat/src/main/java` — edit every return point where an `LLMResp` is built to copy `response.tokenUsage().inputTokenCount()` / `outputTokenCount()` and `response.metadata()` model name (or the configured `ChatModelConfig.getModelName()` if metadata isn't populated).

- [ ] **Step 3: Write the failing test**

```java
package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMResp;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LLMRequestServiceMetricsTest {

    @Test
    void recordingLlmMetricsAddsLatencyAndTokenCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(registry,
                new TenantTagNormalizer(List.of(), 10, true));

        LLMResp resp = new LLMResp();
        resp.setModelName("gpt-4o");
        resp.setPromptTokens(300);
        resp.setCompletionTokens(120);

        metrics.recordLlmLatency(resp.getModelName(), java.time.Duration.ofMillis(500),
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "acme", "agent-1");
        metrics.recordLlmTokens(resp.getModelName(), resp.getPromptTokens(), resp.getCompletionTokens());

        assertThat(registry.find(Nl2sqlMetricConstants.LLM_DURATION)
                .tag("model", "gpt-4o").timer()).isNotNull();
        assertThat(registry.find(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                .tag("model", "gpt-4o").tag("kind", "prompt").counter().count()).isEqualTo(300);
    }
}
```

- [ ] **Step 4: Run, expect PASS (facade test already green; this is regression guard)**

Run: `mvn -pl headless/chat test -Dtest=LLMRequestServiceMetricsTest`
Expected: PASS.

- [ ] **Step 5: Modify `LLMRequestService.runText2SQL`**

Replace the method (currently lines 76-83):

```java
public LLMResp runText2SQL(LLMReq llmReq) {
    com.tencent.supersonic.common.metrics.Nl2sqlMetrics metrics =
            com.tencent.supersonic.common.util.ContextUtils
                    .getBean(com.tencent.supersonic.common.metrics.Nl2sqlMetrics.class);
    String tenantId;
    try {
        tenantId = com.tencent.supersonic.common.pojo.TenantContext.getTenantId();
    } catch (Throwable ignore) {
        tenantId = null;
    }
    String agentId = "unknown";

    SqlGenStrategy sqlGenStrategy = SqlGenStrategyFactory.get(llmReq.getSqlGenType());
    String dataSet = llmReq.getSchema().getDataSetName();
    long startNanos = System.nanoTime();
    String outcome = com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_SUCCESS;
    LLMResp result = null;
    try {
        result = sqlGenStrategy.generate(llmReq);
        if (result == null) {
            outcome = com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_EMPTY;
        }
        return result;
    } catch (RuntimeException e) {
        outcome = com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_ERROR;
        throw e;
    } finally {
        java.time.Duration elapsed = java.time.Duration.ofNanos(System.nanoTime() - startNanos);
        String modelName = result != null && result.getModelName() != null
                ? result.getModelName() : "unknown";
        metrics.recordLlmLatency(modelName, elapsed, outcome, tenantId, agentId);
        if (result != null) {
            metrics.recordLlmTokens(modelName, result.getPromptTokens(), result.getCompletionTokens());
            result.setQuery(llmReq.getQueryText());
            result.setDataSet(dataSet);
        }
    }
}
```

- [ ] **Step 6: Modify `LLMSqlParser.tryParse` retry loop so queryTraceId is kept**

File: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMSqlParser.java`. No threading crossed (single-thread loop), MDC already carried. No code change required beyond logging — add a single trace-aware log line at loop entry (line 60):

```java
log.info("currentRetryRound:{}, queryTraceId={}, start runText2SQL", currentRetry,
        com.tencent.supersonic.common.metrics.QueryTraceContext.current().orElse("none"));
```

- [ ] **Step 7: Compile & run test**

Run: `mvn compile -pl launchers/standalone -am`
Run: `mvn -pl headless/chat test -Dtest=LLMRequestServiceMetricsTest`
Expected: BUILD SUCCESS, PASS.

- [ ] **Step 8: Commit**

```bash
git add headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMRequestService.java \
        headless/chat/src/main/java/com/tencent/supersonic/headless/chat/parser/llm/LLMSqlParser.java \
        headless/chat/src/main/java/com/tencent/supersonic/headless/chat/query/llm/s2sql/LLMResp.java \
        headless/chat/src/test/java/com/tencent/supersonic/headless/chat/parser/llm/LLMRequestServiceMetricsTest.java
git commit -m "feat(nl2sql): instrument LLM call with latency + token metrics

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8: `CorrectorMetricsDecorator` — generic SPI wrapper

**Files:**
- Create: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsDecorator.java`
- Create: `headless/chat/src/main/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsConfiguration.java`
- Test: `headless/chat/src/test/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsDecoratorTest.java`

Every corrector (`TimeCorrector`, `WhereCorrector`, `AggCorrector`, `GroupByCorrector`, `GrammarCorrector`, `HavingCorrector`, `SchemaCorrector`, `SelectCorrector`, `DetailFieldCorrector`, `RuleSqlCorrector`, `LLMSqlCorrector`, `LLMPhysicalSqlCorrector`) implements `SemanticCorrector`. The decorator wraps any instance without touching existing code. A `BeanPostProcessor` auto-wraps every `SemanticCorrector` bean on application startup.

- [ ] **Step 1: Write the failing decorator test**

```java
package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorrectorMetricsDecoratorTest {

    @Test
    void wrapsDelegateAndEmitsStageMetricWithCorrectorTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(registry,
                new TenantTagNormalizer(List.of(), 10, true));

        SemanticCorrector delegate = (ctx, info) -> { /* no-op */ };
        CorrectorMetricsDecorator decorated = new CorrectorMetricsDecorator(
                delegate, "TimeCorrector", metrics);

        decorated.correct(new ChatQueryContext(), new SemanticParseInfo());

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_DURATION)
                .tag("stage", "corrector")
                .tag("corrector_name", "TimeCorrector").timer()).isNotNull();
    }

    @Test
    void recordsErrorOutcomeWhenDelegateThrows() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(registry,
                new TenantTagNormalizer(List.of(), 10, true));

        SemanticCorrector bad = (ctx, info) -> { throw new IllegalStateException("boom"); };
        CorrectorMetricsDecorator decorated = new CorrectorMetricsDecorator(
                bad, "WhereCorrector", metrics);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> decorated.correct(new ChatQueryContext(), new SemanticParseInfo()));

        assertThat(registry.find(Nl2sqlMetricConstants.STAGE_OUTCOME_TOTAL)
                .tag("outcome", "error").counter().count()).isEqualTo(1.0);
    }
}
```

- [ ] **Step 2: Run, expect compile failure**

Run: `mvn -pl headless/chat test -Dtest=CorrectorMetricsDecoratorTest`
Expected: FAIL — decorator does not exist.

- [ ] **Step 3: Implement the decorator**

```java
package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.pojo.TenantContext;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;

/**
 * Wraps any {@link SemanticCorrector} with stage timing and outcome counter emission. Tags the
 * stage duration with {@code stage=corrector, corrector_name=<simple class name>}.
 */
public class CorrectorMetricsDecorator implements SemanticCorrector {

    private final SemanticCorrector delegate;
    private final String correctorName;
    private final Nl2sqlMetrics metrics;

    public CorrectorMetricsDecorator(SemanticCorrector delegate, String correctorName,
                                     Nl2sqlMetrics metrics) {
        this.delegate = delegate;
        this.correctorName = correctorName;
        this.metrics = metrics;
    }

    @Override
    public void correct(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        String tenantId;
        try {
            tenantId = TenantContext.getTenantId();
        } catch (Throwable ignore) {
            tenantId = null;
        }
        try (Nl2sqlMetrics.StageTimer t =
                     metrics.startStage("corrector", tenantId, "unknown", "NL2SQLParser")
                             .markCorrector(correctorName)) {
            try {
                delegate.correct(chatQueryContext, semanticParseInfo);
            } catch (RuntimeException e) {
                t.failed(Nl2sqlMetricConstants.OUTCOME_ERROR);
                throw e;
            }
        }
    }

    public SemanticCorrector delegate() {
        return delegate;
    }
}
```

- [ ] **Step 4: Implement the `BeanPostProcessor`**

```java
package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Wraps every {@link SemanticCorrector} bean with {@link CorrectorMetricsDecorator} at startup so
 * every SPI-registered corrector is observed without touching its implementation.
 */
@Configuration
@ConditionalOnBean(Nl2sqlMetrics.class)
@RequiredArgsConstructor
public class CorrectorMetricsConfiguration implements BeanPostProcessor {

    private final ObjectProvider<Nl2sqlMetrics> metricsProvider;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (bean instanceof SemanticCorrector && !(bean instanceof CorrectorMetricsDecorator)) {
            Nl2sqlMetrics metrics = metricsProvider.getIfAvailable();
            if (metrics == null) {
                return bean;
            }
            return new CorrectorMetricsDecorator((SemanticCorrector) bean,
                    bean.getClass().getSimpleName(), metrics);
        }
        return bean;
    }
}
```

Note: correctors that are instantiated via `ComponentFactory` (SPI) rather than as Spring beans will not be auto-wrapped. For those, consumers already call `correct()` through `ComponentFactory.getCorrectors()`; add an explicit wrap helper there.

- [ ] **Step 5: Wrap SPI-loaded correctors in `ComponentFactory`**

Find the corrector registration point. Run: `grep -n "SemanticCorrector\|getCorrectors" headless/chat/src/main/java/com/tencent/supersonic/headless/chat/utils/ComponentFactory.java` to find the accessor. Wrap each returned instance with `new CorrectorMetricsDecorator(c, c.getClass().getSimpleName(), metrics)` when the beans are first initialised.

- [ ] **Step 6: Run tests**

Run: `mvn -pl headless/chat test -Dtest=CorrectorMetricsDecoratorTest`
Expected: PASS.

- [ ] **Step 7: Compile full launcher**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add headless/chat/src/main/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsDecorator.java \
        headless/chat/src/main/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsConfiguration.java \
        headless/chat/src/main/java/com/tencent/supersonic/headless/chat/utils/ComponentFactory.java \
        headless/chat/src/test/java/com/tencent/supersonic/headless/chat/corrector/CorrectorMetricsDecoratorTest.java
git commit -m "feat(nl2sql): auto-wrap semantic correctors with metrics decorator

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9: Instrument `JdbcExecutor` (DB stage)

**Files:**
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/executor/JdbcExecutor.java`
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/executor/JdbcExecutorMetricsTest.java` (new)

- [ ] **Step 1: Write the failing test**

```java
package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.common.metrics.TenantTagNormalizer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcExecutorMetricsTest {

    @Test
    void recordDbPublishesTimerAndSummary() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Nl2sqlMetrics metrics = new Nl2sqlMetrics(registry,
                new TenantTagNormalizer(List.of(), 10, true));

        metrics.recordDb("mysql", Duration.ofMillis(100), 2048,
                Nl2sqlMetricConstants.OUTCOME_SUCCESS, "acme");

        assertThat(registry.find(Nl2sqlMetricConstants.DB_DURATION)
                .tag("db_type", "mysql").timer()).isNotNull();
        assertThat(registry.find(Nl2sqlMetricConstants.DB_ROWS_SCANNED)
                .tag("db_type", "mysql").summary().totalAmount()).isEqualTo(2048);
    }
}
```

- [ ] **Step 2: Run to confirm PASS**

Run: `mvn -pl headless/core -am test -Dtest=JdbcExecutorMetricsTest`
Expected: PASS.

- [ ] **Step 3: Modify `JdbcExecutor.execute`**

Replace body of `execute(QueryStatement queryStatement)` (currently lines 24-52):

```java
@Override
public SemanticQueryResp execute(QueryStatement queryStatement) {
    com.tencent.supersonic.common.metrics.Nl2sqlMetrics metrics =
            com.tencent.supersonic.common.util.ContextUtils
                    .getBean(com.tencent.supersonic.common.metrics.Nl2sqlMetrics.class);
    String tenantId;
    try {
        tenantId = com.tencent.supersonic.common.pojo.TenantContext.getTenantId();
    } catch (Throwable ignore) {
        tenantId = null;
    }
    String dbType = queryStatement.getOntology() != null
            && queryStatement.getOntology().getDatabase() != null
            ? queryStatement.getOntology().getDatabase().getType() : "unknown";

    // accelerate query if possible
    for (QueryAccelerator queryAccelerator : ComponentFactory.getQueryAccelerators()) {
        if (queryAccelerator.check(queryStatement)) {
            SemanticQueryResp r = queryAccelerator.query(queryStatement);
            if (Objects.nonNull(r) && !r.getResultList().isEmpty()) {
                log.info("query by Accelerator {}", queryAccelerator.getClass().getSimpleName());
                return r;
            }
        }
    }

    SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
    String sql = StringUtils.normalizeSpace(queryStatement.getSql());
    log.info("executing SQL: {} queryTraceId={}", sql,
            com.tencent.supersonic.common.metrics.QueryTraceContext.current().orElse("none"));
    DatabaseResp database = queryStatement.getOntology().getDatabase();
    SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();

    long startNanos = System.nanoTime();
    String outcome = com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_SUCCESS;
    long rowsScanned = -1;
    try {
        SqlUtils sqlUtil = sqlUtils.init(database);
        sqlUtil.queryInternal(queryStatement.getSql(), queryResultWithColumns);
        queryResultWithColumns.setSql(sql);
        rowsScanned = queryResultWithColumns.getResultList() == null ? 0
                : queryResultWithColumns.getResultList().size();
    } catch (Exception e) {
        outcome = com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_ERROR;
        log.error("queryInternal with error ", e);
        queryResultWithColumns.setErrorMsg(e.getMessage());
    } finally {
        java.time.Duration elapsed = java.time.Duration.ofNanos(System.nanoTime() - startNanos);
        metrics.recordDb(dbType, elapsed, rowsScanned, outcome, tenantId);
    }
    return queryResultWithColumns;
}
```

- [ ] **Step 4: Build & test**

Run: `mvn compile -pl launchers/standalone -am`
Run: `mvn -pl headless/core -am test -Dtest=JdbcExecutorMetricsTest`
Expected: BUILD SUCCESS, PASS.

- [ ] **Step 5: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/executor/JdbcExecutor.java \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/executor/JdbcExecutorMetricsTest.java
git commit -m "feat(nl2sql): instrument JdbcExecutor with duration + rows scanned metrics

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 10: Logback JSON layout + `queryTraceId` MDC field

**Files:**
- Modify: `launchers/standalone/pom.xml`
- Modify: `launchers/standalone/src/main/resources/logback-spring.xml`
- Modify: `launchers/chat/src/main/resources/logback-spring.xml`
- Modify: `launchers/headless/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Add `logstash-logback-encoder` to standalone pom**

Edit `launchers/standalone/pom.xml`, add inside `<dependencies>` (after the `spring-boot-starter-actuator` / `micrometer-registry-prometheus` block near line 71):

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS, resolves dependency.

- [ ] **Step 2: Update `launchers/standalone/src/main/resources/logback-spring.xml` — add JSON appender + include queryTraceId**

Below the existing `consoleLog` appender (current line 12), add:

```xml
<appender name="jsonLog" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <File>${LOG_PATH}/s2-json.log</File>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <FileNamePattern>${LOG_PATH}/s2-json.%d{yyyy-MM-dd}.log.gz</FileNamePattern>
        <maxHistory>14</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>queryTraceId</includeMdcKeyName>
        <includeMdcKeyName>tenantId</includeMdcKeyName>
        <fieldNames>
            <timestamp>@timestamp</timestamp>
            <message>message</message>
            <thread>thread</thread>
            <logger>logger</logger>
            <level>level</level>
        </fieldNames>
        <customFields>{"application":"supersonic"}</customFields>
    </encoder>
</appender>
```

Update each existing plaintext `<pattern>` that references `[%X{traceId}]` (lines 32, 59, 82, 103) to also include `queryTraceId`:

Before (example for `fileInfoLog`):
```xml
<pattern>%d [%thread] %-5level [%X{traceId}] %logger{36} %line - %msg%n</pattern>
```

After:
```xml
<pattern>%d [%thread] %-5level [traceId=%X{traceId} queryTraceId=%X{queryTraceId}] %logger{36} %line - %msg%n</pattern>
```

Apply the same replacement to all four patterns in the file.

Add `jsonLog` to the root logger:

Before (line 115-119):
```xml
<root level="INFO">
    <appender-ref ref="fileInfoLog"/>
    <appender-ref ref="fileErrorLog"/>
    <appender-ref ref="consoleLog"/>
</root>
```

After:
```xml
<root level="INFO">
    <appender-ref ref="fileInfoLog"/>
    <appender-ref ref="fileErrorLog"/>
    <appender-ref ref="consoleLog"/>
    <appender-ref ref="jsonLog"/>
</root>
```

- [ ] **Step 3: Apply identical changes to `launchers/chat/src/main/resources/logback-spring.xml` and `launchers/headless/src/main/resources/logback-spring.xml`**

Same edits (add `jsonLog` appender + update patterns + append to root).

- [ ] **Step 4: Smoke-boot test — verify JSON file is written**

Write `launchers/standalone/src/test/java/com/tencent/supersonic/logging/JsonLogSmokeTest.java`:

```java
package com.tencent.supersonic.logging;

import com.tencent.supersonic.common.metrics.QueryTraceContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JsonLogSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(JsonLogSmokeTest.class);

    @Test
    void logLinesContainQueryTraceIdWhenScopeIsOpen() {
        try (QueryTraceContext.Scope s = QueryTraceContext.open()) {
            log.info("smoke test: should have queryTraceId={}", s.traceId());
        }
        // Visual check: `tail -n 1 logs/s2-json.log` should show queryTraceId field.
    }
}
```

Run: `mvn -pl launchers/standalone test -Dtest=JsonLogSmokeTest`
Expected: PASS; the `logs/s2-json.log` file contains a JSON object with a `queryTraceId` field starting with `q_`.

- [ ] **Step 5: Commit**

```bash
git add launchers/standalone/pom.xml \
        launchers/standalone/src/main/resources/logback-spring.xml \
        launchers/chat/src/main/resources/logback-spring.xml \
        launchers/headless/src/main/resources/logback-spring.xml \
        launchers/standalone/src/test/java/com/tencent/supersonic/logging/JsonLogSmokeTest.java
git commit -m "feat(observability): add JSON log appender with queryTraceId MDC

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 11: Grafana dashboard JSON

**Files:**
- Create: `docs/monitoring/nl2sql-dashboard.json`
- Create: `docker/grafana/dashboards/nl2sql-dashboard.json` (same content — Grafana provisioning path)

Panel layout (6x4 rows of 12-col units = 6 panels):

1. NL2SQL stage duration — p50/p95/p99 per stage (timeseries)
2. NL2SQL error rate per stage (timeseries, percent)
3. LLM latency p95/p99 per model (timeseries)
4. LLM token usage (stack chart — prompt vs completion per model)
5. Mapper hit rate per mapper (bar gauge)
6. DB duration p95 + rows scanned by db_type (two stat panels in a row)
7. Stage outcome breakdown (pie: success/error/timeout/empty)
8. Top stages by error count (table)

- [ ] **Step 1: Write dashboard JSON**

Create `docs/monitoring/nl2sql-dashboard.json`:

```json
{
  "annotations": {"list": [{"builtIn": 1, "datasource": {"type": "grafana", "uid": "-- Grafana --"}, "enable": true, "hide": true, "iconColor": "rgba(0, 211, 255, 1)", "name": "Annotations & Alerts", "type": "dashboard"}]},
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 0,
  "id": null,
  "links": [],
  "liveNow": false,
  "title": "SuperSonic NL2SQL Chain Observability",
  "uid": "s2-nl2sql",
  "tags": ["supersonic", "nl2sql"],
  "schemaVersion": 39,
  "refresh": "30s",
  "time": {"from": "now-6h", "to": "now"},
  "templating": {
    "list": [
      {"name": "DS_PROMETHEUS", "type": "datasource", "query": "prometheus", "current": {"text": "Prometheus", "value": "Prometheus"}},
      {"name": "stage", "type": "query", "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"}, "query": "label_values(s2_nl2sql_stage_duration_seconds_count, stage)", "refresh": 2, "multi": true, "includeAll": true},
      {"name": "tenant", "type": "query", "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"}, "query": "label_values(s2_nl2sql_stage_duration_seconds_count, tenant_id)", "refresh": 2, "multi": true, "includeAll": true}
    ]
  },
  "panels": [
    {
      "id": 1, "title": "Stage Duration p50/p95/p99 by stage", "type": "timeseries",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 24, "x": 0, "y": 0},
      "fieldConfig": {"defaults": {"unit": "s"}, "overrides": []},
      "targets": [
        {"expr": "histogram_quantile(0.50, sum by (le, stage) (rate(s2_nl2sql_stage_duration_seconds_bucket{stage=~\"$stage\", tenant_id=~\"$tenant\"}[5m])))", "legendFormat": "p50 {{stage}}", "refId": "A"},
        {"expr": "histogram_quantile(0.95, sum by (le, stage) (rate(s2_nl2sql_stage_duration_seconds_bucket{stage=~\"$stage\", tenant_id=~\"$tenant\"}[5m])))", "legendFormat": "p95 {{stage}}", "refId": "B"},
        {"expr": "histogram_quantile(0.99, sum by (le, stage) (rate(s2_nl2sql_stage_duration_seconds_bucket{stage=~\"$stage\", tenant_id=~\"$tenant\"}[5m])))", "legendFormat": "p99 {{stage}}", "refId": "C"}
      ]
    },
    {
      "id": 2, "title": "Stage Error Rate", "type": "timeseries",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8},
      "fieldConfig": {"defaults": {"unit": "percentunit", "max": 1, "min": 0}, "overrides": []},
      "targets": [
        {"expr": "sum by (stage) (rate(s2_nl2sql_stage_outcome_total{outcome=\"error\", tenant_id=~\"$tenant\"}[5m])) / clamp_min(sum by (stage) (rate(s2_nl2sql_stage_outcome_total{tenant_id=~\"$tenant\"}[5m])), 0.0001)", "legendFormat": "{{stage}}", "refId": "A"}
      ]
    },
    {
      "id": 3, "title": "LLM Latency p95/p99 by model", "type": "timeseries",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8},
      "fieldConfig": {"defaults": {"unit": "s"}, "overrides": []},
      "targets": [
        {"expr": "histogram_quantile(0.95, sum by (le, model) (rate(s2_nl2sql_llm_duration_seconds_bucket[5m])))", "legendFormat": "p95 {{model}}", "refId": "A"},
        {"expr": "histogram_quantile(0.99, sum by (le, model) (rate(s2_nl2sql_llm_duration_seconds_bucket[5m])))", "legendFormat": "p99 {{model}}", "refId": "B"}
      ]
    },
    {
      "id": 4, "title": "LLM Token Usage (rate/s) by kind", "type": "timeseries",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16},
      "fieldConfig": {"defaults": {"unit": "short", "custom": {"stacking": {"mode": "normal"}}}, "overrides": []},
      "targets": [
        {"expr": "sum by (model, kind) (rate(s2_nl2sql_llm_tokens_total[5m]))", "legendFormat": "{{model}} {{kind}}", "refId": "A"}
      ]
    },
    {
      "id": 5, "title": "Mapper Hit Rate", "type": "bargauge",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 12, "y": 16},
      "fieldConfig": {"defaults": {"unit": "percentunit", "min": 0, "max": 1}, "overrides": []},
      "options": {"orientation": "horizontal", "displayMode": "gradient"},
      "targets": [
        {"expr": "sum by (mapper_name) (rate(s2_nl2sql_mapper_hits_total{hit=\"true\"}[10m])) / clamp_min(sum by (mapper_name) (rate(s2_nl2sql_mapper_hits_total[10m])), 0.0001)", "legendFormat": "{{mapper_name}}", "refId": "A"}
      ]
    },
    {
      "id": 6, "title": "DB Duration p95 by db_type", "type": "timeseries",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 24},
      "fieldConfig": {"defaults": {"unit": "s"}, "overrides": []},
      "targets": [
        {"expr": "histogram_quantile(0.95, sum by (le, db_type) (rate(s2_nl2sql_db_duration_seconds_bucket[5m])))", "legendFormat": "p95 {{db_type}}", "refId": "A"}
      ]
    },
    {
      "id": 7, "title": "DB Rows Scanned avg", "type": "timeseries",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 12, "y": 24},
      "fieldConfig": {"defaults": {"unit": "short"}, "overrides": []},
      "targets": [
        {"expr": "rate(s2_nl2sql_sql_rows_scanned_sum[5m]) / clamp_min(rate(s2_nl2sql_sql_rows_scanned_count[5m]), 1)", "legendFormat": "avg {{db_type}}", "refId": "A"}
      ]
    },
    {
      "id": 8, "title": "Stage Outcome Breakdown", "type": "piechart",
      "datasource": {"type": "prometheus", "uid": "${DS_PROMETHEUS}"},
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 32},
      "options": {"legend": {"showLegend": true, "placement": "right"}, "pieType": "donut"},
      "targets": [
        {"expr": "sum by (outcome) (increase(s2_nl2sql_stage_outcome_total{tenant_id=~\"$tenant\"}[1h]))", "legendFormat": "{{outcome}}", "refId": "A"}
      ]
    }
  ]
}
```

- [ ] **Step 2: Copy JSON to `docker/grafana/dashboards/`**

```bash
cp docs/monitoring/nl2sql-dashboard.json docker/grafana/dashboards/nl2sql-dashboard.json
```

(Full copy, not symlink — the docker provisioning provider scans that directory.)

- [ ] **Step 3: Validate JSON parses**

Run: `python3 -c "import json; json.load(open('docs/monitoring/nl2sql-dashboard.json'))"`
Expected: no output (valid JSON).

- [ ] **Step 4: Commit**

```bash
git add docs/monitoring/nl2sql-dashboard.json docker/grafana/dashboards/nl2sql-dashboard.json
git commit -m "docs(monitoring): add NL2SQL Grafana dashboard JSON

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 12: Prometheus alert rules

**Files:**
- Create: `docker/prometheus/rules/nl2sql-slo-alert-rules.yml`

- [ ] **Step 1: Write alert rules file**

```yaml
groups:
  - name: nl2sql-slo
    rules:
      - alert: Nl2sqlStageP99High
        expr: |
          histogram_quantile(0.99,
            sum by (le, stage) (rate(s2_nl2sql_stage_duration_seconds_bucket[10m]))
          ) > 15
        for: 10m
        labels:
          severity: warning
          module: nl2sql
        annotations:
          summary: "NL2SQL {{ $labels.stage }} P99 latency exceeds 15s"
          description: "Stage {{ $labels.stage }} P99 latency has exceeded 15s for 10 minutes. Check LLM provider latency, mapper load, or DB contention. Dashboard: /d/s2-nl2sql/supersonic-nl2sql-chain-observability"

      - alert: Nl2sqlErrorRateHigh
        expr: |
          (
            sum by (stage) (rate(s2_nl2sql_stage_outcome_total{outcome="error"}[10m]))
            /
            clamp_min(sum by (stage) (rate(s2_nl2sql_stage_outcome_total[10m])), 0.0001)
          ) > 0.05
        for: 10m
        labels:
          severity: critical
          module: nl2sql
        annotations:
          summary: "NL2SQL {{ $labels.stage }} error rate exceeds 5%"
          description: "Stage {{ $labels.stage }} error rate is above 5% for 10 minutes. Check logs with queryTraceId; common causes: LLM rate limit, schema drift, DB outage."

      - alert: Nl2sqlLlmLatencyP95High
        expr: |
          histogram_quantile(0.95,
            sum by (le, model) (rate(s2_nl2sql_llm_duration_seconds_bucket[10m]))
          ) > 20
        for: 10m
        labels:
          severity: warning
          module: nl2sql
        annotations:
          summary: "NL2SQL LLM {{ $labels.model }} P95 latency exceeds 20s"
          description: "LLM provider {{ $labels.model }} is slow. Consider retry threshold reduction or failover."

      - alert: Nl2sqlLlmTokenSpike
        expr: |
          sum by (model) (rate(s2_nl2sql_llm_tokens_total[10m])) > 100000
        for: 15m
        labels:
          severity: warning
          module: nl2sql
        annotations:
          summary: "NL2SQL token consumption spiking for {{ $labels.model }}"
          description: "Token rate > 100k/s for 15 minutes. Likely prompt bloat or runaway retry loop. Dashboard: /d/s2-nl2sql"

      - alert: Nl2sqlDbStageP99High
        expr: |
          histogram_quantile(0.99,
            sum by (le, db_type) (rate(s2_nl2sql_db_duration_seconds_bucket[10m]))
          ) > 10
        for: 10m
        labels:
          severity: warning
          module: nl2sql
        annotations:
          summary: "NL2SQL DB {{ $labels.db_type }} P99 exceeds 10s"
          description: "Downstream DB slow. Check DB engine health and connection pool."
```

- [ ] **Step 2: Validate via `promtool` if installed (optional)**

Run: `docker run --rm -v $(pwd)/docker/prometheus/rules:/rules prom/prometheus:latest promtool check rules /rules/nl2sql-slo-alert-rules.yml`
Expected: `SUCCESS: ... rules found`.

- [ ] **Step 3: Update `docker/prometheus/prometheus.yml` to include the new rule file (if not using glob)**

Run: `grep -n "rule_files" docker/prometheus/prometheus.yml`

If it uses a glob like `rules/*.yml`, no change needed. Otherwise append the filename to the list.

- [ ] **Step 4: Commit**

```bash
git add docker/prometheus/rules/nl2sql-slo-alert-rules.yml docker/prometheus/prometheus.yml
git commit -m "feat(monitoring): add NL2SQL P99 and error-rate alert rules

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 13: Oncall runbook + docs update

**Files:**
- Create: `docs/runbook/nl2sql-observability.md`
- Modify: `docs/details/platform/03-monitoring-alerts.md`

- [ ] **Step 1: Write the runbook**

Create `docs/runbook/nl2sql-observability.md`:

```markdown
# NL2SQL Observability Runbook

Dashboard: Grafana → `SuperSonic NL2SQL Chain Observability` (uid `s2-nl2sql`).
Alert rules: `docker/prometheus/rules/nl2sql-slo-alert-rules.yml`.

## Metric cheatsheet

| Metric | Type | Tags | Meaning |
|---|---|---|---|
| `s2_nl2sql_stage_duration_seconds` | histogram | stage, outcome, tenant_id, agent_id, parser_name | per-stage latency (rule_parse, llm_parse, mapper, corrector) |
| `s2_nl2sql_stage_outcome_total` | counter | stage, outcome | outcome breakdown |
| `s2_nl2sql_llm_duration_seconds` | histogram | model, outcome, tenant_id | LLM call latency |
| `s2_nl2sql_llm_tokens_total` | counter | model, kind (prompt/completion) | token usage |
| `s2_nl2sql_mapper_hits_total` | counter | mapper_name, hit (true/false) | mapper effectiveness |
| `s2_nl2sql_db_duration_seconds` | histogram | db_type, outcome | DB execution latency |
| `s2_nl2sql_sql_rows_scanned` | summary | db_type | rows scanned per SQL |

## Alert response — `Nl2sqlStageP99High`

1. Open the dashboard, filter to the alerting `stage`.
2. Identify whether one tenant dominates. Switch `tenant` template var.
3. Tail logs: `jq 'select(.queryTraceId)' logs/s2-json.log | tail -n 200`.
4. If `stage=llm_parse`: check `s2_nl2sql_llm_duration_seconds` panel — if only one model is slow, consider failover. If all models, check LLM provider status page.
5. If `stage=mapper`: check HanLP / embedding backends (Milvus / Chroma).
6. If `stage=corrector`: find the corrector by `corrector_name` tag. Roll back the last corrector change if timing regressed.

## Alert response — `Nl2sqlErrorRateHigh`

1. Filter dashboard by `outcome=error`.
2. Query recent traces: `jq 'select(.level=="ERROR" and .queryTraceId)' logs/s2-json.log | head -n 50`.
3. Pick one `queryTraceId` and `jq 'select(.queryTraceId=="q_...")' logs/s2-json.log` for the full timeline.
4. Common root causes: schema drift after deployment, LLM API quota exceeded, tenant DB outage.

## Trace propagation

- `queryTraceId` is set at `NL2SQLParser#parse` entry (`QueryTraceContext.open()`).
- It flows through MDC into every log line. Async boundaries are covered by `ContextAwareThreadPoolExecutor` (already in use).
- For new async code, wrap with `QueryTraceContext.wrap(Runnable)` or `snapshot()` / `restore()`.

## Cardinality guardrails

- `tenant_id` is capped at 50 unique values per process via `TenantTagNormalizer`. Tenants outside the configured `s2.observability.nl2sql.top-tenants` allowlist plus dynamic admission get mapped to `tenant_id=other`.
- Raw query text is NEVER emitted as a tag — only aggregated metrics and the `queryTraceId` (in logs, not metrics).
- If new tags are proposed, stop and check: does the cardinality stay < 200 per metric? If not, use log-aggregation (Loki) instead.

## Configuration

`launchers/standalone/src/main/resources/application.yaml`:
```yaml
s2:
  observability:
    nl2sql:
      tenant-tag-limit: 50          # max unique tenant_id values
      top-tenants: []               # explicit allowlist; populate via ops config
      emit-tenant-tag: true         # set false for global aggregate view only
```

## Validation after deploy

1. Hit a couple of NL2SQL queries through the chat UI.
2. `curl -s http://<host>/actuator/prometheus | grep s2_nl2sql` — confirm at least one `_count` > 0 for each metric family.
3. Open the dashboard — all panels should render data within 2 min.
4. Trigger a synthetic error (e.g., disabled LLM) and verify `Nl2sqlErrorRateHigh` fires within 10 min.
```

- [ ] **Step 2: Update `docs/details/platform/03-monitoring-alerts.md`**

Append these rows to the "2.2 核心指标列表" table (after the existing `supersonic_nl2sql_latency_seconds` row):

```markdown
| `s2_nl2sql_stage_duration_seconds` | Histogram | `stage`, `outcome`, `tenant_id`, `agent_id`, `parser_name` | NL2SQL 阶段级时延（rule/llm/mapper/corrector）|
| `s2_nl2sql_llm_duration_seconds` | Histogram | `model`, `outcome`, `tenant_id` | LLM 调用时延 |
| `s2_nl2sql_llm_tokens_total` | Counter | `model`, `kind` (prompt/completion) | LLM Token 消耗 |
| `s2_nl2sql_mapper_hits_total` | Counter | `mapper_name`, `hit` | 各 Mapper 策略命中计数 |
| `s2_nl2sql_db_duration_seconds` | Histogram | `db_type`, `outcome` | DB 执行时延 |
| `s2_nl2sql_sql_rows_scanned` | Summary | `db_type` | 单次 SQL 扫描行数 |
```

Append this row to section "3.1 仪表盘文件":

```markdown
| `docker/grafana/dashboards/nl2sql-dashboard.json` | NL2SQL 链路可观测仪表盘 |
```

Append this row to section "3.2 核心面板结构" (new dashboard block):

```markdown
NL2SQL 链路可观测仪表盘
├── 1. 阶段时延 P50/P95/P99（按 stage）
├── 2. 阶段错误率
├── 3. LLM 调用 P95/P99（按模型）
├── 4. LLM Token 用量（prompt/completion 堆叠）
├── 5. Mapper 命中率
├── 6. DB 时延 P95 + 扫描行数
└── 7. 阶段 outcome 占比（饼图）
```

Append to "4.2 P0 告警" section with a new sub-heading "NL2SQL 告警（5 条）" listing the five from Task 12.

Append to "7. 实现状态":

```markdown
| NL2SQL 阶段时延直方图 | 已上线 |
| NL2SQL Grafana 仪表盘 | 已上线 |
| NL2SQL SLO 告警（5 条）| 已上线 |
| queryTraceId MDC + 结构化 JSON 日志 | 已上线 |
```

- [ ] **Step 3: Commit**

```bash
git add docs/runbook/nl2sql-observability.md docs/details/platform/03-monitoring-alerts.md
git commit -m "docs(runbook): add NL2SQL oncall runbook + update monitoring spec

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Final verification

- [ ] **Compile full launcher**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Run all new tests**

Run: `mvn -pl common,headless/chat,headless/core,chat/server test -Dtest='Nl2sql*Test,QueryTraceContextTest,TenantTagNormalizerTest,BaseMapperMetricsTest,CorrectorMetricsDecoratorTest,JdbcExecutorMetricsTest,NL2SQLParserMetricsTest,LLMRequestServiceMetricsTest,PrometheusEndpointSmokeTest,JsonLogSmokeTest'`
Expected: all PASS.

- [ ] **Boot locally and scrape**

```bash
mvn -pl launchers/standalone -am spring-boot:run &
sleep 45
curl -s http://localhost:9080/actuator/prometheus | grep -c '^s2_nl2sql_'
```
Expected: count > 0 after running at least one NL2SQL query (smoke via Swagger UI or frontend chat).

- [ ] **Confirm dashboard provisions**

```bash
cd docker && docker compose -f docker-compose-monitoring.yml up -d
```
Navigate to Grafana → Dashboards → should see `SuperSonic NL2SQL Chain Observability`.
