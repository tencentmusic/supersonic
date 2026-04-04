package com.tencent.supersonic.auth.authentication.interceptor;

import com.tencent.supersonic.common.interceptor.TenantInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Slf4j
@Configuration
public class InterceptorFactory implements WebMvcConfigurer {

    private final List<AuthenticationInterceptor> authenticationInterceptors;

    private final QuotaEnforcementInterceptor quotaEnforcementInterceptor;

    public InterceptorFactory(QuotaEnforcementInterceptor quotaEnforcementInterceptor) {
        this.quotaEnforcementInterceptor = quotaEnforcementInterceptor;
        this.authenticationInterceptors = SpringFactoriesLoader.loadFactories(
                AuthenticationInterceptor.class, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Authentication interceptors
        for (AuthenticationInterceptor authenticationInterceptor : authenticationInterceptors) {
            registry.addInterceptor(authenticationInterceptor).addPathPatterns("/**")
                    .excludePathPatterns("/", "/webapp/**", "/error");
        }

        // Tenant interceptor for multi-tenancy support (must be after authentication)
        TenantInterceptor tenantInterceptor = new TenantInterceptor();
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/api/**").excludePathPatterns(
                // Auth endpoints that don't require tenant context (pre-authentication)
                "/api/auth/user/login", "/api/auth/user/register", "/api/auth/oauth/**",
                "/api/auth/token/**",
                // Admin endpoints that operate across all tenants
                "/api/auth/admin/**",
                // Public and system endpoints
                "/api/public/**");
        log.info("TenantInterceptor registered for /api/** paths");

        // Quota enforcement interceptor (must be after TenantInterceptor)
        registry.addInterceptor(quotaEnforcementInterceptor)
                .addPathPatterns("/api/**", "/openapi/**").excludePathPatterns(
                        "/api/auth/user/login", "/api/auth/user/register", "/api/auth/oauth/**",
                        "/api/auth/token/**", "/api/auth/admin/**", "/api/public/**");
        log.info("QuotaEnforcementInterceptor registered for /api/** and /openapi/** paths");
    }
}
