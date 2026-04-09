package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for multi-tenant support.
 */
@Data
@ConfigurationProperties(prefix = "s2.tenant")
public class TenantConfig {

    /**
     * Whether multi-tenant SQL interceptor is enabled. When false, no tenant filtering is applied
     * to SQL statements.
     */
    private boolean enabled = false;

    /**
     * Whether tenant context is required for requests. If true, requests without tenant context
     * will be rejected.
     */
    private boolean required = true;

    /**
     * Default tenant ID to use when no tenant is specified.
     */
    private Long defaultTenantId = 1L;

    /**
     * Whether to enable header-based tenant resolution.
     */
    private boolean headerEnabled = true;

    /**
     * HTTP header name for tenant ID.
     */
    private String tenantIdHeader = "X-Tenant-Id";

    /**
     * Whether to enable subdomain-based tenant resolution.
     */
    private boolean subdomainEnabled = false;

    /**
     * Tables that should be excluded from tenant filtering. These are system-level tables that
     * don't have tenant_id column.
     */
    private List<String> excludedTables = Arrays.asList(
            // System-level tables
            "s2_tenant", "s2_subscription_plan",
            // RBAC association tables without tenant_id
            "s2_permission", "s2_role_permission", "s2_user_role",
            // Feishu session table — tenant derived via feishu_open_id → s2_feishu_user_mapping
            "s2_feishu_query_session");

    /**
     * URL patterns that should be excluded from tenant validation. These are endpoints that either
     * don't need tenant context or operate across all tenants (admin endpoints).
     */
    private List<String> excludedPaths = Arrays.asList(
            // Auth endpoints that don't require tenant context (pre-authentication)
            "/api/auth/user/login", "/api/auth/user/register", "/api/auth/oauth/**",
            "/api/auth/token/**",
            // Admin endpoints that operate across all tenants
            "/api/auth/admin/**",
            // Public endpoints
            "/api/public/**",
            // Health and monitoring
            "/health", "/actuator/**");

    /**
     * Check if a table should be excluded from tenant filtering.
     */
    public boolean isExcludedTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        // Remove backticks, quotes, and trim whitespace
        String cleanName = tableName.replace("`", "").replace("\"", "").replace("'", "").trim();
        return excludedTables.stream().anyMatch(cleanName::equalsIgnoreCase);
    }

    /**
     * Check if a path should be excluded from tenant validation.
     */
    public boolean isExcludedPath(String path) {
        if (path == null) {
            return true;
        }
        for (String pattern : excludedPaths) {
            if (matchPath(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }
}
