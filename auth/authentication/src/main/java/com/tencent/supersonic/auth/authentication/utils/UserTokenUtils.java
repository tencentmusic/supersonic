package com.tencent.supersonic.auth.authentication.utils;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_ALGORITHM;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_CREATE_TIME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_IS_ADMIN;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_PREFIX;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_DISPLAY_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_EMAIL;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_PASSWORD;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
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
        claims.put(TOKEN_IS_ADMIN, user.getIsAdmin());
        return generate(claims);
    }

    public String generateAdminToken() {
        UserWithPassword admin = new UserWithPassword("admin");
        admin.setId(1L);
        admin.setName("admin");
        admin.setPassword("admin");
        admin.setDisplayName("admin");
        admin.setIsAdmin(1);
        return generateToken(admin);
    }

    public User getUser(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        final Claims claims = getClaims(token);
        Long userId = Long.parseLong(claims.getOrDefault(TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(TOKEN_USER_DISPLAY_NAME));
        Integer isAdmin = claims.get(TOKEN_IS_ADMIN) == null
                ? 0 : Integer.parseInt(claims.get(TOKEN_IS_ADMIN).toString());
        return User.get(userId, userName, displayName, email, isAdmin);
    }

    public UserWithPassword getUserWithPassword(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        if (StringUtils.isBlank(token)) {
            String message = "token is blank, get user failed";
            log.warn("{}, uri: {}", message, request.getServletPath());
            throw new AccessException(message);
        }
        final Claims claims = getClaims(token);
        Long userId = Long.parseLong(claims.getOrDefault(TOKEN_USER_ID, 0).toString());
        String userName = String.valueOf(claims.get(TOKEN_USER_NAME));
        String email = String.valueOf(claims.get(TOKEN_USER_EMAIL));
        String displayName = String.valueOf(claims.get(TOKEN_USER_DISPLAY_NAME));
        String password = String.valueOf(claims.get(TOKEN_USER_PASSWORD));
        Integer isAdmin = claims.get(TOKEN_IS_ADMIN) == null
                ? 0 : Integer.parseInt(claims.get(TOKEN_IS_ADMIN).toString());
        return UserWithPassword.get(userId, userName, displayName, email, password, isAdmin);
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
        Long tokenTimeout = authenticationConfig.getTokenTimeout();
        long expiration = Long.parseLong(claims.get(TOKEN_CREATE_TIME) + "") + tokenTimeout;
        Date expirationDate = new Date(expiration);

        SignatureAlgorithm.valueOf(TOKEN_ALGORITHM);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(claims.get(TOKEN_USER_NAME).toString())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.valueOf(TOKEN_ALGORITHM),
                        authenticationConfig.getTokenSecret().getBytes(StandardCharsets.UTF_8))
                .compact();
    }

}
