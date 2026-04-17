# P2-8: ArchUnit Module Boundary Enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Encode SuperSonic's inter-module dependency rules as ArchUnit tests so that any regression of the hard-won `headless.api` / `headless.server` / `chat.server` / `feishu.server` / `common` boundaries fails the Maven build instead of silently landing on `master`.

**Architecture:** One `ModuleBoundaryTest` test class in `launchers/standalone` (which already declares every sibling module as a Maven dependency, so ArchUnit sees the whole classpath when it imports `com.tencent.supersonic`). JUnit 5 + ArchUnit 1.3.0. Tests run on every `mvn test` — no new CI wiring needed because `.github/workflows/ubuntu-ci.yml` already runs `mvn test` on push + PR to `master`.

**Tech Stack:** Java 21, Maven multi-module, JUnit 5 (jupiter 5.11.4 — already provided by Spring Boot parent), ArchUnit 1.3.0 (compatible with Java 21 per ArchUnit release notes; 1.3.0 added JDK 21 support for class-file parsing).

---

## Background Context (read before starting)

1. **`docs/superpowers/plans/2026-04-14-headless-dto-boundary-migration.md`** — the recent refactor that moved DOs back to `headless-server` and published Req/Resp DTOs from `headless-api`, then dropped the `headless-server` Maven dep from `feishu/server/pom.xml` and `chat/server/pom.xml`. ArchUnit's job is to prevent anyone from reintroducing `headless.server.*` imports on the consumer side.
2. **`CLAUDE.md` — Module Structure** section — the canonical module → package map:
   - `auth/api` → `com.tencent.supersonic.auth.api.*` (sub-packages `authentication`, `authorization`)
   - `auth/authentication` → `com.tencent.supersonic.auth.authentication.*`
   - `auth/authorization` → `com.tencent.supersonic.auth.authorization.*`
   - `billing/api` → `com.tencent.supersonic.billing.api.*`
   - `billing/server` → `com.tencent.supersonic.billing.server.*`
   - `chat/api` → `com.tencent.supersonic.chat.api.*`
   - `chat/server` → `com.tencent.supersonic.chat.server.*`
   - `common` → `com.tencent.supersonic.common.*`
   - `feishu/api` → `com.tencent.supersonic.feishu.api.*`
   - `feishu/server` → `com.tencent.supersonic.feishu.server.*`
   - `headless/api` → `com.tencent.supersonic.headless.api.*`
   - `headless/chat` → `com.tencent.supersonic.headless.chat.*`
   - `headless/core` → `com.tencent.supersonic.headless.core.*`
   - `headless/server` → `com.tencent.supersonic.headless.server.*`
3. **Pre-flight scan (completed while writing this plan) — current codebase state:**
   - `headless.api` has 0 imports from `headless.server.*` → PASS
   - `headless.chat` has 0 imports from `headless.server.*` → PASS
   - `feishu.server` has 0 imports from `chat.server.*` or `headless.server.*` → PASS
   - `chat.server` has 0 imports from `feishu.server.*` → PASS
   - `chat.server` has 0 imports from `headless.server.*` → PASS
   - `auth.api` has 0 imports from `auth.authentication.*` or `auth.authorization.*` → PASS
   - `common` has 0 outbound imports to any `com.tencent.supersonic.*` sibling → PASS
   - No `.api` package contains a class annotated with `@Component / @Service / @Repository / @Controller / @RestController` → PASS
   - No class in any `.api` imports a class in the matching `.server` → PASS (spot-checked via `grep '\.server\.' */api/src`)
   - Cycle pre-check: none observed at module granularity (headless.chat → headless.api only; chat.server → headless.{api,chat,core} only). Full ArchUnit cycle check will confirm at runtime.

   **Conclusion:** the rules in this plan should go GREEN on first run against the current tip of `master`. If any turn RED during execution, stop and file the violation list in the Risks section below — do not relax the rule.

---

## Risks

