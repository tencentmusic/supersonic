package com.tencent.supersonic.quartz;

import com.tencent.supersonic.BaseApplication;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring-wired smoke test: verifies the Quartz Scheduler bean is correctly configured to use a
 * persistent JDBC-backed job store. Guards against silent regressions where someone swaps the job
 * store back to RAMJobStore or removes the isClustered=true property from application.yaml.
 *
 * <h3>Why LocalDataSourceJobStore, not JobStoreTX?</h3> Spring Boot's {@code SchedulerFactoryBean}
 * injects the Spring-managed {@code DataSource} via
 * {@link org.springframework.scheduling.quartz.LocalDataSourceJobStore} (a {@code JobStoreCMT}
 * subclass) using {@code putIfAbsent}. Explicitly setting
 * {@code org.quartz.jobStore.class=JobStoreTX} in application.yaml bypasses this and causes a
 * {@code DataSource name not set} startup failure. Spring's integration is the correct approach.
 *
 * <h3>isClustered assertion</h3> The H2 test profile deliberately overrides
 * {@code isClustered=false} to avoid 20-second cluster check-in overhead on dev boxes. This test
 * verifies the base {@code application.yaml} declares {@code isClustered=true} for production
 * MySQL/Postgres profiles, and separately verifies the scheduler uses persistent JDBC storage
 * regardless of profile.
 */
public class QuartzSchedulerSmokeTest extends BaseApplication {

    @Autowired
    private Scheduler scheduler;

    @Test
    void base_application_yaml_declares_isClustered_true_for_production() throws Exception {
        // application.yaml base config must have isClustered=true so that MySQL/Postgres
        // production profiles get cluster-safe job execution. The H2 profile overrides this
        // to false, so read the base resource directly instead of the merged Environment.
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources =
                loader.load("application.yaml", new ClassPathResource("application.yaml"));
        Object isClustered = sources.stream()
                .map(s -> s.getProperty("spring.quartz.properties.org.quartz.jobStore.isClustered"))
                .filter(v -> v != null).findFirst().orElse(null);
        assertEquals("true", String.valueOf(isClustered),
                "base application.yaml must keep production Quartz clustering enabled");
    }

    @Test
    void scheduler_uses_spring_jdbc_jobstore_and_persistence() throws Exception {
        // Spring Boot's SchedulerFactoryBean wires LocalDataSourceJobStore (a JobStoreCMT
        // subclass) when job-store-type=jdbc and a DataSource bean is present. This is the
        // correct JDBC persistent store for Spring-managed schedulers.
        SchedulerMetaData meta = scheduler.getMetaData();
        assertTrue(meta.isJobStoreSupportsPersistence(),
                "JDBC job store must report persistence support (not RAMJobStore)");
        assertTrue(LocalDataSourceJobStore.class.isAssignableFrom(meta.getJobStoreClass()),
                "Spring-wired scheduler must use LocalDataSourceJobStore (or subclass), "
                        + "not RAMJobStore or naked JobStoreTX. Actual: "
                        + meta.getJobStoreClass().getName());
        assertFalse(
                scheduler.getSchedulerInstanceId() == null
                        || scheduler.getSchedulerInstanceId().isBlank(),
                "instanceId=AUTO must produce a non-blank runtime instanceId");
    }
}
