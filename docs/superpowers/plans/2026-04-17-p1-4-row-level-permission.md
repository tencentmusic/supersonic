# P1-4: Row-Level Permission Push-Down via SqlCorrector Chain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move data-level row/column permission enforcement from `S2DataPermissionAspect` (AOP on service methods) into the SQL translator's corrector chain, so every physical SQL — regardless of UNION / sub-query / CTE / direct SQL — gets policies injected at the SQL AST layer (PostgreSQL-RLS-style).

**Architecture:** Introduce a new SPI `com.tencent.supersonic.headless.core.translator.corrector.PhysicalSqlCorrector` that runs on `QueryStatement.sql` after `DefaultSemanticTranslator.translate()` produced `finalSql` but before `JdbcExecutor` executes. Two concrete correctors are registered via `META-INF/spring.factories`: `RowLevelPolicyCorrector` (walks AST, injects `AND <policy-filter>` into every `PlainSelect.where` found in main/UNION/WITH/sub-query branches) and `ColumnMaskingCorrector` (rewrites `SelectItem`s at the outermost `PlainSelect` to wrap masked columns in a dialect-aware expression). A `PolicyResolver` translates `(user, modelIds, dataSetId)` → policy list; a `PolicyAuditLogger` writes structured audit lines. The existing `S2DataPermissionAspect` keeps running in **shadow mode** (diff-logging) for one release; zero-diff-over-N-days triggers its removal.

**Tech Stack:** Java 21, Spring Boot 3.4.x, JSqlParser (already transitive via `common` module — `net.sf.jsqlparser.*`), JUnit 5, Mockito, SLF4J structured logging, Jackson for audit JSON.

---

## File Structure

**Create (production):**
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PhysicalSqlCorrector.java` — SPI interface (accept + correct).
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java` — AST-walking row filter injector.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrector.java` — SelectItem rewriter.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyContext.java` — carries user + resolved policies between corrector invocations (passed into `correct()`).
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/RowPolicy.java` — `modelId`, `tableBizNames`, `filterExpression` (raw SQL fragment), `policyId`, `description`.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/ColumnPolicy.java` — `modelId`, `columnBizName`, `maskTemplate` (e.g. `LEFT(%s,3)||'****'`), `policyId`.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/PolicyResolver.java` — interface.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolver.java` — stub impl for Task 2.
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolver.java` — real impl wired to `AuthService` + `SensitiveLevelConfig` (Task 6).
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLogger.java` — JSON-line logger.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditEntry.java` — POJO.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ShadowModeComparator.java` — diff old-aspect vs new-corrector output (Task 8).
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyCorrectorProperties.java` — `@ConfigurationProperties("s2.permission.corrector")` with `enabled`, `shadowMode`, `auditLogEnabled`.

**Modify:**
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/DefaultSemanticTranslator.java` — invoke corrector chain after `mergeOntologyQuery`.
- `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/ComponentFactory.java` — add `getPhysicalSqlCorrectors()`.
- `launchers/chat/src/main/resources/META-INF/spring.factories` — register new SPIs.
- `launchers/headless/src/main/resources/META-INF/spring.factories` — register new SPIs.
- `launchers/standalone/src/main/resources/META-INF/spring.factories` — register new SPIs.
- `launchers/standalone/src/main/resources/application.yml` — add `s2.permission.corrector.*` keys.
- `headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java` — mark `@Deprecated`, add shadow-mode comparison hook (Task 9).
- `docs/details/platform/rbac.md` (or create) — add policy-author checklist (Task 10).

**Tests (create):**
- `headless/core/src/test/resources/permission-fixtures/golden-rewrites.json` — input/expected SQL pairs.
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/GoldenRewriteFixtureTest.java` — parameterised test driver.
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrectorTest.java`
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrectorTest.java`
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolverTest.java`
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/SpiOrderingTest.java`
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLoggerTest.java`
- `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ShadowModeComparatorTest.java`
- `headless/server/src/test/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolverTest.java`

---

## Task 1: Golden Test Fixtures

