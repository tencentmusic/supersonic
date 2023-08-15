package com.tencent.supersonic.auth.authentication.interceptor;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorFactory implements WebMvcConfigurer {


    private List<AuthenticationInterceptor> authenticationInterceptors;

    public InterceptorFactory() {
        authenticationInterceptors = SpringFactoriesLoader.loadFactories(AuthenticationInterceptor.class,
                Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        for (AuthenticationInterceptor authenticationInterceptor : authenticationInterceptors) {
            registry.addInterceptor(authenticationInterceptor).addPathPatterns("/**")
                    .excludePathPatterns("/", "/webapp/**", "/error");
        }
    }

}