- **Risk 1 — A rule goes RED at Task 3–8 despite the pre-flight scan.** The scan used `grep '^import ...'` which misses fully qualified class names used inline (rare in this codebase but possible). **Mitigation:** if a task turns red, capture the violation report ArchUnit prints (it names the offending class + the forbidden dependency), then either (a) relax the rule with a `FREEZING_SUBSETS` file (ArchUnit feature for grandfathering existing violations) and file a follow-up ticket, or (b) fix the violation in the same branch if it's 1–2 lines. Default to (a) unless the violation is trivial.
- **Risk 2 — `launchers-common` / `launchers-chat` / `launchers-headless` are not direct deps of `launchers-standalone`.** Only `launchers-common` is (line 30 of `launchers/standalone/pom.xml`). If ArchUnit's imported class set doesn't include the other two launcher sub-modules, rules about them won't fire. **Mitigation:** scan `launchers/standalone/pom.xml` during Task 1 and add missing launcher deps if rules need them. This plan does not write rules scoped to the other launchers because they are deployment wrappers with thin code — low-risk surface.
- **Risk 3 — Test runs late in the build.** `launchers-standalone` is the last module to build. Failing there still fails the build (correct), but a developer gets slow feedback. **Mitigation accepted as-is:** this is the only module that has every sibling on its classpath. A future optimization would be a dedicated `module-boundary-tests` Maven module that runs earlier; out of scope for this plan.
- **Risk 4 — MyBatis-Plus extension classes transitively expose server-only types.** ArchUnit analyses actual classfile references, so Lombok-generated code and `@TableName` annotations won't trigger false positives (they're on server-side DOs). Verified on pre-flight.

---

## File Structure

### New files

```
launchers/standalone/src/test/java/com/tencent/supersonic/archunit/
├── ArchUnitSmokeTest.java          (Task 1 — proves the dep resolves)
└── ModuleBoundaryTest.java         (Tasks 2–8 — all rules live here)
```

### Modified files

```
common/pom.xml                      (Task 1 — add archunit-junit5 test dep)
.github/workflows/ubuntu-ci.yml     (Task 9 — no change needed; document in doc)
docs/details/platform/04-module-boundaries.md  (Task 10 — NEW doc file)
```

**Why `common/pom.xml` for the dep:** every module inherits from `common` (directly or transitively), and it's the only module with `<scope>test</scope>` dependencies already being propagated. Adding archunit there as `<scope>test</scope>` makes it visible to `launchers-standalone` without touching 10+ poms. The test classfile itself lives in `launchers/standalone` because that's the only module on whose classpath every other module resolves.

---

## Task 1: Add ArchUnit dependency and smoke-test

**Goal:** `mvn test -pl launchers/standalone -am -Dtest=ArchUnitSmokeTest` runs green, proving the dep and imports work.

**Files:**
- Modify: `common/pom.xml` — add 1 dependency
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ArchUnitSmokeTest.java`

- [ ] **Step 1: Add the archunit-junit5 dependency to `common/pom.xml`**

Open `/Users/xudong/git/supersonic/common/pom.xml`. Find the block ending at line 253–254:

```xml
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
            <version>${mockito-inline.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

Insert this new `<dependency>` block **immediately before the closing `</dependencies>` tag (line 255)**:

```xml
        <!-- ArchUnit: module boundary enforcement, see launchers/standalone test
             class ModuleBoundaryTest. 1.3.0 is the first release with full
             Java 21 classfile support. -->
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.3.0</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Create the smoke-test directory and file**

```bash
mkdir -p /Users/xudong/git/supersonic/launchers/standalone/src/test/java/com/tencent/supersonic/archunit
```

Create file `/Users/xudong/git/supersonic/launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ArchUnitSmokeTest.java` with exactly this content:

```java
package com.tencent.supersonic.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for the ArchUnit dependency wiring. If this passes, the
 * archunit-junit5 dep is resolved and classfile import works against the
 * full supersonic classpath. Real rules live in {@link ModuleBoundaryTest}.
 */
class ArchUnitSmokeTest {

    @Test
    void canImportSupersonicClasses() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.tencent.supersonic");

        // We expect well over 1000 production classes across all modules.
        assertTrue(classes.size() > 500,
                "Expected >500 imported classes but got " + classes.size()
                        + ". Is launchers-standalone missing a module dep?");
    }
}
```

- [ ] **Step 3: Run the smoke test**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ArchUnitSmokeTest
```

Expected: `BUILD SUCCESS`, `Tests run: 1, Failures: 0`.

If it fails with "Could not resolve dependencies for project … archunit-junit5", the Step 1 edit didn't land in `common/pom.xml` — re-check.

