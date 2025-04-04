package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.response.DeepSeekResponse;
import com.tencent.supersonic.headless.chat.mapper.SchemaMapper;
import reactor.core.publisher.Flux;

public interface DeepSeekService {
    Flux<DeepSeekResponse> streamChat(String question, String sessionId);
}
