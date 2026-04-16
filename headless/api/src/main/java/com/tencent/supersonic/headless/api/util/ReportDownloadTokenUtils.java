package com.tencent.supersonic.headless.api.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public final class ReportDownloadTokenUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private ReportDownloadTokenUtils() {}

    public static long expiresAtEpochSeconds(long ttlSeconds) {
        return Instant.now().getEpochSecond() + ttlSeconds;
    }

    public static String createToken(String secret, Long scheduleId, Long executionId,
            long expiresAt, Long tenantId) {
        if (StringUtils.isBlank(secret) || scheduleId == null || executionId == null
                || tenantId == null) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signature = mac.doFinal(payload(scheduleId, executionId, expiresAt, tenantId)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign report download URL", e);
        }
    }

    public static boolean isValidToken(String secret, Long scheduleId, Long executionId,
            long expiresAt, String token, Long tenantId) {
        if (StringUtils.isAnyBlank(secret, token) || scheduleId == null || executionId == null
                || tenantId == null) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            return false;
        }
        String expected = createToken(secret, scheduleId, executionId, expiresAt, tenantId);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private static String payload(Long scheduleId, Long executionId, long expiresAt,
            Long tenantId) {
        return scheduleId + ":" + executionId + ":" + expiresAt + ":" + tenantId;
    }
}
