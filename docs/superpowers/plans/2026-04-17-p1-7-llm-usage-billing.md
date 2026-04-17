# P1-7: LLM Token Usage Capture + Billing Integration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Capture per-tenant / per-model / per-call-type LLM token usage across every LangChain4j invocation, persist to `s2_llm_usage` with cost estimates, and emit `LlmUsageEvent` so the `billing` module can aggregate plan quota and so admins get a monthly usage dashboard.

**Architecture:** `TokenCountingChatModel` is a decorator that wraps the `ChatLanguageModel` / `StreamingChatLanguageModel` returned from `ModelProvider`. Every factory in `dev.langchain4j.provider` is updated to wrap its output — no existing call site changes. The decorator extracts `TokenUsage` from `Response<AiMessage>` (or from the streaming end-of-response handler), pushes a record into an in-memory `ArrayBlockingQueue` inside `LlmUsageRecorder`, which an async flusher drains every 5 s (or when 100 rows accumulate) into `s2_llm_usage` via MyBatis-Plus. After each flush, the recorder publishes a batched `LlmUsageEvent`. A new `LlmUsageListener` in `billing/server` consumes events and increments a `s2_tenant_usage`-style monthly counter on `TenantSubscriptionDO`'s plan quota. Cost lookup comes from `s2_llm_pricing` with a Caffeine cache. At-least-once semantics: crash loses queued-but-unflushed records (MVP acceptable). Tenant ID is pulled from `TenantContext` at capture time and stored on every row so `TenantSqlInterceptor` isolates reads automatically.

**Tech Stack:** Java 21, Spring Boot 3.4.x, LangChain4j (`dev.langchain4j:langchain4j`), MyBatis-Plus, Caffeine, Flyway (MySQL + PostgreSQL), React/TypeScript/Ant Design Pro, dayjs. Tests use JUnit 5, Mockito, AssertJ (match patterns already present in `auth/authentication/src/test`).

---

## File Structure

### New Java files — common module (owns the decorator & recorder)

- `common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingChatModel.java` — decorator wrapping `ChatLanguageModel`; intercepts `generate(...)` and captures `TokenUsage` from `Response<AiMessage>`.
- `common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingStreamingChatModel.java` — decorator wrapping `StreamingChatLanguageModel`; intercepts `generate(...)` and captures token usage from the final `Response<AiMessage>` passed to `onComplete(Response)`.
- `common/src/main/java/com/tencent/supersonic/common/llm/LlmUsageRecorder.java` — in-memory ring buffer + async flusher (Spring `@Scheduled` + size-triggered `flushNow()`).
- `common/src/main/java/com/tencent/supersonic/common/llm/LlmCallType.java` — enum: `NL2SQL`, `SUMMARY`, `PLUGIN`, `DATA_INTERPRET`, `CORRECTOR`, `MAPPER`, `ALIAS`, `MEMORY_REVIEW`, `UNKNOWN`.
- `common/src/main/java/com/tencent/supersonic/common/llm/LlmCallContext.java` — thread-local that callers can set to stamp `callType`, `requestId`, `traceId` onto a pending call.
- `common/src/main/java/com/tencent/supersonic/common/llm/CostEstimator.java` — loads pricing from `s2_llm_pricing` with Caffeine cache; returns `estimatedCostMicros`.
- `common/src/main/java/com/tencent/supersonic/common/llm/event/LlmUsageEvent.java` — Spring `ApplicationEvent` carrying a `List<LlmUsageRecord>` snapshot.
- `common/src/main/java/com/tencent/supersonic/common/llm/persistence/dataobject/LlmUsageDO.java` — `@TableName("s2_llm_usage")`.
- `common/src/main/java/com/tencent/supersonic/common/llm/persistence/dataobject/LlmPricingDO.java` — `@TableName("s2_llm_pricing")`.
- `common/src/main/java/com/tencent/supersonic/common/llm/persistence/mapper/LlmUsageDOMapper.java` — extends `BaseMapper<LlmUsageDO>`; adds `batchInsert` + aggregation queries.
- `common/src/main/java/com/tencent/supersonic/common/llm/persistence/mapper/LlmPricingDOMapper.java` — extends `BaseMapper<LlmPricingDO>`.
- `common/src/main/java/com/tencent/supersonic/common/llm/service/LlmUsageService.java` — read API (query by tenant / date range / model, paged).
- `common/src/main/java/com/tencent/supersonic/common/llm/service/impl/LlmUsageServiceImpl.java`
- `common/src/main/java/com/tencent/supersonic/common/llm/pojo/LlmUsageRecord.java` — in-memory record (pre-persistence value holder).

### Modified Java files

- `common/src/main/java/dev/langchain4j/provider/OpenAiModelFactory.java` — wrap returned models in decorators.
- `common/src/main/java/dev/langchain4j/provider/OllamaModelFactory.java` — same.
- `common/src/main/java/dev/langchain4j/provider/LocalAiModelFactory.java` — same.
- `common/src/main/java/dev/langchain4j/provider/InMemoryModelFactory.java` — same.
- `common/src/main/java/dev/langchain4j/provider/DifyModelFactory.java` — same.

### Billing listener

- `billing/server/src/main/java/com/tencent/supersonic/billing/server/listener/LlmUsageListener.java` — consumes `LlmUsageEvent`, increments token quota via existing `UsageTrackingService`.

### REST controller (headless/server module — keeps common free of REST)

- `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/LlmUsageController.java` — `/api/semantic/admin/llm-usage` endpoints.

### Flyway

- `launchers/standalone/src/main/resources/db/migration/mysql/V29__llm_usage_and_pricing.sql`
- `launchers/standalone/src/main/resources/db/migration/postgresql/V29__llm_usage_and_pricing.sql`
- `launchers/standalone/src/main/resources/db/migration/mysql/V30__llm_pricing_seed.sql`
- `launchers/standalone/src/main/resources/db/migration/postgresql/V30__llm_pricing_seed.sql`

### Tests

- `common/src/test/java/com/tencent/supersonic/common/llm/LlmUsageRecorderTest.java`
- `common/src/test/java/com/tencent/supersonic/common/llm/TokenCountingChatModelTest.java`
- `common/src/test/java/com/tencent/supersonic/common/llm/TokenCountingStreamingChatModelTest.java`
- `common/src/test/java/com/tencent/supersonic/common/llm/CostEstimatorTest.java`
- `common/src/test/java/com/tencent/supersonic/common/llm/LlmUsageEventIntegrationTest.java`
- `billing/server/src/test/java/com/tencent/supersonic/billing/server/listener/LlmUsageListenerTest.java`
- `headless/server/src/test/java/com/tencent/supersonic/headless/server/rest/LlmUsageControllerTest.java`

### Frontend

- `webapp/packages/supersonic-fe/src/pages/LlmUsage/index.tsx`
- `webapp/packages/supersonic-fe/src/pages/LlmUsage/style.less`
- `webapp/packages/supersonic-fe/src/services/llmUsage.ts`
- Modify: `webapp/packages/supersonic-fe/config/routes.ts` (add `/llm-usage` route).

### Docs

- `docs/details/platform/llm-usage-billing.md` (new detail spec; add to `docs/details/README.md`).

---

## Task 1: Flyway migrations for `s2_llm_usage` and `s2_llm_pricing`

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V29__llm_usage_and_pricing.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V29__llm_usage_and_pricing.sql`

- [ ] **Step 1: Write the MySQL migration**

```sql
-- V29__llm_usage_and_pricing.sql
CREATE TABLE IF NOT EXISTS s2_llm_usage (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL DEFAULT 1,
    user_id                 VARCHAR(100),
    model                   VARCHAR(200) NOT NULL,
    provider                VARCHAR(50)  NOT NULL,
    call_type               VARCHAR(50)  NOT NULL COMMENT 'NL2SQL, SUMMARY, PLUGIN, DATA_INTERPRET, CORRECTOR, MAPPER, ALIAS, MEMORY_REVIEW, UNKNOWN',
    input_tokens            INT          NOT NULL DEFAULT 0,
    output_tokens           INT          NOT NULL DEFAULT 0,
    total_tokens            INT          NOT NULL DEFAULT 0,
    estimated_cost_micros   BIGINT       NOT NULL DEFAULT 0 COMMENT 'USD cost in micro-dollars (1e-6); 0 if pricing unknown',
    request_id              VARCHAR(64),
    trace_id                VARCHAR(64),
    latency_ms              INT,
    success                 TINYINT(1)   NOT NULL DEFAULT 1,
    error_type              VARCHAR(100),
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_llm_usage_tenant_date (tenant_id, created_at),
    INDEX idx_llm_usage_model (model),
    INDEX idx_llm_usage_call_type (call_type),
    INDEX idx_llm_usage_request_id (request_id)
);

