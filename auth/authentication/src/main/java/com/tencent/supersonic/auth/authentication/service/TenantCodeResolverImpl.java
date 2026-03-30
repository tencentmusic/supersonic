package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import com.tencent.supersonic.common.service.TenantCodeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Auth-module implementation for resolving tenant IDs from tenant codes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCodeResolverImpl implements TenantCodeResolver {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final TenantService tenantService;

    @Override
    public Long resolveTenantId(String tenantCode) {
        return tenantService.getTenantByCode(tenantCode).map(this::toActiveTenantId).orElse(null);
    }

    private Long toActiveTenantId(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        if (tenant.getStatus() == null || !tenant.getStatus().equalsIgnoreCase(STATUS_ACTIVE)) {
            log.warn("[TenantResolve] Tenant {} is not ACTIVE (status={})", tenant.getCode(),
                    tenant.getStatus());
            return null;
        }
        return tenant.getId();
    }
}
