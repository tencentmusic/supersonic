package com.tencent.supersonic.common.mybatis;

import com.tencent.supersonic.common.config.TenantConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures the tenant interceptor does not inject tenant_id into Quartz tables. QRTZ_* tables have
 * no tenant_id column — injecting one would break the scheduler.
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
    void excludes_rbac_tables() throws Exception {
        TenantSqlInterceptor interceptor = new TenantSqlInterceptor(new TenantConfig());
        // s2_role is excluded because its queries are scope-aware (PLATFORM roles have null
        // tenant_id) and the service layer enforces cross-tenant write protection explicitly.
        assertTrue(shouldExclude(interceptor, "s2_role"), "s2_role should be excluded");
        assertTrue(shouldExclude(interceptor, "S2_ROLE"),
                "s2_role exclusion must be case-insensitive");
    }

    @Test
    void does_not_exclude_other_tables_by_accident() throws Exception {
        TenantSqlInterceptor interceptor = new TenantSqlInterceptor(new TenantConfig());
        assertFalse(shouldExclude(interceptor, "s2_report_schedule"));
        assertFalse(shouldExclude(interceptor, "s2_domain"));
        assertFalse(shouldExclude(interceptor, "quartz_user_defined")); // no QRTZ_ prefix
    }
}