- [ ] **Step 4: Commit**

```bash
cd /Users/xudong/git/supersonic
git add common/pom.xml \
    launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ArchUnitSmokeTest.java
git commit -m "test(arch): add ArchUnit 1.3.0 and smoke test for classpath import"
```

---

## Task 2: Write `ModuleBoundaryTest` skeleton

**Goal:** Empty test class that compiles and runs with zero rules; will accumulate rules in subsequent tasks.

**Files:**
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Create the test file**

Create `/Users/xudong/git/supersonic/launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`:

```java
package com.tencent.supersonic.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;

/**
 * Enforces SuperSonic's inter-module dependency rules. Every rule in this
 * class is a guard against regressions of boundary decisions already made
 * (see docs/details/platform/04-module-boundaries.md and
 * docs/superpowers/plans/2026-04-14-headless-dto-boundary-migration.md).
 *
 * If a rule fires, do NOT relax it. Either fix the offending class or (if
 * the violation is pre-existing and large) file a grandfather subset via
 * ArchUnit's FREEZING_SUBSETS mechanism and open a cleanup ticket.
 */
@AnalyzeClasses(
        packages = "com.tencent.supersonic",
        importOptions = DO_NOT_INCLUDE_TESTS.class)
class ModuleBoundaryTest {

    // Rules added in Tasks 3–8.

}
```

- [ ] **Step 2: Compile check**

```bash
cd /Users/xudong/git/supersonic
mvn test-compile -pl launchers/standalone -am
```

Expected: `BUILD SUCCESS`. (An empty `@AnalyzeClasses` class compiles fine; ArchUnit doesn't require at least one `@ArchTest`.)

- [ ] **Step 3: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): add ModuleBoundaryTest skeleton"
```

---

## Task 3: Rule — `headless.api` must NOT depend on `headless.server`

**Goal:** The primary regression guard from the 2026-04-14 DTO migration plan. `headless.api` is the public contract; `headless.server` is MyBatis/Spring persistence internals. They must never flow backward.

**Files:**
- Modify: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Add imports and the rule**

Open the test class. Replace the existing `import` block (everything above `@AnalyzeClasses`) with:

```java
package com.tencent.supersonic.archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
```

Then replace the `// Rules added in Tasks 3–8.` comment with:

```java
    @ArchTest
    static final ArchRule headlessApi_shouldNotDependOn_headlessServer =
            noClasses()
                    .that().resideInAPackage("..headless.api..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..headless.server..")
                    .because("headless-api is a pure contract module; "
                            + "headless-server holds MyBatis DOs and Spring beans. "
                            + "See 2026-04-14-headless-dto-boundary-migration.md.");
```

- [ ] **Step 2: Run it — expect GREEN**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ModuleBoundaryTest
```

Expected: `Tests run: 1, Failures: 0`.

**If RED:** ArchUnit will print every offending class and the forbidden `headless.server.X` it depends on. **Stop the plan.** Capture the violation list, add it to this file's Risks section, and decide whether to (a) fix in this branch or (b) add `ArchConfiguration` `archRule.failOnEmptyShould=false` + `FreezingArchRule.freeze(rule)` to grandfather. Do not merely delete the rule.

- [ ] **Step 3: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): forbid headless.api -> headless.server imports"
```

---

## Task 4: Rule — no `*.api` package depends on any `*.server` package

**Goal:** Generalize the Task 3 rule across all modules: `chat.api`, `feishu.api`, `billing.api`, `auth.api`, `headless.api` all must be pure contracts. This catches future modules automatically.

**Files:**
- Modify: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Append the rule**

Add this `@ArchTest` immediately after the Task 3 rule:

```java
    @ArchTest
    static final ArchRule apiPackages_shouldNotDependOn_serverPackages =
            noClasses()
                    .that().resideInAPackage("..(*).api..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..chat.server..",
                            "..headless.server..",
                            "..feishu.server..",
                            "..billing.server..")
                    .because("*.api modules must be pure contracts "
                            + "(POJOs + service interfaces). Server-only "
                            + "implementations must stay in *.server.");
```

