package com.tencent.supersonic.auth.authentication.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.service.UserServiceImpl;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.S2ThreadContext;
import com.tencent.supersonic.common.util.ThreadContext;
import io.jsonwebtoken.Claims;
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
        s2ThreadContext = ContextUtils.getBean(S2ThreadContext.class);
        if (!authenticationConfig.isEnabled()) {
            setFakerUser(request);
            return true;
        }
        if (isInternalRequest(request)) {
            setFakerUser(request);
            return true;
        }
        if (isAppRequest(request)) {
            setFakerUser(request);
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
            setContext(user.getName(), request);
            return true;
        }
        throw new AccessException("authentication failed, please login");
    }

    private void setFakerUser(HttpServletRequest request) {
        String token = generateAdminToken(request);
        reflectSetParam(request, authenticationConfig.getTokenHttpHeaderKey(), token);
        setContext(User.getDefaultUser().getName(), request);
    }

    private void setContext(String userName, HttpServletRequest request) {
        ThreadContext threadContext = ThreadContext.builder()
                .token(request.getHeader(authenticationConfig.getTokenHttpHeaderKey()))
                .userName(userName).build();
        s2ThreadContext.set(threadContext);
    }

    public String generateAdminToken(HttpServletRequest request) {
        UserWithPassword admin = new UserWithPassword("admin");
        admin.setId(1L);
        admin.setName("admin");
        admin.setPassword("c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT");
        admin.setDisplayName("admin");
        admin.setIsAdmin(1);
        return tokenService.generateToken(UserWithPassword.convert(admin), request);
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
