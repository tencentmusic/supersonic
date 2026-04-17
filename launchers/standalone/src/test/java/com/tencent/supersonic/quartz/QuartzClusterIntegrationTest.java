package com.tencent.supersonic.quartz;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JDBC JobStore integration test: verifies that Quartz can persist and execute triggers via the
 * QRTZ_* schema (created by schema-quartz-h2.sql / V29 migrations) using JobStoreTX.
 *
 * <p>
 * <b>Cluster scope note:</b> Verifying that two nodes do NOT double-fire a trigger requires two
 * separate OS processes sharing a real database; that level of test is out of scope for a
 * unit/integration test run. The single-firing guarantee is provided by Quartz's QRTZ_LOCKS table
 * using {@code SELECT ... FOR UPDATE} — a DB-level primitive that JVM-local testing cannot exercise
 * meaningfully. This test validates the JDBC infrastructure layer.
 */
class QuartzClusterIntegrationTest {

    static final AtomicInteger FIRE_COUNT = new AtomicInteger(0);

    private Scheduler scheduler;
    private DataSource ds;

    public static class RecordingJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            FIRE_COUNT.incrementAndGet();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        FIRE_COUNT.set(0);

        JdbcDataSource h2ds = new JdbcDataSource();
        h2ds.setURL(
                "jdbc:h2:mem:quartz-integration-test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL");
        h2ds.setUser("sa");
        ds = h2ds;
        initSchema(ds);

        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "IntegrationTestScheduler");
        props.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.threadPool.threadCount", "2");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.setProperty("org.quartz.jobStore.isClustered", "true");
        props.setProperty("org.quartz.jobStore.clusterCheckinInterval", "2000");
        props.setProperty("org.quartz.jobStore.driverDelegateClass",
                "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        props.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.setProperty("org.quartz.jobStore.dataSource", "testDs");
        props.setProperty("org.quartz.dataSource.testDs.provider", "hikaricp");
        props.setProperty("org.quartz.dataSource.testDs.driver", "org.h2.Driver");
        props.setProperty("org.quartz.dataSource.testDs.URL",
                "jdbc:h2:mem:quartz-integration-test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL");
        props.setProperty("org.quartz.dataSource.testDs.user", "sa");
        props.setProperty("org.quartz.dataSource.testDs.maxConnections", "3");

        scheduler = new StdSchedulerFactory(props).getScheduler();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (scheduler != null && scheduler.isStarted()) {
            scheduler.shutdown(true);
        }
        FIRE_COUNT.set(0);
    }

    @Test
    void jobstore_is_clustered_jdbc_and_trigger_fires_via_qrtz_tables() throws Exception {
        // 1. Verify the scheduler is using JobStoreTX in cluster mode.
        assertTrue(scheduler.getMetaData().isJobStoreClustered(),
                "JobStoreTX must report isClustered=true");
        assertTrue(scheduler.getMetaData().isJobStoreSupportsPersistence(),
                "JobStoreTX must report persistence support");
        assertEquals("org.quartz.impl.jdbcjobstore.JobStoreTX",
                scheduler.getMetaData().getJobStoreClass().getName());

        // 2. Schedule a trigger that fires 1 second from now.
        scheduler.start();

        JobDetail job = JobBuilder.newJob(RecordingJob.class)
                .withIdentity("integration-test-job", "integration-test").storeDurably()
                .usingJobData(new JobDataMap()).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("integration-test-trigger", "integration-test")
                .startAt(new Date(System.currentTimeMillis() + 1_000L))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .forJob(job).build();
        scheduler.scheduleJob(job, trigger);

        // 3. Wait up to 10s for the trigger to fire.
        long deadline = System.currentTimeMillis() + SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline && FIRE_COUNT.get() < 1) {
            Thread.sleep(200);
        }

        assertEquals(1, FIRE_COUNT.get(),
                "Trigger should have fired exactly once via JDBC JobStore");

        // 4. Verify the scheduler registered itself in QRTZ_SCHEDULER_STATE.
        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM QRTZ_SCHEDULER_STATE"
                        + " WHERE SCHED_NAME = 'IntegrationTestScheduler'")) {
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 1,
                    "Scheduler must have registered its heartbeat in QRTZ_SCHEDULER_STATE");
        }
    }

    private static void initSchema(DataSource dataSource) throws Exception {
        try (InputStream is = QuartzClusterIntegrationTest.class.getClassLoader()
                .getResourceAsStream("db/schema-quartz-h2.sql")) {
            if (is == null) {
                throw new IllegalStateException("schema-quartz-h2.sql not found on classpath");
            }
            String ddl = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = dataSource.getConnection();
                    Statement stmt = conn.createStatement()) {
                for (String sql : ddl.split(";")) {
                    // Strip leading comment lines so that blocks like
                    // "-- comment\nCREATE TABLE..." are not accidentally skipped.
                    String stripped = sql.replaceAll("(?m)^\\s*--[^\n]*", "").trim();
                    if (!stripped.isEmpty()) {
                        stmt.execute(stripped);
                    }
                }
            }
        }
    }
}
