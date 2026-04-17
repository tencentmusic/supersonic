package com.tencent.supersonic.feishu.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.service.FeishuBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket long connection client for receiving Feishu events. Activated when
 * s2.feishu.connection-mode=ws. Uses the official Feishu SDK (oapi-sdk) to establish a persistent
 * WebSocket connection, eliminating the need for a public webhook URL (no ngrok required).
 *
 * The SDK handles reconnection, heartbeat, encryption/decryption, and signature verification
 * internally.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${s2.feishu.enabled:false}' == 'true' "
        + "&& '${s2.feishu.connection-mode:webhook}' == 'ws'")
public class FeishuWsClient implements SmartLifecycle {

    private final FeishuProperties properties;
    private final FeishuBotService botService;
    private final ObjectMapper objectMapper;

    private volatile Client wsClient;
    private volatile boolean running = false;

    @Override
    public void start() {
        log.info("[FeishuWS] Starting WebSocket long connection client, appId={}",
                properties.getAppId());

        String verificationToken =
                properties.getVerificationToken() != null ? properties.getVerificationToken() : "";
        String encryptKey = properties.getEncryptKey() != null ? properties.getEncryptKey() : "";

        EventDispatcher dispatcher = EventDispatcher.newBuilder(verificationToken, encryptKey)
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) throws Exception {
                        handleMessageEvent(event);
                    }
                }).build();

        // Catch uncaught exceptions from SDK internal thread pools (e.g. protobuf parsing errors)
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.error("[FeishuWS] Uncaught exception in thread {}", t.getName(), e));

        wsClient = new Client.Builder(properties.getAppId(), properties.getAppSecret())
                .eventHandler(dispatcher).build();
        wsClient.start();
        running = true;
        log.info("[FeishuWS] WebSocket client started successfully");
    }

    @Override
    public void stop() {
        log.info("[FeishuWS] Stopping WebSocket client");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @SuppressWarnings("unchecked")
    private void handleMessageEvent(P2MessageReceiveV1 event) {
        try {
            // Convert SDK typed event to Map for compatibility with existing FeishuBotService
            String eventJson = Jsons.DEFAULT.toJson(event.getEvent());
            Map<String, Object> eventMap = objectMapper.readValue(eventJson, Map.class);
            log.info("[FeishuWS] Received message event via WebSocket: messageId={}",
                    extractMessageId(eventMap));
            botService.handleEventAsync("im.message.receive_v1", eventMap);
        } catch (Exception e) {
            log.error("[FeishuWS] Failed to handle message event", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> event) {
        Map<String, Object> message = (Map<String, Object>) event.get("message");
        return message != null ? (String) message.get("message_id") : "unknown";
    }
}
