package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserDOMapper;
import com.tencent.supersonic.common.service.SubscriptionInfoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TenantServiceImplTest {

    private final AtomicReference<TenantDO> insertedTenant = new AtomicReference<>();
    private final AtomicReference<TenantDO> updatedTenant = new AtomicReference<>();
    private final AtomicReference<Integer> selectCountCalls = new AtomicReference<>(0);

    private TenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        TenantDOMapper tenantDOMapper =
                (TenantDOMapper) Proxy.newProxyInstance(TenantDOMapper.class.getClassLoader(),
                        new Class[] {TenantDOMapper.class}, (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "insert":
                                    insertedTenant.set((TenantDO) args[0]);
                                    return 1;
                                case "updateById":
                                    updatedTenant.set((TenantDO) args[0]);
                                    return 1;
                                case "selectCount":
                                    selectCountCalls.set(selectCountCalls.get() + 1);
                                    return 0L;
                                default:
                                    return null;
                            }
                        });
        SubscriptionInfoProvider subscriptionInfoProvider = (SubscriptionInfoProvider) Proxy
                .newProxyInstance(SubscriptionInfoProvider.class.getClassLoader(),
                        new Class[] {SubscriptionInfoProvider.class},
                        (proxy, method, args) -> null);
        UserDOMapper userDOMapper =
                (UserDOMapper) Proxy.newProxyInstance(UserDOMapper.class.getClassLoader(),
                        new Class[] {UserDOMapper.class}, (proxy, method, args) -> null);
        UsageTrackingService usageTrackingService = (UsageTrackingService) Proxy.newProxyInstance(
                UsageTrackingService.class.getClassLoader(),
                new Class[] {UsageTrackingService.class}, (proxy, method, args) -> null);

        tenantService = new TenantServiceImpl(tenantDOMapper, subscriptionInfoProvider,
                userDOMapper, usageTrackingService, new JdbcTemplate());
    }

    @Test
    void createTenantShouldNormalizeCodeToLowerCase() {
        Tenant tenant = Tenant.builder().name("Acme").code(" AcMe_01 ").build();

        tenantService.createTenant(tenant);

        assertEquals("acme_01", insertedTenant.get().getCode());
    }

    @Test
    void updateTenantShouldNormalizeCodeToLowerCase() {
        Tenant tenant = Tenant.builder().id(1L).name("Acme").code(" AcMe_02 ").build();

        tenantService.updateTenant(tenant);

        assertEquals("acme_02", updatedTenant.get().getCode());
    }

    @Test
    void isTenantCodeAvailableShouldRejectBlankCode() {
        assertFalse(tenantService.isTenantCodeAvailable("   "));
        assertEquals(0, selectCountCalls.get());
    }
}
