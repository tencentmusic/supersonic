package com.tencent.supersonic.auth.authentication.domain.interceptor;


import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.application.UserServiceImpl;
import com.tencent.supersonic.auth.authentication.domain.utils.UserTokenUtils;
import com.tencent.supersonic.common.exception.AccessException;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.common.util.context.S2ThreadContext;
import com.tencent.supersonic.common.util.context.ThreadContext;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
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
            return true;
        }
        if (isInternalRequest(request)) {
            String token = userTokenUtils.generateAdminToken();
            reflectSetparam(request, authenticationConfig.getTokenHttpHeaderKey(), token);
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        AuthenticationIgnore ignore = method.getAnnotation(AuthenticationIgnore.class);
        if (ignore != null) {
            return true;
        }

        String uri = request.getServletPath();
        if (isExcludedUri(uri)) {
            return true;
        }

        UserWithPassword user = userTokenUtils.getUserWithPassword(request);
        if (StringUtils.isNotBlank(user.getName())) {
            ThreadContext threadContext = ThreadContext.builder()
                    .token(request.getHeader(authenticationConfig.getTokenHttpHeaderKey()))
                    .userName(user.getName())
                    .build();
            s2ThreadContext.set(threadContext);
            return true;
        }
        throw new AccessException("authentication failed, please login");
    }


}
