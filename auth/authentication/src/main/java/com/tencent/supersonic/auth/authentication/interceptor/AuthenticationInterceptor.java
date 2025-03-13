package com.tencent.supersonic.auth.authentication.interceptor;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
public abstract class AuthenticationInterceptor implements HandlerInterceptor {

    protected AuthenticationConfig authenticationConfig;

    protected UserService userService;

    protected TokenService tokenService;

    protected boolean isExcludedUri(String uri) {
        String excludePathStr = authenticationConfig.getExcludePath();
        if (StringUtils.isEmpty(excludePathStr)) {
            return false;
        }
        List<String> excludePaths = Arrays.asList(excludePathStr.split(","));
        if (CollectionUtils.isEmpty(excludePaths)) {
            return false;
        }
        return excludePaths.stream().anyMatch(uri::startsWith);
    }

    protected boolean isIncludedUri(String uri) {
        String includePathStr = authenticationConfig.getIncludePath();
        if (StringUtils.isEmpty(includePathStr)) {
            return false;
        }
        List<String> includePaths = Arrays.asList(includePathStr.split(","));
        if (CollectionUtils.isEmpty(includePaths)) {
            return false;
        }
        return includePaths.stream().anyMatch(uri::startsWith);
    }

}
