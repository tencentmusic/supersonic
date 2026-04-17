package com.tencent.supersonic.feishu.server.service;

import com.tencent.supersonic.feishu.api.cache.FeishuCacheService;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuTokenManager {

    private static final String TOKEN_URL =
            "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final int MAX_RETRIES = 3;

    private final FeishuProperties feishuProperties;
    private final FeishuCacheService cacheService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get tenant_access_token, using cache when available. Retries up to 3 times with exponential
     * backoff on failure.
     */
    public String getTenantAccessToken() {
        String cached = cacheService.getToken();
        if (cached != null) {
            return cached;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String token = requestToken();
                if (token != null) {
                    cacheService.putToken(token);
                    return token;
                }
                log.warn("Feishu token request returned null on attempt {}/{}", attempt,
                        MAX_RETRIES);
            } catch (Exception e) {
                log.warn("Failed to get tenant_access_token on attempt {}/{}: {}", attempt,
                        MAX_RETRIES, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try {
                    long sleepMs = (long) Math.pow(2, attempt) * 500;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting to retry token request");
                    break;
                }
            }
        }

        log.error("Failed to obtain tenant_access_token after {} retries", MAX_RETRIES);
        return null;
    }

    @SuppressWarnings("unchecked")
    private String requestToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("app_id", feishuProperties.getAppId(), "app_secret",
                feishuProperties.getAppSecret());

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            log.warn("Empty response body from Feishu token endpoint");
            return null;
        }

        Integer code = (Integer) responseBody.get("code");
        if (code != null && code != 0) {
            log.warn("Feishu token request error: code={}, msg={}", code, responseBody.get("msg"));
            return null;
        }

        String token = (String) responseBody.get("tenant_access_token");
        if (token != null) {
            log.info("Successfully obtained tenant_access_token");
        }
        return token;
    }
}