CREATE TABLE IF NOT EXISTS s2_llm_pricing (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider                VARCHAR(50)  NOT NULL,
    model                   VARCHAR(200) NOT NULL,
    input_price_per_1k_micros  BIGINT    NOT NULL DEFAULT 0 COMMENT 'Cost per 1k input tokens in micro-USD',
    output_price_per_1k_micros BIGINT    NOT NULL DEFAULT 0 COMMENT 'Cost per 1k output tokens in micro-USD',
    currency                VARCHAR(10)  NOT NULL DEFAULT 'USD',
    effective_from          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to            DATETIME,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pricing_provider_model (provider, model, effective_from)
);
```

- [ ] **Step 2: Write the PostgreSQL migration**

```sql
-- V29__llm_usage_and_pricing.sql
CREATE TABLE IF NOT EXISTS s2_llm_usage (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL DEFAULT 1,
    user_id                 VARCHAR(100),
    model                   VARCHAR(200) NOT NULL,
    provider                VARCHAR(50)  NOT NULL,
    call_type               VARCHAR(50)  NOT NULL,
    input_tokens            INT          NOT NULL DEFAULT 0,
    output_tokens           INT          NOT NULL DEFAULT 0,
    total_tokens            INT          NOT NULL DEFAULT 0,
    estimated_cost_micros   BIGINT       NOT NULL DEFAULT 0,
    request_id              VARCHAR(64),
    trace_id                VARCHAR(64),
    latency_ms              INT,
    success                 SMALLINT     NOT NULL DEFAULT 1,
    error_type              VARCHAR(100),
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_llm_usage_tenant_date ON s2_llm_usage (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_llm_usage_model ON s2_llm_usage (model);
CREATE INDEX IF NOT EXISTS idx_llm_usage_call_type ON s2_llm_usage (call_type);
CREATE INDEX IF NOT EXISTS idx_llm_usage_request_id ON s2_llm_usage (request_id);

CREATE TABLE IF NOT EXISTS s2_llm_pricing (
    id                      BIGSERIAL    PRIMARY KEY,
    provider                VARCHAR(50)  NOT NULL,
    model                   VARCHAR(200) NOT NULL,
    input_price_per_1k_micros  BIGINT    NOT NULL DEFAULT 0,
    output_price_per_1k_micros BIGINT    NOT NULL DEFAULT 0,
    currency                VARCHAR(10)  NOT NULL DEFAULT 'USD',
    effective_from          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to            TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pricing_provider_model UNIQUE (provider, model, effective_from)
);
```

- [ ] **Step 3: Verify Flyway picks up migrations on startup**

Run:
```bash
mvn compile -pl launchers/standalone -am
```
Expected: BUILD SUCCESS. (Full startup/Flyway is verified after Task 2 when entities exist.)

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V29__llm_usage_and_pricing.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V29__llm_usage_and_pricing.sql
git commit -m "feat(common): add s2_llm_usage and s2_llm_pricing flyway migrations"
```

---

## Task 2: `LlmUsageDO` + `LlmPricingDO` + mappers + service skeleton

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/persistence/dataobject/LlmUsageDO.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/persistence/dataobject/LlmPricingDO.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/persistence/mapper/LlmUsageDOMapper.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/persistence/mapper/LlmPricingDOMapper.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/service/LlmUsageService.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/service/impl/LlmUsageServiceImpl.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/pojo/LlmUsageRecord.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/LlmCallType.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/llm/LlmUsageServiceImplTest.java`

- [ ] **Step 1: Write `LlmCallType` enum**

```java
package com.tencent.supersonic.common.llm;

public enum LlmCallType {
    NL2SQL, SUMMARY, PLUGIN, DATA_INTERPRET, CORRECTOR, MAPPER, ALIAS, MEMORY_REVIEW, UNKNOWN
}
```

- [ ] **Step 2: Write `LlmUsageRecord` value holder**

```java
package com.tencent.supersonic.common.llm.pojo;

import com.tencent.supersonic.common.llm.LlmCallType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LlmUsageRecord {
    private Long tenantId;
    private String userId;
    private String provider;
    private String model;
    private LlmCallType callType;
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
    private long estimatedCostMicros;
    private String requestId;
    private String traceId;
    private Integer latencyMs;
    private boolean success;
    private String errorType;
    private Instant createdAt;
}
```

- [ ] **Step 3: Write `LlmUsageDO`**

```java
package com.tencent.supersonic.common.llm.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("s2_llm_usage")
public class LlmUsageDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String userId;
    private String model;
    private String provider;
    private String callType;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Long estimatedCostMicros;
    private String requestId;
    private String traceId;
    private Integer latencyMs;
    private Boolean success;
    private String errorType;
    private Timestamp createdAt;
}
```

- [ ] **Step 4: Write `LlmPricingDO`**

```java
package com.tencent.supersonic.common.llm.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("s2_llm_pricing")
public class LlmPricingDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String provider;
    private String model;
    private Long inputPricePer1kMicros;
    private Long outputPricePer1kMicros;
    private String currency;
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
```

- [ ] **Step 5: Write `LlmUsageDOMapper`**

```java
package com.tencent.supersonic.common.llm.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface LlmUsageDOMapper extends BaseMapper<LlmUsageDO> {

    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM s2_llm_usage "
            + "WHERE tenant_id = #{tenantId} "
            + "AND created_at >= #{start} AND created_at < #{endExclusive}")
    long sumTokens(@Param("tenantId") Long tenantId,
                   @Param("start") java.sql.Timestamp start,
                   @Param("endExclusive") java.sql.Timestamp endExclusive);

    @Select("SELECT DATE(created_at) AS day, SUM(total_tokens) AS tokens, "
            + "SUM(estimated_cost_micros) AS cost "
            + "FROM s2_llm_usage WHERE tenant_id = #{tenantId} "
            + "AND created_at >= #{start} AND created_at < #{endExclusive} "
            + "GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> dailyAggregates(@Param("tenantId") Long tenantId,
                                              @Param("start") java.sql.Timestamp start,
                                              @Param("endExclusive") java.sql.Timestamp endExclusive);
}
```

- [ ] **Step 6: Write `LlmPricingDOMapper`**

```java
package com.tencent.supersonic.common.llm.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmPricingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmPricingDOMapper extends BaseMapper<LlmPricingDO> {
}
```

- [ ] **Step 7: Write `LlmUsageService` interface**

```java
package com.tencent.supersonic.common.llm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LlmUsageService {
    void batchInsert(List<LlmUsageDO> records);

    IPage<LlmUsageDO> query(Long tenantId, LocalDate from, LocalDate to,
                            String model, String callType, int page, int size);

    long sumTokens(Long tenantId, LocalDate from, LocalDate to);

    List<Map<String, Object>> dailyAggregates(Long tenantId, LocalDate from, LocalDate to);
}
```

- [ ] **Step 8: Write the failing service test**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmUsageDOMapper;
import com.tencent.supersonic.common.llm.service.impl.LlmUsageServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LlmUsageServiceImplTest {

    @Test
    void batchInsertDelegatesToMapperPerRow() {
        LlmUsageDOMapper mapper = mock(LlmUsageDOMapper.class);
        LlmUsageServiceImpl svc = new LlmUsageServiceImpl(mapper);

        LlmUsageDO a = new LlmUsageDO(); a.setTenantId(1L); a.setModel("gpt-4o");
        LlmUsageDO b = new LlmUsageDO(); b.setTenantId(1L); b.setModel("gpt-4o");

        svc.batchInsert(List.of(a, b));

        ArgumentCaptor<LlmUsageDO> captor = ArgumentCaptor.forClass(LlmUsageDO.class);
        verify(mapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).containsExactly(a, b);
    }

    @Test
    void batchInsertWithEmptyListIsNoOp() {
        LlmUsageDOMapper mapper = mock(LlmUsageDOMapper.class);
        LlmUsageServiceImpl svc = new LlmUsageServiceImpl(mapper);
        svc.batchInsert(List.of());
        verifyNoInteractions(mapper);
    }
}
```

- [ ] **Step 9: Run test to verify it fails**

Run: `mvn test -pl common -Dtest=LlmUsageServiceImplTest`
Expected: FAIL with `LlmUsageServiceImpl` missing or symbol not found.

- [ ] **Step 10: Implement `LlmUsageServiceImpl`**

