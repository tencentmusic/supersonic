package com.tencent.supersonic.feishu.server.rest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.service.FeishuBotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu/webhook")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
public class FeishuEventController {

    private final FeishuProperties properties;
    private final FeishuBotService botService;
    private final FeishuCacheService cacheService;
    private final ObjectMapper objectMapper;

    /**
     * Health check endpoint — GET to verify the webhook URL is accessible.
     */
    @GetMapping
    @AuthenticationIgnore
    public void healthCheck(HttpServletResponse response) throws IOException {
        writeJson(response, "{\"status\":\"ok\",\"module\":\"feishu-webhook\"}");
    }

    /**
     * Handle both url_verification and event callbacks from Feishu. Supports both Event API v1.0
     * (AES body encryption) and v2.0 (HMAC signature). Writes response directly via
     * HttpServletResponse to bypass all Spring middleware (ResponseAdvice, content negotiation,
     * compression).
     */
    @PostMapping
    @AuthenticationIgnore
    @SuppressWarnings("unchecked")
    public void handleEvent(@RequestBody String rawBody, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        log.info("[Webhook] Incoming POST, Content-Length={}, Content-Type={}, RemoteAddr={}",
                request.getContentLength(), request.getContentType(), request.getRemoteAddr());

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(rawBody, Map.class);
        } catch (Exception e) {
            log.warn("[Webhook] Failed to parse request body: {}", e.getMessage());
            writeJson(response, "{\"code\":0}");
            return;
        }

        // 1. v1.0 AES decryption: if body contains "encrypt" field, decrypt first
        String encryptKey = properties.getEncryptKey();
        String encryptedContent = (String) body.get("encrypt");
        if (encryptedContent != null) {
            if (StringUtils.isBlank(encryptKey)) {
                log.warn("[Webhook] Received encrypted event but no encrypt key configured");
                writeJson(response, "{\"code\":0}");
                return;
            }
            try {
                String decrypted = decryptAES(encryptedContent, encryptKey);
                body = objectMapper.readValue(decrypted, Map.class);
                log.info("[Webhook] Decrypted event body, type={}, keys={}", body.get("type"),
                        body.keySet());
            } catch (Exception e) {
                log.error("[Webhook] Failed to decrypt event body: {}", e.getMessage());
                writeJson(response, "{\"code\":0}");
                return;
            }
        }

        // 2. URL verification challenge — return immediately, bypassing all other checks
        if ("url_verification".equals(body.get("type"))) {
            Object challenge = body.getOrDefault("challenge", "");
            String challengeJson = objectMapper.writeValueAsString(Map.of("challenge", challenge));
            log.info("[Webhook] URL verification challenge, returning: {}", challengeJson);
            writeJson(response, challengeJson);
            return;
        }

        // Guard: if no verification mechanism is configured at all, reject unverifiable events.
        // This prevents arbitrary callers from injecting fake Feishu events when the administrator
        // has not yet configured either encryptKey or verificationToken.
        if (StringUtils.isBlank(encryptKey)
                && StringUtils.isBlank(properties.getVerificationToken())) {
            log.warn("[Webhook] No verification mechanism configured (encryptKey and "
                    + "verificationToken are both blank). Rejecting event to prevent forgery. "
                    + "Set feishu.encrypt-key or feishu.verification-token in application.yaml.");
            writeJson(response, "{\"code\":0}");
            return;
        }

        // 3. v2.0 HMAC signature verification
        // When encryptKey is configured, signature headers are MANDATORY — reject if missing.
        // This prevents attackers from bypassing verification by omitting the headers.
        Map<String, Object> header = (Map<String, Object>) body.get("header");
        if (header != null && StringUtils.isNotBlank(encryptKey)) {
            String timestamp = request.getHeader("X-Lark-Request-Timestamp");
            String nonce = request.getHeader("X-Lark-Request-Nonce");
            String signature = request.getHeader("X-Lark-Signature");

            if (StringUtils.isAnyBlank(timestamp, nonce, signature)) {
                log.warn(
                        "[Webhook] Missing signature headers: timestamp={}, nonce={}, signature={}",
                        timestamp != null, nonce != null, signature != null);
                writeJson(response, "{\"code\":0}");
                return;
            }

            if (!verifySignature(timestamp, nonce, encryptKey, rawBody, signature)) {
                log.warn("[Webhook] Signature verification failed");
                writeJson(response, "{\"code\":0}");
                return;
            }
        }

