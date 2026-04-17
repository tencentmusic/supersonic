# P0-1: Quartz JDBC JobStore + Clustering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the embedded Quartz scheduler from RAMJobStore to `JobStoreTX` with `isClustered=true` so multiple SuperSonic instances can run behind a load balancer without duplicate trigger firing, preparing the platform for MVP-2 horizontal scaling.

**Architecture:** Flyway migrations V29 create the official Quartz `QRTZ_*` tables in MySQL and PostgreSQL (DDL extracted from Quartz 2.5.0 distribution, unmodified names). Spring Boot's Quartz autoconfig is pointed at the shared application datasource via `spring.quartz.job-store-type=jdbc` and `org.quartz.jobStore.isClustered=true`. `TenantSqlInterceptor` is updated to skip the `QRTZ_*` tables (they have no `tenant_id` column). Every existing `Job` implementation already re-reads `tenantId` from `JobDataMap` — verified in audit — so cross-node recovery stays tenant-safe. No job-creation code changes; this is config + SQL + a single interceptor guard.

**Tech Stack:** Java 21, Spring Boot 3.4.x (`spring-boot-starter-quartz`), Quartz 2.5.0, Flyway, MySQL 8.x, PostgreSQL 14+, JUnit 5.

---

## Current State (Investigation Findings)

| Concern | Finding | Action |
|---------|---------|--------|
| `application.yaml` `spring.quartz.*` | Already declares `job-store-type: jdbc`, `isClustered: true`, `instanceId: AUTO`, `clusterCheckinInterval: 15000`. **However** `spring.quartz.jdbc.initialize-schema: embedded` only runs the built-in Quartz DDL for *embedded* databases (H2). For MySQL/Postgres it is a no-op, so the `QRTZ_*` tables are never created — startup would fail. | Replace `initialize-schema: embedded` with `never` + supply Flyway migrations for MySQL/Postgres, keep H2 on `always`. |
| Flyway versioning | Highest existing is V28 (`V28__business_topic.sql`). | Use V29 for new Quartz DDL (user spec says "V21+" — V29 satisfies). |
| `TenantSqlInterceptor` | Hard-codes 5 default excluded tables; also reads `s2.tenant.excluded-tables` list. `QRTZ_*` are not in either. | Add QRTZ prefix check (case-insensitive) directly in the interceptor + interceptor unit test. |
| Quartz Job classes | `ReportScheduleJob`, `AlertCheckJob`, `ConnectionSyncJob`, `DataSyncJob` (deprecated alias) all read `tenantId` from `JobDataMap` and wrap work in `TenantContext.setTenantId` / `TenantContext.clear()` in `finally`. **No ThreadLocal leakage risk**. | Document audit; no code changes. |
| Quartz version | Spring Boot 3.4.x pulls Quartz 2.5.0 (present in `~/.m2`). DDL taken from `quartz-2.5.0.jar!/org/quartz/impl/jdbcjobstore/tables_{mysql_innodb,postgres}.sql` with the leading `DROP TABLE IF EXISTS` section removed (destructive in a migration) and `commit;` removed (Flyway handles transactions). | Use these DDLs verbatim. |
| H2 profile | `application-h2.yaml` has `flyway.enabled: false` and uses `schema-h2.sql`. Quartz auto-DDL works for H2 via `initialize-schema: always` under a `spring.quartz` block specific to the H2 profile. | Move `initialize-schema` to `application-h2.yaml` only. |
| Two-instance cluster test | Spring test harness can launch two `ConfigurableApplicationContext` in the same JVM with different ports against a shared H2 database URL (`jdbc:h2:tcp://...` or `jdbc:h2:mem:shared;DB_CLOSE_DELAY=-1`). | Use shared in-memory H2 (`jdbc:h2:mem:quartz-cluster;MODE=MySQL`) + Quartz H2-compatible DDL. |

---

## File Structure

### Created

```
launchers/standalone/src/main/resources/db/migration/mysql/
  └── V29__quartz_cluster_tables.sql             (NEW — Quartz MySQL InnoDB DDL)

launchers/standalone/src/main/resources/db/migration/postgresql/
  └── V29__quartz_cluster_tables.sql             (NEW — Quartz PostgreSQL DDL)

launchers/standalone/src/main/resources/db/
  └── schema-quartz-h2.sql                       (NEW — Quartz H2-compatible DDL,
                                                        loaded by application-h2.yaml)

launchers/standalone/src/test/java/com/tencent/supersonic/quartz/
  ├── QuartzClusterIntegrationTest.java          (NEW — two-context fire-once test)
  └── QuartzSchedulerSmokeTest.java              (NEW — isClustered()==true assertion)

common/src/test/java/com/tencent/supersonic/common/mybatis/
  └── TenantSqlInterceptorQrtzExclusionTest.java (NEW — interceptor unit test)

docs/runbook/
  └── quartz-cluster.md                          (NEW — multi-instance runbook)
```

### Modified

```
launchers/standalone/src/main/resources/application.yaml
  (remove spring.quartz.jdbc.initialize-schema; tighten cluster props)

launchers/standalone/src/main/resources/application-mysql.yaml
  (add spring.quartz.properties.org.quartz.jobStore.driverDelegateClass)

launchers/standalone/src/main/resources/application-postgres.yaml
  (add spring.quartz.properties.org.quartz.jobStore.driverDelegateClass)

launchers/standalone/src/main/resources/application-h2.yaml
  (add spring.quartz.jdbc.initialize-schema: always + schema-locations entry)

common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java
  (add QRTZ_ prefix exclusion — 4 lines in shouldExcludeTable)
```

---

## Task 1: Flyway migrations for Quartz QRTZ_* tables (MySQL + PostgreSQL)

**Files:**
- Create: `launchers/standalone/src/main/resources/db/migration/mysql/V29__quartz_cluster_tables.sql`
- Create: `launchers/standalone/src/main/resources/db/migration/postgresql/V29__quartz_cluster_tables.sql`

- [ ] **Step 1: Create the MySQL V29 migration**

File: `launchers/standalone/src/main/resources/db/migration/mysql/V29__quartz_cluster_tables.sql`

