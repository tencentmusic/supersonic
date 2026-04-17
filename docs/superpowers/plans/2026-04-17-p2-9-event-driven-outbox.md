# P2-9: Transactional Outbox Pattern for Cross-Module Events Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace in-process, fire-and-forget `ApplicationEventPublisher.publishEvent(...)` calls that cross module boundaries with a durable transactional outbox (`s2_outbox` table + polling relay), providing at-least-once delivery without introducing a message broker dependency.

**Architecture:** Callers invoke `OutboxPublisher.publish(event)` which serializes the event to JSON and inserts a row into `s2_outbox` inside the caller's `@Transactional` scope. A cluster-safe `OutboxRelay` (`@Scheduled(fixedDelay=2s)`) polls unprocessed rows via `SELECT ... FOR UPDATE SKIP LOCKED`, republishes them as `ApplicationEvent`, and marks `processed_at`. Deserialization failures get moved to `s2_outbox_dead`. TTL job deletes rows older than 7d. Future work: swap relay target from Spring to Kafka — callers are unaffected.

**Tech Stack:** Java 21 · Spring Boot 3.4.x · MyBatis-Plus · Jackson · Micrometer · Flyway · H2 (tests) · MySQL 8+ / PostgreSQL 12+ (prod).

---

## File Structure

### New files
```
common/src/main/java/com/tencent/supersonic/common/outbox/
├── OutboxEvent.java                   # @TableName("s2_outbox") DO
├── OutboxDeadEvent.java               # @TableName("s2_outbox_dead") DO
├── OutboxMapper.java                  # extends BaseMapper<OutboxEvent>
├── OutboxDeadMapper.java              # extends BaseMapper<OutboxDeadEvent>
├── OutboxEventService.java            # ServiceImpl wrapper + raw SELECT FOR UPDATE SKIP LOCKED
├── OutboxPublisher.java               # public API: publish(ApplicationEvent)
├── OutboxRelay.java                   # @Scheduled poller + republisher
├── OutboxRetentionTask.java           # @Scheduled TTL cleanup
├── OutboxMeterBinder.java             # Micrometer gauges
├── OutboxProperties.java              # @ConfigurationProperties("s2.outbox")
└── OutboxAutoConfiguration.java       # @EnableConfigurationProperties + conditional beans

common/src/test/java/com/tencent/supersonic/common/outbox/
├── OutboxPublisherTest.java           # unit: serialization + insert
├── OutboxRelayTest.java               # integration: dual-relay no-double-dispatch
├── OutboxRetentionTaskTest.java       # TTL
├── OutboxDeadLetterTest.java          # deser failure → dead table
└── TenantSqlInterceptorOutboxExclusionTest.java  # regression

launchers/standalone/src/main/resources/db/migration/mysql/
└── V29__outbox.sql

launchers/standalone/src/main/resources/db/migration/postgresql/
└── V29__outbox.sql
```

### Modified files
- `common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java` — add `s2_outbox`, `s2_outbox_dead` to `DEFAULT_EXCLUDED_TABLES`.
- `common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java` — add same two names to default `excludedTables` list.
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java` — swap `applicationEventPublisher.publishEvent(new TemplateDeployedEvent(...))` for `outboxPublisher.publish(new TemplateDeployedEvent(...))` at lines 267 and 343.
- `docs/details/platform/` — add `outbox.md` detail spec.

### Configuration (application.yml)
```yaml
s2:
  outbox:
    enabled: true            # feature flag; false disables publisher + relay (events go sync)
    poll-interval-ms: 2000
    batch-size: 100
    retention-days: 7
    max-attempts: 5
```

### Current `@EventListener` catalog (so the engineer knows who consumes what)
| Event | Listener | Notes |
|---|---|---|
| `TemplateDeployedEvent` | `chat.server.listener.TemplateDeployedEventListener` | Target for Task 6 migration. Creates agents/plugins/exemplars. |
| `DataEvent` (ADD/UPDATE/DELETE) | `headless.server.listener.MetaEmbeddingListener` | `@Async("eventExecutor")`. |
| `DataEvent` | `headless.server.listener.SchemaDictUpdateListener` | `@Async("eventExecutor")`. |
| `PluginAddEvent` / `PluginUpdateEvent` / `PluginDelEvent` | `chat.server.plugin.PluginManager` | Refreshes plugin cache. |

None currently use `@TransactionalEventListener`, so we don't have to worry about migration conflicts with phase bindings.

---

## Task 1: Flyway V29 migrations for `s2_outbox` (MySQL + PostgreSQL)

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V29__outbox.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V29__outbox.sql`

- [ ] **Step 1: Write MySQL migration**

Create `launchers/standalone/src/main/resources/db/migration/mysql/V29__outbox.sql`:

```sql
-- V29__outbox.sql
-- Transactional outbox for cross-module ApplicationEvents.
-- NOTE: MySQL does NOT support ADD COLUMN IF NOT EXISTS. These are fresh CREATE TABLE only.

CREATE TABLE IF NOT EXISTS s2_outbox (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type   VARCHAR(100) NOT NULL COMMENT 'e.g. SemanticTemplate, Plugin, Model',
    aggregate_id     VARCHAR(100)          COMMENT 'business id (nullable for events without one)',
    event_type       VARCHAR(200) NOT NULL COMMENT 'fully qualified class name',
    payload_json     MEDIUMTEXT   NOT NULL COMMENT 'Jackson-serialized event (source field omitted)',
    tenant_id        BIGINT       DEFAULT 1,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at     DATETIME(3)  NULL,
    processing_node  VARCHAR(100) NULL     COMMENT 'host:pid of relay that is currently holding lock',
    attempts         INT          NOT NULL DEFAULT 0,
    last_error       TEXT         NULL,
    INDEX idx_outbox_unprocessed (processed_at, created_at),
    INDEX idx_outbox_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS s2_outbox_dead (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_id      BIGINT       NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100),
    event_type       VARCHAR(200) NOT NULL,
    payload_json     MEDIUMTEXT   NOT NULL,
    tenant_id        BIGINT       DEFAULT 1,
    failure_reason   TEXT         NOT NULL,
    attempts         INT          NOT NULL,
    created_at       DATETIME(3)  NOT NULL,
    died_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_outbox_dead_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Write PostgreSQL migration**

Create `launchers/standalone/src/main/resources/db/migration/postgresql/V29__outbox.sql`:

```sql
-- V29__outbox.sql
-- Transactional outbox for cross-module ApplicationEvents.

