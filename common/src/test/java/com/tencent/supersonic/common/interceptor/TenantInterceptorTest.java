package com.tencent.supersonic.common.interceptor;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.service.CurrentUserProvider;
import com.tencent.supersonic.common.service.TenantCodeResolver;
import com.tencent.supersonic.common.util.ContextUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantInterceptorTest {

    private final TenantInterceptor tenantInterceptor = new TenantInterceptor();
    private GenericApplicationContext context;
    private TenantConfig tenantConfig;
    private StubTenantCodeResolver tenantCodeResolver;

    @BeforeEach
    void setUp() throws Exception {
        tenantConfig = new TenantConfig();
        tenantConfig.setRequired(true);
        tenantConfig.setHeaderEnabled(true);
        tenantConfig.setSubdomainEnabled(true);
        tenantConfig.setDefaultTenantId(1L);

        tenantCodeResolver = new StubTenantCodeResolver();
        context = new GenericApplicationContext();
        context.registerBean(TenantConfig.class, () -> tenantConfig);
        context.registerBean(CurrentUserProvider.class, () -> (request, response) -> null);
        context.registerBean(TenantCodeResolver.class, () -> tenantCodeResolver);
        context.refresh();
        new ContextUtils().setApplicationContext(context);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        if (context != null) {
            context.close();
        }
    }

    @Test
    void preHandleShouldResolveActiveTenantFromForwardedHost() throws Exception {
        tenantCodeResolver.register("acme", 7L, "ACTIVE");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/query");
        request.addHeader("X-Forwarded-Host", "Acme.example.com:8443");

        assertTrue(
                tenantInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(7L, TenantContext.getTenantId());
    }

    @Test
    void preHandleShouldRejectUnknownSubdomainWhenTenantIsRequired() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/query");
        request.addHeader("X-Forwarded-Host", "unknown.example.com");

        InvalidPermissionException ex =
                assertThrows(InvalidPermissionException.class, () -> tenantInterceptor
                        .preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals("无法解析有效租户信息", ex.getMessage());
    }

    @Test
    void preHandleShouldFallbackToDefaultTenantWhenTenantIsOptional() throws Exception {
        tenantConfig.setRequired(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/query");
        request.addHeader("X-Forwarded-Host", "unknown.example.com");

        assertTrue(
                tenantInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(1L, TenantContext.getTenantId());
    }

    @Test
    void preHandleShouldRejectSuspendedTenantResolvedFromSubdomain() {
        tenantCodeResolver.register("acme", 7L, "SUSPENDED");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/query");
        request.addHeader("X-Forwarded-Host", "acme.example.com");

        assertThrows(InvalidPermissionException.class, () -> tenantInterceptor.preHandle(request,
                new MockHttpServletResponse(), new Object()));
    }

    @Test
    void preHandleShouldPreferHeaderTenantIdOverSubdomain() throws Exception {
        tenantCodeResolver.register("acme", 7L, "ACTIVE");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/query");
        request.addHeader("X-Tenant-Id", "9");
        request.addHeader("X-Forwarded-Host", "acme.example.com");

        assertTrue(
                tenantInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(9L, TenantContext.getTenantId());
    }

    static class StubTenantCodeResolver implements TenantCodeResolver {

        private final Map<String, StubTenant> tenants = new HashMap<>();

        void register(String code, Long tenantId, String status) {
            tenants.put(code, new StubTenant(tenantId, status));
        }

        @Override
        public Long resolveTenantId(String code) {
            return Optional.ofNullable(tenants.get(code)).filter(StubTenant::isActive)
                    .map(StubTenant::getId).orElse(null);
        }
    }

    static class StubTenant {

        private final Long id;
        private final String status;

        StubTenant(Long id, String status) {
            this.id = id;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }

        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status);
        }
    }
}
