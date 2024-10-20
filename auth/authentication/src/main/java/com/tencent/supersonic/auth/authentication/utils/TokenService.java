package com.tencent.supersonic.auth.authentication.utils;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_CREATE_TIME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_PREFIX;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;

@Slf4j
@Component
public class TokenService {

    private AuthenticationConfig authenticationConfig;

    public TokenService(AuthenticationConfig authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    public String generateToken(Map<String, Object> claims, HttpServletRequest request) {
        String appKey = getAppKey(request);
        return generateToken(claims, appKey);
    }

    public String generateToken(Map<String, Object> claims, String appKey) {
        return toTokenString(claims, appKey);
    }

    private String toTokenString(Map<String, Object> claims, String appKey) {
        Long tokenTimeout = authenticationConfig.getTokenTimeout();
        long expiration = Long.parseLong(claims.get(TOKEN_CREATE_TIME) + "") + tokenTimeout;
        Date expirationDate = new Date(expiration);
        String tokenSecret = getTokenSecret(appKey);

        return Jwts.builder().setClaims(claims).setSubject(claims.get(TOKEN_USER_NAME).toString())
                .setExpiration(expirationDate)
                .signWith(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8),
                        SignatureAlgorithm.HS512.getJcaName()), SignatureAlgorithm.HS512)
                .compact();
    }

    private String getTokenSecret(String appKey) {
        Map<String, String> appKeyToSecretMap = authenticationConfig.getAppKeyToSecretMap();
        String secret = appKeyToSecretMap.get(appKey);
        if (StringUtils.isBlank(secret)) {
            throw new AccessException("get secret from appKey failed :" + appKey);
        }
        return secret;
    }

    public Optional<Claims> getClaims(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        String appKey = getAppKey(request);
        return getClaims(token, appKey);
    }

    public Optional<Claims> getClaims(String token, String appKey) {
        try {
            String tokenSecret = getTokenSecret(appKey);
            Claims claims =
                    Jwts.parser().setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                            .build().parseClaimsJws(getTokenString(token)).getBody();
            return Optional.of(claims);
        } catch (Exception e) {
            log.info("can not getClaims from appKey:{} token:{}, please login", appKey, token);
        }
        return Optional.empty();
    }

    private static String getTokenString(String token) {
        return token.startsWith(TOKEN_PREFIX)
                ? token.substring(token.indexOf(TOKEN_PREFIX) + TOKEN_PREFIX.length()).trim()
                : token.trim();
    }

    public String getAppKey(HttpServletRequest request) {
        String appKey = request.getHeader(authenticationConfig.getTokenHttpHeaderAppKey());
        if (StringUtils.isBlank(appKey)) {
            appKey = authenticationConfig.getTokenDefaultAppKey();
        }
        return appKey;
    }
}