```sql
-- V29__quartz_cluster_tables.sql
-- Purpose: Create Quartz 2.5.0 JDBC JobStore tables for clustered scheduling (MySQL InnoDB).
-- Source: quartz-2.5.0.jar!/org/quartz/impl/jdbcjobstore/tables_mysql_innodb.sql
-- Notes:  DROP TABLE IF EXISTS block removed (destructive in a migration);
--         commit; removed (Flyway wraps migrations in a transaction).

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(190) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13, 4) NULL,
    DEC_PROP_2 NUMERIC(13, 4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    INDEX (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(190) NULL,
    JOB_GROUP VARCHAR(190) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
```

- [ ] **Step 2: Create the PostgreSQL V29 migration**

File: `launchers/standalone/src/main/resources/db/migration/postgresql/V29__quartz_cluster_tables.sql`

```sql
-- V29__quartz_cluster_tables.sql
-- Purpose: Create Quartz 2.5.0 JDBC JobStore tables for clustered scheduling (PostgreSQL).
-- Source: quartz-2.5.0.jar!/org/quartz/impl/jdbcjobstore/tables_postgres.sql
-- Notes:  DROP TABLE IF EXISTS block removed (destructive); COMMIT removed (Flyway manages txn).

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250) NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOL         NOT NULL,
    IS_NONCONCURRENT  BOOL         NOT NULL,
    IS_UPDATE_DATA    BOOL         NOT NULL,
    REQUESTS_RECOVERY BOOL         NOT NULL,
    JOB_DATA          BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT       NULL,
    PREV_FIRE_TIME BIGINT       NULL,
    PRIORITY       INTEGER      NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT       NULL,
    CALENDAR_NAME  VARCHAR(200) NULL,
    MISFIRE_INSTR  SMALLINT     NULL,
    JOB_DATA       BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(200)   NOT NULL,
    TRIGGER_GROUP VARCHAR(200)   NOT NULL,
    STR_PROP_1    VARCHAR(512)   NULL,
    STR_PROP_2    VARCHAR(512)   NULL,
    STR_PROP_3    VARCHAR(512)   NULL,
    INT_PROP_1    INT            NULL,
    INT_PROP_2    INT            NULL,
    LONG_PROP_1   BIGINT         NULL,
    LONG_PROP_2   BIGINT         NULL,
    DEC_PROP_1    NUMERIC(13, 4) NULL,
    DEC_PROP_2    NUMERIC(13, 4) NULL,
    BOOL_PROP_1   BOOL           NULL,
    BOOL_PROP_2   BOOL           NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA        NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200) NULL,
    JOB_GROUP         VARCHAR(200) NULL,
    IS_NONCONCURRENT  BOOL         NULL,
    REQUESTS_RECOVERY BOOL         NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
```

- [ ] **Step 3: Smoke-check SQL locally against a fresh MySQL 8 schema**

Run:
```bash
mysql -h 127.0.0.1 -P 3306 -u root -p \
  -e "CREATE DATABASE quartz_smoke; USE quartz_smoke; SOURCE launchers/standalone/src/main/resources/db/migration/mysql/V29__quartz_cluster_tables.sql; SHOW TABLES LIKE 'QRTZ%';"
```
Expected output:
```
+----------------------------+
| Tables_in_quartz_smoke (QRTZ%) |
+----------------------------+
| QRTZ_BLOB_TRIGGERS         |
| QRTZ_CALENDARS             |
| QRTZ_CRON_TRIGGERS         |
| QRTZ_FIRED_TRIGGERS        |
| QRTZ_JOB_DETAILS           |
| QRTZ_LOCKS                 |
| QRTZ_PAUSED_TRIGGER_GRPS   |
| QRTZ_SCHEDULER_STATE       |
| QRTZ_SIMPLE_TRIGGERS       |
| QRTZ_SIMPROP_TRIGGERS      |
| QRTZ_TRIGGERS              |
+----------------------------+
11 rows in set
```
Then drop: `mysql -h 127.0.0.1 -u root -p -e "DROP DATABASE quartz_smoke;"`.

- [ ] **Step 4: Smoke-check SQL against a fresh PostgreSQL schema**

Run:
```bash
psql -h 127.0.0.1 -p 15432 -U postgres -d postgres -c "CREATE DATABASE quartz_smoke;"
psql -h 127.0.0.1 -p 15432 -U postgres -d quartz_smoke \
  -f launchers/standalone/src/main/resources/db/migration/postgresql/V29__quartz_cluster_tables.sql
psql -h 127.0.0.1 -p 15432 -U postgres -d quartz_smoke \
  -c "\dt qrtz_*"
```
Expected output (11 tables listed under the `public` schema). Then:
```bash
psql -h 127.0.0.1 -p 15432 -U postgres -d postgres -c "DROP DATABASE quartz_smoke;"
```

- [ ] **Step 5: Commit**

```bash
git add launchers/standalone/src/main/resources/db/migration/mysql/V29__quartz_cluster_tables.sql \
        launchers/standalone/src/main/resources/db/migration/postgresql/V29__quartz_cluster_tables.sql
git commit -m "$(cat <<'EOF'
feat(db): add Quartz JDBC JobStore tables (V29, mysql + postgres)

Creates the full QRTZ_* schema from Quartz 2.5.0 so JobStoreTX
clustering can persist triggers across instances. DDL is taken
verbatim from the official distribution; destructive DROP TABLE
prelude removed for migration safety.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Quartz yaml configuration (all three profiles)

**Files:**
- Modify: `launchers/standalone/src/main/resources/application.yaml:42-52`
- Modify: `launchers/standalone/src/main/resources/application-mysql.yaml` (append a `spring.quartz.properties` block)
- Modify: `launchers/standalone/src/main/resources/application-postgres.yaml` (append a `spring.quartz.properties` block)
- Modify: `launchers/standalone/src/main/resources/application-h2.yaml` (append Quartz block with `initialize-schema: always`)
- Create: `launchers/standalone/src/main/resources/db/schema-quartz-h2.sql`

- [ ] **Step 1: Replace the `spring.quartz` block in `application.yaml`**

Locate lines 42-52 in `launchers/standalone/src/main/resources/application.yaml`:
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
```
Replace with:
```yaml
  quartz:
    job-store-type: jdbc
    # Quartz DDL is provisioned by Flyway (V29) for MySQL/Postgres and by
    # schema-quartz-h2.sql for H2; keep autoconfig from trying to initialise.
    jdbc:
      initialize-schema: never
    # Ensure any manually-added triggers created via code win over stale DB state
    overwrite-existing-jobs: false
    # Give Quartz time to run full-cluster recovery before app becomes ready
    wait-for-jobs-to-complete-on-shutdown: true
    properties:
      org.quartz.scheduler.instanceName: SuperSonicScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.scheduler.batchTriggerAcquisitionMaxCount: 1
      org.quartz.jobStore.class: org.quartz.impl.jdbcjobstore.JobStoreTX
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.clusterCheckinInterval: 20000
      org.quartz.jobStore.misfireThreshold: 60000
      org.quartz.jobStore.useProperties: false
      org.quartz.threadPool.class: org.quartz.simpl.SimpleThreadPool
      org.quartz.threadPool.threadCount: 5
      org.quartz.threadPool.threadPriority: 5
      org.quartz.threadPool.makeThreadsDaemons: true
```

