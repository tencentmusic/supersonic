package com.tencent.supersonic.auth.authentication.utils;



import javax.crypto.spec.SecretKeySpec;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.ContextUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

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
        long expiration = System.currentTimeMillis() + authenticationConfig.getTokenTimeout();
        return generateToken(claims, appKey, expiration);
    }

    public String generateToken(Map<String, Object> claims, long expiration) {
        String appKey = authenticationConfig.getTokenDefaultAppKey();
        long exp = System.currentTimeMillis() + expiration;
        return generateToken(claims, appKey, exp);
    }

    public String generateToken(Map<String, Object> claims, String appKey) {
        long expiration = System.currentTimeMillis() + authenticationConfig.getTokenTimeout();
        return toTokenString(claims, appKey, expiration);
    }

    public String generateToken(Map<String, Object> claims, String appKey, long expiration) {
        return toTokenString(claims, appKey, expiration);
    }

    public String generateAppUserToken(HttpServletRequest request) {
        String appName = request.getHeader("AppId");
        if (StringUtils.isBlank(appName)) {
            String message = "AppId is blank, get app_user failed";
            log.warn("{}, uri: {}", message, request.getServletPath());
            throw new AccessException(message);
        }

        UserWithPassword appUser = new UserWithPassword(appName);
        appUser.setId(1L);
        appUser.setName(appName);
        appUser.setPassword("c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT");
        appUser.setDisplayName(appName);
        appUser.setIsAdmin(0);
        return generateToken(UserWithPassword.convert(appUser), request);
    }


    public Optional<Claims> getClaims(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        String appKey = getAppKey(request);
        return getClaims(token, appKey);
    }

    private Optional<Claims> getClaims(String token, HttpServletRequest request) {
        Optional<Claims> claims;
        try {
            String appKey = getAppKey(request);
            claims = getClaims(token, appKey);
        } catch (Exception e) {
            throw new AccessException("parse user info from token failed :" + token);
        }
        return claims;
    }

    public Optional<Claims> getClaims(String token, String appKey) {
        try {
            if(StringUtils.isNotBlank(appKey)&&appKey.startsWith("SysDbToken:")) {// 如果是配置的长期令牌，需校验数据库是否存在该配置
                UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
                UserTokenDO dbToken= userRepository.getUserTokenByName(appKey.substring("SysDbToken:".length()));
                if(dbToken==null||!dbToken.getToken().equals(token.replace("Bearer ",""))) {
                    throw new AccessException("Token does not exist :" + appKey);
                }
            }
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

    private String toTokenString(Map<String, Object> claims, String appKey, long expiration) {
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
            if(StringUtils.isNotBlank(appKey)&&appKey.startsWith("SysDbToken:")) { // 是配置的长期令牌
                String realAppKey=appKey.substring("SysDbToken:".length());
                String tmp = "WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==";
                if(tmp.length()<=realAppKey.length()) {
                    return realAppKey;
                }
                else{
                    return realAppKey+tmp.substring(realAppKey.length());
                }
            }
            throw new AccessException("get secret from appKey failed :" + appKey);
        }
        return secret;
    }

    public String getAppKey(HttpServletRequest request) {
        String appKey = request.getHeader(authenticationConfig.getTokenHttpHeaderAppKey());
        if (StringUtils.isBlank(appKey)) {
            appKey = authenticationConfig.getTokenDefaultAppKey();
        }
        return appKey;
    }

    public String getDefaultAppKey() {
        return authenticationConfig.getTokenDefaultAppKey();
    }
}
