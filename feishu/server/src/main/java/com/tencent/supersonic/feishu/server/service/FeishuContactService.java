package com.tencent.supersonic.feishu.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuContactService {

    private static final String CONTACT_URL =
            "https://open.feishu.cn/open-apis/contact/v3/users/{open_id}?user_id_type=open_id";

    private final FeishuTokenManager feishuTokenManager;
    private final FeishuApiRateLimiter rateLimiter;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Retrieve Feishu user contact information by open_id.
     *
     * @param openId the Feishu open_id
     * @return contact info or null if retrieval fails
     */
    @SuppressWarnings("unchecked")
    public FeishuContactInfo getContactInfo(String openId) {
        try {
            String token = feishuTokenManager.getTenantAccessToken();
            if (token == null) {
                log.warn("Cannot get contact info: tenant_access_token is null");
                return null;
            }

            if (rateLimiter.isRateLimited(FeishuApiRateLimiter.ApiType.CONTACT)) {
                log.warn("Contact API rate limited for openId={}", openId);
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response =
                    restTemplate.exchange(CONTACT_URL, HttpMethod.GET, request, Map.class, openId);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("Empty response body from Feishu contact API for openId={}", openId);
                return null;
            }

            Integer code = (Integer) responseBody.get("code");
            if (code != null && code != 0) {
                log.warn("Feishu contact API error for openId={}: code={}, msg={}", openId, code,
                        responseBody.get("msg"));
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            if (data == null) {
                log.warn("No data in Feishu contact response for openId={}", openId);
                return null;
            }

            Map<String, Object> user = (Map<String, Object>) data.get("user");
            if (user == null) {
                log.warn("No user in Feishu contact data for openId={}", openId);
                return null;
            }

            String employeeId = (String) user.get("employee_no");
            String email = (String) user.get("email");
            String mobile = (String) user.get("mobile");
            String name = (String) user.get("name");

            log.debug(
                    "Retrieved contact info for openId={}: name={}, employeeId={}, email={}, mobile={}",
                    openId, name, employeeId, email, mobile);

            return new FeishuContactInfo(employeeId, email, mobile, name);
        } catch (Exception e) {
            log.error("Failed to get contact info for openId={}: {}", openId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Feishu user contact information.
     */
    public record FeishuContactInfo(String employeeId, String email, String mobile, String name) {}
}
