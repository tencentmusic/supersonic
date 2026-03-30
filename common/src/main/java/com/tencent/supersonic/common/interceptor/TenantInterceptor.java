package com.tencent.supersonic.common.interceptor;

import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.service.CurrentUserProvider;
import com.tencent.supersonic.common.service.TenantCodeResolver;
import com.tencent.supersonic.common.util.ContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * HTTP interceptor that extracts tenant information from request headers, JWT tokens, or subdomains
 * and sets it in the TenantContext for the current thread.
 */
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    /**
     * Get TenantConfig bean lazily at runtime.
     */
    private TenantConfig getTenantConfig() {
        try {
            return ContextUtils.getBean(TenantConfig.class);
        } catch (Exception e) {
            log.error("TenantConfig not available: {}", e.getMessage());
            return null;
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        try {
            CurrentUserProvider provider = ContextUtils.getBean(CurrentUserProvider.class);
            if (provider != null) {
                return provider.getCurrentUser(request, null);
            }
        } catch (Exception e) {
            log.error("Failed to get current user: {}", e.getMessage());
        }
        return null;
    }

    private TenantCodeResolver getTenantCodeResolver() {
        try {
            return ContextUtils.getBean(TenantCodeResolver.class);
        } catch (Exception e) {
            log.error("TenantCodeResolver not available: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        TenantConfig config = getTenantConfig();

        // Skip tenant validation for excluded paths
        if (config != null && config.isExcludedPath(requestUri)) {
            return true;
        }
        Long tenantId = resolveTenantId(request, config);

        if (tenantId != null && tenantId > 0) {
            TenantContext.setTenantId(tenantId);
        } else {
            if (config != null && config.isRequired()) {
                throw new InvalidPermissionException("无法解析有效租户信息");
            }
            // Tenant ID not resolved, fallback to default only when tenant context is optional.
            Long defaultTenantId = config != null ? config.getDefaultTenantId() : 1L;
            TenantContext.setTenantId(defaultTenantId);
            log.warn("Using default tenant: tenantId={} for path={}", defaultTenantId, requestUri);
        }

        return true;
    }

    /**
     * Resolves tenant ID from the request. Supports header-based, token-based, and subdomain
     * resolution.
     */
    private Long resolveTenantId(HttpServletRequest request, TenantConfig config) {
        Long tenantId = null;
        // Try to resolve from header first (highest priority)
        if (config != null && config.isHeaderEnabled()) {
            String headerName = config.getTenantIdHeader();
            String headerValue = request.getHeader(headerName);
            if (StringUtils.isNotBlank(headerValue)) {
                try {
                    tenantId = Long.parseLong(headerValue.trim());
                    return tenantId;
                } catch (NumberFormatException e) {
                    log.warn("[TenantResolve] Invalid tenant ID in header: {}", headerValue);
                }
            }
        }

        // Try to resolve from authenticated user's token
        try {
            User user = getCurrentUser(request);
            if (user != null && user.getTenantId() != null && user.getTenantId() > 0) {
                tenantId = user.getTenantId();
                return tenantId;
            } else if (user != null) {
                log.warn("[TenantResolve] User {} has no valid tenantId (tenantId={})",
                        user.getName(), user.getTenantId());
            } else {
                log.info("[TenantResolve] getCurrentUser returned null");
            }
        } catch (Exception e) {
            log.error("[TenantResolve] Failed to resolve from user token: {}", e.getMessage());
        }

        // Try to resolve from subdomain (if enabled)
        if (config != null && config.isSubdomainEnabled()) {
            tenantId = resolveTenantFromSubdomain(request);
        }

        return tenantId;
    }

    /**
     * Resolves tenant ID from the subdomain. Expected format: {tenant-code}.example.com
     */
    private Long resolveTenantFromSubdomain(HttpServletRequest request) {
        String serverName = extractRequestHost(request);
        if (StringUtils.isNotBlank(serverName) && !isLoopbackOrIp(serverName)) {
            // Extract first part of hostname as tenant code
            String[] parts = serverName.split("\\.");
            if (parts.length > 2) {
                String tenantCode = StringUtils.trimToEmpty(parts[0]).toLowerCase();
                Long tenantId = resolveTenantIdByCode(tenantCode);
                if (tenantId != null && tenantId > 0) {
                    return tenantId;
                }
                log.warn("[TenantResolve] Tenant not found by subdomain code: {}", tenantCode);
            }
        }
        return null;
    }

    private String extractRequestHost(HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (StringUtils.isNotBlank(forwardedHost)) {
            return normalizeHost(forwardedHost.split(",")[0]);
        }
        String forwarded = request.getHeader("Forwarded");
        if (StringUtils.isNotBlank(forwarded)) {
            for (String entry : forwarded.split(",")) {
                for (String part : entry.split(";")) {
                    String trimmed = StringUtils.trimToEmpty(part);
                    if (StringUtils.startsWithIgnoreCase(trimmed, "host=")) {
                        return normalizeHost(trimmed.substring(5));
                    }
                }
            }
        }
        String host = request.getHeader("Host");
        if (StringUtils.isNotBlank(host)) {
            return normalizeHost(host);
        }
        return normalizeHost(request.getServerName());
    }

    private String normalizeHost(String rawHost) {
        String host = StringUtils.trimToEmpty(rawHost).toLowerCase();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        if (host.startsWith("\"") && host.endsWith("\"") && host.length() > 1) {
            host = host.substring(1, host.length() - 1);
        }
        int colonIndex = host.indexOf(':');
        if (colonIndex > -1) {
            host = host.substring(0, colonIndex);
        }
        return StringUtils.trimToNull(host);
    }

    private boolean isLoopbackOrIp(String host) {
        return StringUtils.equalsAnyIgnoreCase(host, "localhost", "127.0.0.1", "::1")
                || host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
    }

    /**
     * Resolve tenant ID by tenant code via tenantService bean.
     *
     * <p>
     * This interceptor sits in common module; to avoid direct module dependency on auth-api,
     * reflection is used to call tenantService.getTenantByCode(code) -> Optional<Tenant>.
     * </p>
     */
    private Long resolveTenantIdByCode(String tenantCode) {
        if (StringUtils.isBlank(tenantCode)) {
            return null;
        }
        TenantCodeResolver resolver = getTenantCodeResolver();
        if (resolver == null) {
            return null;
        }
        try {
            return resolver.resolveTenantId(tenantCode);
        } catch (Exception e) {
            log.error("[TenantResolve] Failed to resolve tenant by code {}: {}", tenantCode,
                    e.getMessage());
        }
        return null;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        // No action needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
        // Clear tenant context to prevent memory leaks
        TenantContext.clear();
        if (log.isDebugEnabled()) {
            log.debug("Cleared tenant context for request: {}", request.getRequestURI());
        }
    }
}
