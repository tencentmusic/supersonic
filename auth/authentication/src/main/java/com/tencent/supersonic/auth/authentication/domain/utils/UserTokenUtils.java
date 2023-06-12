package com.tencent.supersonic.auth.authentication.domain.utils;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_ALGORITHM;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_CREATE_TIME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_PREFIX;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_TIME_OUT;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_DISPLAY_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_EMAIL;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_PASSWORD;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.common.exception.AccessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class UserTokenUtils {

    private AuthenticationConfig authenticationConfig;

    public UserTokenUtils(AuthenticationConfig authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    public String generateToken(UserWithPassword user) {
        Map<String, Object> claims = new HashMap<>(5);
        claims.put(TOKEN_USER_ID, user.getId());
        claims.put(TOKEN_USER_NAME, StringUtils.isEmpty(user.getName()) ? "" : user.getName());
        claims.put(TOKEN_USER_PASSWORD, StringUtils.isEmpty(user.getPassword()) ? "" : user.getPassword());
        claims.put(TOKEN_USER_DISPLAY_NAME, user.getDisplayName());
        claims.put(TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }

    public String generateAdminToken() {
        Map<String, Object> claims = new HashMap<>(5);
        claims.put(TOKEN_USER_ID, "1");
        claims.put(TOKEN_USER_NAME, "admin");
        claims.put(TOKEN_USER_PASSWORD, "admin");
        claims.put(TOKEN_USER_DISPLAY_NAME, "admin");
        claims.put(TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }


    public User getUser(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        final Claims claims = getClaims(token);
        Long userId = Long.parseLong(claims.getOrDefault(TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(TOKEN_USER_DISPLAY_NAME));
        return User.get(userId, userName, displayName, email);
    }

    public UserWithPassword getUserWithPassword(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        if (StringUtils.isBlank(token)) {
            throw new AccessException("token is blank, get user failed");
        }
        final Claims claims = getClaims(token);
        Long userId = Long.parseLong(claims.getOrDefault(TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(TOKEN_USER_DISPLAY_NAME));
        String password = String.valueOf(claims.get(TOKEN_USER_PASSWORD));
        return UserWithPassword.get(userId, userName, displayName, email, password);
    }

    private Claims getClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(authenticationConfig.getTokenSecret().getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token.startsWith(TOKEN_PREFIX)
                            ? token.substring(token.indexOf(TOKEN_PREFIX) + TOKEN_PREFIX.length()).trim() :
                            token.trim()).getBody();
        } catch (Exception e) {
            throw new AccessException("parse user info from token failed :" + token);
        }
        return claims;
    }

    private String generate(Map<String, Object> claims) {
        return toTokenString(claims);
    }

    private String toTokenString(Map<String, Object> claims) {
        long expiration = Long.parseLong(claims.get(TOKEN_CREATE_TIME) + "") + TOKEN_TIME_OUT;

        SignatureAlgorithm.valueOf(TOKEN_ALGORITHM);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(claims.get(TOKEN_USER_NAME).toString())
                .setExpiration(new Date(expiration))
                .signWith(SignatureAlgorithm.valueOf(TOKEN_ALGORITHM),
                        authenticationConfig.getTokenSecret().getBytes(StandardCharsets.UTF_8))
                .compact();
    }


}