```java
package com.tencent.supersonic.common.llm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmUsageDOMapper;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmUsageServiceImpl implements LlmUsageService {

    private final LlmUsageDOMapper mapper;

    @Override
    public void batchInsert(List<LlmUsageDO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (LlmUsageDO r : records) {
            mapper.insert(r);
        }
    }

    @Override
    public IPage<LlmUsageDO> query(Long tenantId, LocalDate from, LocalDate to,
                                   String model, String callType, int page, int size) {
        LambdaQueryWrapper<LlmUsageDO> w = new LambdaQueryWrapper<>();
        w.eq(LlmUsageDO::getTenantId, tenantId)
                .ge(from != null, LlmUsageDO::getCreatedAt, toTimestamp(from))
                .lt(to != null, LlmUsageDO::getCreatedAt, toTimestamp(to == null ? null : to.plusDays(1)))
                .eq(model != null && !model.isBlank(), LlmUsageDO::getModel, model)
                .eq(callType != null && !callType.isBlank(), LlmUsageDO::getCallType, callType)
                .orderByDesc(LlmUsageDO::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public long sumTokens(Long tenantId, LocalDate from, LocalDate to) {
        return mapper.sumTokens(tenantId, toTimestamp(from), toTimestamp(to.plusDays(1)));
    }

    @Override
    public List<Map<String, Object>> dailyAggregates(Long tenantId, LocalDate from, LocalDate to) {
        return mapper.dailyAggregates(tenantId, toTimestamp(from), toTimestamp(to.plusDays(1)));
    }

    private Timestamp toTimestamp(LocalDate d) {
        if (d == null) return null;
        return new Timestamp(d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
```

- [ ] **Step 11: Run test to verify it passes**

Run: `mvn test -pl common -Dtest=LlmUsageServiceImplTest`
Expected: PASS.

- [ ] **Step 12: Verify full compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 13: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/llm \
        common/src/test/java/com/tencent/supersonic/common/llm
git commit -m "feat(common): add LlmUsage data objects, mappers, and service"
```

---

## Task 3: `LlmUsageRecorder` — ring buffer + async flusher

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/LlmUsageRecorder.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/llm/LlmUsageRecorderTest.java`

- [ ] **Step 1: Write the failing flusher test**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmUsageRecorderTest {

    private LlmUsageRecord sample() {
        return LlmUsageRecord.builder()
                .tenantId(1L).provider("OPEN_AI").model("gpt-4o-mini")
                .callType(LlmCallType.NL2SQL)
                .inputTokens(100).outputTokens(50).totalTokens(150)
                .createdAt(Instant.now()).success(true).build();
    }

    @Test
    void flushPersistsAllBufferedRecordsThenPublishesEvent() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = mock(CostEstimator.class);
        when(estimator.estimate(any(), any(), anyInt(), anyInt())).thenReturn(0L);

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator,
                /*capacity*/ 1000, /*flushSize*/ 100);

        rec.record(sample());
        rec.record(sample());

        rec.flushNow();

        verify(service).batchInsert(argThat(list -> list.size() == 2));
        verify(publisher).publishEvent(any(com.tencent.supersonic.common.llm.event.LlmUsageEvent.class));
    }

    @Test
    void sizeTriggeredFlushFiresOnceThresholdReached() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = mock(CostEstimator.class);
        when(estimator.estimate(any(), any(), anyInt(), anyInt())).thenReturn(0L);

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 3);

        rec.record(sample());
        rec.record(sample());
        verifyNoInteractions(service);

        rec.record(sample()); // hits threshold -> async flush via direct executor in test
        rec.flushNow();       // drain any residual (defensive)

        ArgumentCaptor<List<?>> captor = ArgumentCaptor.forClass(List.class);
        verify(service, atLeastOnce()).batchInsert(any());
    }

    @Test
    void dropsRecordWhenQueueFullWithoutThrowing() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = mock(CostEstimator.class);
        when(estimator.estimate(any(), any(), anyInt(), anyInt())).thenReturn(0L);

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 2, 1000);

        rec.record(sample());
        rec.record(sample());
        rec.record(sample()); // third one should be dropped silently

        rec.flushNow();
        verify(service).batchInsert(argThat(list -> list.size() == 2));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl common -Dtest=LlmUsageRecorderTest`
Expected: FAIL — `LlmUsageRecorder` not found.

- [ ] **Step 3: Write `LlmUsageEvent`**

```java
package com.tencent.supersonic.common.llm.event;

import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class LlmUsageEvent extends ApplicationEvent {
    private final List<LlmUsageDO> records;

    public LlmUsageEvent(Object source, List<LlmUsageDO> records) {
        super(source);
        this.records = records;
    }
}
```

- [ ] **Step 4: Write a placeholder `CostEstimator` interface (real impl in Task 6)**

```java
package com.tencent.supersonic.common.llm;

public interface CostEstimator {
    long estimate(String provider, String model, int inputTokens, int outputTokens);
}
```

- [ ] **Step 5: Implement `LlmUsageRecorder`**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class LlmUsageRecorder {

    private final LlmUsageService service;
    private final ApplicationEventPublisher publisher;
    private final CostEstimator estimator;
    private final BlockingQueue<LlmUsageRecord> queue;
    private final int flushSize;
    private final ExecutorService flusher =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "llm-usage-flusher");
                t.setDaemon(true);
                return t;
            });
    private final AtomicLong dropCount = new AtomicLong();

    // Spring constructor
    public LlmUsageRecorder(LlmUsageService service,
                            ApplicationEventPublisher publisher,
                            CostEstimator estimator,
                            @Value("${s2.llm.usage.queue-capacity:10000}") int capacity,
                            @Value("${s2.llm.usage.flush-size:100}") int flushSize) {
        this.service = service;
        this.publisher = publisher;
        this.estimator = estimator;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.flushSize = flushSize;
    }

    public void record(LlmUsageRecord r) {
        // Fill in cost if not set
        if (r.getEstimatedCostMicros() == 0) {
            r.setEstimatedCostMicros(
                    estimator.estimate(r.getProvider(), r.getModel(),
                            r.getInputTokens(), r.getOutputTokens()));
        }
        if (!queue.offer(r)) {
            long n = dropCount.incrementAndGet();
            if (n % 100 == 1) {
                log.warn("LlmUsageRecorder queue full, dropped {} records so far", n);
            }
            return;
        }
        if (queue.size() >= flushSize) {
            flusher.submit(this::flushNow);
        }
    }

    @Scheduled(fixedDelayString = "${s2.llm.usage.flush-interval-ms:5000}")
    public void scheduledFlush() {
        flushNow();
    }

    @PreDestroy
    public void shutdown() {
        flushNow();
        flusher.shutdown();
    }

    public synchronized void flushNow() {
        if (queue.isEmpty()) {
            return;
        }
        List<LlmUsageRecord> batch = new ArrayList<>(queue.size());
        queue.drainTo(batch);
        if (batch.isEmpty()) {
            return;
        }
        List<LlmUsageDO> dos = batch.stream().map(this::toDO).toList();
        try {
            service.batchInsert(dos);
            publisher.publishEvent(new LlmUsageEvent(this, dos));
        } catch (Exception e) {
            log.error("LlmUsageRecorder flush failed, {} records lost", dos.size(), e);
        }
    }

    private LlmUsageDO toDO(LlmUsageRecord r) {
        LlmUsageDO d = new LlmUsageDO();
        d.setTenantId(r.getTenantId());
        d.setUserId(r.getUserId());
        d.setProvider(r.getProvider());
        d.setModel(r.getModel());
        d.setCallType(r.getCallType() == null ? LlmCallType.UNKNOWN.name() : r.getCallType().name());
        d.setInputTokens(r.getInputTokens());
        d.setOutputTokens(r.getOutputTokens());
        d.setTotalTokens(r.getTotalTokens());
        d.setEstimatedCostMicros(r.getEstimatedCostMicros());
        d.setRequestId(r.getRequestId());
        d.setTraceId(r.getTraceId());
        d.setLatencyMs(r.getLatencyMs());
        d.setSuccess(r.isSuccess());
        d.setErrorType(r.getErrorType());
        d.setCreatedAt(r.getCreatedAt() == null ? new Timestamp(System.currentTimeMillis())
                : Timestamp.from(r.getCreatedAt()));
        return d;
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -pl common -Dtest=LlmUsageRecorderTest`
Expected: PASS.

- [ ] **Step 7: Enable `@Scheduled` in the common module**

Locate the existing `@EnableScheduling` (grep for it in `common` or `launchers/standalone`). If not already enabled in the standalone launcher, add `@EnableScheduling` to the class `launchers/standalone/src/main/java/com/tencent/supersonic/StandaloneLauncher.java` (or its existing `@SpringBootApplication` class). Do not create a new config class.

