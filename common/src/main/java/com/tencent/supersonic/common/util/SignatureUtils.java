package com.tencent.supersonic.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.tencent.supersonic.common.pojo.Pair;

import java.nio.charset.StandardCharsets;

public class SignatureUtils {

    private static final long TIME_OUT = 60 * 1000 * 30;

    public static String generateSignature(String appSecret, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec =
                    new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static Pair<Boolean, String> isValidSignature(String appKey, String appSecret,
            long timestamp, String signatureToCheck) {
        long currentTimeMillis = System.currentTimeMillis();

        if (currentTimeMillis < timestamp) {
            return new Pair<>(false, "Timestamp is in the future");
        }

        if (currentTimeMillis - timestamp > TIME_OUT) {
            return new Pair<>(false, "Timestamp is too old");
        }

        String generatedSignature = generateSignature(appSecret, timestamp);

        if (generatedSignature.equals(signatureToCheck)) {
            return new Pair<>(true, "Signature is valid");
        } else {
            return new Pair<>(false, "Invalid signature");
        }
    }
}