CREATE TABLE IF NOT EXISTS s2_outbox (
    id               BIGSERIAL PRIMARY KEY,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100),
    event_type       VARCHAR(200) NOT NULL,
    payload_json     TEXT         NOT NULL,
    tenant_id        BIGINT       DEFAULT 1,
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at     TIMESTAMP(3),
    processing_node  VARCHAR(100),
    attempts         INT          NOT NULL DEFAULT 0,
    last_error       TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unprocessed ON s2_outbox (processed_at, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_tenant      ON s2_outbox (tenant_id);

CREATE TABLE IF NOT EXISTS s2_outbox_dead (
    id               BIGSERIAL PRIMARY KEY,
    original_id      BIGINT       NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100),
    event_type       VARCHAR(200) NOT NULL,
    payload_json     TEXT         NOT NULL,
    tenant_id        BIGINT       DEFAULT 1,
    failure_reason   TEXT         NOT NULL,
    attempts         INT          NOT NULL,
    created_at       TIMESTAMP(3) NOT NULL,
    died_at          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_dead_tenant ON s2_outbox_dead (tenant_id);
```

- [ ] **Step 3: Verify migrations compile by running Flyway dry-run**

Run:
```bash
mvn -pl launchers/standalone -am compile
```
Expected: `BUILD SUCCESS` (Flyway files are resources, no syntax check at compile time, but compile must pass with no typos in other code).

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V29__outbox.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V29__outbox.sql
git commit -m "feat(outbox): add V29 migration for s2_outbox + s2_outbox_dead"
```

---

## Task 2: `OutboxEvent` entity + mapper + service

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxEvent.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxDeadEvent.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxMapper.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxDeadMapper.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxEventService.java`

- [ ] **Step 1: Write `OutboxEvent` DO**

```java
package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("s2_outbox")
public class OutboxEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String payloadJson;

    private Long tenantId;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private String processingNode;

    private Integer attempts;

    private String lastError;
}
```

- [ ] **Step 2: Write `OutboxDeadEvent` DO**

```java
package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("s2_outbox_dead")
public class OutboxDeadEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long originalId;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String payloadJson;

    private Long tenantId;

    private String failureReason;

    private Integer attempts;

    private LocalDateTime createdAt;

    private LocalDateTime diedAt;
}
```

- [ ] **Step 3: Write mappers with raw SELECT FOR UPDATE SKIP LOCKED**

`OutboxMapper.java`:
```java
package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxEvent> {

    /**
     * Lock-and-claim a batch of unprocessed rows. Works on MySQL 8+ and PostgreSQL 9.5+.
     * SKIP LOCKED lets parallel relays each grab a disjoint slice.
     * MUST be called from within a transaction (the lock is released on commit/rollback).
     */
    @Select("SELECT * FROM s2_outbox "
          + "WHERE processed_at IS NULL "
          + "ORDER BY created_at ASC "
          + "LIMIT #{limit} "
          + "FOR UPDATE SKIP LOCKED")
    List<OutboxEvent> lockUnprocessed(@Param("limit") int limit);

    @Update("UPDATE s2_outbox SET processed_at = #{processedAt}, processing_node = #{node}, "
          + "attempts = attempts + 1 WHERE id = #{id}")
    int markProcessed(@Param("id") Long id,
                      @Param("processedAt") LocalDateTime processedAt,
                      @Param("node") String node);

    @Update("UPDATE s2_outbox SET attempts = attempts + 1, last_error = #{error}, "
          + "processing_node = NULL WHERE id = #{id}")
    int recordFailure(@Param("id") Long id, @Param("error") String error);

    @Update("DELETE FROM s2_outbox WHERE processed_at IS NOT NULL AND processed_at < #{cutoff}")
    int deleteProcessedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Select("SELECT COUNT(*) FROM s2_outbox WHERE processed_at IS NULL")
    long countUnprocessed();

    @Select("SELECT MIN(created_at) FROM s2_outbox WHERE processed_at IS NULL")
    LocalDateTime oldestUnprocessedCreatedAt();
}
```

`OutboxDeadMapper.java`:
```java
package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxDeadMapper extends BaseMapper<OutboxDeadEvent> {
}
```

- [ ] **Step 4: Write `OutboxEventService` (MyBatis-Plus ServiceImpl)**

```java
package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventService extends ServiceImpl<OutboxMapper, OutboxEvent> {
}
```

- [ ] **Step 5: Compile**

Run:
```bash
mvn -pl common -am compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/outbox/
git commit -m "feat(outbox): add OutboxEvent entity, mappers, and service"
```

---

## Task 3: `OutboxPublisher` — serialize and insert inside caller transaction

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxProperties.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxPublisher.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/outbox/OutboxPublisherTest.java`

- [ ] **Step 1: Write `OutboxProperties`**

```java
package com.tencent.supersonic.common.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s2.outbox")
public class OutboxProperties {

    /** Master switch. When false, OutboxPublisher falls back to synchronous publish. */
    private boolean enabled = true;

    /** Relay polling interval (ms). */
    private long pollIntervalMs = 2000L;

    /** Max rows to claim per poll. */
    private int batchSize = 100;

    /** Retention for processed rows (days). TTL job deletes older. */
    private int retentionDays = 7;

    /** After this many attempts, the row is moved to s2_outbox_dead. */
    private int maxAttempts = 5;
}
```

- [ ] **Step 2: Write failing test for `OutboxPublisher`**

Create `common/src/test/java/com/tencent/supersonic/common/outbox/OutboxPublisherTest.java`:

