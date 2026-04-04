package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantDO;
import com.tencent.supersonic.auth.authentication.persistence.mapper.TenantDOMapper;
import com.tencent.supersonic.auth.authentication.persistence.mapper.UserDOMapper;
import com.tencent.supersonic.common.pojo.PlanQuota;
import com.tencent.supersonic.common.service.SubscriptionInfoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                        new Class[] {SubscriptionInfoProvider.class}, (proxy, method, args) -> {
                            if ("getActivePlanQuota".equals(method.getName())) {
                                return Optional.empty();
                            }
                            return null;
                        });
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

    @Test
    void isModelLimitReachedShouldReturnFalseWhenNoSubscription() {
        // Default subscriptionInfoProvider returns null (no quota)
        assertFalse(tenantService.isModelLimitReached(1L));
    }

    @Test
    void isModelLimitReachedShouldReturnFalseWhenUnlimited() {
        TenantServiceImpl svc =
                createServiceWithQuota(PlanQuota.builder().maxModels(-1).build(), 0);
        assertFalse(svc.isModelLimitReached(1L));
    }

    @Test
    void isModelLimitReachedShouldReturnTrueWhenAtLimit() {
        TenantServiceImpl svc = createServiceWithQuota(PlanQuota.builder().maxModels(5).build(), 5);
        assertTrue(svc.isModelLimitReached(1L));
    }

    @Test
    void isModelLimitReachedShouldReturnFalseWhenBelowLimit() {
        TenantServiceImpl svc = createServiceWithQuota(PlanQuota.builder().maxModels(5).build(), 3);
        assertFalse(svc.isModelLimitReached(1L));
    }

    private TenantServiceImpl createServiceWithQuota(PlanQuota quota, long currentCount) {
        TenantDOMapper mapper =
                (TenantDOMapper) Proxy.newProxyInstance(TenantDOMapper.class.getClassLoader(),
                        new Class[] {TenantDOMapper.class}, (proxy, method, args) -> null);
        SubscriptionInfoProvider provider = (SubscriptionInfoProvider) Proxy.newProxyInstance(
                SubscriptionInfoProvider.class.getClassLoader(),
                new Class[] {SubscriptionInfoProvider.class}, (proxy, method, args) -> {
                    if ("getActivePlanQuota".equals(method.getName())) {
                        return Optional.of(quota);
                    }
                    return null;
                });
        UserDOMapper userMapper =
                (UserDOMapper) Proxy.newProxyInstance(UserDOMapper.class.getClassLoader(),
                        new Class[] {UserDOMapper.class}, (proxy, method, args) -> null);
        UsageTrackingService usageService = (UsageTrackingService) Proxy.newProxyInstance(
                UsageTrackingService.class.getClassLoader(),
                new Class[] {UsageTrackingService.class}, (proxy, method, args) -> null);
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
                @SuppressWarnings("unchecked")
                T result = (T) Long.valueOf(currentCount);
                return result;
            }
        };
        return new TenantServiceImpl(mapper, provider, userMapper, usageService, jdbc);
    }
}