**Why explicit list instead of a wildcard:** ArchUnit's `resideInAPackage` supports only one captured `(*)` per pattern. `..(*).api..` + `..(*).server..` would let `chat.api` depend on `chat.server` (since both capture the same group), which is exactly what we want to forbid. Listing server packages explicitly is clearer and covers the 4 modules that have a `.server` sibling today. When a new `xxx.server` module lands, add it here (the future doc in Task 10 instructs this).

Note: `auth.authentication` and `auth.authorization` are **not** named `auth.server`, so the general rule skips them — they are handled by Task 6-bis (below, inside Task 4 continuation) and Task 10's docs.

- [ ] **Step 2: Add the auth-specific sub-rule**

Immediately after the rule above, append:

```java
    @ArchTest
    static final ArchRule authApi_shouldNotDependOn_authImpl =
            noClasses()
                    .that().resideInAPackage("..auth.api..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..auth.authentication..",
                            "..auth.authorization..")
                    .because("auth-api is the public contract; "
                            + "auth-authentication and auth-authorization are "
                            + "the implementation modules.");
```

- [ ] **Step 3: Run — expect GREEN**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ModuleBoundaryTest
```

Expected: `Tests run: 3, Failures: 0`.

If RED on `authApi_shouldNotDependOn_authImpl`, apply the same stop-and-report protocol as Task 3.

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): forbid *.api -> *.server and auth.api -> auth impls"
```

---

## Task 5: Rule — `feishu.server` and `chat.server` cross-dependency ban

**Goal:** Feishu is a delivery channel; it must consume the chat layer only via `chat.api` (plus internal Feishu SDK). Chat must never call back into Feishu (event-driven via Spring `ApplicationEvent`, per `CLAUDE.md` "Cross-module communication").

**Files:**
- Modify: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Append the rule**

Append after the Task 4 rules:

```java
    @ArchTest
    static final ArchRule feishuServer_shouldNotDependOn_chatOrHeadlessServer =
            noClasses()
                    .that().resideInAPackage("..feishu.server..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..chat.server..",
                            "..headless.server..")
                    .because("feishu-server is a delivery channel. It must "
                            + "consume chat / headless only via their .api "
                            + "modules. Direct server deps were removed in "
                            + "the 2026-04-14 boundary migration.");

    @ArchTest
    static final ArchRule chatServer_shouldNotDependOn_feishuServer =
            noClasses()
                    .that().resideInAPackage("..chat.server..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..feishu.server..")
                    .because("chat-server must not know about feishu-server. "
                            + "Cross-module notifications go via Spring "
                            + "ApplicationEvent (see CLAUDE.md).");
```

- [ ] **Step 2: Run — expect GREEN**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ModuleBoundaryTest
```

Expected: `Tests run: 5, Failures: 0`.

- [ ] **Step 3: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): forbid cross-server deps between feishu and chat"
```

---

## Task 6: Rule — `common` has zero outbound deps on sibling modules

**Goal:** `common` is the foundation; every other module depends on it. Any `common` → sibling edge creates a cycle at the Maven level.

**Files:**
- Modify: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Append the rule**

Append after Task 5's rules:

```java
    @ArchTest
    static final ArchRule common_shouldNotDependOn_anySibling =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..auth..",
                            "..billing..",
                            "..chat..",
                            "..feishu..",
                            "..headless..")
                    .because("common is the shared base. Any outbound dep "
                            + "to a sibling module creates a circular "
                            + "Maven reactor graph.");
```

Note: `..common..` matches both `com.tencent.supersonic.common.*` and `launchers.common.*`. The launcher-common module is tiny and holds shared launcher config — we want the same guarantee there. If this yields false positives on the launcher-common package specifically, tighten to `..supersonic.common..` (adjust the `because` too).

- [ ] **Step 2: Run — expect GREEN**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ModuleBoundaryTest
```

Expected: `Tests run: 6, Failures: 0`.

**If RED on `launchers.common`:** change the `that()` clause to `.that().resideInAPackage("..supersonic.common..")` (keeps the rule strict for the real `common` module, exempts the launcher). Rerun.

- [ ] **Step 3: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): forbid common -> sibling module deps"
```

---

## Task 7: Rule — no Spring stereotypes in `.api` packages

**Goal:** `.api` modules must be pure contracts: POJOs, interfaces, enums, constants. Any `@Component / @Service / @Repository / @Controller / @RestController` in an `.api` package means an implementation leaked — consumers would be forced to bring Spring wiring they might not want.

