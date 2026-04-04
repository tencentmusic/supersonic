package com.tencent.supersonic.auth.authentication.interceptor;

import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import com.tencent.supersonic.auth.api.authentication.service.UsageTrackingService;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.exception.QuotaExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.StringJoiner;

/**
 * Interceptor that enforces API call quotas based on the tenant's subscription plan. Must run AFTER
 * TenantInterceptor so that TenantContext is already populated.
 *
 * <p>
 * Controlled by the {@code s2.billing.quota-enforcement.enabled} property (default: false).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaEnforcementInterceptor implements HandlerInterceptor {

    private static final List<String> COUNTED_PREFIXES =
            List.of("/api/semantic/", "/api/chat/query", "/openapi/");

    private static final int WARNING_THRESHOLD = 90;

    private static final String QUOTA_WARNING_HEADER = "X-Quota-Warning";

    private final TenantService tenantService;
    private final UsageTrackingService usageTrackingService;

    @Value("${s2.billing.quota-enforcement.enabled:false}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) {
        if (!enabled) {
            return true;
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true;
        }

        String path = request.getRequestURI();
        if (!isCountedPath(path)) {
            return true;
        }

        // Record first, then check — avoids TOCTOU race under concurrent requests.
        // At most one extra call is counted before rejection, which resets daily.
        usageTrackingService.recordApiCall(tenantId);
        usageTrackingService.recordActiveUser(tenantId);

        if (tenantService.isApiCallLimitReached(tenantId)) {
            throw new QuotaExceededException("apiCalls", "今日 API 调用已达上限，请升级套餐或明日再试");
        }

        // Soft warnings via response header
        StringJoiner warnings = new StringJoiner("; ");
        int apiPercent = tenantService.getApiCallUsagePercent(tenantId);
        if (apiPercent >= WARNING_THRESHOLD) {
            warnings.add("API calls at " + apiPercent + "%");
        }
        int tokenPercent = tenantService.getTokenUsagePercent(tenantId);
        if (tokenPercent >= WARNING_THRESHOLD) {
            warnings.add("Token usage at " + tokenPercent + "%");
        }
        if (warnings.length() > 0) {
            response.setHeader(QUOTA_WARNING_HEADER, warnings.toString());
        }

        return true;
    }

    private boolean isCountedPath(String path) {
        for (String prefix : COUNTED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
