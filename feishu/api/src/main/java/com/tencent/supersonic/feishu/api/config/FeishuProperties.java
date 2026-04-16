package com.tencent.supersonic.feishu.api.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "s2.feishu")
public class FeishuProperties {
    private boolean enabled = false;
    /** Connection mode: "webhook" (HTTP callback) or "ws" (WebSocket long connection) */
    private String connectionMode = "webhook";
    /** Base URL of the SuperSonic API for internal HTTP calls */
    private String apiBaseUrl = "";
    private String appId;
    private String appSecret;
    private String verificationToken;
    private String encryptKey;
    private int defaultAgentId = 1;
    private long queryTimeoutMs = 30000;
    private int maxTableRows = 20;
    private UserMappingConfig userMapping = new UserMappingConfig();
    private ExportConfig export = new ExportConfig();
    private CacheConfig cache = new CacheConfig();
    private RateLimitConfig rateLimit = new RateLimitConfig();
    private AsyncConfig async = new AsyncConfig();
    private OAuthBindingConfig oauth = new OAuthBindingConfig();

    @PostConstruct
    void validate() {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new IllegalStateException(
                    "[Feishu] s2.feishu.enabled=true but app-id or app-secret is not configured. "
                            + "Set FEISHU_APP_ID and FEISHU_APP_SECRET environment variables.");
        }
    }

    @Data
    public static class AsyncConfig {
        private int corePoolSize = 4;
        private int maxPoolSize = 8;
        private int queueCapacity = 100;
    }

    @Data
    public static class CacheConfig {
        /** Cache type: caffeine or redis */
        private String type = "caffeine";
    }

    @Data
    public static class UserMappingConfig {
        private boolean autoMatchEnabled = true;
        private List<String> matchFields = List.of("EMPLOYEE_ID", "EMAIL", "MOBILE");
    }

    @Data
    public static class ExportConfig {
        private int maxRows = 100000;
    }

    @Data
    public static class RateLimitConfig {
        /** Whether rate limiting is enabled */
        private boolean enabled = true;
        /** Max requests per user per window */
        private int maxRequests = 20;
        /** IM message API QPS limit (platform limit 50, 20% headroom) */
        private int messageQps = 40;
        /** Contact API QPS limit (low-frequency, conservative) */
        private int contactQps = 5;
    }

    @Data
    public static class OAuthBindingConfig {
        /** Whether self-service OAuth binding is enabled */
        private boolean enabled = false;
        /** Bind token TTL in minutes */
        private int bindTokenTtlMinutes = 30;
        /** Automatically activate mapping after successful binding */
        private boolean autoActivate = true;
        /** Allow already-bound users to rebind */
        private boolean allowRebind = false;
    }
}
