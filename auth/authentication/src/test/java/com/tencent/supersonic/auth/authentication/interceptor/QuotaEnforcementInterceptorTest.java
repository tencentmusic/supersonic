package com.tencent.supersonic.auth.authentication.interceptor;

import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuotaEnforcementInterceptorTest {

    private final AtomicInteger apiCalls = new AtomicInteger();

    private QuotaEnforcementInterceptor interceptor;

    @BeforeEach
    void setUp() {
        TenantService tenantService =
                (TenantService) Proxy.newProxyInstance(TenantService.class.getClassLoader(),
                        new Class[] {TenantService.class}, (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "isApiCallLimitReached" -> apiCalls.get() > 100;
                                case "getApiCallUsagePercent" -> apiCalls.get();
                                case "getTokenUsagePercent" -> 0;
                                default -> false;
                            };
                        });
        UsageTrackingService usageTrackingService = (UsageTrackingService) Proxy.newProxyInstance(
                UsageTrackingService.class.getClassLoader(),
                new Class[] {UsageTrackingService.class}, (proxy, method, args) -> {
                    if ("recordApiCall".equals(method.getName())) {
                        apiCalls.incrementAndGet();
                    }
                    return null;
                });
        interceptor = new QuotaEnforcementInterceptor(tenantService, usageTrackingService);
        ReflectionTestUtils.setField(interceptor, "enabled", true);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldAllowRequestThatExactlyReachesLimit() {
        apiCalls.set(99);

        assertDoesNotThrow(() -> interceptor.preHandle(countedRequest(),
                new MockHttpServletResponse(), new Object()));
        assertEquals(100, apiCalls.get());
    }

    @Test
    void shouldRejectRequestAfterLimitIsExceeded() {
        apiCalls.set(100);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> interceptor
                .preHandle(countedRequest(), new MockHttpServletResponse(), new Object()));
        assertEquals("QuotaExceededException", exception.getClass().getSimpleName());
        assertEquals(101, apiCalls.get());
    }

    private MockHttpServletRequest countedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/chat/query");
        return request;
    }
}