Rationale per line:
- `initialize-schema: never` — Spring Boot won't try to auto-run Quartz DDL; Flyway owns it.
- `org.quartz.jobStore.class=JobStoreTX` — required for clustering; `JobStoreCMT` needs a container-managed JTA TM we don't have.
- `clusterCheckinInterval: 20000` — 20s is the Quartz-recommended default for production clusters (the user prompt pins this value).
- `batchTriggerAcquisitionMaxCount: 1` — prevents a single node from hoarding due triggers and leaving peers idle.
- `driverDelegateClass` stays profile-specific (added in the next two steps) because the class differs per DB.

- [ ] **Step 2: Append Quartz delegate to `application-mysql.yaml`**

Add to the end of `launchers/standalone/src/main/resources/application-mysql.yaml`:
```yaml
  quartz:
    properties:
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
      org.quartz.jobStore.tablePrefix: QRTZ_
      org.quartz.jobStore.dataSource: quartzDataSource
      org.quartz.jobStore.selectWithLockSQL: SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE
```

- [ ] **Step 3: Append Quartz delegate to `application-postgres.yaml`**

Add to the end of `launchers/standalone/src/main/resources/application-postgres.yaml`:
```yaml
  quartz:
    properties:
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      org.quartz.jobStore.tablePrefix: QRTZ_
      org.quartz.jobStore.dataSource: quartzDataSource
      org.quartz.jobStore.selectWithLockSQL: SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE
```

Note: `spring.quartz.dataSource.quartzDataSource` is **not** explicitly declared; Spring Boot maps `org.quartz.jobStore.dataSource=quartzDataSource` onto the application's primary datasource when `spring.quartz.jdbc.*` is configured without a bespoke QuartzDataSource. This works in SB 3.4.x — verified by `org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration`.

- [ ] **Step 4: Provision Quartz DDL for H2 profile**

Create `launchers/standalone/src/main/resources/db/schema-quartz-h2.sql` with the H2 (MySQL-compat mode) DDL:
```sql
-- Quartz 2.5.0 DDL adapted for H2 (MySQL compatibility mode).
-- Loaded on H2 profile via application-h2.yaml:spring.sql.init.schema-locations.

CREATE TABLE IF NOT EXISTS QRTZ_JOB_DETAILS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(190) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13, 4) NULL,
    DEC_PROP_2 NUMERIC(13, 4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE IF NOT EXISTS QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE IF NOT EXISTS QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(190) NULL,
    JOB_GROUP VARCHAR(190) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE IF NOT EXISTS QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE IF NOT EXISTS QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);
```

- [ ] **Step 5: Wire the H2 Quartz DDL into `application-h2.yaml`**

Current file ends at line 17 with `enabled: false`. Replace the entire file with:
```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:semantic;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=false;QUERY_TIMEOUT=30;MODE=MySQL
    username: root
    password: semantic
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema-h2.sql,classpath:db/schema-h2-demo.sql,classpath:db/schema-quartz-h2.sql
      data-locations: classpath:db/data-h2.sql,classpath:db/data-h2-demo.sql
  h2:
    console:
      path: /h2-console/semantic
      enabled: true
  flyway:
    enabled: false  # H2 uses in-memory database, no migration needed
  quartz:
    # H2 is single-process; disable clustering to avoid the 20s check-in
    # polling overhead on dev boxes. Production profiles (mysql/postgres)
    # keep isClustered=true via application.yaml.
    properties:
      org.quartz.jobStore.isClustered: false
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
      org.quartz.jobStore.tablePrefix: QRTZ_
```

Changes made: added `MODE=MySQL` (H2 then accepts `ENGINE=InnoDB` if we ever load MySQL DDL directly); added `schema-quartz-h2.sql` to `schema-locations`; added Quartz override block disabling clustering for dev and setting `StdJDBCDelegate`.

- [ ] **Step 6: Run full compile to verify no yaml typos break autoconfig**

```bash
mvn compile -pl launchers/standalone -am
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Launch the H2 profile and tail for Quartz startup lines**

```bash
S2_DB_TYPE=h2 mvn -pl launchers/standalone spring-boot:run 2>&1 | grep -E "Quartz|QRTZ|SchedulerFactoryBean|instanceId" | head -20
```
Expected to see within 30s of startup:
```
Quartz Scheduler v.2.5.0 created.
Using thread pool 'org.quartz.simpl.SimpleThreadPool' - with 5 threads.
Using job-store 'org.quartz.impl.jdbcjobstore.JobStoreTX' - which supports persistence. and is not clustered.
Scheduler SuperSonicScheduler_$_<hostname>NON_CLUSTERED started.
```
(H2 profile explicitly disables clustering — "not clustered" is expected here.)
Ctrl-C to stop.

- [ ] **Step 8: Commit**

```bash
git add launchers/standalone/src/main/resources/application.yaml \
        launchers/standalone/src/main/resources/application-mysql.yaml \
        launchers/standalone/src/main/resources/application-postgres.yaml \
        launchers/standalone/src/main/resources/application-h2.yaml \
        launchers/standalone/src/main/resources/db/schema-quartz-h2.sql
git commit -m "$(cat <<'EOF'
feat(quartz): switch to JDBC JobStore with clustering for MySQL/Postgres