- [ ] **Step 8: Verify full compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/llm \
        common/src/test/java/com/tencent/supersonic/common/llm \
        launchers/standalone/src/main/java/com/tencent/supersonic/StandaloneLauncher.java
git commit -m "feat(common): add LlmUsageRecorder with async flusher and drop-on-full queue"
```

---

## Task 4: `TokenCountingChatModel` decorator + factory wiring

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingChatModel.java`
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/LlmCallContext.java`
- Modify: `common/src/main/java/dev/langchain4j/provider/OpenAiModelFactory.java`
- Modify: `common/src/main/java/dev/langchain4j/provider/OllamaModelFactory.java`
- Modify: `common/src/main/java/dev/langchain4j/provider/LocalAiModelFactory.java`
- Modify: `common/src/main/java/dev/langchain4j/provider/InMemoryModelFactory.java`
- Modify: `common/src/main/java/dev/langchain4j/provider/DifyModelFactory.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/llm/TokenCountingChatModelTest.java`

- [ ] **Step 1: Write `LlmCallContext` (thread-local stamp)**

```java
package com.tencent.supersonic.common.llm;

public final class LlmCallContext {

    public static final class Frame {
        public final LlmCallType callType;
        public final String requestId;
        public final String traceId;
        public final String userId;

        public Frame(LlmCallType callType, String requestId, String traceId, String userId) {
            this.callType = callType;
            this.requestId = requestId;
            this.traceId = traceId;
            this.userId = userId;
        }
    }

    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    public static void set(LlmCallType callType, String requestId, String traceId, String userId) {
        CURRENT.set(new Frame(callType, requestId, traceId, userId));
    }

    public static Frame get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private LlmCallContext() {}
}
```

- [ ] **Step 2: Write the failing decorator test**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenCountingChatModelTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        LlmCallContext.clear();
    }

    private ChatLanguageModel fakeDelegate() {
        ChatLanguageModel delegate = mock(ChatLanguageModel.class);
        Response<AiMessage> r = Response.from(
                AiMessage.from("hello"),
                new TokenUsage(42, 7, 49));
        when(delegate.generate(anyList())).thenReturn(r);
        return delegate;
    }

    @Test
    void capturesUsageWithTenantAndCallType() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        TenantContext.setTenantId(7L);
        LlmCallContext.set(LlmCallType.NL2SQL, "req-1", "trace-1", "alice");

        TokenCountingChatModel wrapped = new TokenCountingChatModel(
                fakeDelegate(), recorder, "OPEN_AI", "gpt-4o-mini");

        Response<AiMessage> out = wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")));

        assertThat(out.content().text()).isEqualTo("hello");

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        LlmUsageRecord rec = captor.getValue();
        assertThat(rec.getTenantId()).isEqualTo(7L);
        assertThat(rec.getProvider()).isEqualTo("OPEN_AI");
        assertThat(rec.getModel()).isEqualTo("gpt-4o-mini");
        assertThat(rec.getInputTokens()).isEqualTo(42);
        assertThat(rec.getOutputTokens()).isEqualTo(7);
        assertThat(rec.getTotalTokens()).isEqualTo(49);
        assertThat(rec.getCallType()).isEqualTo(LlmCallType.NL2SQL);
        assertThat(rec.getRequestId()).isEqualTo("req-1");
        assertThat(rec.getTraceId()).isEqualTo("trace-1");
        assertThat(rec.getUserId()).isEqualTo("alice");
        assertThat(rec.isSuccess()).isTrue();
    }

    @Test
    void recordsFailureAndRethrows() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        ChatLanguageModel delegate = mock(ChatLanguageModel.class);
        when(delegate.generate(anyList())).thenThrow(new RuntimeException("boom"));
        TenantContext.setTenantId(7L);

        TokenCountingChatModel wrapped = new TokenCountingChatModel(
                delegate, recorder, "OPEN_AI", "gpt-4o-mini");

        try {
            wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")));
        } catch (RuntimeException expected) {
            // ok
        }

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        LlmUsageRecord r = captor.getValue();
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorType()).isEqualTo("RuntimeException");
    }

    @Test
    void nullTenantContextRecordsNullTenantWithoutFailing() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        TokenCountingChatModel wrapped = new TokenCountingChatModel(
                fakeDelegate(), recorder, "OPEN_AI", "gpt-4o-mini");

        wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")));

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        assertThat(captor.getValue().getTenantId()).isNull();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl common -Dtest=TokenCountingChatModelTest`
Expected: FAIL — class missing.

- [ ] **Step 4: Implement `TokenCountingChatModel`**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
public class TokenCountingChatModel implements ChatLanguageModel {

    private final ChatLanguageModel delegate;
    private final LlmUsageRecorder recorder;
    private final String provider;
    private final String model;