        // 4. Extract token, eventId, eventType, event — v1.0 vs v2.0
        String token;
        String eventId;
        String eventType;
        Map<String, Object> event;

        if (header != null) {
            // v2.0: {schema, header: {event_id, event_type, token}, event: {...}}
            token = (String) header.get("token");
            eventId = (String) header.get("event_id");
            eventType = (String) header.get("event_type");
            event = (Map<String, Object>) body.get("event");
            log.info("[Webhook] v2.0 event: eventId={}, eventType={}", eventId, eventType);
        } else if ("event_callback".equals(body.get("type"))) {
            // v1.0: {uuid, token, ts, type: "event_callback", event: {type, ...}}
            token = (String) body.get("token");
            eventId = (String) body.get("uuid");
            event = (Map<String, Object>) body.get("event");
            eventType = event != null ? (String) event.get("type") : null;
            log.info("[Webhook] v1.0 event: uuid={}, eventType={}", eventId, eventType);
        } else {
            log.warn("[Webhook] Unknown event format, keys: {}", body.keySet());
            writeJson(response, "{\"code\":0}");
            return;
        }

        // 5. Verify token
        if (StringUtils.isNotBlank(properties.getVerificationToken())
                && !properties.getVerificationToken().equals(token)) {
            log.warn("[Webhook] Token verification failed, expected={}, got={}",
                    properties.getVerificationToken(), token);
            writeJson(response, "{\"code\":0}");
            return;
        }

        // 6. Event dedup
        if (cacheService.isDuplicateEvent(eventId)) {
            log.info("[Webhook] Duplicate event: {}", eventId);
            writeJson(response, "{\"code\":0}");
            return;
        }

        // 7. Async dispatch — webhook must return within 3s
        log.info("[Webhook] Dispatching event: eventType={}, eventId={}", eventType, eventId);
        botService.handleEventAsync(eventType, event);

        writeJson(response, "{\"code\":0}");
    }

    /**
     * Write JSON response directly to HttpServletResponse, bypassing all Spring middleware
     * (ResponseAdvice, content negotiation, compression filters).
     */
    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * Decrypt v1.0 encrypted event body. Algorithm: AES-256-CBC, key = SHA256(encrypt_key), IV =
     * first 16 bytes of ciphertext.
     */
    private String decryptAES(String encrypted, String key) throws Exception {
        byte[] keyBytes =
                MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        byte[] cipherBytes = Base64.getDecoder().decode(encrypted);

        byte[] iv = Arrays.copyOfRange(cipherBytes, 0, 16);
        byte[] data = Arrays.copyOfRange(cipherBytes, 16, cipherBytes.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(data);

        // Remove PKCS7 padding
        int padLen = decrypted[decrypted.length - 1];
        return new String(decrypted, 0, decrypted.length - padLen, StandardCharsets.UTF_8);
    }

    /**
     * Verify v2.0 event signature: SHA256(timestamp + nonce + encryptKey + body).
     */
    private boolean verifySignature(String timestamp, String nonce, String encryptKey, String body,
            String expected) {
        try {
            String content = timestamp + nonce + encryptKey + body;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] calculated = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            byte[] expectedBytes = hexToBytes(expected);
            // Timing-safe comparison to prevent timing attacks
            return MessageDigest.isEqual(calculated, expectedBytes);
        } catch (Exception e) {
            log.error("[Webhook] Signature verification error", e);
            return false;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }
}
