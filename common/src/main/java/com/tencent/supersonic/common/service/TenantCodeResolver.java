package com.tencent.supersonic.common.service;

/**
 * Resolves a tenant ID from a tenant code, typically extracted from a subdomain.
 */
@FunctionalInterface
public interface TenantCodeResolver {

    /**
     * Resolve an active tenant ID by tenant code.
     *
     * @param tenantCode tenant code extracted from request host
     * @return resolved tenant ID, or null when not found / not active
     */
    Long resolveTenantId(String tenantCode);
}
