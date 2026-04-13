package com.tencent.supersonic.headless.server.service.delivery;

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
            long expiresAt) {
        if (StringUtils.isBlank(secret) || scheduleId == null || executionId == null) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signature = mac.doFinal(
                    payload(scheduleId, executionId, expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new DeliveryException("Failed to sign report download URL: " + e.getMessage(), e);
        }
    }

    public static boolean isValidToken(String secret, Long scheduleId, Long executionId,
            long expiresAt, String token) {
        if (StringUtils.isAnyBlank(secret, token) || scheduleId == null || executionId == null) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            return false;
        }
        String expected = createToken(secret, scheduleId, executionId, expiresAt);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private static String payload(Long scheduleId, Long executionId, long expiresAt) {
        return scheduleId + ":" + executionId + ":" + expiresAt;
    }
}
