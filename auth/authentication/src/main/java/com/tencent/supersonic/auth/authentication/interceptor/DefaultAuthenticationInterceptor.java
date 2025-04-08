package com.tencent.supersonic.auth.authentication.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.api.authentication.request.UserReq;
import com.tencent.supersonic.auth.api.authentication.response.AnalysisCloudTokenProjectLoginResponse;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.auth.authentication.exception.AuthErrorEnum;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.auth.authentication.utils.ComponentFactory;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.*;
import com.tencent.supersonic.common.util.ContextUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.*;

@Slf4j
public class DefaultAuthenticationInterceptor extends AuthenticationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws AccessException {
        authenticationConfig = ContextUtils.getBean(AuthenticationConfig.class);
        userService = ContextUtils.getBean(UserService.class);
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
        if (GET_CURRENT_USER.equals(uri)
                && ComponentFactory.getUserAdaptor().verifyParameters(request)) {
            if (verifyAnalysisCloud(request)) {
                return true;
            }
            throw new AccessException("分析云Token认证失败,请联系管理员！");
        }

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

    private boolean verifyAnalysisCloud(HttpServletRequest request) {

        String userName = request.getParameter("userName");
        String token = request.getParameter("token");
        String projectId = request.getParameter("projectId");
        // 校验用户名，用户是否存在
        UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
        UserDO userDO = userRepository.getUser(userName);
        if (userDO == null) {
            throw new AccessException("user not exist,please register");
        }
        // 校验分析云token
        // analysisCloudTokenLogin(analysisToken, projectId);

        return true;
    }

    public void analysisCloudTokenLogin(String token, String projectId) {
        List<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("token", token));
        String result = HttpClientUtils.doPost(ANALYSIS_CLOUD_URL + TOKEN_PATH + "?id=" + projectId,
                headers, "", "UTF-8");
        log.info("结果：" + result);
        if (result == null) {
            log.error("Token认证响应为null");
            throw new AccessException("分析云Token认证失败");
        }
        try {
            AnalysisCloudTokenProjectLoginResponse response =
                    JSONObject.parseObject(result, AnalysisCloudTokenProjectLoginResponse.class);
            if (!response.getRspcode().equals("0")) {
                log.error("[分析云token认证失败,{}]", response.getRspdesc());
                throw new AccessException(response.getRspdesc());
            }
        } catch (Throwable t) {
            log.error("", t);
            throw new AccessException(
                    String.valueOf(AuthErrorEnum.ANALYSIS_CLOUD_TOKEN_LOGIN_FAILED));
        }
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