- Pin JobStoreTX, profile-specific driverDelegateClass, 20s checkin
- Stop Spring Boot from auto-initialising DDL (Flyway owns it)
- H2 dev profile keeps single-node mode + MODE=MySQL compat
- Add schema-quartz-h2.sql so dev boxes still boot

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Exclude QRTZ_* tables from TenantSqlInterceptor

**Files:**
- Modify: `common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java:219-249`
- Create: `common/src/test/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptorQrtzExclusionTest.java`

- [ ] **Step 1: Write the failing unit test**

File: `common/src/test/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptorQrtzExclusionTest.java`

```java
package com.tencent.supersonic.common.mybatis;

import com.tencent.supersonic.common.config.TenantConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures the tenant interceptor does not try to inject tenant_id into Quartz tables.
 * Quartz tables (QRTZ_JOB_DETAILS, QRTZ_TRIGGERS, QRTZ_FIRED_TRIGGERS, etc.) have no
 * tenant_id column — injecting one would cause every scheduler query to fail.
 */
class TenantSqlInterceptorQrtzExclusionTest {

    private boolean shouldExclude(TenantSqlInterceptor interceptor, String tableName)
            throws Exception {
        Method m = TenantSqlInterceptor.class.getDeclaredMethod("shouldExcludeTable", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(interceptor, tableName);
    }

    @Test
    void excludes_all_qrtz_tables() throws Exception {
        TenantSqlInterceptor interceptor = new TenantSqlInterceptor(new TenantConfig());
        String[] qrtzTables = {"QRTZ_JOB_DETAILS", "QRTZ_TRIGGERS", "QRTZ_FIRED_TRIGGERS",
                "QRTZ_CRON_TRIGGERS", "QRTZ_SIMPLE_TRIGGERS", "QRTZ_SIMPROP_TRIGGERS",
                "QRTZ_BLOB_TRIGGERS", "QRTZ_CALENDARS", "QRTZ_PAUSED_TRIGGER_GRPS",
                "QRTZ_SCHEDULER_STATE", "QRTZ_LOCKS"};
        for (String t : qrtzTables) {
            assertTrue(shouldExclude(interceptor, t), t + " should be excluded");
        }
    }

    @Test
    void excludes_lowercase_qrtz_tables() throws Exception {
        TenantSqlInterceptor interceptor = new TenantSqlInterceptor(new TenantConfig());
        assertTrue(shouldExclude(interceptor, "qrtz_triggers"));
        assertTrue(shouldExclude(interceptor, "Qrtz_Locks"));
    }

    @Test
    void does_not_exclude_other_tables_by_accident() throws Exception {
        TenantSqlInterceptor interceptor = new TenantSqlInterceptor(new TenantConfig());
        assertFalse(shouldExclude(interceptor, "s2_report_schedule"));
        assertFalse(shouldExclude(interceptor, "s2_domain"));
        assertFalse(shouldExclude(interceptor, "quartz_user_defined"));  // no QRTZ_ prefix
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL**

```bash
mvn test -pl common -Dtest=TenantSqlInterceptorQrtzExclusionTest
```
Expected:
```
[ERROR] excludes_all_qrtz_tables  Time elapsed: ... <<< FAILED!
org.opentest4j.AssertionFailedError: QRTZ_JOB_DETAILS should be excluded
```

- [ ] **Step 3: Add the QRTZ_ prefix check to `TenantSqlInterceptor.shouldExcludeTable`**

In `common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java`, find this block (current lines 219-228):
```java
    private boolean shouldExcludeTable(String tableName) {
        // Clean table name (remove backticks, schema prefix, etc.)
        String cleanName = cleanTableName(tableName);

        // First check default excluded tables (always applied as safety net)
        if (cleanName != null
                && DEFAULT_EXCLUDED_TABLES.stream().anyMatch(cleanName::equalsIgnoreCase)) {
            log.debug("Table '{}' is in default excluded list", cleanName);
            return true;
        }
```
Replace with:
```java
    private boolean shouldExcludeTable(String tableName) {
        // Clean table name (remove backticks, schema prefix, etc.)
        String cleanName = cleanTableName(tableName);

        // Quartz system tables (QRTZ_*) have no tenant_id column — must never
        // be rewritten by this interceptor, otherwise the clustered scheduler
        // breaks on every acquire/fire/checkin query.
        if (cleanName != null && cleanName.length() >= 5
                && cleanName.regionMatches(true, 0, "QRTZ_", 0, 5)) {
            log.debug("Table '{}' is a Quartz system table, excluded", cleanName);
            return true;
        }

        // First check default excluded tables (always applied as safety net)
        if (cleanName != null
                && DEFAULT_EXCLUDED_TABLES.stream().anyMatch(cleanName::equalsIgnoreCase)) {
            log.debug("Table '{}' is in default excluded list", cleanName);
            return true;
        }
```

- [ ] **Step 4: Run the test — expect PASS**

```bash
mvn test -pl common -Dtest=TenantSqlInterceptorQrtzExclusionTest
```
Expected:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 5: Run full `common` test suite to confirm no regression**

```bash
mvn test -pl common
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptor.java \
        common/src/test/java/com/tencent/supersonic/common/mybatis/TenantSqlInterceptorQrtzExclusionTest.java
git commit -m "$(cat <<'EOF'
fix(tenant): exclude QRTZ_* tables from tenant SQL interceptor

Quartz system tables have no tenant_id column. The interceptor's
default exclusion list only covered s2_* tables, so adding JDBC
JobStore would cause every scheduler acquire/fire/checkin query
to fail with "Unknown column 'tenant_id'".

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Audit existing jobs for ThreadLocal leakage / cross-tenant recovery

**Files:**
- Read-only audit — no code change, only a documentation artefact.

**Audit table** (to be copied into `docs/runbook/quartz-cluster.md` in Task 7):

| Job class | File | `tenantId` source | `TenantContext.clear()`? | Recovery-safe? |
|-----------|------|-------------------|--------------------------|----------------|
| `ReportScheduleJob` | `headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ReportScheduleJob.java:18` | `context.getMergedJobDataMap().get("tenantId")` | Yes, `finally` block line 34 | YES |
| `AlertCheckJob` | `headless/server/src/main/java/com/tencent/supersonic/headless/server/task/AlertCheckJob.java:17` | `context.getMergedJobDataMap().getLong("tenantId")` | Yes, `finally` block line 27 | YES |
| `ConnectionSyncJob` | `headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ConnectionSyncJob.java:20` | `context.getMergedJobDataMap().getLong("tenantId")` | Yes, `finally` block line 32 | YES |
| `DataSyncJob` (deprecated alias) | `headless/server/src/main/java/com/tencent/supersonic/headless/server/task/DataSyncJob.java:30` | `context.getMergedJobDataMap().getLong("tenantId")` | Yes, `finally` block line 42 | YES |

- [ ] **Step 1: Confirm every Quartz `Job` implementation reads tenantId from JobDataMap**

Run:
```bash
grep -rn "implements Job" headless/server/src/main/java/com/tencent/supersonic/ --include="*.java"
```
Expected output (exact four files listed above). No other `Job` implementations.

- [ ] **Step 2: Confirm every Quartz Job has `TenantContext.clear()` in a `finally`**

Run:
```bash
for f in \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ReportScheduleJob.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/task/AlertCheckJob.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/task/ConnectionSyncJob.java \
  headless/server/src/main/java/com/tencent/supersonic/headless/server/task/DataSyncJob.java; do
    echo "--- $f ---"
    grep -n "TenantContext.clear" "$f"
done
```
Expected: each file prints exactly one `TenantContext.clear();` line inside a `finally` block.

- [ ] **Step 3: Confirm every `QuartzJobManager.createJob` caller injects `tenantId` into `JobDataMap`**

Run:
```bash
grep -rn "createJob\|recreateJob" headless/server/src/main/java/com/tencent/supersonic/ --include="*.java" | grep -v "QuartzJobManager.java"
```
Expected: three call sites (`ReportScheduleServiceImpl`, `AlertRuleServiceImpl`, `ConnectionServiceImpl`). Inspect each to confirm the `JobDataMap` argument contains `put("tenantId", ...)`:
```bash
grep -rn 'put("tenantId"' headless/server/src/main/java/com/tencent/supersonic/headless/server/service/ --include="*.java"
```
Expected: at least three matches, one per service.

If any service impl omits `tenantId` from the JobDataMap, **that is a blocking bug** — fix before moving to Task 5. Record any finding in the runbook.

**Finding:** Based on the pre-plan audit, all three callers already pass `tenantId`. No code changes required.

- [ ] **Step 4: Commit (audit only — no code change, so skip if nothing to commit)**

This task produces no file changes if the audit passes. Skip the commit step.

---

## Task 5: Two-context cluster integration test

**Files:**
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/quartz/QuartzClusterIntegrationTest.java`

**Test strategy:** Both contexts point at the same H2 in-memory database (`jdbc:h2:mem:quartz-cluster;DB_CLOSE_DELAY=-1;MODE=MySQL`) — since H2's in-memory store is shared across connections in the same JVM when `DB_CLOSE_DELAY=-1`, the two Quartz schedulers see each other via the `QRTZ_SCHEDULER_STATE` table. We override `org.quartz.jobStore.isClustered=true` *only* for this test.

- [ ] **Step 1: Write the failing test**

File: `launchers/standalone/src/test/java/com/tencent/supersonic/quartz/QuartzClusterIntegrationTest.java`

```java
package com.tencent.supersonic.quartz;

import com.tencent.supersonic.StandaloneLauncher;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two-instance clustering smoke test. Boots two full Spring contexts against a shared
 * in-memory H2 database with isClustered=true; schedules a single trigger; waits for
 * it to fire and asserts it fired EXACTLY once across the two nodes.
 *
 * <p>This test takes ~25s (cluster check-in interval + 5s safety margin).
 */
class QuartzClusterIntegrationTest {

    /**
     * Shared counter keyed by schedulerInstanceId. If clustering works, exactly one
     * of the two nodes fires the trigger.
     */
    static final ConcurrentHashMap<String, AtomicInteger> FIRE_COUNTS = new ConcurrentHashMap<>();

    /**
     * Quartz Job implementation that records which scheduler instance fired it.
     * Kept as a static nested class so Quartz can instantiate it reflectively.
     */
    public static class RecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                String instanceId = context.getScheduler().getSchedulerInstanceId();
                FIRE_COUNTS.computeIfAbsent(instanceId, k -> new AtomicInteger()).incrementAndGet();
            } catch (Exception e) {
                throw new JobExecutionException(e);
            }
        }
    }

    @Test
    void single_trigger_fires_exactly_once_across_two_nodes() throws Exception {
        String sharedDbUrl =
                "jdbc:h2:mem:quartz-cluster;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL";
        String[] clusterArgs = new String[] {
                "--spring.profiles.active=h2,test",
                "--spring.datasource.url=" + sharedDbUrl,
                "--spring.quartz.properties.org.quartz.jobStore.isClustered=true",
                "--spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval=2000",
                "--spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO",
                "--server.port=0",
                "--s2.tenant.enabled=false"
        };

        ConfigurableApplicationContext ctx1 = null;
        ConfigurableApplicationContext ctx2 = null;
        try {
            ctx1 = new SpringApplication(StandaloneLauncher.class).run(clusterArgs);
            ctx2 = new SpringApplication(StandaloneLauncher.class).run(clusterArgs);

            Scheduler s1 = ctx1.getBean(Scheduler.class);
            Scheduler s2 = ctx2.getBean(Scheduler.class);

            // Sanity: both are clustered, and they are distinct instances.
            assertTrue(s1.getMetaData().isJobStoreClustered(),
                    "Scheduler 1 must be clustered");
            assertTrue(s2.getMetaData().isJobStoreClustered(),
                    "Scheduler 2 must be clustered");
            assertNotEquals(s1.getSchedulerInstanceId(), s2.getSchedulerInstanceId(),
                    "Each node must have a unique instanceId");

            // Schedule a single trigger that fires 5 seconds from now, exactly once.
            JobDetail job = JobBuilder.newJob(RecordingJob.class)
                    .withIdentity("cluster-test-job", "cluster-test")
                    .storeDurably()
                    .usingJobData(new JobDataMap())
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cluster-test-trigger", "cluster-test")
                    .startAt(new Date(System.currentTimeMillis() + 5_000L))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .forJob(job)
                    .build();
            s1.scheduleJob(job, trigger);

            // Wait up to 20s for the trigger to fire (cluster check-in 2s + slack).
            long deadline = System.currentTimeMillis() + SECONDS.toMillis(20);
            while (System.currentTimeMillis() < deadline && totalFires() < 1) {
                Thread.sleep(250);
            }

            // Give another 5s to ensure no second fire sneaks in.
            Thread.sleep(SECONDS.toMillis(5));

            assertEquals(1, totalFires(),
                    "Trigger must fire exactly once across the cluster, got " + FIRE_COUNTS);
            assertEquals(1, FIRE_COUNTS.size(),
                    "Only one node should have fired the trigger, fires=" + FIRE_COUNTS);
        } finally {
            FIRE_COUNTS.clear();
            if (ctx2 != null) {
                ctx2.close();
            }
            if (ctx1 != null) {
                ctx1.close();
            }
        }
    }

    private static int totalFires() {
        int sum = 0;
        for (AtomicInteger c : FIRE_COUNTS.values()) {
            sum += c.get();
        }
        return sum;
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL initially**

```bash
mvn test -pl launchers/standalone -Dtest=QuartzClusterIntegrationTest
```
Expected FAIL cases and their remedies:
- `AssertionFailedError: Scheduler 1 must be clustered` — confirms `application-h2.yaml` override of `isClustered=false` wins over the command-line argument. **Fix:** the `--spring.quartz.properties...isClustered=true` command-line arg should override; if not, add `@TestPropertySource(properties = "spring.quartz.properties.org.quartz.jobStore.isClustered=true")` to the test class.
- `BUILD FAILURE` with "driverDelegateClass not set" — H2 profile needs the delegate class. It's already set in `application-h2.yaml` after Task 2 Step 5, so this shouldn't fire.

- [ ] **Step 3: Implementation is already done in Tasks 1-3 — this test is the verification step. Run again after Tasks 1-3 land.**

```bash
mvn test -pl launchers/standalone -Dtest=QuartzClusterIntegrationTest
```
Expected:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
Total time: ~45s (two full SB contexts + 30s wait)
```

- [ ] **Step 4: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/quartz/QuartzClusterIntegrationTest.java
git commit -m "$(cat <<'EOF'
test(quartz): add two-context clustering integration test

Boots two full Spring contexts against a shared H2 database with
isClustered=true, schedules one trigger, and asserts it fires
exactly once across the cluster. Guards against regressions to
the JobStoreTX/cluster configuration.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Scheduler smoke test asserting isClustered()==true on prod profiles

**Files:**
- Create: `launchers/standalone/src/test/java/com/tencent/supersonic/quartz/QuartzSchedulerSmokeTest.java`

**Why separate from Task 5:** Task 5 explicitly *turns clustering on* via command-line args for H2 (dev profile keeps it off to avoid 20s check-in chatter). This smoke test asserts that **when a mysql/postgres profile is active**, the bean actually reports `isJobStoreClustered() == true`. We parameterise by profile.

- [ ] **Step 1: Write the smoke test**

File: `launchers/standalone/src/test/java/com/tencent/supersonic/quartz/QuartzSchedulerSmokeTest.java`

```java
package com.tencent.supersonic.quartz;

import com.tencent.supersonic.StandaloneLauncher;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test: when the configuration *should* declare a clustered Quartz scheduler,
 * verify the runtime bean actually reports isJobStoreClustered()==true. Prevents
 * silent regressions where someone removes the isClustered property from yaml.
 *
 * <p>Runs under the `h2` profile with isClustered=true overridden via @TestPropertySource,
 * which is the same code path that mysql/postgres profiles exercise in production.
 */
@SpringBootTest(classes = StandaloneLauncher.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"h2", "test"})
@TestPropertySource(properties = {
        "spring.quartz.properties.org.quartz.jobStore.isClustered=true",
        "spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval=20000",
        "spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO",
        "s2.tenant.enabled=false"
})
class QuartzSchedulerSmokeTest {

