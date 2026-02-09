package com.tencent.supersonic.common.service;

import com.tencent.supersonic.common.pojo.PlanQuota;

import java.util.Optional;

/**
 * Provides subscription information for a tenant. Implemented by the billing module, consumed by
 * auth and other modules.
 */
public interface SubscriptionInfoProvider {

    /**
     * Get the active subscription plan name for a tenant.
     */
    Optional<String> getActivePlanName(Long tenantId);

    /**
     * Get the active subscription plan quota for a tenant.
     */
    Optional<PlanQuota> getActivePlanQuota(Long tenantId);
}
