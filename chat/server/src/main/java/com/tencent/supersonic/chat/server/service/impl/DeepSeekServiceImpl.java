package com.tencent.supersonic.chat.server.service.impl;

import com.tencent.supersonic.chat.api.pojo.request.DeepSeekParams;
import com.tencent.supersonic.chat.api.pojo.request.DeepSeekRequest;
import com.tencent.supersonic.chat.api.pojo.request.Message;
import com.tencent.supersonic.chat.api.pojo.response.DeepSeekResponse;
import com.tencent.supersonic.chat.server.config.CrabConfig;
import com.tencent.supersonic.chat.server.service.DeepSeekService;
import com.tencent.supersonic.common.util.MiguApiUrlUtils;
import com.tencent.supersonic.headless.chat.mapper.SchemaMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@Slf4j
public class DeepSeekServiceImpl implements DeepSeekService {

    private final WebClient webClient = WebClient.create();
    @Autowired
    private CrabConfig crabConfig;

    @Override
    public Flux<DeepSeekResponse> streamChat(String question, String sessionId) {
        // 生成请求ID
        String requestId = UUID.randomUUID().toString();

        // 如果没有sessionId，则生成一个新的
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        // 构建请求消息
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", question));

        DeepSeekRequest request = new DeepSeekRequest();
        request.setRequestId(requestId);
        request.setSessionId(sessionId);
        request.setServiceName(crabConfig.getDsModel());
        request.setServiceType(crabConfig.getDsType());

        DeepSeekParams params = new DeepSeekParams();
        params.setMessages(messages);
        params.setModel(crabConfig.getDsModel());
        params.setStream(true);
        // params.setMaxTokens(4096);

        request.setParams(params);
        Map<String, Object> map = new HashMap<>();
        String urlpath = MiguApiUrlUtils.doSignature(crabConfig.getDeepseekUrl(), "post", map,
                crabConfig.getAppId(), crabConfig.getSecretKey());
        // 调用DeepSeek流式API
        return webClient.post().uri(crabConfig.getHost() + urlpath)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToFlux(DeepSeekResponse.class)
                .takeUntil(response -> "1".equals(response.getEndFlag()));
    }
}
