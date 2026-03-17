package com.tencent.supersonic.auth.authentication.utils;

import javax.crypto.SecretKey;

import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.pojo.UserWithPassword;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserTokenDO;
import com.tencent.supersonic.auth.authentication.persistence.repository.UserRepository;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.util.ContextUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_PREFIX;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;

@Slf4j
@Component
public class TokenService {

    private static final String SYS_DB_TOKEN_PREFIX = "SysDbToken:";
    private static final int HS512_MIN_KEY_LENGTH = 64;
    private static final String DEFAULT_KEY_PADDING =
            "WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==";

    private final AuthenticationConfig authenticationConfig;

    // 缓存 SecretKey，避免重复创建
    private final Map<String, SecretKey> secretKeyCache = new ConcurrentHashMap<>();

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

        UserWithPassword appUser = new UserWithPassword();
        appUser.setId(1L);
        appUser.setName(appName);
        appUser.setPassword("c3VwZXJzb25pY0BiaWNvbdktJJYWw6A3rEmBUPzbn/6DNeYnD+y3mAwDKEMS3KVT");
        appUser.setDisplayName(appName);
        appUser.setIsAdmin(0);
        appUser.setTenantId(1L);
        return generateToken(UserWithPassword.convert(appUser), request);
    }

    public Optional<Claims> getClaims(HttpServletRequest request) {
        String token = request.getHeader(authenticationConfig.getTokenHttpHeaderKey());
        String appKey = getAppKey(request);
        return getClaims(token, appKey);
    }

    public Optional<Claims> getClaims(String token, String appKey) {
        if (StringUtils.isBlank(token)) {
            log.debug("token is blank, appKey:{}", appKey);
            return Optional.empty();
        }

        try {
            validateSysDbToken(token, appKey);
            SecretKey key = getOrCreateSecretKey(appKey);
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(getTokenString(token)).getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.info("Token expired for appKey:{}, reason:{}", appKey, e.getMessage());
        } catch (SignatureException e) {
            log.warn("Token signature verification failed for appKey:{}", appKey);
        } catch (MalformedJwtException e) {
            log.warn("Malformed token for appKey:{}", appKey);
        } catch (AccessException e) {
            log.warn("Access denied: {}", e.getMessage());
        } catch (Exception e) {
            log.info("Cannot getClaims from appKey:{}, token:{}, reason:{}", appKey,
                    token.substring(0, Math.min(8, token.length())) + "***", e.getMessage());
        }
        return Optional.empty();
    }

    private void validateSysDbToken(String token, String appKey) {
        if (StringUtils.isNotBlank(appKey) && appKey.startsWith(SYS_DB_TOKEN_PREFIX)) {
            UserRepository userRepository = ContextUtils.getBean(UserRepository.class);
            String tokenName = appKey.substring(SYS_DB_TOKEN_PREFIX.length());
            UserTokenDO dbToken = userRepository.getUserTokenByName(tokenName);

            if (dbToken == null || !dbToken.getToken().equals(token.replace("Bearer ", ""))) {
                throw new AccessException("Token does not exist: " + appKey);
            }
        }
    }

    private static String getTokenString(String token) {
        if (token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length()).trim();
        }
        return token.trim();
    }

    private String toTokenString(Map<String, Object> claims, String appKey, long expiration) {
        Date expirationDate = new Date(expiration);
        SecretKey key = getOrCreateSecretKey(appKey);

        return Jwts.builder().claims(claims).subject(String.valueOf(claims.get(TOKEN_USER_NAME)))
                .expiration(expirationDate).signWith(key, Jwts.SIG.HS512).compact();
    }

    /**
     * 获取或创建 SecretKey（带缓存）
     */
    private SecretKey getOrCreateSecretKey(String appKey) {
        return secretKeyCache.computeIfAbsent(appKey, this::createSecretKey);
    }

    /**
     * 创建 SecretKey，确保满足 HS512 最小长度要求
     */
    private SecretKey createSecretKey(String appKey) {
        String tokenSecret = getTokenSecret(appKey);
        byte[] keyBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);

        // HS512 要求至少 64 字节密钥
        if (keyBytes.length < HS512_MIN_KEY_LENGTH) {
            keyBytes = extendKeyBytes(keyBytes);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 扩展密钥长度到 64 字节
     */
    private byte[] extendKeyBytes(byte[] shortKey) {
        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            return sha512.digest(shortKey);
        } catch (NoSuchAlgorithmException e) {
            // SHA-512 应该总是可用的，这里做个兜底
            log.error("SHA-512 algorithm not available", e);
            throw new RuntimeException("Failed to extend key bytes", e);
        }
    }

    private String getTokenSecret(String appKey) {
        Map<String, String> appKeyToSecretMap = authenticationConfig.getAppKeyToSecretMap();
        String secret = appKeyToSecretMap.get(appKey);

        if (StringUtils.isNotBlank(secret)) {
            return secret;
        }

        // 处理 SysDbToken 类型的 appKey
        if (StringUtils.isNotBlank(appKey) && appKey.startsWith(SYS_DB_TOKEN_PREFIX)) {
            String realAppKey = appKey.substring(SYS_DB_TOKEN_PREFIX.length());
            return realAppKey.length() >= DEFAULT_KEY_PADDING.length() ? realAppKey
                    : realAppKey + DEFAULT_KEY_PADDING.substring(realAppKey.length());
        }

        throw new AccessException("Get secret from appKey failed: " + appKey);
    }

    public String getAppKey(HttpServletRequest request) {
        String appKey = request.getHeader(authenticationConfig.getTokenHttpHeaderAppKey());
        return StringUtils.isBlank(appKey) ? authenticationConfig.getTokenDefaultAppKey() : appKey;
    }

    public String getDefaultAppKey() {
        return authenticationConfig.getTokenDefaultAppKey();
    }

    public long getTokenTimeout() {
        return authenticationConfig.getTokenTimeout();
    }

    /**
     * 清除密钥缓存（配置变更时调用）
     */
    public void clearSecretKeyCache() {
        secretKeyCache.clear();
    }
}