**Files:**
- Create: `headless/core/src/test/resources/permission-fixtures/golden-rewrites.json`
- Create: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/GoldenRewriteFixtureTest.java`

- [ ] **Step 1: Create the fixture file**

Write `headless/core/src/test/resources/permission-fixtures/golden-rewrites.json`:

```json
[
  {
    "name": "simple_select_single_filter",
    "user": "alice",
    "rowPolicies": [
      {"policyId": "P1", "modelId": 1, "tableBizNames": ["s2_user_orders"], "filterExpression": "region = 'APAC'"}
    ],
    "columnPolicies": [],
    "inputSql": "SELECT user_id, amount FROM s2_user_orders WHERE status = 'PAID'",
    "expectedSql": "SELECT user_id, amount FROM s2_user_orders WHERE status = 'PAID' AND (region = 'APAC')"
  },
  {
    "name": "select_with_inner_join",
    "user": "alice",
    "rowPolicies": [
      {"policyId": "P1", "modelId": 1, "tableBizNames": ["s2_user_orders"], "filterExpression": "region = 'APAC'"},
      {"policyId": "P2", "modelId": 2, "tableBizNames": ["s2_product"], "filterExpression": "owner = 'alice'"}
    ],
    "columnPolicies": [],
    "inputSql": "SELECT o.id FROM s2_user_orders o JOIN s2_product p ON o.pid = p.id WHERE o.ts > '2026-01-01'",
    "expectedSql": "SELECT o.id FROM s2_user_orders o JOIN s2_product p ON o.pid = p.id WHERE o.ts > '2026-01-01' AND (region = 'APAC') AND (owner = 'alice')"
  },
  {
    "name": "nested_subquery_in_from",
    "user": "alice",
    "rowPolicies": [
      {"policyId": "P1", "modelId": 1, "tableBizNames": ["s2_user_orders"], "filterExpression": "region = 'APAC'"}
    ],
    "columnPolicies": [],
    "inputSql": "SELECT t.amount FROM (SELECT user_id, amount FROM s2_user_orders WHERE status = 'PAID') t",
    "expectedSql": "SELECT t.amount FROM (SELECT user_id, amount FROM s2_user_orders WHERE status = 'PAID' AND (region = 'APAC')) t"
  },
  {
    "name": "union_all_both_branches_filtered",
    "user": "alice",
    "rowPolicies": [
      {"policyId": "P1", "modelId": 1, "tableBizNames": ["s2_user_orders"], "filterExpression": "region = 'APAC'"}
    ],
    "columnPolicies": [],
    "inputSql": "SELECT user_id FROM s2_user_orders WHERE status = 'PAID' UNION ALL SELECT user_id FROM s2_user_orders WHERE status = 'REFUND'",
    "expectedSql": "SELECT user_id FROM s2_user_orders WHERE status = 'PAID' AND (region = 'APAC') UNION ALL SELECT user_id FROM s2_user_orders WHERE status = 'REFUND' AND (region = 'APAC')"
  },
  {
    "name": "with_cte",
    "user": "alice",
    "rowPolicies": [
      {"policyId": "P1", "modelId": 1, "tableBizNames": ["s2_user_orders"], "filterExpression": "region = 'APAC'"}
    ],
    "columnPolicies": [],
    "inputSql": "WITH paid AS (SELECT user_id FROM s2_user_orders WHERE status = 'PAID') SELECT COUNT(*) FROM paid",
    "expectedSql": "WITH paid AS (SELECT user_id FROM s2_user_orders WHERE status = 'PAID' AND (region = 'APAC')) SELECT COUNT(*) FROM paid"
  },
  {
    "name": "select_star_with_masking",
    "user": "alice",
    "rowPolicies": [],
    "columnPolicies": [
      {"policyId": "C1", "modelId": 1, "columnBizName": "phone", "maskTemplate": "CONCAT(LEFT(%s,3),'****')"}
    ],
    "inputSql": "SELECT user_id, phone FROM s2_user_orders",
    "expectedSql": "SELECT user_id, CONCAT(LEFT(phone,3),'****') AS phone FROM s2_user_orders"
  },
  {
    "name": "column_masking_with_row_filter",
    "user": "alice",
    "rowPolicies": [
      {"policyId": "P1", "modelId": 1, "tableBizNames": ["s2_user_orders"], "filterExpression": "region = 'APAC'"}
    ],
    "columnPolicies": [
      {"policyId": "C1", "modelId": 1, "columnBizName": "phone", "maskTemplate": "CONCAT(LEFT(%s,3),'****')"}
    ],
    "inputSql": "SELECT user_id, phone FROM s2_user_orders",
    "expectedSql": "SELECT user_id, CONCAT(LEFT(phone,3),'****') AS phone FROM s2_user_orders WHERE (region = 'APAC')"
  },
  {
    "name": "no_policy_passthrough",
    "user": "alice",
    "rowPolicies": [],
    "columnPolicies": [],
    "inputSql": "SELECT user_id FROM s2_user_orders WHERE status = 'PAID'",
    "expectedSql": "SELECT user_id FROM s2_user_orders WHERE status = 'PAID'"
  }
]
```

- [ ] **Step 2: Create the parameterised driver (FAILING — impl doesn't exist yet)**

Write `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/GoldenRewriteFixtureTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoldenRewriteFixtureTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Case {
        public String name;
        public String user;
        public List<RowPolicy> rowPolicies;
        public List<ColumnPolicy> columnPolicies;
        public String inputSql;
        public String expectedSql;
    }

    static Stream<Case> loadCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = GoldenRewriteFixtureTest.class
                .getResourceAsStream("/permission-fixtures/golden-rewrites.json")) {
            List<Case> cases = mapper.readValue(in,
                    mapper.getTypeFactory().constructCollectionType(List.class, Case.class));
            return cases.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadCases")
    void rewritesMatchGolden(Case c) {
        PolicyContext ctx = new PolicyContext();
        ctx.setUser(User.getDefaultUser(c.user));
        ctx.setRowPolicies(c.rowPolicies == null ? List.of() : c.rowPolicies);
        ctx.setColumnPolicies(c.columnPolicies == null ? List.of() : c.columnPolicies);

        String sql = c.inputSql;
        sql = new RowLevelPolicyCorrector().rewrite(sql, ctx);
        sql = new ColumnMaskingCorrector().rewrite(sql, ctx);

        assertEquals(normalize(c.expectedSql), normalize(sql), "case=" + c.name);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
```

- [ ] **Step 3: Run tests — expect compilation errors**

Run: `mvn test -pl headless/core -Dtest=GoldenRewriteFixtureTest`
Expected: compile error — `RowLevelPolicyCorrector`, `ColumnMaskingCorrector`, `PolicyContext`, `RowPolicy`, `ColumnPolicy` don't exist yet. Good — we've captured the behaviour contract.

- [ ] **Step 4: Commit**

```bash
git add headless/core/src/test/resources/permission-fixtures/golden-rewrites.json \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/GoldenRewriteFixtureTest.java
git commit -m "test(permission): add golden fixtures for SQL policy rewrites"
```

---

## Task 2: PolicyResolver + In-Memory Stub

**Files:**
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/RowPolicy.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/ColumnPolicy.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/PolicyResolver.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolver.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyContext.java`
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolverTest.java`

- [ ] **Step 1: Write the failing resolver test**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolverTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.policy;

import com.tencent.supersonic.common.pojo.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryPolicyResolverTest {

    @Test
    void returnsEmptyWhenNoFixtureRegistered() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        List<RowPolicy> rows = resolver.resolveRowPolicies(User.getDefaultUser("alice"), Set.of(1L));
        assertTrue(rows.isEmpty());
    }

    @Test
    void returnsRegisteredPolicyForMatchingUserAndModel() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        RowPolicy p = new RowPolicy("P1", 1L, List.of("s2_user_orders"), "region = 'APAC'", "APAC only");
        resolver.register("alice", p);

        List<RowPolicy> rows = resolver.resolveRowPolicies(User.getDefaultUser("alice"), Set.of(1L));
        assertEquals(1, rows.size());
        assertEquals("P1", rows.get(0).getPolicyId());
    }

    @Test
    void filtersByRequestedModelIds() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        resolver.register("alice", new RowPolicy("P1", 1L, List.of("t1"), "x=1", null));
        resolver.register("alice", new RowPolicy("P2", 2L, List.of("t2"), "y=2", null));

        List<RowPolicy> rows = resolver.resolveRowPolicies(User.getDefaultUser("alice"), Set.of(1L));
        assertEquals(1, rows.size());
        assertEquals("P1", rows.get(0).getPolicyId());
    }
}
```

- [ ] **Step 2: Run it — expect compile failures**

Run: `mvn test -pl headless/core -Dtest=InMemoryPolicyResolverTest`
Expected: compile error — `RowPolicy`, `InMemoryPolicyResolver` not found.

- [ ] **Step 3: Create the POJOs**

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/RowPolicy.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RowPolicy {
    private String policyId;
    private Long modelId;
    private List<String> tableBizNames;
    private String filterExpression;
    private String description;
}
```

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/ColumnPolicy.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnPolicy {
    private String policyId;
    private Long modelId;
    private String columnBizName;
    /** Pattern with a single %s placeholder, e.g. "CONCAT(LEFT(%s,3),'****')". */
    private String maskTemplate;
}
```

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyContext.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class PolicyContext {
    private User user;
    private Set<Long> modelIds;
    private Long dataSetId;
    private List<RowPolicy> rowPolicies = new ArrayList<>();
    private List<ColumnPolicy> columnPolicies = new ArrayList<>();
    /** If true, do not rewrite; only compute what would change (shadow mode). */
    private boolean shadowMode;
}
```

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/PolicyResolver.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.policy;

import com.tencent.supersonic.common.pojo.User;

import java.util.List;
import java.util.Set;

public interface PolicyResolver {

    List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds);

    List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds);
}
```

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolver.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.policy;

import com.tencent.supersonic.common.pojo.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Fixture-only resolver used by tests and for Task 2 wiring. Not a @Component. */
public class InMemoryPolicyResolver implements PolicyResolver {

    private final Map<String, List<RowPolicy>> rowByUser = new HashMap<>();
    private final Map<String, List<ColumnPolicy>> colByUser = new HashMap<>();

    public void register(String userName, RowPolicy policy) {
        rowByUser.computeIfAbsent(userName, k -> new ArrayList<>()).add(policy);
    }

    public void register(String userName, ColumnPolicy policy) {
        colByUser.computeIfAbsent(userName, k -> new ArrayList<>()).add(policy);
    }

    @Override
    public List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds) {
        return rowByUser.getOrDefault(user.getName(), List.of()).stream()
                .filter(p -> modelIds == null || modelIds.isEmpty() || modelIds.contains(p.getModelId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds) {
        return colByUser.getOrDefault(user.getName(), List.of()).stream()
                .filter(p -> modelIds == null || modelIds.isEmpty() || modelIds.contains(p.getModelId()))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: Run — expect PASS**

Run: `mvn test -pl headless/core -Dtest=InMemoryPolicyResolverTest`
Expected: 3 tests pass.

- [ ] **Step 5: Verify compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/policy/InMemoryPolicyResolverTest.java
git commit -m "feat(permission): add PolicyResolver SPI with in-memory stub"
```

---

## Task 3: RowLevelPolicyCorrector

**Files:**
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PhysicalSqlCorrector.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java`
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrectorTest.java`

- [ ] **Step 1: Write failing unit tests**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrectorTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowLevelPolicyCorrectorTest {

    private String norm(String sql) { return sql.replaceAll("\\s+", " ").trim(); }

    private PolicyContext ctx(RowPolicy... policies) {
        PolicyContext c = new PolicyContext();
        c.setUser(User.getDefaultUser("alice"));
        c.setRowPolicies(List.of(policies));
        return c;
    }

    @Test
    void noPolicies_passthrough() {
        String input = "SELECT a FROM t WHERE a = 1";
        String out = new RowLevelPolicyCorrector().rewrite(input, ctx());
        assertEquals(norm(input), norm(out));
    }

    @Test
    void simpleWhereInjection() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "region = 'APAC'", null);
        String out = new RowLevelPolicyCorrector().rewrite("SELECT a FROM t WHERE a = 1", ctx(p));
        assertTrue(norm(out).contains("AND (region = 'APAC')"));
    }

    @Test
    void injectsIntoBothUnionBranches() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "region = 'APAC'", null);
        String out = new RowLevelPolicyCorrector().rewrite(
                "SELECT a FROM t WHERE a=1 UNION ALL SELECT a FROM t WHERE a=2", ctx(p));
        // Both branches should be filtered
        long occurrences = norm(out).split("region = 'APAC'", -1).length - 1;
        assertEquals(2, occurrences);
    }

    @Test
    void injectsIntoSubSelectInFrom() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        String out = new RowLevelPolicyCorrector().rewrite(
                "SELECT x.a FROM (SELECT a FROM t WHERE a=1) x", ctx(p));
        assertTrue(norm(out).contains("AND (r = 1)"));
    }

    @Test
    void injectsIntoCte() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        String out = new RowLevelPolicyCorrector().rewrite(
                "WITH c AS (SELECT a FROM t WHERE a=1) SELECT * FROM c", ctx(p));
        assertTrue(norm(out).contains("AND (r = 1)"));
    }

    @Test
    void onlyAppliesToReferencedTables() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("orders"), "region = 'APAC'", null);
        String out = new RowLevelPolicyCorrector().rewrite(
                "SELECT a FROM products WHERE a=1", ctx(p));
        assertEquals(norm("SELECT a FROM products WHERE a=1"), norm(out));
    }

    @Test
    void skipsRewriteWhenShadowModeEnabled() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        PolicyContext c = ctx(p);
        c.setShadowMode(true);
        String input = "SELECT a FROM t";
        String out = new RowLevelPolicyCorrector().rewrite(input, c);
        assertEquals(norm(input), norm(out));
    }

    @Test
    void malformedSqlReturnsOriginalUnchanged() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        String broken = "SELECT FROM WHERE";
        String out = new RowLevelPolicyCorrector().rewrite(broken, ctx(p));
        assertEquals(broken, out);
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `mvn test -pl headless/core -Dtest=RowLevelPolicyCorrectorTest`
Expected: `RowLevelPolicyCorrector` symbol not found.

- [ ] **Step 3: Create SPI interface**

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PhysicalSqlCorrector.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

/**
 * SPI for physical-SQL corrections applied AFTER DefaultSemanticTranslator.mergeOntologyQuery()
 * but BEFORE JdbcExecutor executes. Runs on the final dialect SQL that will hit the user DB.
 *
 * Implementations must:
 *  - Be idempotent: applying twice yields the same result.
 *  - Never throw on parse errors — log and return the input unchanged.
 *  - Respect PolicyContext.shadowMode (no rewrite when true).
 */
public interface PhysicalSqlCorrector {

    /** Return rewritten SQL (or the input unchanged if the corrector doesn't apply). */
    String rewrite(String sql, PolicyContext context);
}
```

- [ ] **Step 4: Implement the corrector**

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class RowLevelPolicyCorrector implements PhysicalSqlCorrector {

    @Override
    public String rewrite(String sql, PolicyContext ctx) {
        if (sql == null || sql.isBlank()) return sql;
        if (ctx == null || ctx.isShadowMode()) return sql;
        if (ctx.getRowPolicies() == null || ctx.getRowPolicies().isEmpty()) return sql;

        // Build lookup: tableBizName (lower-case) -> list of filter expressions
        Map<String, List<String>> tableToFilters = ctx.getRowPolicies().stream()
                .filter(p -> p != null && p.getTableBizNames() != null && p.getFilterExpression() != null)
                .flatMap(p -> p.getTableBizNames().stream()
                        .map(t -> Map.entry(t.toLowerCase(Locale.ROOT), p.getFilterExpression())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        if (tableToFilters.isEmpty()) return sql;

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof Select select) {
                walk(select, tableToFilters);
                return stmt.toString();
            }
            return sql;
        } catch (JSQLParserException e) {
            log.warn("RowLevelPolicyCorrector parse failed; returning SQL unchanged. err={}",
                    e.getMessage());
            return sql;
        }
    }

    private void walk(Select select, Map<String, List<String>> filters) {
        // WITH items first
        if (select.getWithItemsList() != null) {
            for (WithItem w : select.getWithItemsList()) {
                if (w.getSelect() != null) walk(w.getSelect(), filters);
            }
        }
        if (select instanceof PlainSelect ps) {
            walkPlain(ps, filters);
        } else if (select instanceof SetOperationList sol) {
            if (sol.getSelects() != null) {
                for (Select child : sol.getSelects()) walk(child, filters);
            }
        } else if (select instanceof ParenthesedSelect pss) {
            walk(pss.getSelect(), filters);
        }
    }

    private void walkPlain(PlainSelect ps, Map<String, List<String>> filters) {
        // 1. recurse into FROM
        if (ps.getFromItem() != null) descend(ps.getFromItem(), filters);
        // 2. recurse into JOINs
        if (ps.getJoins() != null) {
            for (Join j : ps.getJoins()) if (j.getFromItem() != null) descend(j.getFromItem(), filters);
        }
        // 3. collect referenced base tables and inject filters that apply
        List<Expression> toAdd = collectApplicableFilters(ps, filters);
        for (Expression newCond : toAdd) {
            Expression wrapped = new Parenthesis(newCond);
            if (ps.getWhere() == null) ps.setWhere(wrapped);
            else ps.setWhere(new AndExpression(ps.getWhere(), wrapped));
        }
    }

    private void descend(FromItem fi, Map<String, List<String>> filters) {
        if (fi instanceof ParenthesedSelect pss) walk(pss.getSelect(), filters);
    }

    private List<Expression> collectApplicableFilters(PlainSelect ps,
                                                      Map<String, List<String>> filters) {
        return referencedTableNames(ps).stream()
                .map(t -> t.toLowerCase(Locale.ROOT))
                .distinct()
                .filter(filters::containsKey)
                .flatMap(t -> filters.get(t).stream())
                .map(this::parseCondSafe)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> referencedTableNames(PlainSelect ps) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        if (ps.getFromItem() instanceof Table t) names.add(t.getName());
        if (ps.getJoins() != null) {
            for (Join j : ps.getJoins()) {
                if (j.getFromItem() instanceof Table t) names.add(t.getName());
            }
        }
        return names;
    }

    private Expression parseCondSafe(String expr) {
        try {
            return CCJSqlParserUtil.parseCondExpression(expr);
        } catch (JSQLParserException e) {
            log.warn("Failed to parse policy expression '{}' — skipping", expr);
            return null;
        }
    }
}
```

- [ ] **Step 5: Run tests — expect PASS**

Run: `mvn test -pl headless/core -Dtest=RowLevelPolicyCorrectorTest`
Expected: all 8 tests pass.

- [ ] **Step 6: Verify compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PhysicalSqlCorrector.java \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrectorTest.java
git commit -m "feat(permission): implement RowLevelPolicyCorrector with AST walk"
```

---

## Task 4: ColumnMaskingCorrector

**Files:**
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrector.java`
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrectorTest.java`

- [ ] **Step 1: Write failing tests**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrectorTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnMaskingCorrectorTest {

    private String norm(String s) { return s.replaceAll("\\s+", " ").trim(); }

    private PolicyContext ctx(ColumnPolicy... cps) {
        PolicyContext c = new PolicyContext();
        c.setUser(User.getDefaultUser("alice"));
        c.setColumnPolicies(List.of(cps));
        return c;
    }

    @Test
    void noPolicies_passthrough() {
        String in = "SELECT user_id, phone FROM t";
        assertEquals(norm(in), norm(new ColumnMaskingCorrector().rewrite(in, ctx())));
    }

    @Test
    void wrapsPlainColumn() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite("SELECT user_id, phone FROM t", ctx(cp));
        assertTrue(norm(out).contains("CONCAT(LEFT(phone, 3), '****') AS phone"));
    }

    @Test
    void preservesExistingAliasByReplacingWithMaskedAlias() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite(
                "SELECT user_id, phone AS p FROM t", ctx(cp));
        assertTrue(norm(out).contains("CONCAT(LEFT(phone, 3), '****') AS p"));
    }

    @Test
    void doesNotWrapNonMaskedColumns() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite("SELECT user_id FROM t", ctx(cp));
        assertEquals(norm("SELECT user_id FROM t"), norm(out));
    }

    @Test
    void shadowModeSkipsRewrite() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        PolicyContext c = ctx(cp);
        c.setShadowMode(true);
        String in = "SELECT phone FROM t";
        assertEquals(norm(in), norm(new ColumnMaskingCorrector().rewrite(in, c)));
    }

    @Test
    void selectStarIsPassedThroughUntouched() {
        // SELECT * cannot be surgically masked without schema — corrector leaves it alone
        // and RowLevelPolicyCorrector will still have applied row filters (handled separately).
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String in = "SELECT * FROM t";
        assertEquals(norm(in), norm(new ColumnMaskingCorrector().rewrite(in, ctx(cp))));
    }

    @Test
    void malformedSqlReturnsOriginal() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String broken = "SELECT FROM WHERE";
        assertEquals(broken, new ColumnMaskingCorrector().rewrite(broken, ctx(cp)));
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `mvn test -pl headless/core -Dtest=ColumnMaskingCorrectorTest`
Expected: `ColumnMaskingCorrector` not found.

- [ ] **Step 3: Implement**

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrector.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ColumnMaskingCorrector implements PhysicalSqlCorrector {

    @Override
    public String rewrite(String sql, PolicyContext ctx) {
        if (sql == null || sql.isBlank()) return sql;
        if (ctx == null || ctx.isShadowMode()) return sql;
        List<ColumnPolicy> policies = ctx.getColumnPolicies();
        if (policies == null || policies.isEmpty()) return sql;

        Map<String, String> colToMask = policies.stream()
                .filter(p -> p != null && p.getColumnBizName() != null && p.getMaskTemplate() != null)
                .collect(Collectors.toMap(
                        p -> p.getColumnBizName().toLowerCase(Locale.ROOT),
                        ColumnPolicy::getMaskTemplate,
                        (a, b) -> a));

        if (colToMask.isEmpty()) return sql;

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof Select select) {
                walk(select, colToMask);
                return stmt.toString();
            }
            return sql;
        } catch (JSQLParserException e) {
            log.warn("ColumnMaskingCorrector parse failed; returning unchanged. err={}",
                    e.getMessage());
            return sql;
        }
    }

    private void walk(Select select, Map<String, String> colToMask) {
        if (select.getWithItemsList() != null) {
            for (WithItem w : select.getWithItemsList()) {
                if (w.getSelect() != null) walk(w.getSelect(), colToMask);
            }
        }
        if (select instanceof PlainSelect ps) {
            rewritePlain(ps, colToMask);
        } else if (select instanceof SetOperationList sol) {
            if (sol.getSelects() != null) {
                for (Select child : sol.getSelects()) walk(child, colToMask);
            }
        } else if (select instanceof ParenthesedSelect pss) {
            walk(pss.getSelect(), colToMask);
        }
    }

    private void rewritePlain(PlainSelect ps, Map<String, String> colToMask) {
        List<SelectItem<?>> items = ps.getSelectItems();
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            SelectItem<?> si = items.get(i);
            Expression expr = si.getExpression();
            if (expr instanceof Column col) {
                String name = col.getColumnName();
                if (name == null) continue;
                String mask = colToMask.get(name.toLowerCase(Locale.ROOT));
                if (mask == null) continue;
                // Build new expression from template, then parse it
                String rendered = String.format(mask, col.toString());
                try {
                    Expression newExpr = CCJSqlParserUtil.parseExpression(rendered);
                    Alias existingAlias = si.getAlias();
                    Alias alias = existingAlias != null
                            ? existingAlias
                            : new Alias(name);
                    SelectItem<Expression> replaced = new SelectItem<>(newExpr);
                    replaced.setAlias(alias);
                    items.set(i, replaced);
                } catch (JSQLParserException e) {
                    log.warn("Failed to render mask template '{}' for column '{}'", mask, name);
                }
            }
            // AllColumns (SELECT *) / AllTableColumns: skip — cannot be surgically masked
            if (expr instanceof AllColumns || expr instanceof AllTableColumns) {
                log.debug("SELECT * cannot be masked — skipping");
            }
        }
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -pl headless/core -Dtest=ColumnMaskingCorrectorTest`
Expected: 7 tests pass.

- [ ] **Step 5: Run the golden fixture driver**

Run: `mvn test -pl headless/core -Dtest=GoldenRewriteFixtureTest`
Expected: all 8 cases pass.

- [ ] **Step 6: Verify compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrector.java \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrectorTest.java
git commit -m "feat(permission): implement ColumnMaskingCorrector"
```

---

## Task 5: SPI Registration + ComponentFactory + Translator Wiring

**Files:**
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/ComponentFactory.java`
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/DefaultSemanticTranslator.java`
- Modify: `launchers/chat/src/main/resources/META-INF/spring.factories`
- Modify: `launchers/headless/src/main/resources/META-INF/spring.factories`
- Modify: `launchers/standalone/src/main/resources/META-INF/spring.factories`
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/SpiOrderingTest.java`

- [ ] **Step 1: Read ComponentFactory to find the add point**

Read: `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/ComponentFactory.java`

- [ ] **Step 2: Write the SPI ordering test FIRST**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/SpiOrderingTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures correctors load in the correct order:
 *   Row-level filter must run BEFORE column masking (we reason over pre-masked rows).
 * Both must be present via SPI.
 */
class SpiOrderingTest {

    @Test
    void rowCorrectorLoadsBeforeColumnCorrector() {
        List<PhysicalSqlCorrector> loaded = SpringFactoriesLoader.loadFactories(
                PhysicalSqlCorrector.class, Thread.currentThread().getContextClassLoader());
        List<Class<?>> classes = loaded.stream().map(Object::getClass).toList();

        int rowIdx = indexOf(classes, RowLevelPolicyCorrector.class);
        int colIdx = indexOf(classes, ColumnMaskingCorrector.class);

        assertTrue(rowIdx >= 0, "RowLevelPolicyCorrector must be registered via SPI");
        assertTrue(colIdx >= 0, "ColumnMaskingCorrector must be registered via SPI");
        assertTrue(rowIdx < colIdx, "Row corrector must precede Column corrector");
    }

    private int indexOf(List<Class<?>> list, Class<?> target) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).equals(target)) return i;
        return -1;
    }
}
```

- [ ] **Step 3: Run it — expect FAIL (not yet registered)**

Run: `mvn test -pl headless/core -Dtest=SpiOrderingTest`
Expected: FAIL — empty list loaded.

- [ ] **Step 4: Register in the three launchers**

Modify `launchers/chat/src/main/resources/META-INF/spring.factories`: append at EOF:

```
com.tencent.supersonic.headless.core.translator.corrector.PhysicalSqlCorrector=\
    com.tencent.supersonic.headless.core.translator.corrector.RowLevelPolicyCorrector,\
    com.tencent.supersonic.headless.core.translator.corrector.ColumnMaskingCorrector
```

Modify `launchers/headless/src/main/resources/META-INF/spring.factories`: append the same block.

Modify `launchers/standalone/src/main/resources/META-INF/spring.factories`: append the same block.

Note: For the test under `headless/core`, these files are on other modules' classpaths. Also add a test-scoped file so `SpiOrderingTest` passes when running only `headless/core`:

Create `headless/core/src/test/resources/META-INF/spring.factories`:

```
com.tencent.supersonic.headless.core.translator.corrector.PhysicalSqlCorrector=\
    com.tencent.supersonic.headless.core.translator.corrector.RowLevelPolicyCorrector,\
    com.tencent.supersonic.headless.core.translator.corrector.ColumnMaskingCorrector
```

- [ ] **Step 5: Run SpiOrderingTest — expect PASS**

Run: `mvn test -pl headless/core -Dtest=SpiOrderingTest`
Expected: PASS.

- [ ] **Step 6: Add `getPhysicalSqlCorrectors()` to ComponentFactory**

Open `headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/ComponentFactory.java` and add:

```java
import com.tencent.supersonic.headless.core.translator.corrector.PhysicalSqlCorrector;

// ... inside the class, alongside the other static lists:
private static List<PhysicalSqlCorrector> physicalSqlCorrectors = new ArrayList<>();

public static List<PhysicalSqlCorrector> getPhysicalSqlCorrectors() {
    return CollectionUtils.isEmpty(physicalSqlCorrectors)
            ? init(PhysicalSqlCorrector.class, physicalSqlCorrectors)
            : physicalSqlCorrectors;
}
```

(Follow the exact pattern of `getQueryParsers()` / `getQueryOptimizers()` that already exists in this file — do NOT introduce a different style.)

- [ ] **Step 7: Wire the chain into DefaultSemanticTranslator**

Open `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/DefaultSemanticTranslator.java`. After the `mergeOntologyQuery(queryStatement);` call and before the `QueryOptimizer` loop, insert:

```java
applyPhysicalSqlCorrectors(queryStatement);
```

Add the helper method to the class:

```java
private void applyPhysicalSqlCorrectors(QueryStatement queryStatement) {
    PolicyContext ctx = queryStatement.getPolicyContext();
    if (ctx == null) return;
    String sql = queryStatement.getSql();
    for (PhysicalSqlCorrector c : ComponentFactory.getPhysicalSqlCorrectors()) {
        try {
            sql = c.rewrite(sql, ctx);
        } catch (Exception e) {
            log.error("PhysicalSqlCorrector {} failed", c.getClass().getSimpleName(), e);
        }
    }
    queryStatement.setSql(sql);
}
```

Required imports:

```java
import com.tencent.supersonic.headless.core.translator.corrector.PhysicalSqlCorrector;
import com.tencent.supersonic.headless.core.translator.corrector.PolicyContext;
```

- [ ] **Step 8: Add `policyContext` field to `QueryStatement`**

Find: `headless/core/src/main/java/com/tencent/supersonic/headless/core/pojo/QueryStatement.java` and add:

```java
@com.fasterxml.jackson.annotation.JsonIgnore
private com.tencent.supersonic.headless.core.translator.corrector.PolicyContext policyContext;
```

(The Lombok `@Data` on `QueryStatement` will generate getter/setter.)

- [ ] **Step 9: Verify compile**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 10: Commit**

```bash
git add launchers/chat/src/main/resources/META-INF/spring.factories \
        launchers/headless/src/main/resources/META-INF/spring.factories \
        launchers/standalone/src/main/resources/META-INF/spring.factories \
        headless/core/src/test/resources/META-INF/spring.factories \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/utils/ComponentFactory.java \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/DefaultSemanticTranslator.java \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/pojo/QueryStatement.java \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/SpiOrderingTest.java
git commit -m "feat(permission): register PhysicalSqlCorrector SPI and wire into translator"
```

---

## Task 6: Wire PolicyResolver to Real Auth Data

**Files:**
- Create: `headless/server/src/main/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolver.java`
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/facade/service/impl/S2SemanticLayerService.java` — populate `PolicyContext` before calling translator.
- Test: `headless/server/src/test/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolverTest.java`

- [ ] **Step 1: Write failing test**

Create `headless/server/src/test/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolverTest.java`:

```java
package com.tencent.supersonic.headless.server.permission;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.service.SchemaService;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthBackedPolicyResolverTest {

    private AuthService authService;
    private SchemaService schemaService;
    private SensitiveLevelConfig sensitiveLevelConfig;
    private AuthBackedPolicyResolver resolver;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        schemaService = mock(SchemaService.class);
        sensitiveLevelConfig = mock(SensitiveLevelConfig.class);
        resolver = new AuthBackedPolicyResolver(authService, schemaService, sensitiveLevelConfig);
    }

    @Test
    void mapsDimensionFiltersToRowPolicies() {
        AuthorizedResourceResp resp = new AuthorizedResourceResp();
        DimensionFilter f = new DimensionFilter();
        f.setExpressions(new ArrayList<>(List.of("region = 'APAC'")));
        f.setDescription("APAC only");
        resp.setFilters(new ArrayList<>(List.of(f)));
        when(authService.queryAuthorizedResources(any(QueryAuthResReq.class), any(User.class)))
                .thenReturn(resp);

        SemanticSchemaResp schema = new SemanticSchemaResp();
        schema.setModelIds(List.of(1L));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);

        List<RowPolicy> out = resolver.resolveRowPolicies(User.getDefaultUser("alice"), Set.of(1L));
        assertEquals(1, out.size());
        assertEquals("region = 'APAC'", out.get(0).getFilterExpression());
        assertEquals("APAC only", out.get(0).getDescription());
    }

    @Test
    void mapsHighSensitiveColumnsWithDefaultMaskToColumnPolicies() {
        AuthorizedResourceResp resp = new AuthorizedResourceResp();
        resp.setAuthResList(new ArrayList<>()); // alice is not authorised for phone
        when(authService.queryAuthorizedResources(any(QueryAuthResReq.class), any(User.class)))
                .thenReturn(resp);

        SemanticSchemaResp schema = new SemanticSchemaResp();
        DimSchemaResp phone = new DimSchemaResp();
        phone.setBizName("phone");
        phone.setSensitiveLevel(2); // HIGH
        schema.setDimensions(new ArrayList<>(List.of(phone)));
        when(schemaService.fetchSemanticSchema(any())).thenReturn(schema);
        when(sensitiveLevelConfig.isMidLevelRequireAuth()).thenReturn(false);

        List<ColumnPolicy> out = resolver.resolveColumnPolicies(User.getDefaultUser("alice"), Set.of(1L));
        assertTrue(out.stream().anyMatch(cp -> "phone".equals(cp.getColumnBizName())));
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `mvn test -pl headless/server -Dtest=AuthBackedPolicyResolverTest`
Expected: compile failure — class doesn't exist.

- [ ] **Step 3: Implement the resolver**

Create `headless/server/src/main/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolver.java`:

```java
package com.tencent.supersonic.headless.server.permission;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRes;
import com.tencent.supersonic.auth.api.authorization.pojo.DimensionFilter;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.config.SensitiveLevelConfig;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.headless.api.pojo.request.SchemaFilterReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.service.SchemaService;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.PolicyResolver;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthBackedPolicyResolver implements PolicyResolver {

    /** Default mask template when a high-sensitive column has no explicit mask. */
    private static final String DEFAULT_MASK = "CONCAT(LEFT(%s,3),'****')";

    private final AuthService authService;
    private final SchemaService schemaService;
    private final SensitiveLevelConfig sensitiveLevelConfig;

    @Override
    public List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds) {
        AuthorizedResourceResp auth = fetchAuth(user, modelIds);
        if (auth == null || CollectionUtils.isEmpty(auth.getFilters())) return List.of();
        List<RowPolicy> result = new ArrayList<>();
        for (DimensionFilter f : auth.getFilters()) {
            if (f.getExpressions() == null) continue;
            for (String expr : f.getExpressions()) {
                if (expr == null || expr.isBlank()) continue;
                RowPolicy p = new RowPolicy();
                p.setPolicyId("row-" + UUID.nameUUIDFromBytes(expr.getBytes()));
                p.setModelId(modelIds.iterator().next());
                p.setTableBizNames(allTableBizNames(modelIds));
                p.setFilterExpression(expr);
                p.setDescription(f.getDescription());
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds) {
        SemanticSchemaResp schema = fetchSchema(modelIds);
        if (schema == null) return List.of();
        boolean includeMid = sensitiveLevelConfig.isMidLevelRequireAuth();
        AuthorizedResourceResp auth = fetchAuth(user, modelIds);
        Set<String> authedCols = auth == null ? Set.of()
                : auth.getAuthResList().stream().map(AuthRes::getName).collect(Collectors.toSet());

        List<ColumnPolicy> out = new ArrayList<>();
        if (!CollectionUtils.isEmpty(schema.getDimensions())) {
            for (DimSchemaResp d : schema.getDimensions()) {
                if (shouldMask(d.getSensitiveLevel(), includeMid, d.getBizName(), authedCols)) {
                    out.add(new ColumnPolicy("col-" + d.getBizName(),
                            modelIds.iterator().next(), d.getBizName(), DEFAULT_MASK));
                }
            }
        }
        if (!CollectionUtils.isEmpty(schema.getMetrics())) {
            for (MetricSchemaResp m : schema.getMetrics()) {
                if (shouldMask(m.getSensitiveLevel(), includeMid, m.getBizName(), authedCols)) {
                    out.add(new ColumnPolicy("col-" + m.getBizName(),
                            modelIds.iterator().next(), m.getBizName(), DEFAULT_MASK));
                }
            }
        }
        return out;
    }

    private boolean shouldMask(Integer level, boolean includeMid, String bizName, Set<String> authed) {
        if (bizName == null || authed.contains(bizName)) return false;
        if (SensitiveLevelEnum.HIGH.getCode().equals(level)) return true;
        return includeMid && SensitiveLevelEnum.MID.getCode().equals(level);
    }

    private AuthorizedResourceResp fetchAuth(User user, Set<Long> modelIds) {
        try {
            QueryAuthResReq req = new QueryAuthResReq();
            req.setModelIds(new ArrayList<>(modelIds));
            return authService.queryAuthorizedResources(req, user);
        } catch (Exception e) {
            log.warn("auth fetch failed for user={} models={}", user.getName(), modelIds, e);
            return null;
        }
    }

    private SemanticSchemaResp fetchSchema(Set<Long> modelIds) {
        try {
            SchemaFilterReq f = new SchemaFilterReq();
            f.setModelIds(new ArrayList<>(modelIds));
            return schemaService.fetchSemanticSchema(f);
        } catch (Exception e) {
            log.warn("schema fetch failed for models={}", modelIds, e);
            return null;
        }
    }

    private List<String> allTableBizNames(Set<Long> modelIds) {
        // RLS-wide: apply the row filter to every table referenced in the query.
        // We return an empty list + an explicit wildcard marker so the corrector
        // treats missing table match as "apply to every base table". BUT for now we
        // keep the stricter behaviour: resolve model biz-names via schema service.
        SemanticSchemaResp schema = fetchSchema(modelIds);
        if (schema == null || schema.getModelResps() == null) return List.of();
        return schema.getModelResps().stream()
                .map(m -> m.getBizName() == null ? m.getName() : m.getBizName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: Populate PolicyContext in S2SemanticLayerService**

Open `headless/server/src/main/java/com/tencent/supersonic/headless/server/facade/service/impl/S2SemanticLayerService.java`. Find the two methods annotated `@S2DataPermission` (lines 96 and 106 per `grep`). Directly before the translator is invoked on the resolved `QueryStatement`, add:

```java
if (queryStatement.getPolicyContext() == null && user != null) {
    PolicyContext ctx = new PolicyContext();
    ctx.setUser(user);
    ctx.setModelIds(modelIds);
    ctx.setDataSetId(queryReq.getDataSetId());
    ctx.setRowPolicies(policyResolver.resolveRowPolicies(user, modelIds));
    ctx.setColumnPolicies(policyResolver.resolveColumnPolicies(user, modelIds));
    ctx.setShadowMode(correctorProperties.isShadowMode());
    queryStatement.setPolicyContext(ctx);
}
```

Add field and constructor params (the class uses `@RequiredArgsConstructor` per project convention):

```java
private final PolicyResolver policyResolver;
private final com.tencent.supersonic.headless.core.translator.corrector.PolicyCorrectorProperties correctorProperties;
```

- [ ] **Step 5: Run auth-resolver test — expect PASS**

Run: `mvn test -pl headless/server -Dtest=AuthBackedPolicyResolverTest`
Expected: PASS.

- [ ] **Step 6: Compile check**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/permission/ \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/facade/service/impl/S2SemanticLayerService.java \
        headless/server/src/test/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolverTest.java
git commit -m "feat(permission): wire PolicyResolver to AuthService and SensitiveLevelConfig"
```

---

## Task 7: PolicyAuditLogger

**Files:**
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditEntry.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLogger.java`
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java` — emit audit entries when injecting.
- Modify: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrector.java` — same.
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLoggerTest.java`

- [ ] **Step 1: Write failing audit log test (captures log output)**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLoggerTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyAuditLoggerTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(PolicyAuditLogger.AUDIT_LOGGER);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void emitsJsonLineWithExpectedFields() {
        PolicyAuditLogger pal = new PolicyAuditLogger();
        pal.log(new PolicyAuditEntry(
                "P1", "alice", "row",
                "SELECT * FROM t",
                "SELECT * FROM t WHERE region='APAC'",
                "sha256:abc"));
        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        String msg = events.get(0).getFormattedMessage();
        assertTrue(msg.contains("\"policyId\":\"P1\""));
        assertTrue(msg.contains("\"user\":\"alice\""));
        assertTrue(msg.contains("\"policyType\":\"row\""));
        assertTrue(msg.contains("\"sqlDigest\":\"sha256:abc\""));
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `mvn test -pl headless/core -Dtest=PolicyAuditLoggerTest`
Expected: `PolicyAuditLogger`, `PolicyAuditEntry` not found.

- [ ] **Step 3: Implement audit classes**

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditEntry.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAuditEntry {
    private String policyId;
    private String user;
    /** "row" | "column" */
    private String policyType;
    private String sqlBefore;
    private String sqlAfter;
    private String sqlDigest;
}
```

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLogger.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class PolicyAuditLogger {

    /** Dedicated logger name; ops can route this to a separate file/topic. */
    public static final String AUDIT_LOGGER = "s2.permission.audit";

    private static final Logger AUDIT = LoggerFactory.getLogger(AUDIT_LOGGER);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void log(PolicyAuditEntry entry) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts", Instant.now().toString());
            m.put("policyId", entry.getPolicyId());
            m.put("user", entry.getUser());
            m.put("policyType", entry.getPolicyType());
            m.put("sqlDigest", entry.getSqlDigest());
            // sqlBefore/After omitted by default to avoid logging PII;
            // ops can enable via log level or turn on the debug flag downstream.
            AUDIT.info(MAPPER.writeValueAsString(m));
        } catch (JsonProcessingException e) {
            log.warn("audit log serialisation failed", e);
        }
    }

    public static String digest(String sql) {
        if (sql == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(md.digest(sql.getBytes())).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
```

- [ ] **Step 4: Thread the auditor through correctors**

Edit `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java`. Add a field and a minimal hook:

```java
private final PolicyAuditLogger auditLogger = new PolicyAuditLogger();
```

Replace the loop in `walkPlain` that appends filters to call audit on each added expression:

```java
for (int i = 0; i < toAdd.size(); i++) {
    Expression newCond = toAdd.get(i);
    String beforeSql = ps.toString();
    Expression wrapped = new Parenthesis(newCond);
    if (ps.getWhere() == null) ps.setWhere(wrapped);
    else ps.setWhere(new AndExpression(ps.getWhere(), wrapped));
    auditLogger.log(new com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditEntry(
            appliedPolicyIds.get(i),
            "unknown", // ctx.user.name filled in by caller via thread local - see note below
            "row",
            null, null,
            PolicyAuditLogger.digest(beforeSql)));
}
```

To pass `ctx` (and thus user) through without breaking existing method signatures, change `walkPlain(PlainSelect, Map)` to `walkPlain(PlainSelect, Map, PolicyContext ctx)` and thread `ctx` into `collectApplicableFilters`, returning a parallel `List<String> appliedPolicyIds`. Adjust all internal call sites. Same threading for `walk(Select, ...)` chain.

The full updated method set (replaces the relevant parts of `RowLevelPolicyCorrector`):

```java
@Override
public String rewrite(String sql, PolicyContext ctx) {
    if (sql == null || sql.isBlank()) return sql;
    if (ctx == null || ctx.isShadowMode()) return sql;
    if (ctx.getRowPolicies() == null || ctx.getRowPolicies().isEmpty()) return sql;

    try {
        Statement stmt = CCJSqlParserUtil.parse(sql);
        if (stmt instanceof Select select) {
            walk(select, ctx);
            return stmt.toString();
        }
        return sql;
    } catch (JSQLParserException e) {
        log.warn("RowLevelPolicyCorrector parse failed; returning SQL unchanged. err={}",
                e.getMessage());
        return sql;
    }
}

private void walk(Select select, PolicyContext ctx) {
    if (select.getWithItemsList() != null) {
        for (WithItem w : select.getWithItemsList()) if (w.getSelect() != null) walk(w.getSelect(), ctx);
    }
    if (select instanceof PlainSelect ps) walkPlain(ps, ctx);
    else if (select instanceof SetOperationList sol) {
        if (sol.getSelects() != null) for (Select child : sol.getSelects()) walk(child, ctx);
    } else if (select instanceof ParenthesedSelect pss) walk(pss.getSelect(), ctx);
}

private void walkPlain(PlainSelect ps, PolicyContext ctx) {
    if (ps.getFromItem() instanceof ParenthesedSelect pss) walk(pss.getSelect(), ctx);
    if (ps.getJoins() != null) for (Join j : ps.getJoins())
        if (j.getFromItem() instanceof ParenthesedSelect pss) walk(pss.getSelect(), ctx);

    List<String> refs = referencedTableNames(ps).stream()
            .map(t -> t.toLowerCase(Locale.ROOT)).distinct().toList();
    String userName = ctx.getUser() != null ? ctx.getUser().getName() : "unknown";
    for (RowPolicy p : ctx.getRowPolicies()) {
        if (p == null || p.getTableBizNames() == null || p.getFilterExpression() == null) continue;
        boolean match = p.getTableBizNames().stream()
                .map(t -> t.toLowerCase(Locale.ROOT)).anyMatch(refs::contains);
        if (!match) continue;
        Expression cond = parseCondSafe(p.getFilterExpression());
        if (cond == null) continue;
        String before = ps.toString();
        Expression wrapped = new Parenthesis(cond);
        if (ps.getWhere() == null) ps.setWhere(wrapped);
        else ps.setWhere(new AndExpression(ps.getWhere(), wrapped));
        auditLogger.log(new com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditEntry(
                p.getPolicyId(), userName, "row", null, null,
                PolicyAuditLogger.digest(before)));
    }
}
```

Delete the now-unused `collectApplicableFilters` helper. (Clean-up code debt.)

Apply the analogous audit call inside `ColumnMaskingCorrector.rewritePlain` right after `items.set(i, replaced)`:

```java
auditLogger.log(new com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditEntry(
        "col-" + name,
        ctx.getUser() != null ? ctx.getUser().getName() : "unknown",
        "column", null, null,
        PolicyAuditLogger.digest(ps.toString())));
```

Add `private final PolicyAuditLogger auditLogger = new PolicyAuditLogger();` to `ColumnMaskingCorrector` and pass `ctx` through `walk`/`rewritePlain` (same pattern as row corrector).

- [ ] **Step 5: Run audit + existing corrector tests — expect PASS**

Run: `mvn test -pl headless/core -Dtest=PolicyAuditLoggerTest,RowLevelPolicyCorrectorTest,ColumnMaskingCorrectorTest,GoldenRewriteFixtureTest`
Expected: all pass.

- [ ] **Step 6: Compile check**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/audit/ \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/RowLevelPolicyCorrector.java \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/ColumnMaskingCorrector.java \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/audit/PolicyAuditLoggerTest.java
git commit -m "feat(permission): structured audit logging for policy injection"
```

---

## Task 8: Shadow Mode (Parallel-Run Diff)

**Files:**
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyCorrectorProperties.java`
- Create: `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ShadowModeComparator.java`
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java` — call comparator after its own rewrite.
- Modify: `launchers/standalone/src/main/resources/application.yml` — add the flag.
- Test: `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ShadowModeComparatorTest.java`

- [ ] **Step 1: Write failing comparator test**

Create `headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ShadowModeComparatorTest.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.shadow;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowModeComparatorTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(ShadowModeComparator.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() { logger.detachAppender(appender); }

    @Test
    void logsWarnWhenOldAndNewDiffer() {
        new ShadowModeComparator().compare(
                "old: SELECT a FROM t WHERE x=1",
                "new: SELECT a FROM t WHERE x=1 AND region='APAC'",
                "alice");
        assertEquals(1, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("shadow-diff"));
    }

    @Test
    void silentWhenIdentical() {
        new ShadowModeComparator().compare("SELECT 1", "SELECT 1", "alice");
        assertEquals(0, appender.list.size());
    }
}
```

- [ ] **Step 2: Run — expect compile failure**

Run: `mvn test -pl headless/core -Dtest=ShadowModeComparatorTest`
Expected: class not found.

- [ ] **Step 3: Implement properties + comparator**

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyCorrectorProperties.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "s2.permission.corrector")
@Data
public class PolicyCorrectorProperties {
    /** When true, corrector chain runs but does NOT rewrite; old aspect still active. */
    private boolean shadowMode = true;
    /** Master kill-switch: false disables the whole chain. */
    private boolean enabled = true;
    private boolean auditLogEnabled = true;
}
```

Create `headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ShadowModeComparator.java`:

```java
package com.tencent.supersonic.headless.core.translator.corrector.shadow;

import com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class ShadowModeComparator {

    public void compare(String oldSql, String newSql, String user) {
        if (Objects.equals(normalize(oldSql), normalize(newSql))) return;
        log.warn("shadow-diff user={} oldDigest={} newDigest={} oldSql={} newSql={}",
                user,
                PolicyAuditLogger.digest(oldSql),
                PolicyAuditLogger.digest(newSql),
                oldSql, newSql);
    }

    private String normalize(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }
}
```

- [ ] **Step 4: Hook the comparator into S2DataPermissionAspect**

Open `headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java`. Add `private final ShadowModeComparator shadowComparator;` to the existing constructor-injected fields (keep `@RequiredArgsConstructor`). In `doRowPermission(QuerySqlReq, ...)`, after `querySqlReq.setSql(modifiedSql);`, also capture the simulated "new" SQL:

```java
// Run the new corrector in shadow mode on the same request so we can diff.
try {
    String simulated = simulateNewCorrector(sqlReq, originalSql);
    shadowComparator.compare(modifiedSql, simulated,
            joinPoint.getArgs().length > 1 && joinPoint.getArgs()[1] instanceof User u
                    ? u.getName() : "unknown");
} catch (Exception e) {
    log.debug("shadow compare failed", e);
}
```

Where `simulateNewCorrector(QuerySqlReq, String)` is a small helper that applies `RowLevelPolicyCorrector` using the same `AuthorizedResourceResp` mapped into `RowPolicy` objects. Full method:

```java
private String simulateNewCorrector(QuerySqlReq req, String originalSql) {
    PolicyContext ctx = new PolicyContext();
    // Map authorizedResource.filters -> RowPolicy (in-method, no DI needed).
    // Intentionally no table-bizName filtering: shadow wants to see ANY diff.
    List<RowPolicy> rowPolicies = authorizedResource.getFilters().stream()
            .flatMap(f -> f.getExpressions().stream())
            .filter(Objects::nonNull)
            .filter(s -> !s.isBlank())
            .map(expr -> new RowPolicy("shadow", null,
                    SqlSelectHelper.getAllTableExpression(originalSql).stream()
                            .map(net.sf.jsqlparser.schema.Table::getName).toList(),
                    expr, "shadow"))
            .toList();
    ctx.setRowPolicies(rowPolicies);
    ctx.setUser((User) joinPoint.getArgs()[1]);
    return new RowLevelPolicyCorrector().rewrite(originalSql, ctx);
}
```

Note: the aspect already has `authorizedResource` in scope within the `@Around` method — pull `simulateNewCorrector` to be a **static helper** that accepts `(AuthorizedResourceResp, QuerySqlReq, String, User)` so it can be unit-tested and doesn't leak state.

- [ ] **Step 5: Add application.yml flag**

Open `launchers/standalone/src/main/resources/application.yml` and add under the `s2:` root:

```yaml
s2:
  permission:
    corrector:
      enabled: true
      shadow-mode: true
      audit-log-enabled: true
```

- [ ] **Step 6: Run comparator test — expect PASS**

Run: `mvn test -pl headless/core -Dtest=ShadowModeComparatorTest`
Expected: 2 tests pass.

- [ ] **Step 7: Compile check**

Run: `mvn compile -pl launchers/standalone -am`
Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/PolicyCorrectorProperties.java \
        headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ \
        headless/core/src/test/java/com/tencent/supersonic/headless/core/translator/corrector/shadow/ \
        headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java \
        launchers/standalone/src/main/resources/application.yml
git commit -m "feat(permission): shadow-mode parallel-run comparator and kill-switch"
```

---

## Task 9: Migration — Deprecate the Aspect

**Files:**
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java`
- Modify: `headless/server/src/main/java/com/tencent/supersonic/headless/server/facade/service/impl/S2SemanticLayerService.java` (keeps `@S2DataPermission` for now)
- Modify: `launchers/standalone/src/main/resources/application.yml` — add removal-target note
- Create: `docs/details/platform/permission-corrector-migration.md`

- [ ] **Step 1: Mark the aspect @Deprecated**

Add `@Deprecated(since = "2026-04-17", forRemoval = true)` above the class declaration in `S2DataPermissionAspect.java`. Add a Javadoc block:

```java
/**
 * @deprecated Replaced by the {@link
 *   com.tencent.supersonic.headless.core.translator.corrector.PhysicalSqlCorrector} chain
 *   (RowLevelPolicyCorrector + ColumnMaskingCorrector). Kept in shadow mode for one release
 *   to catch gaps. Remove once shadow-diff logs report zero discrepancies for 30 days.
 *   Tracking: docs/details/platform/permission-corrector-migration.md
 */
```

- [ ] **Step 2: Author the migration doc**

Create `docs/details/platform/permission-corrector-migration.md`:

```markdown
---
status: in-progress
module: permission
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/permission/AuthBackedPolicyResolver.java
---

# Row-Level Permission Corrector Migration

## Objective
Move data row/column permission from `S2DataPermissionAspect` (AOP on `@S2DataPermission` methods) to the physical-SQL corrector chain.

## Timeline
| Phase | Flag | Action |
|-------|------|--------|
| 1. Ship (T0) | `shadow-mode=true` | Both run; diff logs WARN |
| 2. Monitor (T0 → T+30d) | | Count WARN lines daily; target = 0 for 7 consecutive days |
| 3. Flip (T+30d) | `shadow-mode=false` | New corrector rewrites; aspect still runs for safety |
| 4. Remove (T+60d) | — | Delete `S2DataPermissionAspect`, remove `@S2DataPermission` annotation, drop `needAuth` coupling |

## Kill-switch
`s2.permission.corrector.enabled=false` disables the whole chain — use if shadow-mode reports a production-breaking diff. Emergencies only.

## Audit log format
JSON line on logger `s2.permission.audit`:
```json
{"ts":"2026-04-17T10:00:00Z","policyId":"P1","user":"alice","policyType":"row","sqlDigest":"sha256:abc"}
```

## Known diff causes (rule out before flipping)
1. Aspect injects `(a OR b)` joined by `OR`; new corrector AND-joins per policy. If you want OR-within-policy, encode it in `filterExpression` itself.
2. Aspect wraps all existing WHERE in parens before adding; new corrector relies on JSqlParser re-serialisation. Whitespace normalisation handles this — if not, bug.
3. `SELECT *` is masked by aspect via post-result-set column drop; new corrector no-ops. This is a **known degradation** documented here. Fix: require explicit columns in LLM prompt, or add a projection rewriter in a later iteration.
```

- [ ] **Step 3: Verify compile + full corrector tests**

Run: `mvn compile -pl launchers/standalone -am`
Run: `mvn test -pl headless/core -Dtest='com.tencent.supersonic.headless.core.translator.corrector.**'`
Expected: BUILD SUCCESS and all corrector tests pass.

- [ ] **Step 4: Commit**

```bash
git add headless/server/src/main/java/com/tencent/supersonic/headless/server/aspect/S2DataPermissionAspect.java \
        docs/details/platform/permission-corrector-migration.md
git commit -m "docs(permission): deprecate S2DataPermissionAspect and document migration"
```

---

## Task 10: Reviewer Checklist for Writing New Policies

**Files:**
- Create: `docs/details/platform/permission-corrector-authoring.md`

- [ ] **Step 1: Write the authoring guide**

Create `docs/details/platform/permission-corrector-authoring.md`:

```markdown
---
status: stable
module: permission
key-files:
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/RowPolicy.java
  - headless/core/src/main/java/com/tencent/supersonic/headless/core/translator/corrector/policy/ColumnPolicy.java
---

# Authoring Row / Column Policies

## Row policy cheat-sheet

A `RowPolicy` maps (user, model) → a SQL boolean expression injected into `WHERE` of every `PlainSelect` that touches a listed `tableBizName`. Walk happens on JSqlParser AST, so the filter survives UNION, CTE, nested sub-queries.

### Checklist for every new policy

- [ ] `filterExpression` is a **pure boolean** (no trailing `AND`, no `;`, no comment).
- [ ] Columns referenced exist on **every** table in `tableBizNames` (or use fully-qualified `table.col`).
- [ ] No sub-query inside `filterExpression` — JSqlParser can handle it, but audit gets noisy.
- [ ] Tested against the dialect that runs the SQL (ClickHouse / Hive / MySQL / PG / H2 / DuckDB) — JSqlParser is generic but emits standard SQL; dialect-specific functions may need escape.
- [ ] `description` is human-readable (shown in result `QueryAuthorization` hint).
- [ ] Golden fixture added to `headless/core/src/test/resources/permission-fixtures/golden-rewrites.json`.

### Anti-patterns

| Bad | Why | Fix |
|-----|-----|-----|
| `filterExpression: "region = 'APAC';"` | Trailing `;` breaks parse | Strip it |
| `filterExpression: "user_id IN (SELECT id FROM s2_user WHERE dept_id = 1)"` | Sub-query runs per-row in some engines | Materialise as dimension, reference directly |
| Two policies with **different** `modelId` but the same `tableBizNames` | Both injected AND-wise → nothing matches | Merge or narrow |

## Column masking cheat-sheet

A `ColumnPolicy` wraps a raw column reference in a mask template. Applied only to outermost `SELECT items` (not sub-queries — by design, to avoid masking a column that's then used in a `WHERE`).

### Checklist

- [ ] `maskTemplate` has exactly one `%s`, no leading/trailing whitespace.
- [ ] Template is valid in the target dialect (`CONCAT` / `||` differ — test with the right engine).
- [ ] `columnBizName` matches the **ontology biz-name** (post-`convertNameToBizName`), not the raw schema column.
- [ ] Fixture added to golden-rewrites.json covering the template.
- [ ] If `SELECT *` might hit this column, add a docstring noting the known gap (SELECT * is not masked).

### Review questions (PR reviewer asks)

1. Does the policy close or open access? (open = smell)
2. Is the same policy enforceable at dataset-level instead? (simpler)
3. If shadow-mode flipped off today, would this policy still work? (run the golden test)
4. Does the policy appear in audit logs when exercised? (check `s2.permission.audit` logger)
5. Is there a sibling policy for the same model's JOINed tables, or will analysts bypass via JOIN? (row-level filters must cover all join partners)
```

- [ ] **Step 2: Commit**

```bash
git add docs/details/platform/permission-corrector-authoring.md
git commit -m "docs(permission): add authoring checklist for row/column policies"
```

---

## Verification Checklist

Run these before declaring the plan complete:

- [ ] `mvn compile -pl launchers/standalone -am` → BUILD SUCCESS
- [ ] `mvn test -pl headless/core -Dtest='com.tencent.supersonic.headless.core.translator.corrector.**'` → green
- [ ] `mvn test -pl headless/server -Dtest=AuthBackedPolicyResolverTest` → green
- [ ] Start the app with `shadow-mode=true` → no startup errors
- [ ] Trigger a `@S2DataPermission` query with an active row filter → observe `shadow-diff` log lines are empty (or explainable)
- [ ] Flip `shadow-mode=false` locally → same query → row filter now injected by corrector, verified via `s2.permission.audit` log

## Known Risks & Dialect Caveats

1. **Dialect divergence in `||` vs `CONCAT`** — ClickHouse uses `CONCAT(...)` but not `||`; H2/PG accept `||`. The `maskTemplate` is **opaque** to the corrector; authors must choose a template that parses under JSqlParser *and* runs on the target engine. Add fixtures per-dialect if this bites.
2. **JSqlParser grammar gaps** — very new syntax (e.g. Hive `LATERAL VIEW`, ClickHouse `SAMPLE`, DuckDB `QUALIFY`) may fail to parse; the corrector returns the input unchanged and logs a WARN. Pre-Task-8 shadow mode will surface such queries.
3. **`SELECT *` masking gap** — documented in Task 9 migration doc. Not a regression vs aspect for most cases because the aspect only drops columns post-result-set, which we cannot do at SQL-rewrite layer without schema. Mitigations listed in authoring guide.
4. **Alias collisions with mask** — if the user wrote `SELECT phone AS p` and a masking policy exists, the output column name is still `p`. Tested in Task 4 step 1 (`preservesExistingAliasByReplacingWithMaskedAlias`). Be sure downstream consumers rely on alias, not column position.
5. **Performance**: AST parse + re-serialise on every query adds ~1-3ms. Acceptable for OLAP workloads but monitor via `PolicyAuditLogger` timing if needed.

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-17-p1-4-row-level-permission.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — execute tasks in this session using superpowers:executing-plans with checkpoints for review.

**Which approach?**