```java
package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {

    static class ToyEvent extends ApplicationEvent {
        public String payload;
        public ToyEvent(Object source, String payload) {
            super(source);
            this.payload = payload;
        }
        public ToyEvent() { super("test"); } // needed for Jackson deserialization
    }

    private OutboxEventService service;
    private ApplicationEventPublisher springPublisher;
    private OutboxPublisher publisher;
    private OutboxProperties props;
    private final List<OutboxEvent> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = mock(OutboxEventService.class);
        springPublisher = mock(ApplicationEventPublisher.class);
        props = new OutboxProperties();
        props.setEnabled(true);
        ObjectMapper mapper = new ObjectMapper();
        publisher = new OutboxPublisher(service, springPublisher, props, mapper);

        when(service.save(any(OutboxEvent.class))).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return true;
        });
        TenantContext.setTenantId(42L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void publish_serializesEventAndInsertsRow() {
        publisher.publish(new ToyEvent(this, "hello"));

        assertThat(saved).hasSize(1);
        OutboxEvent row = saved.get(0);
        assertThat(row.getEventType()).isEqualTo(ToyEvent.class.getName());
        assertThat(row.getTenantId()).isEqualTo(42L);
        assertThat(row.getPayloadJson()).contains("hello");
        assertThat(row.getProcessedAt()).isNull();
        verify(springPublisher, never()).publishEvent(any());
    }

    @Test
    void publish_whenDisabled_fallsBackToSyncPublish() {
        props.setEnabled(false);
        ToyEvent event = new ToyEvent(this, "bye");

        publisher.publish(event);

        verify(springPublisher).publishEvent(event);
        assertThat(saved).isEmpty();
    }

    @Test
    void publish_nullTenant_defaultsToOne() {
        TenantContext.clear();
        publisher.publish(new ToyEvent(this, "no-tenant"));
        assertThat(saved.get(0).getTenantId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run:
```bash
mvn -pl common test -Dtest=OutboxPublisherTest
```
Expected: FAIL — `OutboxPublisher` class does not exist.

- [ ] **Step 4: Implement `OutboxPublisher`**

Create `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxPublisher.java`:

```java
package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Durable cross-module event publisher.
 *
 * <p>Writes the serialized event into {@code s2_outbox} inside the caller's transaction. The
 * event is NOT dispatched to Spring listeners immediately — {@link OutboxRelay} polls the table
 * and re-publishes asynchronously. If {@code s2.outbox.enabled=false}, falls back to synchronous
 * Spring publish (rollback path for operational safety).
 */