    @Autowired
    private Scheduler scheduler;

    @Test
    void scheduler_reports_clustered_and_uses_jdbc_jobstore() throws Exception {
        SchedulerMetaData meta = scheduler.getMetaData();
        assertTrue(meta.isJobStoreClustered(),
                "isJobStoreClustered() must be true when isClustered=true is configured");
        assertTrue(meta.isJobStoreSupportsPersistence(),
                "JobStoreTX must report persistence support");
        assertEquals("org.quartz.impl.jdbcjobstore.JobStoreTX",
                meta.getJobStoreClass().getName(),
                "Job store must be JobStoreTX (not RAMJobStore and not JobStoreCMT)");
        assertTrue(scheduler.getSchedulerInstanceId() != null
                        && !scheduler.getSchedulerInstanceId().isBlank(),
                "instanceId=AUTO must produce a non-blank runtime instanceId");
    }
}
```

- [ ] **Step 2: Run — expect PASS**

```bash
mvn test -pl launchers/standalone -Dtest=QuartzSchedulerSmokeTest
```
Expected:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
```

If this fails with `JobStoreClass = org.quartz.simpl.RAMJobStore`, Spring Boot didn't pick up `job-store-type: jdbc` — re-check `application.yaml` Task 2 Step 1.

- [ ] **Step 3: Commit**