**Files:**
- Modify: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Add the additional import**

Near the top imports in `ModuleBoundaryTest.java`, append this import (keep alphabetical order under `archunit.lang.syntax.ArchRuleDefinition`):

```java
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
// (already present from Task 3 — keep; no duplicate needed)
```

(No new import actually needed — we use only `noClasses()` + `beAnnotatedWith(String)`.)

- [ ] **Step 2: Append the rule**

Append after Task 6's rule:

```java
    @ArchTest
    static final ArchRule apiPackages_shouldNotContain_springStereotypes =
            noClasses()
                    .that().resideInAPackage("..(*).api..")
                    .should().beAnnotatedWith("org.springframework.stereotype.Component")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Controller")
                    .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .because("*.api modules must be pure contracts. "
                            + "Classes annotated with Spring stereotypes "
                            + "belong in *.server (or authentication/authorization).");
```

We reference annotations by string so the test doesn't require Spring on the unit-test classpath at parse time (though Spring is already there transitively via `common`).

- [ ] **Step 3: Run — expect GREEN**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ModuleBoundaryTest
```

Expected: `Tests run: 7, Failures: 0`.

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): forbid Spring stereotypes in *.api packages"
```

---

## Task 8: Rule — no package cycles across top-level module slices

**Goal:** Detect cycles among the top-level supersonic modules (e.g. `chat → feishu → chat`). ArchUnit's `slices()` API does exactly this at the captured-group level.

**Files:**
- Modify: `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`

- [ ] **Step 1: Append the rule**

Append after Task 7's rule:

```java
    /**
     * Cycle check at the top-level module granularity. The slice pattern
     * captures the segment immediately after "supersonic." — e.g. "chat",
     * "headless", "feishu", "auth", "billing", "common". If class A in
     * supersonic.chat.* depends (transitively via same-slice calls) on
     * class B in supersonic.feishu.* which depends back on supersonic.chat.*,
     * ArchUnit reports the cycle with the full chain.
     */
    @ArchTest
    static final ArchRule noCyclesBetweenTopLevelModules =
            slices()
                    .matching("..supersonic.(*)..")
                    .should().beFreeOfCycles();
```

- [ ] **Step 2: Run — expect GREEN**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -am -Dtest=ModuleBoundaryTest
```

Expected: `Tests run: 8, Failures: 0`.

Expected output on the console for a passing run:

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: N.NNN s — in com.tencent.supersonic.archunit.ModuleBoundaryTest
```

Expected output if the rule fires (for reference — copied from ArchUnit 1.3.0 docs, for the reviewer, **do not expect this**):

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] — Rule 'slices matching '..supersonic.(*)..' should be free of cycles' was violated (1 times):
Cycle detected in slices ...supersonic.(chat) -> ...supersonic.(feishu) -> ...supersonic.(chat)
  1. Dependencies of slice ...supersonic.(chat)
     - Method <com.tencent.supersonic.chat.server.X.foo()> calls method <com.tencent.supersonic.feishu.server.Y.bar()> in (X.java:42)
  2. Dependencies of slice ...supersonic.(feishu)
     - Method <com.tencent.supersonic.feishu.server.Y.bar()> calls method <com.tencent.supersonic.chat.server.X.baz()> in (Y.java:17)
```

- [ ] **Step 3: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
git commit -m "test(arch): enforce no cycles between top-level supersonic modules"
```

---

## Task 9: Confirm CI runs the new test (no workflow change needed)

**Goal:** Verify the existing `.github/workflows/ubuntu-ci.yml` already runs `ModuleBoundaryTest`, document this, and add a short README-level pointer. **No workflow YAML edits required** — this is a verification task.

**Files:**
- Read-only: `.github/workflows/ubuntu-ci.yml`, `.github/workflows/mac-ci.yml`, `.github/workflows/windows-ci.yml`, `.github/workflows/centos-ci.yml`
- Output: confirmation in Task 10's doc (written there, not here)

- [ ] **Step 1: Read each workflow yaml**

```bash
cd /Users/xudong/git/supersonic
for f in .github/workflows/*.yml; do
  echo "=== $f ==="
  grep -E '^\s*(run|name):' "$f" | head -20
done
```

