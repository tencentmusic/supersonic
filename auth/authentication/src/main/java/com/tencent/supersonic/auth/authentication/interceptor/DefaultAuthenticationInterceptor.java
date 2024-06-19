package com.tencent.supersonic.auth.authentication.interceptor;


import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.service.UserServiceImpl;
import com.tencent.supersonic.auth.authentication.utils.UserTokenUtils;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.S2ThreadContext;
import com.tencent.supersonic.common.util.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Slf4j
public class DefaultAuthenticationInterceptor extends AuthenticationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws AccessException {
        authenticationConfig = ContextUtils.getBean(AuthenticationConfig.class);
        userServiceImpl = ContextUtils.getBean(UserServiceImpl.class);
        userTokenUtils = ContextUtils.getBean(UserTokenUtils.class);
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

        UserWithPassword user = userTokenUtils.getUserWithPassword(request);
        if (StringUtils.isNotBlank(user.getName())) {
            setContext(user.getName(), request);
            return true;
        }
        throw new AccessException("authentication failed, please login");
    }

    private void setFakerUser(HttpServletRequest request) {
        String token = userTokenUtils.generateAdminToken(request);
        reflectSetParam(request, authenticationConfig.getTokenHttpHeaderKey(), token);
        setContext(User.getFakeUser().getName(), request);
    }

    private void setContext(String userName, HttpServletRequest request) {
        ThreadContext threadContext = ThreadContext.builder()
                .token(request.getHeader(authenticationConfig.getTokenHttpHeaderKey()))
                .userName(userName)
                .build();
        s2ThreadContext.set(threadContext);
    }

}