@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxEventService service;
    private final ApplicationEventPublisher springPublisher;
    private final OutboxProperties props;
    private final ObjectMapper mapper;

    public OutboxPublisher(OutboxEventService service,
                           ApplicationEventPublisher springPublisher,
                           OutboxProperties props,
                           ObjectMapper mapper) {
        this.service = service;
        this.springPublisher = springPublisher;
        this.props = props;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(ApplicationEvent event) {
        if (!props.isEnabled()) {
            springPublisher.publishEvent(event);
            return;
        }

        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (Exception e) {
            // Fail loudly inside the caller's transaction — bad payload must NOT commit silently.
            throw new IllegalStateException(
                    "Failed to serialize outbox event " + event.getClass().getName(), e);
        }

        OutboxEvent row = new OutboxEvent();
        row.setAggregateType(deriveAggregateType(event));
        row.setAggregateId(deriveAggregateId(event));
        row.setEventType(event.getClass().getName());
        row.setPayloadJson(json);
        Long tenantId = TenantContext.getTenantId();
        row.setTenantId(tenantId == null ? 1L : tenantId);
        row.setCreatedAt(LocalDateTime.now());
        row.setAttempts(0);

        service.save(row);
        log.debug("Outbox row id={} type={} tenant={}",
                row.getId(), row.getEventType(), row.getTenantId());
    }

    private String deriveAggregateType(ApplicationEvent event) {
        // Short name by default; individual events can override by exposing getAggregateType()
        // via reflection if needed later. Keep simple for MVP.
        return event.getClass().getSimpleName();
    }

    private String deriveAggregateId(ApplicationEvent event) {
        // Best-effort: if event exposes getId() returning a value, use toString; otherwise null.
        try {
            var m = event.getClass().getMethod("getId");
            Object v = m.invoke(event);
            return v == null ? null : v.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
mvn -pl common test -Dtest=OutboxPublisherTest
```
Expected: PASS, 3 tests.

- [ ] **Step 6: Full module compile**

Run:
```bash
mvn compile -pl launchers/standalone -am
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/outbox/OutboxProperties.java \
        common/src/main/java/com/tencent/supersonic/common/outbox/OutboxPublisher.java \
        common/src/test/java/com/tencent/supersonic/common/outbox/OutboxPublisherTest.java
git commit -m "feat(outbox): add OutboxPublisher with Jackson serialization and enabled flag"
```

---

## Task 4: `OutboxRelay` — polling, SKIP LOCKED, dispatch

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRelay.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/outbox/OutboxRelayTest.java`

- [ ] **Step 1: Write failing integration test using H2**

Create `common/src/test/java/com/tencent/supersonic/common/outbox/OutboxRelayTest.java`:

```java
package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

@SpringBootTest(classes = OutboxTestConfig.class)
@TestPropertySource(properties = {
        "s2.outbox.enabled=true",
        "s2.outbox.batch-size=50",
        "spring.datasource.url=jdbc:h2:mem:outbox-relay;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false"
})
class OutboxRelayTest {

    @Autowired private OutboxPublisher publisher;
    @Autowired private OutboxRelay relay;
    @Autowired private OutboxEventService service;
    @Autowired private TestListener listener;

    @Test
    void relay_dispatchesRowsAndMarksProcessed() {
        TenantContext.setTenantId(1L);
        for (int i = 0; i < 5; i++) {
            publisher.publish(new OutboxPublisherTest.ToyEvent(this, "msg-" + i));
        }
        TenantContext.clear();

        relay.pollOnce();

        await().atMost(ofSeconds(2)).untilAsserted(() ->
                assertThat(listener.received).hasSize(5));
        assertThat(service.getBaseMapper().countUnprocessed()).isZero();
    }

    @Test
    void twoParallelRelays_doNotDoubleProcess() throws Exception {
        TenantContext.setTenantId(1L);
        for (int i = 0; i < 20; i++) {
            publisher.publish(new OutboxPublisherTest.ToyEvent(this, "p-" + i));
        }
        TenantContext.clear();
        listener.received.clear();

        var executor = Executors.newFixedThreadPool(2);
        CompletableFuture<Void> a = CompletableFuture.runAsync(relay::pollOnce, executor);
        CompletableFuture<Void> b = CompletableFuture.runAsync(relay::pollOnce, executor);
        CompletableFuture.allOf(a, b).get();
        executor.shutdown();

        await().atMost(ofSeconds(3)).untilAsserted(() ->
                assertThat(listener.received).hasSize(20));
        // No duplicates
        assertThat(listener.received.stream().distinct().count()).isEqualTo(20);
    }

    @Component
    static class TestListener {
        final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

        @EventListener
        public void on(OutboxPublisherTest.ToyEvent e) {
            received.add(e.payload);
        }
    }
}
```

Also create the Spring test config `OutboxTestConfig.java` in the same package:

```java
package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.tencent.supersonic.common.outbox")
@MapperScan(basePackages = "com.tencent.supersonic.common.outbox")
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
class OutboxTestConfig {

    @Bean
    ObjectMapper objectMapper() { return new ObjectMapper(); }

    public static void main(String[] args) {
        SpringApplication.run(OutboxTestConfig.class, args);
    }
}
```

Add an H2 `schema.sql` under `common/src/test/resources/schema-h2.sql` loaded via `spring.sql.init.schema-locations=classpath:schema-h2.sql`:

```sql
CREATE TABLE IF NOT EXISTS s2_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    event_type VARCHAR(200) NOT NULL,
    payload_json CLOB NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP(3),
    processing_node VARCHAR(100),
    attempts INT NOT NULL DEFAULT 0,
    last_error CLOB
);
CREATE TABLE IF NOT EXISTS s2_outbox_dead (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),
    event_type VARCHAR(200) NOT NULL,
    payload_json CLOB NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    failure_reason CLOB NOT NULL,
    attempts INT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    died_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Add to test `application.yml` or via @TestPropertySource: `spring.sql.init.schema-locations=classpath:schema-h2.sql`, `spring.sql.init.mode=always`.

Note: H2 in MySQL mode supports `FOR UPDATE` but does NOT support `SKIP LOCKED`. Workaround: the relay `@Mapper` uses `FOR UPDATE SKIP LOCKED`, which H2 2.x accepts as of 2.1+ in regular mode. If the test fails here, use the bash command `mvn -pl common dependency:tree | grep h2` to confirm version >=2.2.220; upgrade if needed.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
mvn -pl common test -Dtest=OutboxRelayTest
```
Expected: FAIL — `OutboxRelay` class does not exist.

- [ ] **Step 3: Implement `OutboxRelay`**

Create `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRelay.java`:

```java
package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls {@code s2_outbox} for unprocessed rows, re-publishes as Spring ApplicationEvents, marks
 * {@code processed_at}. Cluster-safe via {@code SELECT ... FOR UPDATE SKIP LOCKED}.
 *
 * <p>Poisoned rows (deserialization failures) are moved to {@code s2_outbox_dead} — see
 * {@link OutboxRelay#handlePoisonedRow}.
 */
@Component
@ConditionalOnProperty(prefix = "s2.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class OutboxRelay {

    private final OutboxEventService service;
    private final OutboxDeadMapper deadMapper;
    private final ApplicationEventPublisher springPublisher;
    private final OutboxProperties props;
    private final ObjectMapper mapper;
    private final String nodeId;

    @Autowired
    public OutboxRelay(OutboxEventService service,
                       OutboxDeadMapper deadMapper,
                       ApplicationEventPublisher springPublisher,
                       OutboxProperties props,
                       ObjectMapper mapper) {
        this.service = service;
        this.deadMapper = deadMapper;
        this.springPublisher = springPublisher;
        this.props = props;
        this.mapper = mapper;
        this.nodeId = computeNodeId();
    }

    @Scheduled(fixedDelayString = "${s2.outbox.poll-interval-ms:2000}")
    public void poll() {
        try {
            pollOnce();
        } catch (Exception e) {
            log.warn("Outbox poll failed: {}", e.getMessage(), e);
        }
    }

    /** Visible for testing — drives one claim+dispatch cycle in the calling thread. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pollOnce() {
        OutboxMapper m = service.getBaseMapper();
        List<OutboxEvent> batch = m.lockUnprocessed(props.getBatchSize());
        if (batch.isEmpty()) {
            return;
        }
        log.debug("Outbox relay claimed {} rows on node {}", batch.size(), nodeId);

        for (OutboxEvent row : batch) {
            try {
                ApplicationEvent event = deserialize(row);
                springPublisher.publishEvent(event);
                m.markProcessed(row.getId(), LocalDateTime.now(), nodeId);
            } catch (DeserializationException de) {
                handlePoisonedRow(row, de.getMessage());
            } catch (Exception e) {
                log.warn("Listener for outbox id={} failed: {}", row.getId(), e.getMessage(), e);
                // Still mark processed — at-least-once to listeners. Listeners handle own retries.
                // If you need retry on listener failure, change to recordFailure + re-queue.
                int attempts = row.getAttempts() == null ? 0 : row.getAttempts();
                if (attempts + 1 >= props.getMaxAttempts()) {
                    handlePoisonedRow(row,
                            "Listener failed after " + (attempts + 1) + " attempts: " + e.getMessage());
                } else {
                    m.recordFailure(row.getId(), e.getMessage());
                }
            }
        }
    }

    private ApplicationEvent deserialize(OutboxEvent row) {
        try {
            Class<?> cls = Class.forName(row.getEventType());
            Object obj = mapper.readValue(row.getPayloadJson(), cls);
            if (!(obj instanceof ApplicationEvent ae)) {
                throw new DeserializationException(
                        "Row " + row.getId() + " payload is not ApplicationEvent: " + cls);
            }
            return ae;
        } catch (Exception e) {
            throw new DeserializationException(
                    "Deserialize failed for row " + row.getId() + ": " + e.getMessage(), e);
        }
    }

    private void handlePoisonedRow(OutboxEvent row, String reason) {
        log.error("Moving outbox row id={} type={} to dead table: {}",
                row.getId(), row.getEventType(), reason);
        OutboxDeadEvent dead = new OutboxDeadEvent();
        dead.setOriginalId(row.getId());
        dead.setAggregateType(row.getAggregateType());
        dead.setAggregateId(row.getAggregateId());
        dead.setEventType(row.getEventType());
        dead.setPayloadJson(row.getPayloadJson());
        dead.setTenantId(row.getTenantId());
        dead.setFailureReason(reason);
        dead.setAttempts(row.getAttempts() == null ? 0 : row.getAttempts());
        dead.setCreatedAt(row.getCreatedAt());
        dead.setDiedAt(LocalDateTime.now());
        deadMapper.insert(dead);
        // Mark as processed so the relay doesn't re-pick it.
        service.getBaseMapper().markProcessed(row.getId(), LocalDateTime.now(), nodeId);
    }

    private String computeNodeId() {
        try {
            return InetAddress.getLocalHost().getHostName()
                    + ":" + ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception e) {
            return "unknown:" + System.nanoTime();
        }
    }

    private static final class DeserializationException extends RuntimeException {
        DeserializationException(String msg) { super(msg); }
        DeserializationException(String msg, Throwable cause) { super(msg, cause); }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
mvn -pl common test -Dtest=OutboxRelayTest
```
Expected: PASS — both tests green. Specifically verify "twoParallelRelays_doNotDoubleProcess" receives exactly 20 distinct messages.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRelay.java \
        common/src/test/java/com/tencent/supersonic/common/outbox/OutboxRelayTest.java \
        common/src/test/java/com/tencent/supersonic/common/outbox/OutboxTestConfig.java \
        common/src/test/resources/schema-h2.sql
git commit -m "feat(outbox): add OutboxRelay with SKIP LOCKED polling and parallel-safety test"
```

---

## Task 5: Update `TenantSqlInterceptor` exclusion list

**Files:**
- Modify: `common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java`
- Modify: `common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/outbox/TenantSqlInterceptorOutboxExclusionTest.java`

Why: the relay runs cross-tenant (needs to see every tenant's rows). `TenantContext` in `@Scheduled` threads is null by default (so interceptor is a no-op today), but defense-in-depth means we exclude `s2_outbox` and `s2_outbox_dead` from tenant filtering regardless.

- [ ] **Step 1: Write failing regression test**

Create `common/src/test/java/com/tencent/supersonic/common/outbox/TenantSqlInterceptorOutboxExclusionTest.java`:

```java
package com.tencent.supersonic.common.outbox;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.mybatis.TenantSqlInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSqlInterceptorOutboxExclusionTest {

    private TenantSqlInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantSqlInterceptor(new TenantConfig());
        TenantContext.setTenantId(99L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExcludeOutboxTables() throws Exception {
        Method m = TenantSqlInterceptor.class.getDeclaredMethod("shouldExcludeTable", String.class);
        m.setAccessible(true);
        assertThat((Boolean) m.invoke(interceptor, "s2_outbox")).isTrue();
        assertThat((Boolean) m.invoke(interceptor, "s2_outbox_dead")).isTrue();
    }

    @Test
    void tenantConfigExcludesOutboxTables() {
        TenantConfig config = new TenantConfig();
        assertThat(config.isExcludedTable("s2_outbox")).isTrue();
        assertThat(config.isExcludedTable("s2_outbox_dead")).isTrue();
    }
}
```

- [ ] **Step 2: Run test — expect fail**

Run:
```bash
mvn -pl common test -Dtest=TenantSqlInterceptorOutboxExclusionTest
```
Expected: FAIL — both assertions fail because default list doesn't include outbox tables.

- [ ] **Step 3: Update `TenantSqlInterceptor`**

Edit `common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java` — change line 63-65:

Old:
```java
    private static final Set<String> DEFAULT_EXCLUDED_TABLES =
            new HashSet<>(Arrays.asList("s2_tenant", "s2_subscription_plan", "s2_permission",
                    "s2_role_permission", "s2_user_role"));
```

New:
```java
    private static final Set<String> DEFAULT_EXCLUDED_TABLES =
            new HashSet<>(Arrays.asList("s2_tenant", "s2_subscription_plan", "s2_permission",
                    "s2_role_permission", "s2_user_role",
                    "s2_outbox", "s2_outbox_dead"));
```

- [ ] **Step 4: Update `TenantConfig`**

Edit `common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java` lines 52-58:

Old:
```java
    private List<String> excludedTables = Arrays.asList(
            // System-level tables
            "s2_tenant", "s2_subscription_plan",
            // RBAC association tables without tenant_id
            "s2_permission", "s2_role_permission", "s2_user_role",
            // Feishu session table — tenant derived via feishu_open_id → s2_feishu_user_mapping
            "s2_feishu_query_session");
```

New:
```java
    private List<String> excludedTables = Arrays.asList(
            // System-level tables
            "s2_tenant", "s2_subscription_plan",
            // RBAC association tables without tenant_id
            "s2_permission", "s2_role_permission", "s2_user_role",
            // Feishu session table — tenant derived via feishu_open_id → s2_feishu_user_mapping
            "s2_feishu_query_session",
            // Outbox tables — relay runs cross-tenant (scheduled), rows carry tenant_id for audit only
            "s2_outbox", "s2_outbox_dead");
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
mvn -pl common test -Dtest=TenantSqlInterceptorOutboxExclusionTest
```
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java \
        common/src/main/java/com/tencent/supersonic/common/config/TenantConfig.java \
        common/src/test/java/com/tencent/supersonic/common/outbox/TenantSqlInterceptorOutboxExclusionTest.java
git commit -m "feat(outbox): exclude s2_outbox and s2_outbox_dead from tenant SQL filtering"
```

---

## Task 6: Migrate `TemplateDeployedEvent` to the outbox

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java`
- Modify: `headless/api/src/main/java/com/tencent/supersonic/headless/api/event/TemplateDeployedEvent.java` (add no-arg ctor for Jackson)
- Test: `headless/server/src/test/java/com/tencent/supersonic/headless/server/listener/TemplateDeployedOutboxTest.java` (new)

Why this event: crosses `headless` → `chat` boundary, creates durable Agent/Plugin rows. Losing it leaves a deployed template with no agent — a real user-visible bug.

- [ ] **Step 1: Add no-arg constructor to `TemplateDeployedEvent`**

Edit `headless/api/src/main/java/com/tencent/supersonic/headless/api/event/TemplateDeployedEvent.java`:

Old (lines 8-33):
```java
public class TemplateDeployedEvent extends ApplicationEvent {

    private final SemanticDeployResult result;
    private final SemanticTemplateConfig config;
    private final User user;

    public TemplateDeployedEvent(Object source, SemanticDeployResult result,
            SemanticTemplateConfig config, User user) {
        super(source);
        this.result = result;
        this.config = config;
        this.user = user;
    }
    // ... getters ...
}
```

New (fields become non-final + no-arg ctor for Jackson):
```java
public class TemplateDeployedEvent extends ApplicationEvent {

    private SemanticDeployResult result;
    private SemanticTemplateConfig config;
    private User user;

    /** Jackson deserialization constructor. */
    @com.fasterxml.jackson.annotation.JsonCreator
    public TemplateDeployedEvent() {
        super("outbox");
    }

    public TemplateDeployedEvent(Object source, SemanticDeployResult result,
            SemanticTemplateConfig config, User user) {
        super(source);
        this.result = result;
        this.config = config;
        this.user = user;
    }

    public SemanticDeployResult getResult() { return result; }
    public void setResult(SemanticDeployResult result) { this.result = result; }

    public SemanticTemplateConfig getConfig() { return config; }
    public void setConfig(SemanticTemplateConfig config) { this.config = config; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
```

- [ ] **Step 2: Inject `OutboxPublisher` into `SemanticTemplateServiceImpl`**

Locate the class field declarations (search for `applicationEventPublisher`). Add:

```java
    private final com.tencent.supersonic.common.outbox.OutboxPublisher outboxPublisher;
```

Add it to the constructor / `@RequiredArgsConstructor` if applicable. If the class uses `@Autowired` on the field, keep the style consistent.

- [ ] **Step 3: Swap publisher call at line 267**

Old:
```java
                applicationEventPublisher.publishEvent(new TemplateDeployedEvent(this, result,
                        template.getTemplateConfig(), user));
```

New:
```java
                outboxPublisher.publish(new TemplateDeployedEvent(this, result,
                        template.getTemplateConfig(), user));
```

- [ ] **Step 4: Swap publisher call at line 343**

Old:
```java
            applicationEventPublisher.publishEvent(
                    new TemplateDeployedEvent(this, result, template.getTemplateConfig(), user));
```

New:
```java
            outboxPublisher.publish(
                    new TemplateDeployedEvent(this, result, template.getTemplateConfig(), user));
```

- [ ] **Step 5: Listener side — verify `TemplateDeployedEventListener` still works unchanged**

File: `chat/server/src/main/java/com/tencent/supersonic/chat/server/listener/TemplateDeployedEventListener.java`. It uses plain `@EventListener`, and `OutboxRelay` re-publishes as `ApplicationEvent`, so no change needed. Confirm by reading the file.

Note on phase: the relay does NOT run inside the caller's transaction, so this listener is already effectively "after commit" — stronger semantics than today (today it fires mid-transaction; if the outer deploy fails after, agents were still created). Call out in docs.

- [ ] **Step 6: Write integration test**

Create `headless/server/src/test/java/com/tencent/supersonic/headless/server/listener/TemplateDeployedOutboxTest.java`:

```java
package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.common.outbox.OutboxEvent;
import com.tencent.supersonic.common.outbox.OutboxEventService;
import com.tencent.supersonic.common.outbox.OutboxPublisher;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.event.TemplateDeployedEvent;
import com.tencent.supersonic.headless.api.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TemplateDeployedOutboxTest {

    @Autowired private OutboxPublisher publisher;
    @Autowired private OutboxEventService service;

    @Test
    void publishingTemplateDeployedEvent_writesToOutbox() {
        User user = new User();
        user.setName("tester");
        SemanticDeployResult result = new SemanticDeployResult();
        SemanticTemplateConfig cfg = new SemanticTemplateConfig();

        publisher.publish(new TemplateDeployedEvent(this, result, cfg, user));

        long count = service.getBaseMapper().countUnprocessed();
        assertThat(count).isGreaterThanOrEqualTo(1);
        OutboxEvent row = service.list().get(0);
        assertThat(row.getEventType()).isEqualTo(TemplateDeployedEvent.class.getName());
        assertThat(row.getPayloadJson()).contains("tester");
    }
}
```

- [ ] **Step 7: Run full compile and tests**

Run:
```bash
mvn compile -pl launchers/standalone -am
mvn test -pl headless/server -Dtest=TemplateDeployedOutboxTest
```
Expected: `BUILD SUCCESS`, test PASS.

- [ ] **Step 8: Commit**

```bash
git add headless/api/src/main/java/com/tencent/supersonic/headless/api/event/TemplateDeployedEvent.java \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/SemanticTemplateServiceImpl.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/listener/TemplateDeployedOutboxTest.java
git commit -m "feat(outbox): migrate TemplateDeployedEvent to transactional outbox"
```

---

## Task 7: Monitoring — Micrometer gauges

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxMeterBinder.java`

- [ ] **Step 1: Implement meter binder**

```java
package com.tencent.supersonic.common.outbox;

import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Exposes:
 *   - s2_outbox_unprocessed_count : gauge of NULL processed_at rows
 *   - s2_outbox_lag_seconds       : age of the oldest unprocessed row (created_at vs now)
 */
@Component
@ConditionalOnProperty(prefix = "s2.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class OutboxMeterBinder extends AbstractMeterBinder {

    private final OutboxEventService service;

    public OutboxMeterBinder(OutboxEventService service) {
        super(Tags.of("subsystem", "outbox"));
        this.service = service;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        registry.gauge("s2_outbox_unprocessed_count", commonTags(), this, b -> b.unprocessed());
        registry.gauge("s2_outbox_lag_seconds",       commonTags(), this, b -> b.lagSeconds());
    }

    double unprocessed() {
        try {
            return service.getBaseMapper().countUnprocessed();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    double lagSeconds() {
        try {
            LocalDateTime oldest = service.getBaseMapper().oldestUnprocessedCreatedAt();
            if (oldest == null) return 0.0;
            return Duration.between(oldest, LocalDateTime.now()).toSeconds();
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run:
```bash
mvn compile -pl common -am
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/outbox/OutboxMeterBinder.java
git commit -m "feat(outbox): add Micrometer gauges for unprocessed count and lag seconds"
```

---

## Task 8: TTL cleanup job

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRetentionTask.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/outbox/OutboxRetentionTaskTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.tencent.supersonic.common.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxTestConfig.class)
@TestPropertySource(properties = {
        "s2.outbox.retention-days=7",
        "spring.datasource.url=jdbc:h2:mem:outbox-retention;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.sql.init.mode=always"
})
class OutboxRetentionTaskTest {

    @Autowired private OutboxEventService service;
    @Autowired private OutboxRetentionTask task;

    @Test
    void deletesOnlyProcessedRowsOlderThanRetention() {
        OutboxEvent old = row(LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(10));
        OutboxEvent fresh = row(LocalDateTime.now(), LocalDateTime.now());
        OutboxEvent unprocessedOld = row(LocalDateTime.now().minusDays(10), null);
        service.saveBatch(java.util.List.of(old, fresh, unprocessedOld));

        task.runCleanup();

        assertThat(service.count()).isEqualTo(2); // fresh + unprocessedOld survive
    }

    private static OutboxEvent row(LocalDateTime created, LocalDateTime processed) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("X");
        e.setEventType("X");
        e.setPayloadJson("{}");
        e.setTenantId(1L);
        e.setCreatedAt(created);
        e.setProcessedAt(processed);
        e.setAttempts(0);
        return e;
    }
}
```

- [ ] **Step 2: Run test, expect fail**

Run:
```bash
mvn -pl common test -Dtest=OutboxRetentionTaskTest
```
Expected: FAIL — class missing.

- [ ] **Step 3: Implement task**

```java
package com.tencent.supersonic.common.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(prefix = "s2.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class OutboxRetentionTask {

    private final OutboxEventService service;
    private final OutboxProperties props;

    public OutboxRetentionTask(OutboxEventService service, OutboxProperties props) {
        this.service = service;
        this.props = props;
    }

    /** Runs daily at 03:15 local time. */
    @Scheduled(cron = "0 15 3 * * *")
    public void runCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(props.getRetentionDays());
        int n = service.getBaseMapper().deleteProcessedBefore(cutoff);
        if (n > 0) {
            log.info("Outbox TTL cleanup: deleted {} rows processed before {}", n, cutoff);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
mvn -pl common test -Dtest=OutboxRetentionTaskTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRetentionTask.java \
        common/src/test/java/com/tencent/supersonic/common/outbox/OutboxRetentionTaskTest.java
git commit -m "feat(outbox): add TTL cleanup task for processed rows older than retention window"
```

---

## Task 9: Failure path — deserialization errors move to dead table

**Files:**
- Test: `common/src/test/java/com/tencent/supersonic/common/outbox/OutboxDeadLetterTest.java`

The implementation landed in Task 4; this task only adds a test to lock the behavior.

- [ ] **Step 1: Write test**

```java
package com.tencent.supersonic.common.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OutboxTestConfig.class)
@TestPropertySource(properties = {
        "s2.outbox.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:outbox-dead;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.sql.init.mode=always"
})
class OutboxDeadLetterTest {

    @Autowired private OutboxEventService service;
    @Autowired private OutboxDeadMapper deadMapper;
    @Autowired private OutboxRelay relay;

    @Test
    void undeserializableRow_moveToDeadTable() {
        OutboxEvent bad = new OutboxEvent();
        bad.setAggregateType("X");
        bad.setEventType("com.example.DoesNotExist"); // ClassNotFoundException
        bad.setPayloadJson("{}");
        bad.setTenantId(1L);
        bad.setCreatedAt(LocalDateTime.now());
        bad.setAttempts(0);
        service.save(bad);

        relay.pollOnce();

        // Original row marked processed, NOT re-selected
        assertThat(service.getBaseMapper().countUnprocessed()).isZero();
        // Dead row exists referencing originalId
        assertThat(deadMapper.selectCount(null)).isEqualTo(1L);
        OutboxDeadEvent dead = deadMapper.selectList(null).get(0);
        assertThat(dead.getOriginalId()).isEqualTo(bad.getId());
        assertThat(dead.getFailureReason()).contains("DoesNotExist");
    }

    @Test
    void malformedJson_moveToDeadTable() {
        OutboxEvent bad = new OutboxEvent();
        bad.setAggregateType("X");
        bad.setEventType(OutboxPublisherTest.ToyEvent.class.getName());
        bad.setPayloadJson("{not json");
        bad.setTenantId(1L);
        bad.setCreatedAt(LocalDateTime.now());
        bad.setAttempts(0);
        service.save(bad);

        relay.pollOnce();

        assertThat(deadMapper.selectCount(null)).isGreaterThanOrEqualTo(1L);
    }
}
```

- [ ] **Step 2: Run test**

Run:
```bash
mvn -pl common test -Dtest=OutboxDeadLetterTest
```
Expected: PASS (both tests).

- [ ] **Step 3: Commit**

```bash
git add common/src/test/java/com/tencent/supersonic/common/outbox/OutboxDeadLetterTest.java
git commit -m "test(outbox): lock down dead-letter behavior for unparseable rows"
```

---

## Task 10: Docs + migration roadmap + rollback

**Files:**
- Create: `docs/details/platform/outbox.md`
- Modify: `docs/details/README.md` (add link)

- [ ] **Step 1: Write detail spec**

Create `docs/details/platform/outbox.md`:

```markdown
---
status: active
module: platform
key-files:
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxPublisher.java
  - common/src/main/java/com/tencent/supersonic/common/outbox/OutboxRelay.java
  - launchers/standalone/src/main/resources/db/migration/mysql/V29__outbox.sql
---

# Transactional Outbox

## 目标
为需要"跨模块且不能丢"的 `ApplicationEvent` 提供 at-least-once 投递，不引入 MQ。

## 主链路
1. Service 在自身 `@Transactional` 中调用 `outboxPublisher.publish(event)`。
2. `OutboxPublisher` 用 Jackson 序列化，写入 `s2_outbox`（与业务变更同事务）。
3. 业务事务提交 → 行可见。
4. `OutboxRelay`（`@Scheduled` fixedDelay=2s）在独立事务中 `SELECT ... FOR UPDATE SKIP LOCKED`，拿到一批行。
5. 对每行反序列化为原事件类 → `ApplicationEventPublisher.publishEvent(event)` → mark `processed_at`。
6. 监听器（`@EventListener`）照常消费，不需改动。
7. 反序列化失败或达到 `maxAttempts` → 行迁移到 `s2_outbox_dead`，并在 `s2_outbox` 上 mark processed（防止反复重试）。

## 保证与不保证
- **保证**：业务提交后事件至少被投递一次。
- **不保证**：严格顺序（`ORDER BY created_at`，但 SKIP LOCKED 不保证跨节点绝对 FIFO）。
- **不保证**：恰好一次——监听器必须幂等。

## 当前已迁移的事件
| 事件 | 迁移原因 |
|---|---|
| `TemplateDeployedEvent` | 跨 headless/chat/feishu 三模块；失败会造成"模板已部署但无 Agent" |

## 建议下一批迁移
| 事件 | 原因 | 优先级 |
|---|---|---|
| `DataEvent` (ADD/UPDATE/DELETE) | 驱动 embedding + 字典，chat 分词依赖此，缺失会导致查询命中率下降 | P1 |
| `PluginAddEvent/PluginUpdateEvent/PluginDelEvent` | 插件缓存刷新，失败会让 chat 匹配到已删除插件 | P2 |

保留同步的事件（暂不迁移）：
- 纯模块内事件（只有同模块监听）。
- 幂等、可回放的周期任务触发（如 `RefreshEvent`）。

## 监控
- `s2_outbox_unprocessed_count` — 应接近 0，>100 告警
- `s2_outbox_lag_seconds` — 最老未处理行的年龄，>60s 告警

## 回滚
1. 设置 `s2.outbox.enabled=false` 并重启。`OutboxPublisher.publish()` 改为走同步 Spring 发布，relay bean 不注册。
2. 历史堆积：手动 `DELETE FROM s2_outbox WHERE processed_at IS NULL` 或让 relay 在下次启用时消费。
3. 如需彻底移除：删除 Task 6 的改动（恢复 `applicationEventPublisher.publishEvent(...)`），不要删表（保留以便审计）。

## 风险
- **重复消费**：relay 重启时若 mark 前崩溃，行会被再次 claim → 监听器必须幂等。`TemplateDeployedEventListener` 的 agent 创建当前不是幂等的——列为已知限制，下一步在 agent service 加 unique key。
- **H2 兼容**：H2 2.2+ 支持 `FOR UPDATE SKIP LOCKED`；旧版本需升级。
- **JSON 膨胀**：`payload_json` 是 `MEDIUMTEXT/TEXT`，极端大 event（如 `TemplateDeployedEvent` with large config）需审视。
```

- [ ] **Step 2: Link from README**

Edit `docs/details/README.md`. Locate the platform section and add:

```markdown
- [outbox.md](./platform/outbox.md) — 事务性 Outbox 模式，跨模块事件持久化
```

- [ ] **Step 3: Final end-to-end validation**

Run:
```bash
mvn compile -pl launchers/standalone -am
mvn test -pl common -Dtest='Outbox*'
```
Expected: `BUILD SUCCESS`; all outbox tests pass.

- [ ] **Step 4: Commit**

```bash
git add docs/details/platform/outbox.md docs/details/README.md
git commit -m "docs(outbox): add detail spec with migration roadmap and rollback plan"
```

---

## Self-Review Notes

- **Ordering**: `ORDER BY created_at ASC` gives approximate FIFO per table; concurrent relays + SKIP LOCKED mean exact order is **not** guaranteed. Listeners must not rely on order. Documented in `outbox.md`.
- **Duplicates / idempotency**: at-least-once means listeners **must** be idempotent. `TemplateDeployedEventListener` is currently NOT idempotent (creates a new Agent every call). Before enabling outbox in prod, either (a) add uniqueness on `Agent.name + tenantId`, or (b) short-circuit when an agent with that name already exists. Flagged in docs as known limitation.
- **Transaction boundary**: `OutboxPublisher.publish()` uses `@Transactional(REQUIRED)` — joins the caller's tx if present, else creates one. Async callers without an outer tx still get durable writes.
- **Multi-instance**: SKIP LOCKED is supported on MySQL 8.0+ and PostgreSQL 9.5+. If a consumer needs MySQL 5.7 support, they must set `s2.outbox.enabled=false` or we'd have to fall back to optimistic CAS on `processing_node`. Not in scope for MVP.
- **Flyway convention**: V29 is the next free version after V28 (confirmed via `ls launchers/standalone/src/main/resources/db/migration/mysql/`). MySQL variant uses plain `CREATE TABLE IF NOT EXISTS` (NOT `ADD COLUMN IF NOT EXISTS` — MySQL doesn't support it, per known gotcha).
- **Relay visibility-for-test**: `pollOnce()` is public so tests drive it synchronously without racing `@Scheduled`. The `@Scheduled` method `poll()` wraps it.
- **Listener failure semantics**: chose "mark processed even on listener failure after maxAttempts" to avoid infinite reprocessing loops. Controversial — documented the trade-off.
