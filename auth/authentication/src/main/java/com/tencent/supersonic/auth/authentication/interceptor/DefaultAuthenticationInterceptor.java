package com.tencent.supersonic.auth.authentication.interceptor;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.service.UserServiceImpl;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.ContextUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_IS_ADMIN;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_DISPLAY_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_EMAIL;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_PASSWORD;

@Slf4j
public class DefaultAuthenticationInterceptor extends AuthenticationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws AccessException {
        authenticationConfig = ContextUtils.getBean(AuthenticationConfig.class);
        userServiceImpl = ContextUtils.getBean(UserServiceImpl.class);
        tokenService = ContextUtils.getBean(TokenService.class);
        if (!authenticationConfig.isEnabled()) {
            return true;
        }

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            AuthenticationIgnore ignore = method.getAnnotation(AuthenticationIgnore.class);
            if (ignore != null) {
                return true;
            }
        }

        String uri = request.getServletPath();
        if (!isIncludedUri(uri)) {
            return true;
        }

        if (isExcludedUri(uri)) {
            return true;
        }

        UserWithPassword user = getUserWithPassword(request);
        if (user != null) {
            return true;
        }
        throw new AccessException("authentication failed, please login");
    }

    public UserWithPassword getUserWithPassword(HttpServletRequest request) {
        final Optional<Claims> claimsOptional = tokenService.getClaims(request);
        if (!claimsOptional.isPresent()) {
            return null;
        }
        Claims claims = claimsOptional.get();
        Long userId = Long.parseLong(claims.getOrDefault(TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(TOKEN_USER_DISPLAY_NAME));
        String password = String.valueOf(claims.get(TOKEN_USER_PASSWORD));
        Integer isAdmin = claims.get(TOKEN_IS_ADMIN) == null ? 0
                : Integer.parseInt(claims.get(TOKEN_IS_ADMIN).toString());
        return UserWithPassword.get(userId, userName, displayName, email, password, isAdmin);
    }
}
