package com.tencent.supersonic.feishu.server.render;

import com.tencent.supersonic.auth.api.authentication.service.InternalTokenGenerator;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fetches and caches the sensitivity level for each schema field (dimension and metric) by calling
 * the headless REST API. Results are cached for {@link #CACHE_TTL_SECONDS} seconds to avoid
 * per-request overhead.
 *
 * <p>
 * Returns a map of {@code bizName → sensitiveLevel code} (see {@link SensitiveLevelEnum}). Only
 * HIGH (2) and MID (1) entries are stored; LOW fields are omitted.
 */
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@Slf4j
public class SensitiveFieldService {

    /** Cache time-to-live in seconds (5 minutes). */
    private static final long CACHE_TTL_SECONDS = 300;

    /** Internal Feishu system user used for API calls (no personal permissions needed). */
    private static final User INTERNAL_USER =
            User.builder().name("feishu-internal").tenantId(1L).build();

    private final FeishuProperties properties;
    private final InternalTokenGenerator tokenGenerator;
    private final RestTemplate restTemplate;

    /** Cached map: bizName → sensitiveLevel code (HIGH=2, MID=1). */
    private volatile Map<String, Integer> cachedSensitiveFields = Collections.emptyMap();
    private volatile Instant cacheExpiry = Instant.EPOCH;
    private final ReentrantLock refreshLock = new ReentrantLock();

    public SensitiveFieldService(FeishuProperties properties,
            InternalTokenGenerator tokenGenerator) {
        this.properties = properties;
        this.tokenGenerator = tokenGenerator;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Returns the cached sensitivity map, refreshing if stale.
     *
     * @return map of bizName → sensitiveLevel code (1=MID, 2=HIGH)
     */
    public Map<String, Integer> getSensitiveFields() {
        if (Instant.now().isBefore(cacheExpiry)) {
            return cachedSensitiveFields;
        }
        // Only one thread refreshes at a time; others return stale cache while refresh is in flight
        if (refreshLock.tryLock()) {
            try {
                if (Instant.now().isBefore(cacheExpiry)) {
                    return cachedSensitiveFields; // double-check after acquiring lock
                }
                Map<String, Integer> fresh = fetchSensitiveFields();
                cachedSensitiveFields = fresh;
                cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
                log.debug("Sensitive field cache refreshed: {} fields", fresh.size());
            } catch (Exception e) {
                log.warn("Failed to refresh sensitive field cache: {}", e.getMessage());
                // Keep serving stale data; bump expiry slightly to avoid storm
                cacheExpiry = Instant.now().plusSeconds(30);
            } finally {
                refreshLock.unlock();
            }
        }
        return cachedSensitiveFields;
    }

    /** Fetches HIGH + MID sensitive fields from the headless API. */
    private Map<String, Integer> fetchSensitiveFields() {
        Map<String, Integer> result = new HashMap<>();
        HttpHeaders headers = buildHeaders();

        // HIGH sensitive dimensions
        fetchHighSensitiveDimensions(headers, result);

        // HIGH sensitive metrics
        fetchHighSensitiveMetrics(headers, result);

        // MID sensitive dimensions
        fetchSensitiveByLevel(headers, result, "dimension", SensitiveLevelEnum.MID.getCode());

        // MID sensitive metrics
        fetchSensitiveByLevel(headers, result, "metric", SensitiveLevelEnum.MID.getCode());

        return result;
    }

    private void fetchHighSensitiveDimensions(HttpHeaders headers, Map<String, Integer> result) {
        String url =
                properties.getApiBaseUrl() + "/api/semantic/dimension/getAllHighSensitiveDimension";
        try {
            ResponseEntity<List<Map<String, Object>>> resp =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            extractBizNames(resp.getBody(), SensitiveLevelEnum.HIGH.getCode(), result);
        } catch (Exception e) {
            log.debug("Could not fetch high-sensitive dimensions: {}", e.getMessage());
        }
    }

    private void fetchHighSensitiveMetrics(HttpHeaders headers, Map<String, Integer> result) {
        String url = properties.getApiBaseUrl() + "/api/semantic/metric/getAllHighSensitiveMetric";
        try {
            ResponseEntity<List<Map<String, Object>>> resp =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            extractBizNames(resp.getBody(), SensitiveLevelEnum.HIGH.getCode(), result);
        } catch (Exception e) {
            log.debug("Could not fetch high-sensitive metrics: {}", e.getMessage());
        }
    }

    /**
     * Queries the pageable dimension/metric endpoint for a specific sensitive level. Uses page size
     * 500 to avoid multiple round-trips in typical deployments.
     */
    @SuppressWarnings("unchecked")
    private void fetchSensitiveByLevel(HttpHeaders headers, Map<String, Integer> result,
            String resourceType, int sensitiveLevel) {
        String url = properties.getApiBaseUrl() + "/api/semantic/" + resourceType + "/query"
                + capitalize(resourceType);

        Map<String, Object> req = new HashMap<>();
        req.put("sensitiveLevel", sensitiveLevel);
        req.put("current", 1);
        req.put("pageSize", 500);

        try {
            ResponseEntity<Map<String, Object>> resp =
                    restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(req, headers),
                            new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> body = resp.getBody();
            if (body == null) {
                return;
            }
            // PageInfo structure: { "list": [...], "total": N, ... }
            Object listObj = body.get("list");
            if (listObj instanceof List) {
                extractBizNames((List<Map<String, Object>>) listObj, sensitiveLevel, result);
            }
        } catch (Exception e) {
            log.debug("Could not fetch {}-level {} fields: {}", sensitiveLevel, resourceType,
                    e.getMessage());
        }
    }

    private void extractBizNames(List<Map<String, Object>> items, int level,
            Map<String, Integer> result) {
        if (items == null) {
            return;
        }
        for (Map<String, Object> item : items) {
            Object bizName = item.get("bizName");
            if (bizName instanceof String && !((String) bizName).isBlank()) {
                // Only add if this level is more restrictive than what's already stored
                result.merge((String) bizName, level, Math::max);
            }
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String token = tokenGenerator.generateToken(INTERNAL_USER);
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