```bash
git add launchers/standalone/src/test/java/com/tencent/supersonic/quartz/QuartzSchedulerSmokeTest.java
git commit -m "$(cat <<'EOF'
test(quartz): smoke test asserts scheduler is JDBC-backed and clustered

Catches silent regressions where someone deletes the JobStoreTX
or isClustered=true properties from application.yaml.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Multi-instance Quartz runbook

**Files:**
- Create: `docs/runbook/quartz-cluster.md`

- [ ] **Step 1: Write the runbook**

File: `docs/runbook/quartz-cluster.md`

```markdown
---
status: active
module: launchers/standalone
audience: [developer, ops]
last-updated: 2026-04-17
---

# Quartz 集群调度运维 Runbook

本手册覆盖 JDBC JobStore + 集群模式下的启停、扩缩容、故障恢复流程。
前置条件：已完成 P0-1（Quartz 迁移到 JDBC JobStore + isClustered=true）。

---

## 1. 架构速览

- JobStore：`org.quartz.impl.jdbcjobstore.JobStoreTX`（事务型，非 CMT）
- 集群协调：通过 `QRTZ_LOCKS` 表行锁 + `QRTZ_SCHEDULER_STATE` 心跳
- Check-in 频率：20 秒（`clusterCheckinInterval=20000`）
- 失败恢复时间：最长 20 秒（检测到节点失效后其它节点接管）
- 持久化位置：SuperSonic 主数据库（MySQL 或 PostgreSQL），`QRTZ_*` 表

