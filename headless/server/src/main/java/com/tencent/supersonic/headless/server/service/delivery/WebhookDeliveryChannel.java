package com.tencent.supersonic.headless.server.service.delivery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook delivery channel. Sends report data to a configured HTTP endpoint.
 */
@Component
@Slf4j
public class WebhookDeliveryChannel implements ReportDeliveryChannel {

    private final RestTemplate restTemplate;

    public WebhookDeliveryChannel() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public DeliveryType getType() {
        return DeliveryType.WEBHOOK;
    }

    @Override
    public void deliver(String configJson, DeliveryContext context) throws DeliveryException {
        WebhookConfig config = parseConfig(configJson);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add custom headers if configured
            if (config.getHeaders() != null) {
                config.getHeaders().forEach(headers::add);
            }

            // Build payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("scheduleId", context.getScheduleId());
            payload.put("executionId", context.getExecutionId());
            payload.put("scheduleName", context.getScheduleName());
            payload.put("reportName", context.getReportName());
            payload.put("outputFormat", context.getOutputFormat());
            payload.put("rowCount", context.getRowCount());
            payload.put("executionTime", context.getExecutionTime());

            // Include file content if configured
            if (config.getIncludeFileContent() != null && config.getIncludeFileContent()
                    && StringUtils.isNotBlank(context.getFileLocation())) {
                File file = new File(context.getFileLocation());
                if (file.exists() && file.length() < 10 * 1024 * 1024) { // Max 10MB
                    byte[] content = Files.readAllBytes(file.toPath());
                    payload.put("fileContent", Base64.getEncoder().encodeToString(content));
                    payload.put("fileName", file.getName());
                }
            } else {
                payload.put("fileLocation", context.getFileLocation());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(config.getUrl(), HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DeliveryException(
                        "Webhook returned non-2xx status: " + response.getStatusCode());
            }

            log.info("Webhook delivery successful: url={}, scheduleId={}", config.getUrl(),
                    context.getScheduleId());

        } catch (RestClientException e) {
            log.error("Webhook delivery failed: url={}", config.getUrl(), e);
            throw new DeliveryException("Webhook request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Webhook delivery error: url={}", config.getUrl(), e);
            throw new DeliveryException("Webhook delivery error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateConfig(String configJson) {
        WebhookConfig config = parseConfig(configJson);
        if (StringUtils.isBlank(config.getUrl())) {
            throw new IllegalArgumentException("Webhook URL is required");
        }
        if (!config.getUrl().startsWith("http://") && !config.getUrl().startsWith("https://")) {
            throw new IllegalArgumentException("Webhook URL must start with http:// or https://");
        }
        return true;
    }

    private WebhookConfig parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            throw new IllegalArgumentException("Webhook config is required");
        }
        try {
            return JSON.parseObject(configJson, WebhookConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid webhook config JSON: " + e.getMessage());
        }
    }

    @Data
    public static class WebhookConfig {
        private String url;
        private Map<String, String> headers;
        private Boolean includeFileContent;
    }
}
