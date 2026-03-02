package com.tencent.supersonic.feishu.server.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuBindTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String USED_PREFIX = "bind:used:";

    private final FeishuProperties properties;
    private final FeishuCacheService cacheService;

    @Data
    public static class BindTokenPayload {
        private String openId;
        private Long mappingId;
        private String feishuUserName;
        private Long tenantId;
        private long exp;
    }

    /**
     * Generate a signed bind token containing the Feishu user info.
     */
    public String generateToken(String openId, Long mappingId, String feishuUserName,
            Long tenantId) {
        BindTokenPayload payload = new BindTokenPayload();
        payload.setOpenId(openId);
        payload.setMappingId(mappingId);
        payload.setFeishuUserName(feishuUserName);
        payload.setTenantId(tenantId);

        long ttlMs = properties.getOauth().getBindTokenTtlMinutes() * 60L * 1000L;
        payload.setExp(System.currentTimeMillis() + ttlMs);

        String payloadJson = JSON.toJSONString(payload);
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha256(payloadB64);

        return payloadB64 + "." + signature;
    }

    /**
     * Validate a bind token: verify signature, check expiry, check not already used.
     *
     * @return the decoded payload, or null if invalid
     */
    public BindTokenPayload validateToken(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return null;
        }

        String payloadB64 = parts[0];
        String signature = parts[1];

        // Verify HMAC signature (timing-safe comparison)
        String expectedSignature = hmacSha256(payloadB64);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Bind token signature mismatch");
            return null;
        }

        // Decode payload
        BindTokenPayload payload;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
            payload = JSON.parseObject(new String(decoded, StandardCharsets.UTF_8),
                    BindTokenPayload.class);
        } catch (Exception e) {
            log.warn("Failed to decode bind token payload: {}", e.getMessage());
            return null;
        }

        // Check expiry
        if (System.currentTimeMillis() > payload.getExp()) {
            log.debug("Bind token expired for openId={}", payload.getOpenId());
            return null;
        }

        // Check not already used
        String tokenHash = sha256(token);
        if (cacheService.get(USED_PREFIX + tokenHash) != null) {
            log.debug("Bind token already used for openId={}", payload.getOpenId());
            return null;
        }

        return payload;
    }

    /**
     * Mark a token as used (one-time use).
     */
    public void markUsed(String token) {
        String tokenHash = sha256(token);
        cacheService.put(USED_PREFIX + tokenHash, "1");
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    properties.getAppSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }
}
