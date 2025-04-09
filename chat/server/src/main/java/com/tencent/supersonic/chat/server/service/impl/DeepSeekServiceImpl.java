package com.tencent.supersonic.chat.server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.server.config.CrabConfig;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.DeepSeekService;
import com.tencent.supersonic.common.pojo.FileInfo;
import com.tencent.supersonic.common.util.MiguApiUrlUtils;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeepSeekServiceImpl implements DeepSeekService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final CrabConfig crabConfig;
    private final ChatQueryServiceImpl chatQueryService;
    private final ChatManageService chatManageService;
    private static final String FILE_ID = "fileId";
    @Autowired
    public DeepSeekServiceImpl(WebClient.Builder webClientBuilder,
                               ObjectMapper objectMapper,
                               CrabConfig crabConfig,
                               ChatQueryServiceImpl chatQueryService,
                               ChatManageService chatManageService) {
        this.objectMapper = objectMapper;
        this.crabConfig = crabConfig;
        this.chatQueryService = chatQueryService;
        this.chatManageService = chatManageService;
        this.webClient = webClientBuilder
                .baseUrl(crabConfig.getHost())
                .build();
    }

    @Override
    public SseEmitter streamChat(ChatExecuteReq chatExecuteReq) {
        // 初始化请求
        String fileIds = "";
        if (chatExecuteReq.getFileInfoList() != null && !chatExecuteReq.getFileInfoList().isEmpty()) {
            if (chatExecuteReq.getQueryText() == null){
                chatExecuteReq.setQueryText("请解析文本内容：");
            }
            fileIds = chatExecuteReq.getFileInfoList().stream()
                    .map(FileInfo::getFileId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(";"));
        }

        String requestBody = buildRequestBody(chatExecuteReq);
        String urlPath = buildSignedUrl();

        // 创建SSE发射器（120秒超时）
        SseEmitter emitter = new SseEmitter(120_000L);
        StringBuilder contentAccumulator = new StringBuilder();
        AtomicReference<Disposable> disposableRef = new AtomicReference<>();

        // 设置超时和错误处理
        emitter.onCompletion(() -> {
            disposeSafely(disposableRef.get());
            log.info("SSE completed normally");
        });

        emitter.onTimeout(() -> {
            savePartialResult(chatExecuteReq, contentAccumulator.toString());
            disposeSafely(disposableRef.get());
            emitter.complete();
            log.warn("SSE terminated by timeout");
        });

        emitter.onError(e -> {
            savePartialResult(chatExecuteReq, contentAccumulator.toString());
            disposeSafely(disposableRef.get());
            log.error("SSE error occurred", e);
        });

        // 调用DeepSeek API
        Disposable disposable = webClient.post()
                .uri(urlPath)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(FILE_ID, fileIds)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new RuntimeException("API request failed")))
                .bodyToFlux(JsonNode.class)
                .doOnSubscribe(sub -> log.info("Subscription started"))
                .onBackpressureBuffer(100)
                .flatMap(response -> processStreamResponse(response, contentAccumulator))
                .doOnCancel(() -> log.warn("Downstream cancelled"))
                .subscribe(
                        chunk -> sendSseChunk(emitter, chunk),
                        error -> handleStreamError(emitter, error),
                        () -> completeStream(emitter, chatExecuteReq, contentAccumulator)
                );
        disposableRef.set(disposable);
        return emitter;
    }

    // 释放资源避免内存泄露
    private void disposeSafely(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            log.debug("Subscription disposed");
        }
    }

    private String buildSignedUrl() {
        Map<String, Object> map = new HashMap<>();
        String urlpath = MiguApiUrlUtils.doSignature(crabConfig.getDeepseekUrl(), "post", map, crabConfig.getAppId(), crabConfig.getSecretKey());
        return urlpath;
    }

    private String buildRequestBody(ChatExecuteReq req) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("serviceName", crabConfig.getDsServiceName());
            request.put("serviceType", crabConfig.getDsServiceType());
            request.put("requestId", UUID.randomUUID().toString());
            request.put("sessionId", req.getSessionId() != null ? req.getSessionId() : UUID.randomUUID().toString());

            ObjectNode params = request.putObject("params");
            params.set("messages", buildMessagesArray(req));
            params.put("model", crabConfig.getDsModel(crabConfig.getDsServiceName()));
            params.put("stream", true);

            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode buildMessagesArray(ChatExecuteReq req) {
        ArrayNode messages = objectMapper.createArrayNode();

        // 添加历史对话
        getHistoryQueries(req.getChatId()).forEach(query -> {
            if (query.getQueryResult().getHasFile()) {
                query.getQueryResult().getFileInfoList().forEach(file -> {
                    messages.add(objectMapper.createObjectNode()
                            .put("role", "user")
                            .put("content", file.getFileContent()));
                });
            }
            messages.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", query.getQueryText()));
            messages.add(objectMapper.createObjectNode()
                    .put("role", "assistant")
                    .put("content", query.getQueryResult().getTextResult()));
        });
        if (req.getFileInfoList() != null && !req.getFileInfoList().isEmpty()) {
            for (FileInfo file : req.getFileInfoList()) {
                messages.add(objectMapper.createObjectNode()
                        .put("role", "user")
                        .put("content", file.getFileContent()));
            }
        }
        // 添加当前问题
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", req.getQueryText()));

        return messages;
    }

    private Flux<String> processStreamResponse(JsonNode response, StringBuilder accumulator) {
        try {
            // 错误处理
            if (response.has("errorMessage")) {
                String errorMsg = response.path("errorMessage").asText();
                log.error("API error: {}", errorMsg);
                return Flux.error(new RuntimeException(errorMsg));
            }

            JsonNode body = response.path("body");

            // 累积content
            if (body.has("content")) {
                String content = body.path("content").asText();
                if (!content.isEmpty()) {
                    accumulator.append(content);
                }
            }

            // 构建返回给前端的JSON
            ObjectNode result = objectMapper.createObjectNode();
            if (body.has("reasonContent") && !body.path("reasonContent").isNull()) {
                String reasonContent = body.path("reasonContent").asText();
                if (!reasonContent.isEmpty()) {
                    result.put("type", "reason");
                    result.put("message", reasonContent);
                    return Flux.just(objectMapper.writeValueAsString(result));
                }
            }

            if (body.has("content") && !body.path("content").isNull()) {
                String content = body.path("content").asText();
                if (!content.isEmpty()) {
                    result.put("type", "answer");
                    result.put("message", content);
                    return Flux.just(objectMapper.writeValueAsString(result));
                }
            }

            return Flux.empty();
        } catch (JsonProcessingException e) {
            return Flux.error(new RuntimeException("JSON processing error", e));
        }
    }

    private void sendSseChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException e) {
            log.error("Failed to send SSE chunk", e);
            throw new RuntimeException(e);
        }
    }

    private void handleStreamError(SseEmitter emitter, Throwable error) {
        log.error("Stream processing error", error);
        emitter.completeWithError(error);
    }

    private void completeStream(SseEmitter emitter, ChatExecuteReq req, StringBuilder accumulator) {
        chatQueryService.saveFinalResult(req, accumulator.toString());
        emitter.complete();
        log.info("Stream completed successfully");
    }

    private void savePartialResult(ChatExecuteReq req, String partialContent) {
        if (!partialContent.isEmpty()) {
            log.warn("Saving partial result due to timeout/error");
            chatQueryService.saveFinalResult(req, partialContent);
        }
    }

    private List<QueryResp> getHistoryQueries(int chatId) {
        List<QueryResp> contextualParseInfoList = chatManageService.getChatQueries(chatId).stream()
                .filter(q -> Objects.nonNull(q.getQueryResult())
                        && q.getQueryResult().getQueryState() == QueryState.SUCCESS)
                .collect(Collectors.toList());
        Collections.reverse(contextualParseInfoList);
        return contextualParseInfoList;
    }
}