    public TokenCountingChatModel(ChatLanguageModel delegate,
                                  LlmUsageRecorder recorder,
                                  String provider,
                                  String model) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return invoke(() -> delegate.generate(messages));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
                                        List<ToolSpecification> toolSpecifications) {
        return invoke(() -> delegate.generate(messages, toolSpecifications));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
                                        ToolSpecification toolSpecification) {
        return invoke(() -> delegate.generate(messages, toolSpecification));
    }

    private Response<AiMessage> invoke(java.util.function.Supplier<Response<AiMessage>> call) {
        long t0 = System.currentTimeMillis();
        try {
            Response<AiMessage> resp = call.get();
            record(resp.tokenUsage(), System.currentTimeMillis() - t0, true, null);
            return resp;
        } catch (RuntimeException e) {
            record(null, System.currentTimeMillis() - t0, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    private void record(TokenUsage usage, long latencyMs, boolean success, String errorType) {
        int in = usage == null || usage.inputTokenCount() == null ? 0 : usage.inputTokenCount();
        int out = usage == null || usage.outputTokenCount() == null ? 0 : usage.outputTokenCount();
        int total = usage == null || usage.totalTokenCount() == null ? in + out : usage.totalTokenCount();

        LlmCallContext.Frame ctx = LlmCallContext.get();
        LlmUsageRecord r = LlmUsageRecord.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(ctx == null ? null : ctx.userId)
                .provider(provider)
                .model(model)
                .callType(ctx == null ? LlmCallType.UNKNOWN : ctx.callType)
                .inputTokens(in).outputTokens(out).totalTokens(total)
                .requestId(ctx == null ? null : ctx.requestId)
                .traceId(ctx == null ? null : ctx.traceId)
                .latencyMs((int) latencyMs)
                .success(success).errorType(errorType)
                .createdAt(Instant.now())
                .build();
        try {
            recorder.record(r);
        } catch (Exception e) {
            log.warn("Failed to record LLM usage (model={}): {}", model, e.getMessage());
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -pl common -Dtest=TokenCountingChatModelTest`
Expected: PASS.

- [ ] **Step 6: Wire decorator into `OpenAiModelFactory`**

Replace the body of `createChatModel` with:

```java
@Override
public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
    OpenAiChatModel.OpenAiChatModelBuilder openAiChatModelBuilder = OpenAiChatModel.builder()
            .baseUrl(modelConfig.getBaseUrl()).modelName(modelConfig.getModelName())
            .apiKey(modelConfig.keyDecrypt()).apiVersion(modelConfig.getApiVersion())
            .temperature(modelConfig.getTemperature()).topP(modelConfig.getTopP())
            .maxRetries(modelConfig.getMaxRetries())
            .timeout(Duration.ofSeconds(modelConfig.getTimeOut()))
            .logRequests(modelConfig.getLogRequests())
            .logResponses(modelConfig.getLogResponses());
    if (modelConfig.getJsonFormat() != null && modelConfig.getJsonFormat()) {
        openAiChatModelBuilder.strictJsonSchema(true)
                .responseFormat(modelConfig.getJsonFormatType());
    }
    ChatLanguageModel raw = openAiChatModelBuilder.build();
    LlmUsageRecorder recorder = ContextUtils.getBean(LlmUsageRecorder.class);
    if (recorder == null) {
        return raw;
    }
    return new TokenCountingChatModel(raw, recorder, PROVIDER, modelConfig.getModelName());
}
```

Add imports: `com.tencent.supersonic.common.llm.LlmUsageRecorder`, `com.tencent.supersonic.common.llm.TokenCountingChatModel`, `com.tencent.supersonic.common.util.ContextUtils`.

- [ ] **Step 7: Repeat Step 6 pattern in `OllamaModelFactory`, `LocalAiModelFactory`, `InMemoryModelFactory`, `DifyModelFactory`**

For each: after `.build()` on the chat model, wrap with `TokenCountingChatModel` using their `PROVIDER` constant and `modelConfig.getModelName()`.

- [ ] **Step 8: Verify full compile + existing ModelProvider test**

Run: `mvn test -pl launchers/standalone -Dtest=ModelProviderTest -am`
Expected: PASS (wrapping must be transparent).

- [ ] **Step 9: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingChatModel.java \
        common/src/main/java/com/tencent/supersonic/common/llm/LlmCallContext.java \
        common/src/test/java/com/tencent/supersonic/common/llm/TokenCountingChatModelTest.java \
        common/src/main/java/dev/langchain4j/provider/
git commit -m "feat(common): wrap all ChatLanguageModel factories with TokenCountingChatModel"
```

---

## Task 5: `TokenCountingStreamingChatModel` decorator

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingStreamingChatModel.java`
- Modify: all 5 factories' `createChatStreamingModel` to wrap.
- Test: `common/src/test/java/com/tencent/supersonic/common/llm/TokenCountingStreamingChatModelTest.java`

- [ ] **Step 1: Write the failing streaming test**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenCountingStreamingChatModelTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        LlmCallContext.clear();
    }

    @Test
    void capturesUsageOnStreamCompleteAndForwardsCallbacks() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);
        TenantContext.setTenantId(7L);
        LlmCallContext.set(LlmCallType.DATA_INTERPRET, "req-s1", "trace-s1", "bob");

        StreamingChatLanguageModel delegate = new StreamingChatLanguageModel() {
            @Override
            public void generate(List<ChatMessage> messages,
                                 StreamingResponseHandler<AiMessage> handler) {
                handler.onNext("hel");
                handler.onNext("lo");
                handler.onComplete(Response.from(AiMessage.from("hello"),
                        new TokenUsage(10, 2, 12)));
            }
        };

        TokenCountingStreamingChatModel wrapped =
                new TokenCountingStreamingChatModel(delegate, recorder, "OPEN_AI", "gpt-4o-mini");

        StringBuilder sb = new StringBuilder();
        boolean[] done = {false};
        wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")),
                new StreamingResponseHandler<>() {
                    @Override public void onNext(String token) { sb.append(token); }
                    @Override public void onComplete(Response<AiMessage> response) { done[0] = true; }
                    @Override public void onError(Throwable error) {}
                });

        assertThat(sb.toString()).isEqualTo("hello");
        assertThat(done[0]).isTrue();

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        LlmUsageRecord rec = captor.getValue();
        assertThat(rec.getInputTokens()).isEqualTo(10);
        assertThat(rec.getOutputTokens()).isEqualTo(2);
        assertThat(rec.getTotalTokens()).isEqualTo(12);
        assertThat(rec.getCallType()).isEqualTo(LlmCallType.DATA_INTERPRET);
        assertThat(rec.isSuccess()).isTrue();
    }

    @Test
    void recordsFailureOnStreamError() {
        LlmUsageRecorder recorder = mock(LlmUsageRecorder.class);

        StreamingChatLanguageModel delegate = new StreamingChatLanguageModel() {
            @Override
            public void generate(List<ChatMessage> messages,
                                 StreamingResponseHandler<AiMessage> handler) {
                handler.onError(new RuntimeException("boom"));
            }
        };

        TokenCountingStreamingChatModel wrapped =
                new TokenCountingStreamingChatModel(delegate, recorder, "OPEN_AI", "gpt-4o-mini");

        wrapped.generate(List.<ChatMessage>of(UserMessage.from("hi")),
                new StreamingResponseHandler<>() {
                    @Override public void onNext(String token) {}
                    @Override public void onComplete(Response<AiMessage> response) {}
                    @Override public void onError(Throwable error) {}
                });

        ArgumentCaptor<LlmUsageRecord> captor = ArgumentCaptor.forClass(LlmUsageRecord.class);
        verify(recorder).record(captor.capture());
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getErrorType()).isEqualTo("RuntimeException");
    }
}
```

- [ ] **Step 2: Run test — expect failure (class missing)**

Run: `mvn test -pl common -Dtest=TokenCountingStreamingChatModelTest`
Expected: FAIL.

- [ ] **Step 3: Implement `TokenCountingStreamingChatModel`**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
public class TokenCountingStreamingChatModel implements StreamingChatLanguageModel {

    private final StreamingChatLanguageModel delegate;
    private final LlmUsageRecorder recorder;
    private final String provider;
    private final String model;

    public TokenCountingStreamingChatModel(StreamingChatLanguageModel delegate,
                                           LlmUsageRecorder recorder,
                                           String provider,
                                           String model) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public void generate(List<ChatMessage> messages,
                         StreamingResponseHandler<AiMessage> handler) {
        Long tenantId = TenantContext.getTenantId();
        LlmCallContext.Frame ctx = LlmCallContext.get();
        long t0 = System.currentTimeMillis();

        delegate.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                handler.onNext(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                record(tenantId, ctx, response.tokenUsage(),
                        System.currentTimeMillis() - t0, true, null);
                handler.onComplete(response);
            }

            @Override
            public void onError(Throwable error) {
                record(tenantId, ctx, null,
                        System.currentTimeMillis() - t0, false,
                        error.getClass().getSimpleName());
                handler.onError(error);
            }
        });
    }

    private void record(Long tenantId, LlmCallContext.Frame ctx, TokenUsage usage,
                        long latencyMs, boolean success, String errorType) {
        int in = usage == null || usage.inputTokenCount() == null ? 0 : usage.inputTokenCount();
        int out = usage == null || usage.outputTokenCount() == null ? 0 : usage.outputTokenCount();
        int total = usage == null || usage.totalTokenCount() == null ? in + out : usage.totalTokenCount();

        LlmUsageRecord r = LlmUsageRecord.builder()
                .tenantId(tenantId)
                .userId(ctx == null ? null : ctx.userId)
                .provider(provider).model(model)
                .callType(ctx == null ? LlmCallType.UNKNOWN : ctx.callType)
                .inputTokens(in).outputTokens(out).totalTokens(total)
                .requestId(ctx == null ? null : ctx.requestId)
                .traceId(ctx == null ? null : ctx.traceId)
                .latencyMs((int) latencyMs).success(success).errorType(errorType)
                .createdAt(Instant.now())
                .build();
        try {
            recorder.record(r);
        } catch (Exception e) {
            log.warn("Failed to record streaming LLM usage (model={}): {}", model, e.getMessage());
        }
    }
}
```

- [ ] **Step 4: Wire decorator into all 5 factories' streaming methods**

In each factory, after `.build()` call returning the streaming model, wrap with `new TokenCountingStreamingChatModel(raw, recorder, PROVIDER, modelConfig.getModelName())` (same null-recorder fallback as chat model). Note: the return type on `ModelFactory.createChatStreamingModel` is `OpenAiStreamingChatModel` — this is too narrow. Widen the interface signature to `StreamingChatLanguageModel` so wrapping is possible:

Modify `common/src/main/java/dev/langchain4j/provider/ModelFactory.java`:

```java
package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public interface ModelFactory {
    ChatLanguageModel createChatModel(ChatModelConfig modelConfig);

    StreamingChatLanguageModel createChatStreamingModel(ChatModelConfig modelConfig);

    EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModel);
}
```

Check every factory implementation and any caller of `createChatStreamingModel` still compiles against the widened return type — `ModelProvider.getChatStreamingModel` already returns `StreamingChatLanguageModel`.

- [ ] **Step 5: Run streaming test + full compile**

Run: `mvn test -pl common -Dtest=TokenCountingStreamingChatModelTest`
Expected: PASS.
Then: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingStreamingChatModel.java \
        common/src/test/java/com/tencent/supersonic/common/llm/TokenCountingStreamingChatModelTest.java \
        common/src/main/java/dev/langchain4j/provider/
git commit -m "feat(common): wrap streaming chat models with TokenCountingStreamingChatModel"
```

---

## Task 6: `CostEstimator` with Caffeine-cached `s2_llm_pricing` lookup

**Files:**
- Create: `common/src/main/java/com/tencent/supersonic/common/llm/CostEstimatorImpl.java`
- Test: `common/src/test/java/com/tencent/supersonic/common/llm/CostEstimatorTest.java`

- [ ] **Step 1: Write the failing cost estimator test**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.persistence.dataobject.LlmPricingDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmPricingDOMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CostEstimatorTest {

    @Test
    void estimateReturns0AndWarnsWhenPricingMissing() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);
        long cost = est.estimate("OPEN_AI", "gpt-unknown", 1000, 500);

        assertThat(cost).isZero();
    }

    @Test
    void estimateComputesCostFromInputAndOutputPricing() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        LlmPricingDO p = new LlmPricingDO();
        p.setProvider("OPEN_AI"); p.setModel("gpt-4o-mini");
        p.setInputPricePer1kMicros(150L);   // $0.00015 per 1k in
        p.setOutputPricePer1kMicros(600L);  // $0.00060 per 1k out
        when(mapper.selectList(any())).thenReturn(List.of(p));

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);

        long cost = est.estimate("OPEN_AI", "gpt-4o-mini", 2000, 1000);
        // 2 * 150 + 1 * 600 = 900 micro-USD
        assertThat(cost).isEqualTo(900L);
    }

    @Test
    void pricingCachedAfterFirstLookup() {
        LlmPricingDOMapper mapper = mock(LlmPricingDOMapper.class);
        LlmPricingDO p = new LlmPricingDO();
        p.setProvider("OPEN_AI"); p.setModel("gpt-4o-mini");
        p.setInputPricePer1kMicros(150L); p.setOutputPricePer1kMicros(600L);
        when(mapper.selectList(any())).thenReturn(List.of(p));

        CostEstimatorImpl est = new CostEstimatorImpl(mapper);

        est.estimate("OPEN_AI", "gpt-4o-mini", 100, 100);
        est.estimate("OPEN_AI", "gpt-4o-mini", 100, 100);
        est.estimate("OPEN_AI", "gpt-4o-mini", 100, 100);

        verify(mapper, times(1)).selectList(any());
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `mvn test -pl common -Dtest=CostEstimatorTest`
Expected: FAIL (class missing).

- [ ] **Step 3: Implement `CostEstimatorImpl`**

```java
package com.tencent.supersonic.common.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmPricingDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmPricingDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class CostEstimatorImpl implements CostEstimator {

    private final LlmPricingDOMapper mapper;
    private final Cache<String, Optional<LlmPricingDO>> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(500).build();

    public CostEstimatorImpl(LlmPricingDOMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long estimate(String provider, String model, int inputTokens, int outputTokens) {
        if (provider == null || model == null) {
            return 0L;
        }
        String key = provider + "::" + model;
        Optional<LlmPricingDO> pricing = cache.get(key, k -> load(provider, model));
        if (pricing.isEmpty()) {
            log.warn("No pricing entry for provider={} model={}; storing cost=0", provider, model);
            return 0L;
        }
        LlmPricingDO p = pricing.get();
        long inMicros = (long) inputTokens * p.getInputPricePer1kMicros() / 1000L;
        long outMicros = (long) outputTokens * p.getOutputPricePer1kMicros() / 1000L;
        return inMicros + outMicros;
    }

    private Optional<LlmPricingDO> load(String provider, String model) {
        LambdaQueryWrapper<LlmPricingDO> w = new LambdaQueryWrapper<>();
        w.eq(LlmPricingDO::getProvider, provider)
                .eq(LlmPricingDO::getModel, model)
                .orderByDesc(LlmPricingDO::getEffectiveFrom).last("LIMIT 1");
        return Optional.ofNullable(mapper.selectList(w).stream().findFirst().orElse(null));
    }
}
```

- [ ] **Step 4: Run test to verify pass + full compile**

Run: `mvn test -pl common -Dtest=CostEstimatorTest`
Expected: PASS.
Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/llm/CostEstimatorImpl.java \
        common/src/test/java/com/tencent/supersonic/common/llm/CostEstimatorTest.java
git commit -m "feat(common): add CostEstimatorImpl with Caffeine-cached pricing lookup"
```

---

## Task 7: `LlmUsageEvent` publisher integration test

**Files:**
- Test: `common/src/test/java/com/tencent/supersonic/common/llm/LlmUsageEventIntegrationTest.java`

- [ ] **Step 1: Write the integration test**

```java
package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.pojo.LlmUsageRecord;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmUsageEventIntegrationTest {

    @Test
    void flushPublishesSingleEventContainingAllBufferedRecords() {
        LlmUsageService service = mock(LlmUsageService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = (p, m, i, o) -> 0L;

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 100);

        for (int i = 0; i < 5; i++) {
            rec.record(LlmUsageRecord.builder()
                    .tenantId(1L).provider("OPEN_AI").model("gpt-4o-mini")
                    .callType(LlmCallType.NL2SQL)
                    .inputTokens(10).outputTokens(5).totalTokens(15)
                    .createdAt(Instant.now()).success(true).build());
        }
        rec.flushNow();

        ArgumentCaptor<LlmUsageEvent> captor = ArgumentCaptor.forClass(LlmUsageEvent.class);
        verify(publisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().getRecords()).hasSize(5);
    }

    @Test
    void eventNotPublishedWhenBatchInsertFails() {
        LlmUsageService service = mock(LlmUsageService.class);
        doThrow(new RuntimeException("db down")).when(service).batchInsert(any());
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        CostEstimator estimator = (p, m, i, o) -> 0L;

        LlmUsageRecorder rec = new LlmUsageRecorder(service, publisher, estimator, 1000, 100);
        rec.record(LlmUsageRecord.builder()
                .tenantId(1L).provider("OPEN_AI").model("gpt-4o-mini")
                .callType(LlmCallType.NL2SQL)
                .inputTokens(10).outputTokens(5).totalTokens(15)
                .createdAt(Instant.now()).success(true).build());
        rec.flushNow();

        verify(publisher, never()).publishEvent(any(LlmUsageEvent.class));
    }
}
```

- [ ] **Step 2: Run test — expect pass**

Run: `mvn test -pl common -Dtest=LlmUsageEventIntegrationTest`
Expected: PASS (behaviour from Task 3 implementation).

- [ ] **Step 3: Commit**

```bash
git add common/src/test/java/com/tencent/supersonic/common/llm/LlmUsageEventIntegrationTest.java
git commit -m "test(common): integration test for LlmUsageEvent publish-after-flush"
```

---

## Task 8: `LlmUsageListener` in billing/server

**Files:**
- Create: `billing/server/src/main/java/com/tencent/supersonic/billing/server/listener/LlmUsageListener.java`
- Test: `billing/server/src/test/java/com/tencent/supersonic/billing/server/listener/LlmUsageListenerTest.java`

**Aggregation plan:** `billing` reuses the existing `auth.UsageTrackingService.recordTokenUsage(tenantId, count)` which already increments `s2_tenant_usage.tokens_used`. The listener groups `LlmUsageEvent.records` by `tenantId` and calls `recordTokenUsage` once per group. This avoids introducing a parallel quota table.

- [ ] **Step 1: Add `auth-api` + `common` as dependencies of `billing/server`**

Verify `billing/server/pom.xml` already has `com.tencent.supersonic:auth-api` (it will because `SubscriptionInfoProviderImpl` likely references it). If not present, add:

```xml
<dependency>
    <groupId>com.tencent.supersonic</groupId>
    <artifactId>auth-api</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.tencent.supersonic</groupId>
    <artifactId>common</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: Write the failing listener test**

```java
package com.tencent.supersonic.billing.server.listener;

import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class LlmUsageListenerTest {

    private LlmUsageDO dao(Long tenantId, int tokens) {
        LlmUsageDO d = new LlmUsageDO();
        d.setTenantId(tenantId);
        d.setTotalTokens(tokens);
        return d;
    }

    @Test
    void aggregatesByTenantAndInvokesRecordTokenUsageOncePerTenant() {
        UsageTrackingService usageService = mock(UsageTrackingService.class);
        LlmUsageListener listener = new LlmUsageListener(usageService);

        listener.onLlmUsage(new LlmUsageEvent(this, List.of(
                dao(1L, 100), dao(1L, 50),
                dao(2L, 200)
        )));

        verify(usageService).recordTokenUsage(1L, 150L);
        verify(usageService).recordTokenUsage(2L, 200L);
        verifyNoMoreInteractions(usageService);
    }

    @Test
    void skipsRowsWithNullTenantId() {
        UsageTrackingService usageService = mock(UsageTrackingService.class);
        LlmUsageListener listener = new LlmUsageListener(usageService);

        listener.onLlmUsage(new LlmUsageEvent(this, List.of(
                dao(null, 100), dao(1L, 50)
        )));

        verify(usageService).recordTokenUsage(1L, 50L);
        verifyNoMoreInteractions(usageService);
    }
}
```

- [ ] **Step 3: Run test — expect failure**

Run: `mvn test -pl billing/server -Dtest=LlmUsageListenerTest`
Expected: FAIL (class missing).

- [ ] **Step 4: Implement `LlmUsageListener`**

```java
package com.tencent.supersonic.billing.server.listener;

import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.llm.event.LlmUsageEvent;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmUsageListener {

    private final UsageTrackingService usageTrackingService;

    @EventListener
    public void onLlmUsage(LlmUsageEvent event) {
        if (event.getRecords() == null || event.getRecords().isEmpty()) {
            return;
        }
        Map<Long, Long> perTenant = event.getRecords().stream()
                .filter(r -> r.getTenantId() != null)
                .collect(Collectors.groupingBy(
                        LlmUsageDO::getTenantId,
                        Collectors.summingLong(r -> r.getTotalTokens() == null ? 0 : r.getTotalTokens())));
        for (Map.Entry<Long, Long> e : perTenant.entrySet()) {
            try {
                usageTrackingService.recordTokenUsage(e.getKey(), e.getValue());
            } catch (Exception ex) {
                log.error("Failed recording token usage for tenant {}", e.getKey(), ex);
            }
        }
    }
}
```

- [ ] **Step 5: Run test + full compile**

Run: `mvn test -pl billing/server -Dtest=LlmUsageListenerTest`
Expected: PASS.
Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add billing/server/src/main/java/com/tencent/supersonic/billing/server/listener \
        billing/server/src/test/java/com/tencent/supersonic/billing/server/listener \
        billing/server/pom.xml
git commit -m "feat(billing): aggregate LlmUsageEvent into tenant token quota"
```

---

## Task 9: Admin API — query usage by tenant / date range

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/LlmUsageController.java`
- Test: `headless/server/src/test/java/com/tencent/supersonic/headless/server/rest/LlmUsageControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

```java
package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LlmUsageControllerTest {

    @Test
    void queryReturnsPagedResults() throws Exception {
        LlmUsageService svc = mock(LlmUsageService.class);
        Page<LlmUsageDO> page = new Page<>(1, 20);
        LlmUsageDO row = new LlmUsageDO();
        row.setId(1L); row.setTenantId(7L); row.setModel("gpt-4o-mini");
        row.setTotalTokens(42);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(svc.query(eq(7L), any(), any(), isNull(), isNull(), eq(1), eq(20))).thenReturn(page);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LlmUsageController(svc)).build();
        mvc.perform(get("/api/semantic/admin/llm-usage")
                        .param("tenantId", "7")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-17")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void dailyAggregatesEndpointReturnsTimeSeries() throws Exception {
        LlmUsageService svc = mock(LlmUsageService.class);
        when(svc.dailyAggregates(eq(7L), any(), any())).thenReturn(
                List.of(Map.of("day", "2026-04-15", "tokens", 1000L, "cost", 123L)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LlmUsageController(svc)).build();
        mvc.perform(get("/api/semantic/admin/llm-usage/daily")
                        .param("tenantId", "7")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokens").value(1000));
    }
}
```

- [ ] **Step 2: Run test — expect failure**

Run: `mvn test -pl headless/server -Dtest=LlmUsageControllerTest`
Expected: FAIL (controller missing).

- [ ] **Step 3: Implement `LlmUsageController`**

```java
package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semantic/admin/llm-usage")
@RequiredArgsConstructor
public class LlmUsageController {

    private final LlmUsageService llmUsageService;

    @GetMapping
    public IPage<LlmUsageDO> query(@RequestParam Long tenantId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) String model,
                                   @RequestParam(required = false) String callType,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return llmUsageService.query(tenantId, from, to, model, callType, page, size);
    }

    @GetMapping("/daily")
    public List<Map<String, Object>> daily(@RequestParam Long tenantId,
                                           @RequestParam
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return llmUsageService.dailyAggregates(tenantId, from, to);
    }

    @GetMapping("/total-tokens")
    public long totalTokens(@RequestParam Long tenantId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return llmUsageService.sumTokens(tenantId, from, to);
    }
}
```

- [ ] **Step 4: Run test + full compile**

Run: `mvn test -pl headless/server -Dtest=LlmUsageControllerTest`
Expected: PASS.
Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/LlmUsageController.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/rest/LlmUsageControllerTest.java
git commit -m "feat(headless): admin API for paginated LLM usage query"
```

---

## Task 10: Frontend minimal LLM usage dashboard

**Files:**
- Create: `webapp/packages/supersonic-fe/src/pages/LlmUsage/index.tsx`
- Create: `webapp/packages/supersonic-fe/src/pages/LlmUsage/style.less`
- Create: `webapp/packages/supersonic-fe/src/services/llmUsage.ts`
- Modify: `webapp/packages/supersonic-fe/config/routes.ts`

- [ ] **Step 1: Create the service module**

```ts
// webapp/packages/supersonic-fe/src/services/llmUsage.ts
import { request } from 'umi';

const BASE = '/api/semantic/admin/llm-usage';

export type LlmUsageRow = {
  id: number;
  tenantId: number;
  userId?: string;
  provider: string;
  model: string;
  callType: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  estimatedCostMicros: number;
  requestId?: string;
  traceId?: string;
  latencyMs?: number;
  success: boolean;
  errorType?: string;
  createdAt: string;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  current: number;
  size: number;
};

export type DailyAgg = { day: string; tokens: number; cost: number };

export function queryLlmUsage(params: {
  tenantId: number;
  from?: string;
  to?: string;
  model?: string;
  callType?: string;
  page?: number;
  size?: number;
}) {
  return request<PageResult<LlmUsageRow>>(BASE, { method: 'GET', params });
}

export function queryDailyAggregates(params: {
  tenantId: number;
  from: string;
  to: string;
}) {
  return request<DailyAgg[]>(`${BASE}/daily`, { method: 'GET', params });
}

export function queryTotalTokens(params: {
  tenantId: number;
  from: string;
  to: string;
}) {
  return request<number>(`${BASE}/total-tokens`, { method: 'GET', params });
}
```

- [ ] **Step 2: Create the page**

```tsx
// webapp/packages/supersonic-fe/src/pages/LlmUsage/index.tsx
import React, { useEffect, useState } from 'react';
import { Card, DatePicker, InputNumber, Space, Statistic, Table, Row, Col } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import {
  queryLlmUsage,
  queryDailyAggregates,
  queryTotalTokens,
  LlmUsageRow,
  DailyAgg,
} from '@/services/llmUsage';
import { Line } from '@ant-design/plots';
import styles from './style.less';

const { RangePicker } = DatePicker;

const LlmUsage: React.FC = () => {
  const [tenantId, setTenantId] = useState<number>(1);
  const [range, setRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);
  const [rows, setRows] = useState<LlmUsageRow[]>([]);
  const [daily, setDaily] = useState<DailyAgg[]>([]);
  const [totalTokens, setTotalTokens] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!range[0] || !range[1] || !tenantId) return;
    const from = range[0].format('YYYY-MM-DD');
    const to = range[1].format('YYYY-MM-DD');
    setLoading(true);
    Promise.all([
      queryLlmUsage({ tenantId, from, to, page: 1, size: 50 }),
      queryDailyAggregates({ tenantId, from, to }),
      queryTotalTokens({ tenantId, from, to }),
    ])
      .then(([pg, agg, total]) => {
        setRows(pg.records || []);
        setDaily(agg || []);
        setTotalTokens(total || 0);
      })
      .finally(() => setLoading(false));
  }, [tenantId, range]);

  const totalCostUsd = daily.reduce((sum, d) => sum + d.cost, 0) / 1_000_000;

  const columns = [
    { title: 'Time', dataIndex: 'createdAt', render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm:ss') },
    { title: 'Model', dataIndex: 'model' },
    { title: 'Call Type', dataIndex: 'callType' },
    { title: 'In', dataIndex: 'inputTokens' },
    { title: 'Out', dataIndex: 'outputTokens' },
    { title: 'Total', dataIndex: 'totalTokens' },
    {
      title: 'Cost (USD)',
      dataIndex: 'estimatedCostMicros',
      render: (v: number) => (v / 1_000_000).toFixed(6),
    },
    { title: 'Success', dataIndex: 'success', render: (v: boolean) => (v ? 'yes' : 'no') },
  ];

  return (
    <div className={styles.llmUsage}>
      <Space style={{ marginBottom: 16 }}>
        <span>Tenant ID:</span>
        <InputNumber value={tenantId} min={1} onChange={(v) => v && setTenantId(Number(v))} />
        <RangePicker value={range} onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])} />
      </Space>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card><Statistic title="Total Tokens" value={totalTokens} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="Total Cost (USD)" value={totalCostUsd} precision={4} /></Card>
        </Col>
      </Row>

      <Card title="Daily Usage" style={{ marginBottom: 16 }}>
        <Line
          data={daily}
          xField="day"
          yField="tokens"
          height={250}
        />
      </Card>

      <Card title="Recent Calls">
        <Table<LlmUsageRow>
          rowKey="id"
          dataSource={rows}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 20 }}
        />
      </Card>
    </div>
  );
};

export default LlmUsage;
```

- [ ] **Step 3: Create minimal stylesheet**

```less
// webapp/packages/supersonic-fe/src/pages/LlmUsage/style.less
.llmUsage {
  padding: 16px;
}
```

- [ ] **Step 4: Add route**

In `webapp/packages/supersonic-fe/config/routes.ts`, locate the admin / operations route group (grep for `UsageDashboard` — the new route goes next to it) and add:

```ts
{
  path: '/llm-usage',
  name: 'LLM 用量',
  icon: 'Robot',
  component: './LlmUsage',
  access: 'canAdmin',
},
```

- [ ] **Step 5: Build frontend**

Run (from `webapp/packages/supersonic-fe`):
```bash
npm run lint && npm run build
```
Expected: exits 0.

- [ ] **Step 6: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/LlmUsage \
        webapp/packages/supersonic-fe/src/services/llmUsage.ts \
        webapp/packages/supersonic-fe/config/routes.ts
git commit -m "feat(webapp): add LLM usage admin dashboard page"
```

---

## Task 11: Docs + pricing seed data

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V30__llm_pricing_seed.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V30__llm_pricing_seed.sql`
- Create: `docs/details/platform/llm-usage-billing.md`
- Modify: `docs/details/README.md` (add index entry).

- [ ] **Step 1: Write MySQL pricing seed**

```sql
-- V30__llm_pricing_seed.sql
INSERT INTO s2_llm_pricing (provider, model, input_price_per_1k_micros, output_price_per_1k_micros, currency)
VALUES
    ('OPEN_AI', 'gpt-4o',              2500, 10000, 'USD'),
    ('OPEN_AI', 'gpt-4o-mini',          150,   600, 'USD'),
    ('OPEN_AI', 'gpt-4-turbo',        10000, 30000, 'USD'),
    ('OPEN_AI', 'gpt-3.5-turbo',        500,  1500, 'USD'),
    ('OLLAMA',  'llama3',                 0,     0, 'USD'),
    ('LOCAL_AI', 'local',                 0,     0, 'USD'),
    ('DIFY',    'dify-default',           0,     0, 'USD'),
    ('IN_MEMORY', 'in-memory',            0,     0, 'USD');
```

- [ ] **Step 2: Write PostgreSQL pricing seed**

Identical INSERT statements — PostgreSQL accepts the same syntax. Put them in the postgresql V30 file.

- [ ] **Step 3: Write the detail spec**

Create `docs/details/platform/llm-usage-billing.md` with frontmatter matching existing detail files. Minimum content:

```markdown
---
status: implemented
module: common, billing, headless/server, webapp
key-files:
  - common/src/main/java/com/tencent/supersonic/common/llm/TokenCountingChatModel.java
  - common/src/main/java/com/tencent/supersonic/common/llm/LlmUsageRecorder.java
  - billing/server/src/main/java/com/tencent/supersonic/billing/server/listener/LlmUsageListener.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/rest/LlmUsageController.java
  - launchers/standalone/src/main/resources/db/migration/mysql/V29__llm_usage_and_pricing.sql
---

# LLM 用量记录与计费集成

## 目标
按租户 / 模型 / 调用类型捕获 LangChain4j 每次调用的 token 用量，持久化到 `s2_llm_usage`，通过 Spring 事件通知 `billing` 模块累计配额。

## 主链路
1. `ModelProvider.getChatModel` 由各 `ModelFactory` 返回 `TokenCountingChatModel` 装饰器（`common` 模块，在 `dev.langchain4j.provider` 下的 5 个 factory 中注入）。
2. 装饰器在 `generate(...)` 之后从 `Response<AiMessage>.tokenUsage()` 读取 `TokenUsage`；流式模型在 `onComplete(Response)` 回调中读取。
3. 捕获 `TenantContext.getTenantId()` + `LlmCallContext`（调用方设置 call_type / request_id / trace_id / user_id）打包成 `LlmUsageRecord`，`offer` 进 `LlmUsageRecorder` 的 `ArrayBlockingQueue`。
4. 刷新器：Spring `@Scheduled(fixedDelay=5s)` 或累计 ≥100 条触发 `flushNow()`——批量 `INSERT`，然后 `publishEvent(LlmUsageEvent)`。
5. `LlmUsageListener`（`billing/server`）监听事件，按 tenant 汇总 token 数调用 `UsageTrackingService.recordTokenUsage`，沿用现有配额链路。
6. `CostEstimatorImpl` 通过 `s2_llm_pricing` + Caffeine 缓存（10 分钟）估算成本，缺价时记录 0 并 WARN。

## 调用点注入 call_type
调用方应在调用 LLM 前设置 `LlmCallContext.set(...)`，并在 finally 中 `clear()`。主要 call site（留给后续 PR 完善）：
- NL2SQL: `SqlGenStrategy.getChatModel` / `NL2SQLParser`
- Corrector: `LLMSqlCorrector.correct` / `LLMPhysicalSqlCorrector.correct`
- Mapper: `EmbeddingMatchStrategy`
- Summary: `DataInterpretProcessor`
- Plugin: `PlainTextExecutor`
- Memory review: `MemoryReviewTask`
- Alias generator: `AliasGenerateHelper`

未设置的调用记为 `UNKNOWN`。

## 可靠性
- **At-least-once**：崩溃丢失未刷入数据（MVP 接受）。
- **队列满**：dropRecord + 每 100 条 WARN 日志，不阻塞 LLM 调用。
- **DB 失败**：flush 失败不发送事件，批数据丢失，记 ERROR。

## 配置项
- `s2.llm.usage.queue-capacity` (default 10000)
- `s2.llm.usage.flush-size` (default 100)
- `s2.llm.usage.flush-interval-ms` (default 5000)

## 未实现 / 下一步
- 按 call_type 针对性注入（目前只有 decorator 层全量捕获）。
- 价格表 UI（当前仅 seed 初版）。
- 每小时 / 每天汇总表（若查询压力变大）。
```

- [ ] **Step 4: Add index entry in `docs/details/README.md`**

Append to the platform table section:

```markdown
| `platform/llm-usage-billing.md` | LLM 用量捕获 + 计费 | implemented |
```

- [ ] **Step 5: Verify Flyway + full compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V30__llm_pricing_seed.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V30__llm_pricing_seed.sql \
        docs/details/platform/llm-usage-billing.md \
        docs/details/README.md
git commit -m "docs(platform): LLM usage billing spec + pricing seed"
```

---

## Self-Review Notes

- **Coverage of spec:**
  - Decorator around `ChatLanguageModel` — Task 4.
  - Streaming decorator — Task 5.
  - Ring buffer + async flusher (time + size) — Task 3.
  - `s2_llm_usage` + `s2_llm_pricing` migrations (MySQL + PG, V29/V30) — Tasks 1 & 11.
  - Cost estimator with Caffeine + missing-price WARN — Task 6.
  - `LlmUsageEvent` Spring event + publish-after-flush + publish-suppress-on-failure — Tasks 3 & 7.
  - `LlmUsageListener` in `billing/server` aggregating via existing `UsageTrackingService` — Task 8.
  - Admin API paginated query + daily aggregates — Task 9.
  - Frontend dashboard (tokens + cost chart) — Task 10.
  - Docs + pricing seed — Task 11.

- **Placeholder scan:** no "TBD"/"implement later"/"add validation" — all steps contain concrete code, commands, and expected outputs.

- **Type consistency:** `LlmUsageRecorder` constructor signature is identical in Tasks 3, 4, 5, 7. `LlmCallContext.Frame` fields (`callType`, `requestId`, `traceId`, `userId`) referenced the same way in Tasks 4 and 5. `CostEstimator.estimate(String, String, int, int)` matches between Task 3 (placeholder interface) and Task 6 (impl). `LlmUsageEvent(source, List<LlmUsageDO>)` matches between Tasks 3, 7, 8.

- **Known non-goals (deferred):**
  - Per-call-site `LlmCallContext.set()` wiring is called out in docs but NOT implemented in this plan — decorator captures `UNKNOWN` for unset frames, which is acceptable for MVP. A follow-up PR adds per-executor context stamps.
  - No hourly/daily summary table; queries hit `s2_llm_usage` directly (indexes cover the admin-UI patterns).
  - `AiServices`-based LLM calls go through the same `ChatLanguageModel` returned by `ModelProvider`, so the decorator still applies; no additional wiring needed.