Expected: at minimum `ubuntu-ci.yml` contains a step `run: mvn test`. Confirmed during plan writing — line 38–39 of `ubuntu-ci.yml` has `- name: Test with Maven` / `run: mvn test`. This runs every module's surefire tests, including `launchers-standalone`.

- [ ] **Step 2: Verify `mvn test` from the repo root exercises `ModuleBoundaryTest`**

```bash
cd /Users/xudong/git/supersonic
mvn test -pl launchers/standalone -Dtest=ModuleBoundaryTest
```

Expected: 8 tests run, 0 failures. (This confirms surefire discovers the class via `@ArchTest` — archunit-junit5 ships a JUnit Platform engine that's auto-detected.)

- [ ] **Step 3: No commit**

Nothing to commit — this is a verification task. Proceed to Task 10.

---

## Task 10: Document the rules in `docs/details/platform/`

**Goal:** Give future contributors a self-contained spec file explaining what the rules enforce, why, and how to add a new one. Per `CLAUDE.md`, platform-level specs live at `docs/details/platform/*.md` with frontmatter.

**Files:**
- Create: `docs/details/platform/04-module-boundaries.md`
- Modify: `docs/details/README.md` — add one line to the index

- [ ] **Step 1: Create the new doc file**

Write `/Users/xudong/git/supersonic/docs/details/platform/04-module-boundaries.md` with exactly this content:

```markdown
---
status: active
module: platform
key-files:
  - launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java
  - common/pom.xml
depends-on: []
---

# 模块边界强制（ArchUnit）

> 主文档：[智能运营数据中台设计方案](../../智能运营数据中台设计方案.md)
> 相关重构：[headless DTO boundary migration](../../superpowers/plans/2026-04-14-headless-dto-boundary-migration.md)

## 1. 为什么需要这些规则

SuperSonic 曾多次出现模块边界被破坏的情况：MyBatis DO 被错误地放进 `.api` 模块；
`feishu.server` 直接 import `headless.server` 的类；`chat.server` 绕过 `headless.api`
直接依赖 `headless.server`。这些问题都在上线后才被发现，回退成本高。

ArchUnit 在 `mvn test` 阶段就让构建失败，把"模块边界"从团队约定变成编译期约束。

## 2. 当前规则清单

所有规则写在 `launchers/standalone/src/test/java/com/tencent/supersonic/archunit/ModuleBoundaryTest.java`。

| # | 规则 | 目的 |
|---|------|------|
| 1 | `headless.api` 禁依赖 `headless.server` | DTO/契约不能反向依赖持久化实现 |
| 2 | 所有 `*.api` 禁依赖 `*.server` | 通用版本，适用于 chat/feishu/billing/headless |
| 3 | `auth.api` 禁依赖 `auth.authentication` / `auth.authorization` | auth 模块契约分离 |
| 4 | `feishu.server` 禁依赖 `chat.server` / `headless.server` | 飞书作为投递渠道，只能消费 `.api` |
| 5 | `chat.server` 禁依赖 `feishu.server` | 跨模块通知走 Spring `ApplicationEvent` |
| 6 | `common` 禁依赖任何同级模块 | `common` 必须是纯基础设施 |
| 7 | `*.api` 类禁用 Spring `@Component/@Service/@Repository/@Controller/@RestController` | API 是纯契约 |
| 8 | 顶级模块切片间无循环依赖 | 由 `slices().beFreeOfCycles()` 兜底 |

## 3. 运行方式

本地：
```bash
mvn test -pl launchers/standalone -Dtest=ModuleBoundaryTest
```

CI：`.github/workflows/ubuntu-ci.yml` 在 push / PR 到 `master` 时执行 `mvn test`，
覆盖所有模块（含 `launchers-standalone` 的 `ModuleBoundaryTest`）。其他三个平台的
CI（mac/windows/centos）同样跑 `mvn test`，多重保护。无需额外 workflow 配置。

## 4. 违规时怎么办

1. **读 ArchUnit 输出。** 它会打印每个违规类 + 禁止依赖的类，精确到行号。
2. **修复而不是绕过。** 99% 的违规是真 Bug（错放了类、加错了 pom 依赖、
   想走捷径）。按 `CLAUDE.md` 的模块边界把类移到正确的模块。
3. **只在批量遗留违规时才 freeze。** 如果是一次大规模重构还没收尾，可以用
   `FreezingArchRule.freeze(rule)` 临时冻结已知违规集合（`archunit_store/`
   目录），并立刻开清理 ticket。默认**不允许**。

## 5. 如何新增规则

在 `ModuleBoundaryTest.java` 里追加一个 `@ArchTest static final ArchRule …`
字段。两种最常用的模式：

### 5.1 禁止某模块依赖另一模块

```java
@ArchTest
static final ArchRule xxx_shouldNotDependOn_yyy =
        noClasses()
                .that().resideInAPackage("..xxx..")
                .should().dependOnClassesThat().resideInAPackage("..yyy..")
                .because("<明确的一句理由>");
```

### 5.2 禁止某注解出现在某包

```java
@ArchTest
static final ArchRule xxx_shouldNotBeAnnotatedWith_yyy =
        noClasses()
                .that().resideInAPackage("..xxx..")
                .should().beAnnotatedWith("fully.qualified.Annotation")
                .because("<明确的一句理由>");
```

### 5.3 新增 `*.server` 模块时

更新规则 #2 的 `resideInAnyPackage(...)` 列表，加入 `..<new>.server..`。

## 6. 常见误用

- **不要写 `..api..` 去匹配所有 `.api` 包。** ArchUnit 的 `..` 匹配的是
  "任意长度的包前缀"，`..api..` 会命中 `com.xxx.api.yyy` 但也会命中
  `com.xxx.internal.api.foo`。用 `..(*).api..` 配合捕获组更精确。
- **注解用全限定名字符串。** `beAnnotatedWith(String)` 避免把注解类拉进
  测试 classpath，也让规则对 annotation 版本变化更稳定。
- **`slices()` 的捕获组位置很关键。** `..supersonic.(*)..` 捕获
  `supersonic` 之后第一段（`chat`, `headless`, …），正好对应顶级模块。
  写成 `(*)..` 会捕获到 `com`，不符合本项目需求。
```

- [ ] **Step 2: Update the platform docs index**

Open `/Users/xudong/git/supersonic/docs/details/README.md`. Find the section listing `platform/` files. Add an entry for `04-module-boundaries.md`.

To locate the insert point, run:

```bash
grep -n "03-monitoring-alerts" /Users/xudong/git/supersonic/docs/details/README.md
```

Then use the Edit tool to insert this line **immediately after** the existing `03-monitoring-alerts` line, matching the surrounding format. Example target (exact surrounding text depends on the README layout — adapt to existing bullet style):

Before:
```markdown
- [03-monitoring-alerts.md](platform/03-monitoring-alerts.md) — 监控、告警、SLO
```

After:
```markdown
- [03-monitoring-alerts.md](platform/03-monitoring-alerts.md) — 监控、告警、SLO
- [04-module-boundaries.md](platform/04-module-boundaries.md) — ArchUnit 模块边界强制
```

If `docs/details/README.md` doesn't exist (unusual given `CLAUDE.md` references it), skip Step 2 — no other part of the plan depends on it.

- [ ] **Step 3: Commit**

```bash
cd /Users/xudong/git/supersonic
git add docs/details/platform/04-module-boundaries.md docs/details/README.md
git commit -m "docs(platform): document ArchUnit module boundary rules"
```

---

## Final Verification

- [ ] **Run the full test class one last time**

```bash
cd /Users/xudong/git/supersonic
mvn clean test -pl launchers/standalone -am -Dtest='ArchUnitSmokeTest,ModuleBoundaryTest'
```

Expected:
```
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Run the main compile gate (CLAUDE.md mandate)**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl launchers/standalone -am
```

Expected: `BUILD SUCCESS`.

- [ ] **Confirm the test lives in the standard test phase** — no additional `<execution>` is required in `launchers/standalone/pom.xml`. Surefire auto-discovers `@ArchTest` via the `archunit-junit5` engine.

---

## Summary

10 tasks. 8 rules. 1 new test class + 1 smoke test + 1 pom edit + 1 doc file + 1 README index update. All expected to go GREEN against current `master` per pre-flight scan. Ships behind existing `mvn test` CI gate — no workflow YAML changes required.
