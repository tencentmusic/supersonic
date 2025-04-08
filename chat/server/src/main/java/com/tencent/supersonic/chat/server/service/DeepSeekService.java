package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DeepSeekService {

    SseEmitter streamChat(ChatExecuteReq chatExecuteReq);
}
