package com.tencent.supersonic.feishu.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.feishu.server.service.FeishuApiRateLimiter.ApiType;
import com.tencent.supersonic.feishu.server.service.FeishuApiRateLimiter.FeishuApiRateLimitedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuMessageSender {

    private static final String BASE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";
    private static final String FILES_URL = "https://open.feishu.cn/open-apis/im/v1/files";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FeishuTokenManager feishuTokenManager;
    private final FeishuApiRateLimiter rateLimiter;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Reply with a plain text message.
     */
    public void replyText(String messageId, String text) {
        String content = toJsonString(Map.of("text", text));
        Map<String, String> body = Map.of("content", content, "msg_type", "text");
        doReply(messageId, body);
    }

    /**
     * Reply with an interactive card message.
     */
    public void replyCard(String messageId, Map<String, Object> card) {
        String content = toJsonString(card);
        Map<String, String> body = Map.of("content", content, "msg_type", "interactive");
        doReply(messageId, body);
    }

    /**
     * Send a plain text message to a user by open_id.
     */
    public void sendText(String receiveId, String text) {
        String content = toJsonString(Map.of("text", text));
        Map<String, String> body =
                Map.of("receive_id", receiveId, "content", content, "msg_type", "text");
        doSend(body);
    }

    /**
     * Send an interactive card message to a user by open_id.
     */
    public void sendCard(String receiveId, Map<String, Object> card) {
        String content = toJsonString(card);
        Map<String, String> body =
                Map.of("receive_id", receiveId, "content", content, "msg_type", "interactive");
        doSend(body);
    }

    /**
     * Upload a file to Feishu and return the file_key. Uses multipart/form-data POST to
     * /open-apis/im/v1/files.
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(File file, String fileName, String fileType) {
        if (rateLimiter.isRateLimited(ApiType.MESSAGE)) {
            throw new FeishuApiRateLimitedException("IM file upload API rate limited");
        }
        String token = feishuTokenManager.getTenantAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file_type", fileType);
        body.add("file_name", fileName);
        body.add("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(FILES_URL, request, Map.class);

        if (response != null && response.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return (String) data.get("file_key");
        }
        throw new RuntimeException("Failed to upload file: unexpected response " + response);
    }

    /**
     * Send a file message to a user by open_id using an uploaded file_key.
     */
    public void sendFile(String receiveId, String fileKey) {
        String content = toJsonString(Map.of("file_key", fileKey));
        Map<String, String> body =
                Map.of("receive_id", receiveId, "content", content, "msg_type", "file");
        doSend(body);
    }

    private void doReply(String messageId, Map<String, String> body) {
        if (rateLimiter.isRateLimited(ApiType.MESSAGE)) {
            throw new FeishuApiRateLimitedException("IM message API rate limited");
        }
        try {
            String url = BASE_URL + "/" + messageId + "/reply";
            HttpEntity<Map<String, String>> request = buildRequest(body);
            restTemplate.postForEntity(url, request, Map.class);
            log.debug("Replied to message {} successfully", messageId);
        } catch (Exception e) {
            log.error("Failed to reply to message {}: {}", messageId, e.getMessage(), e);
            throw e;
        }
    }

    private void doSend(Map<String, String> body) {
        if (rateLimiter.isRateLimited(ApiType.MESSAGE)) {
            throw new FeishuApiRateLimitedException("IM message API rate limited");
        }
        try {
            String url = BASE_URL + "?receive_id_type=open_id";
            HttpEntity<Map<String, String>> request = buildRequest(body);
            restTemplate.postForEntity(url, request, Map.class);
            log.debug("Sent message to {} successfully", body.get("receive_id"));
        } catch (Exception e) {
            log.error("Failed to send message to {}: {}", body.get("receive_id"), e.getMessage(),
                    e);
            throw e;
        }
    }

    private HttpEntity<Map<String, String>> buildRequest(Map<String, String> body) {
        String token = feishuTokenManager.getTenantAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        } else {
            log.warn("Sending Feishu message without token — request will likely fail");
        }
        return new HttpEntity<>(body, headers);
    }

    private String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage(), e);
            return "{}";
        }
    }
}