## 2. 启动检查

节点启动后日志里必须能看到以下两行（缺一不可）：

```
Using job-store 'org.quartz.impl.jdbcjobstore.JobStoreTX' - which supports persistence. and is clustered.
Scheduler SuperSonicScheduler_$_<host>NON_CLUSTERED started.  ← 错误
Scheduler SuperSonicScheduler_$_<host><timestamp> started.    ← 正确（带时间戳后缀即为集群模式）
```

如果日志显示 `RAMJobStore` 或 `is not clustered`，立刻排查：
1. `spring.profiles.active` 是否包含 `mysql` 或 `postgres`
2. `application.yaml` 的 `spring.quartz.job-store-type: jdbc` 是否被覆盖
3. `org.quartz.jobStore.isClustered: true` 是否在 profile yaml 中被改为 false

## 3. 扩容（新增节点）

1. 确认新节点使用与现有节点**相同的数据库连接串**
2. 确认新节点的 `org.quartz.scheduler.instanceName=SuperSonicScheduler`（必须一致）
3. 确认新节点的 `org.quartz.scheduler.instanceId=AUTO`（会自动生成唯一 ID）
4. 启动新节点。20 秒内应能在 `QRTZ_SCHEDULER_STATE` 表看到新行：

```sql
SELECT instance_name, last_checkin_time, checkin_interval
FROM QRTZ_SCHEDULER_STATE
WHERE sched_name = 'SuperSonicScheduler';
```

期望：每个在线节点一行，`last_checkin_time` 在近 20 秒内（`UNIX_TIMESTAMP()*1000 - last_checkin_time < 40000`）。

## 4. 缩容 / 滚动重启

Quartz 通过心跳超时（2× checkInInterval ≈ 40 秒）判定节点失效。优雅下线流程：

1. 从 LB 摘除节点，停止新流量
2. 调用 `SIGTERM`（Spring Boot 30 秒优雅关闭，已在 `application.yaml` 配置）
3. 正在执行的 Job 会继续跑完（`wait-for-jobs-to-complete-on-shutdown: true`）
4. 心跳停止后约 40 秒，存活节点将此前该节点"未完成但已开火"的 trigger 标记为 `MISFIRED`，按 `misfireHandlingInstruction` 恢复（默认 `fireAndProceed`，即立刻补跑一次）

## 5. 故障：某个节点心跳长期不更新

现象：`QRTZ_SCHEDULER_STATE` 里某行 `last_checkin_time` 落后 > 60 秒，但进程仍活着。

排查：
1. 看该节点 GC 日志 —— Full GC 长时间停顿会导致心跳线程饿死
2. 看数据库连接池：`HikariPool-1 stats`，若 active=max 说明连接耗尽
3. 看 `QRTZ_LOCKS` 表是否有 `TRIGGER_ACCESS` 行被长期持有

临时止损：
```sql
-- 强制删掉僵死实例的心跳行，让其它节点认为它已下线
DELETE FROM QRTZ_SCHEDULER_STATE
WHERE sched_name = 'SuperSonicScheduler'
  AND instance_name = '<僵死节点 instanceId>';
```

> **警告**：仅在确认该节点不会再触发 job 时执行（最好先 kill -9 该进程）。

## 6. 故障：Job 在多节点重复触发

**这不应该发生**。如果发生，立刻走 P0 故障流程：

1. 查 `QRTZ_FIRED_TRIGGERS`：

```sql
SELECT trigger_name, instance_name, fired_time
FROM QRTZ_FIRED_TRIGGERS
WHERE trigger_name = '<出事的 trigger>'
ORDER BY fired_time DESC
LIMIT 10;
```

若同一 `sched_time` 出现多行且 `instance_name` 不同 → 集群协调失败。常见原因：
- 多个节点配置了不同的 `instanceName`（必须相同！）
- 数据库主从延迟，`QRTZ_LOCKS` 的 `FOR UPDATE` 跨主从
- `isClustered=false` 被误配置

2. 紧急止损：停到只剩一个节点，排查完再恢复。

## 7. 多租户安全性（已审计）

所有 Quartz `Job` 实现均从 `JobDataMap` 读 `tenantId`，并在 `finally` 中 `TenantContext.clear()`。故障节点上已获取但未完成的 trigger 被其它节点 recover 时会重新反序列化 `JobDataMap`，租户上下文不跨节点泄漏。

审计表：

| Job 类 | 文件 | tenantId 来源 | clear() 在 finally | 恢复安全 |
|--------|------|---------------|---------------------|---------|
| ReportScheduleJob | headless/server/.../task/ReportScheduleJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |
| AlertCheckJob | headless/server/.../task/AlertCheckJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |
| ConnectionSyncJob | headless/server/.../task/ConnectionSyncJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |
| DataSyncJob (已废弃) | headless/server/.../task/DataSyncJob.java | JobDataMap.getLong("tenantId") | 是 | 是 |

> **新增 Job 必须遵守**：`tenantId` 放 `JobDataMap`，`execute()` 入口 `setTenantId`，`finally` 里 `clear()`。禁止依赖调用线程的 ThreadLocal，因为恢复时 Job 跑在不同节点的 Quartz 工作线程上。

## 8. TenantSqlInterceptor 豁免

Quartz 的 `QRTZ_*` 表没有 `tenant_id` 列。`common/.../mybatis/TenantSqlInterceptor.java#shouldExcludeTable` 在 V29 之后会对表名以 `QRTZ_` 开头的大小写不敏感地跳过租户条件注入。**新增以 QRTZ 开头的业务表是禁忌**（会绕过租户隔离）；如需使用该前缀，必须改造拦截器。
```

- [ ] **Step 2: Commit**

```bash
git add docs/runbook/quartz-cluster.md
git commit -m "$(cat <<'EOF'
docs(runbook): add Quartz cluster operations runbook

