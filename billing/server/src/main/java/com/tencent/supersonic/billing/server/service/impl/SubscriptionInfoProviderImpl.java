package com.tencent.supersonic.billing.server.service.impl;

import com.tencent.supersonic.billing.api.pojo.SubscriptionPlan;
import com.tencent.supersonic.billing.api.service.SubscriptionService;
import com.tencent.supersonic.common.pojo.PlanQuota;
import com.tencent.supersonic.common.service.SubscriptionInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Billing module implementation of SubscriptionInfoProvider. Bridges the billing-internal
 * SubscriptionService to the common interface.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionInfoProviderImpl implements SubscriptionInfoProvider {

    private final SubscriptionService subscriptionService;

    @Override
    public Optional<String> getActivePlanName(Long tenantId) {
        return subscriptionService.getActiveSubscription(tenantId).flatMap(subscription -> {
            if (subscription.getPlanName() != null) {
                return Optional.of(subscription.getPlanName());
            }
            return subscriptionService.getPlanById(subscription.getPlanId())
                    .map(SubscriptionPlan::getName);
        });
    }

    @Override
    public Optional<PlanQuota> getActivePlanQuota(Long tenantId) {
        return subscriptionService.getActiveSubscription(tenantId)
                .flatMap(subscription -> subscriptionService.getPlanById(subscription.getPlanId()))
                .map(this::toPlanQuota);
    }

    private PlanQuota toPlanQuota(SubscriptionPlan plan) {
        return PlanQuota.builder().planName(plan.getName()).maxUsers(plan.getMaxUsers())
                .maxDatasets(plan.getMaxDatasets()).maxModels(plan.getMaxModels())
                .maxAgents(plan.getMaxAgents()).maxApiCallsPerDay(plan.getMaxApiCallsPerDay())
                .maxTokensPerMonth(plan.getMaxTokensPerMonth()).build();
    }
}
