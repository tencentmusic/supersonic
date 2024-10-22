package com.tencent.supersonic.auth.authentication.strategy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.constant.UserConstants;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.authentication.utils.TokenService;
import com.tencent.supersonic.common.pojo.User;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HttpHeaderUserStrategy implements UserStrategy {

    private final TokenService tokenService;

    public HttpHeaderUserStrategy(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean accept(boolean isEnableAuthentication) {
        return isEnableAuthentication;
    }

    @Override
    public User findUser(HttpServletRequest request, HttpServletResponse response) {
        return getUser(request);
    }

    @Override
    public User findUser(String token, String appKey) {
        return getUser(token, appKey);
    }

    public User getUser(HttpServletRequest request) {
        final Optional<Claims> claimsOptional = tokenService.getClaims(request);
        return claimsOptional.map(this::getUser).orElse(User.getVisitUser());
    }

    public User getUser(String token, String appKey) {
        final Optional<Claims> claimsOptional = tokenService.getClaims(token, appKey);
        return claimsOptional.map(this::getUser).orElse(User.getVisitUser());
    }

    private User getUser(Claims claims) {
        Long userId =
                Long.parseLong(claims.getOrDefault(UserConstants.TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(UserConstants.TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(UserConstants.TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(UserConstants.TOKEN_USER_DISPLAY_NAME));
        Integer isAdmin = claims.get(UserConstants.TOKEN_IS_ADMIN) == null ? 0
                : Integer.parseInt(claims.get(UserConstants.TOKEN_IS_ADMIN).toString());
        return User.get(userId, userName, displayName, email, isAdmin);
    }

}