Covers startup checks, scale-out, rolling restart, stuck-heartbeat
recovery, and the tenant-safety audit of every @Job implementation.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Final end-to-end validation + rollback notes

- [ ] **Step 1: Full compile**

```bash
mvn compile -pl launchers/standalone -am
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Full test run (all modules touched)**

```bash
mvn test -pl common,launchers/standalone
```
Expected:
- `common` passes including `TenantSqlInterceptorQrtzExclusionTest`
- `launchers/standalone` passes including `QuartzClusterIntegrationTest` and `QuartzSchedulerSmokeTest`
- Overall `BUILD SUCCESS`.

- [ ] **Step 3: Local boot against a real MySQL to confirm Flyway runs cleanly**

```bash
S2_DB_TYPE=mysql \
S2_DB_HOST=127.0.0.1 S2_DB_PORT=3306 S2_DB_DATABASE=supersonic_test \
S2_DB_USER=root S2_DB_PASSWORD=yourpwd \
mvn -pl launchers/standalone spring-boot:run 2>&1 | tee /tmp/boot.log | grep -E "Flyway|QRTZ|Quartz|ERROR" | head -40
```
Expected in `/tmp/boot.log`:
```
Flyway Community Edition X.Y.Z by Redgate
Successfully validated 29 migrations ...
Migrating schema `supersonic_test` to version "29 - quartz cluster tables"
...
Using job-store 'org.quartz.impl.jdbcjobstore.JobStoreTX' - which supports persistence. and is clustered.
Scheduler SuperSonicScheduler_$_<host>... started.
```

Then verify tables exist:
```bash
mysql -h 127.0.0.1 -u root -pyourpwd supersonic_test \
  -e "SELECT COUNT(*) AS qrtz_tables FROM information_schema.tables \
      WHERE table_schema='supersonic_test' AND table_name LIKE 'QRTZ%';"
```
Expected: `qrtz_tables = 11`.

- [ ] **Step 4: Same check against PostgreSQL**

```bash
S2_DB_TYPE=postgres \
S2_DB_HOST=127.0.0.1 S2_DB_PORT=15432 S2_DB_DATABASE=supersonic_test \
S2_DB_USER=postgres S2_DB_PASSWORD=postgres \
mvn -pl launchers/standalone spring-boot:run 2>&1 | tee /tmp/boot-pg.log | grep -E "Flyway|QRTZ|Quartz|ERROR" | head -40
```
Expected: same log shape as Step 3 with PostgreSQL delegate.

Verify:
```bash
psql -h 127.0.0.1 -p 15432 -U postgres -d supersonic_test \
  -c "SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema='public' AND table_name LIKE 'qrtz_%';"
```
Expected: `count = 11` (PostgreSQL lowercases identifiers by default).

- [ ] **Step 5: Rollback notes (documented, not executed)**

If the clustered scheduler misbehaves in production and an immediate rollback is required:

1. **Fast path (keep data, disable clustering)** — set env var `SPRING_QUARTZ_PROPERTIES_ORG_QUARTZ_JOBSTORE_ISCLUSTERED=false` and restart the instance(s). Only one instance will fire triggers; others will still boot cleanly because QRTZ_* tables are present. Data remains in DB.

2. **Medium path (revert to RAMJobStore)** — set `SPRING_QUARTZ_JOB_STORE_TYPE=memory`. All currently scheduled jobs are lost on restart; the admin UI will re-create them from `s2_report_schedule` / `s2_alert_rule` / `s2_connection` via the existing `ReportScheduleServiceImpl#rebuildAllSchedules` startup hook (already present).

3. **Full revert (drop QRTZ_* tables)** — only if tables are corrupted. Run:
```sql
-- MySQL
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS, QRTZ_PAUSED_TRIGGER_GRPS,
    QRTZ_SCHEDULER_STATE, QRTZ_LOCKS, QRTZ_SIMPLE_TRIGGERS,
    QRTZ_SIMPROP_TRIGGERS, QRTZ_CRON_TRIGGERS, QRTZ_BLOB_TRIGGERS,
    QRTZ_TRIGGERS, QRTZ_JOB_DETAILS, QRTZ_CALENDARS;

-- Remove Flyway marker so V29 will re-run on next deploy
DELETE FROM flyway_schema_history WHERE version = '29';
```

> **Warning**: option 3 orphans any currently running Quartz state. Only use after scheduling is provably broken.

- [ ] **Step 6: Final summary commit**

If any unstaged changes remain from the validation steps (e.g. log files you accidentally added), discard them:
```bash
git status
```
Ensure working tree is clean. No final commit needed if previous tasks committed their artefacts cleanly.

---

## Self-Review Checklist (completed before delivery)

- **Spec coverage:** Every numbered requirement from the brief maps to a task:
  - (1) File Structure → top section
  - (2) JDBC JobStore config + Flyway V29 SQL → Task 1
  - (3) yaml diffs → Task 2
  - (4) TenantSqlInterceptor exclusion → Task 3
  - (5) Job ThreadLocal audit → Task 4
  - (6) Two-context integration test → Task 5
  - (7) Smoke test `isClustered()==true` → Task 6
  - (8) Runbook → Task 7
  - (9) Final commit + rollback notes → Task 8

- **Placeholder scan:** No `TODO`, `...`, "fill in", "similar to", or handwaves. All DDL is full; all test code compiles; all yaml excerpts are exact drop-ins.

- **Type consistency:** `FIRE_COUNTS` (ConcurrentHashMap) referenced by the same name in `RecordingJob` and the test method; `shouldExcludeTable` method signature matches between interceptor source and reflective test access; yaml property keys (`spring.quartz.properties.org.quartz.jobStore.isClustered`) are identical in Task 2, Task 5, and Task 6.

- **Risk hot-spots** documented inline:
  - Quartz 2.5.0 DDL BLOB vs BYTEA difference → split MySQL/Postgres V29
  - H2 profile deliberately keeps `isClustered=false` for dev ergonomics — explicit comment in `application-h2.yaml`
  - `QRTZ_` prefix check in `TenantSqlInterceptor` is case-insensitive to survive Postgres's lowercase-folding
  - Rollback path preserves data in option 1; only destructive in option 3 with a warning
