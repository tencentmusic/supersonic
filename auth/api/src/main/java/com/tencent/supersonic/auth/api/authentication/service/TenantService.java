package com.tencent.supersonic.auth.api.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for tenant management.
 */
public interface TenantService {

    /**
     * Create a new tenant.
     *
     * @param tenant the tenant to create
     * @return the created tenant with ID
     */
    Tenant createTenant(Tenant tenant);

    /**
     * Update an existing tenant.
     *
     * @param tenant the tenant to update
     * @return the updated tenant
     */
    Tenant updateTenant(Tenant tenant);

    /**
     * Get a tenant by ID.
     *
     * @param id the tenant ID
     * @return the tenant if found
     */
    Optional<Tenant> getTenantById(Long id);

    /**
     * Get a tenant by code.
     *
     * @param code the tenant code
     * @return the tenant if found
     */
    Optional<Tenant> getTenantByCode(String code);

    /**
     * Get all tenants.
     *
     * @return list of all tenants
     */
    List<Tenant> getAllTenants();

    /**
     * Get all active tenants.
     *
     * @return list of active tenants
     */
    List<Tenant> getActiveTenants();

    /**
     * Delete a tenant by ID.
     *
     * @param id the tenant ID
     */
    void deleteTenant(Long id);

    /**
     * Suspend a tenant.
     *
     * @param id the tenant ID
     */
    void suspendTenant(Long id);

    /**
     * Activate a tenant.
     *
     * @param id the tenant ID
     */
    void activateTenant(Long id);

    /**
     * Check if a tenant has reached its user limit.
     *
     * @param tenantId the tenant ID
     * @return true if limit reached
     */
    boolean isUserLimitReached(Long tenantId);

    /**
     * Check if a tenant has reached its dataset limit.
     *
     * @param tenantId the tenant ID
     * @return true if limit reached
     */
    boolean isDatasetLimitReached(Long tenantId);

    /**
     * Check if a tenant has reached its model limit.
     *
     * @param tenantId the tenant ID
     * @return true if limit reached
     */
    boolean isModelLimitReached(Long tenantId);

    /**
     * Check if a tenant has reached its agent limit.
     *
     * @param tenantId the tenant ID
     * @return true if limit reached
     */
    boolean isAgentLimitReached(Long tenantId);

    /**
     * Check if a tenant has reached its daily API call limit.
     *
     * @param tenantId the tenant ID
     * @return true if limit reached
     */
    boolean isApiCallLimitReached(Long tenantId);

    /**
     * Check if a tenant code is available.
     *
     * @param code the code to check
     * @return true if available
     */
    boolean isTenantCodeAvailable(String code);

    /**
     * Get the percentage of daily API call quota used by a tenant.
     *
     * @param tenantId the tenant ID
     * @return usage percentage (0-100), or 0 if no limit is configured
     */
    int getApiCallUsagePercent(Long tenantId);

    /**
     * Get the percentage of monthly token quota used by a tenant.
     *
     * @param tenantId the tenant ID
     * @return usage percentage (0-100), or 0 if no limit is configured
     */
    int getTokenUsagePercent(Long tenantId);
}
